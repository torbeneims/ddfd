package fdiscovery.approach.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import fdiscovery.columns.Relation;


import fdiscovery.columns.ColumnCollection;
import fdiscovery.general.FunctionalDependencies;
import fdiscovery.general.Miner;
import fdiscovery.partitions.FileBasedPartition;
import fdiscovery.partitions.FileBasedPartitions;
import fdiscovery.preprocessing.SVFileProcessor;

public class DDFDMiner extends Miner implements Runnable {

    private final int numberOfColumns;
    private final FunctionalDependencies minimalDependencies;
    private final FileBasedPartitions fileBasedPartitions;
    public static int THREADS = 4;
    public static int PARTITION_FACTOR = 0;
    public static String FILE = DDFDMiner.input;
    public static int HASH = 0;

    public static void main(String[] args) {
        parseCLIArgs(args);

        createColumDirectory();

        File source = new File(FILE);
        System.out.printf("Loading file %s\n", source.getAbsolutePath());
        try {
            long timeStart = System.currentTimeMillis();

            SVFileProcessor inputFileProcessor = new SVFileProcessor(source);
            inputFileProcessor.init();
            System.out.println("Delimiter:\t" + inputFileProcessor.getDelimiter());
            System.out.println("Columns:\t" + inputFileProcessor.getNumberOfColumns());
            System.out.println("Rows:\t" + inputFileProcessor.getNumberOfRows());
            inputFileProcessor.createColumnFiles();
            DDFDMiner dfdRunner = new DDFDMiner(inputFileProcessor);

            dfdRunner.run();

            System.out.println(String.format("Number of dependencies:\t%d", Integer.valueOf(dfdRunner.minimalDependencies.getCount())));
            long timeFindFDs = System.currentTimeMillis();
//            System.out.println(dfdRunner.getDependencies());
            int hash = dfdRunner.getDependencies().hashCode();
            if (hash != -1122082685) {
                throw new RuntimeException("Invalid result, hash is: " + hash);
            }
            System.out.println("Total time:\t" + (timeFindFDs - timeStart) / 1000.0 + "s");

        } catch (FileNotFoundException e) {
            System.out.println("The input file could not be found.");
        } catch (IOException e) {
            System.out.println("The input reader could not be reset.");
        }
    }

    public static void parseCLIArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i].toLowerCase()) {
                    case "--sharepartitions":
                    case "-p":
                        GraphTraverser.SHARE_PARTITIONS = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--forceconcurrentpartitions":
                    case "-c":
                        GraphTraverser.FORCE_CONCURRENT_PARTITIONS = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--sharemfds":
                    case "-m":
                        GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--shareobservations":
                    case "-o":
                        GraphTraverser.SHARE_OBSERVATIONS = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--sharefds":
                    case "-d":
                        GraphTraverser.SHARE_FUNCTIONAL_DEPENDENCIES = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--sharerelation":
                    case "-r":
                        GraphTraverser.SHARE_RELATION = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--verbose":
                    case "-v":
                        GraphTraverser.VERBOSE = Boolean.parseBoolean(args[++i]);
                        break;
                    case "--threads":
                    case "-t":
                        THREADS = Integer.parseInt(args[++i]);
                        break;
                    case "--partitionfactor":
                    case "-s":
                        PARTITION_FACTOR = Integer.parseInt(args[++i]);
                        break;
                    case "--input":
                    case "-i":
                        FILE = args[++i];
                        break;
                    case "--hash":
                    case "-h":
                        HASH = Integer.parseInt(args[++i]);
                        break;
                    default:
                        System.out.println("Ignored undefined argument: " + args[i]);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error encountered while parsing the command-line arguments. Please check your inputs.");
        }

        printSettings();
    }

    public static void printSettings() {
        System.out.println("SHARE_PARTITIONS: " + GraphTraverser.SHARE_PARTITIONS);
        System.out.println("FORCE_CONCURRENT_PARTITIONS: " + GraphTraverser.FORCE_CONCURRENT_PARTITIONS);
        System.out.println("SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES: " + GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES);
        System.out.println("SHARE_OBSERVATIONS: " + GraphTraverser.SHARE_OBSERVATIONS);
        System.out.println("SHARE_FUNCTIONAL_DEPENDENCIES: " + GraphTraverser.SHARE_FUNCTIONAL_DEPENDENCIES);
        System.out.println("SHARE_RELATION: " + GraphTraverser.SHARE_RELATION);
        System.out.println("THREADS: " + THREADS);
        System.out.println("PARTITION_FACTOR: " + PARTITION_FACTOR);
        System.out.println("input: " + FILE);
        System.out.println("hash: " + HASH + (HASH == 0 ? " (ignored)" : " (checked)"));
    }

    public DDFDMiner(SVFileProcessor table) throws OutOfMemoryError {
        this.numberOfColumns = table.getNumberOfColumns();
        this.minimalDependencies = new FunctionalDependencies();
        this.fileBasedPartitions = new FileBasedPartitions(table);
    }

    public void run() throws OutOfMemoryError {

        ArrayList<ColumnCollection> keys = findUniqueColumnCombinations();

        Relation relation = new Relation(numberOfColumns);

        for (int i = 0; i < PARTITION_FACTOR; i++) {
            relation.clear(numberOfColumns - 1 - i);
        }
        System.out.printf("Partition factor is %d, relation is %s\n", PARTITION_FACTOR, relation);

        // Create an ExecutorService with a thread pool size equal to the number of RHS
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
        List<Future<GraphTraverser>> futures = new ArrayList<>();

        AtomicInteger found = new AtomicInteger(0);

        // Submit tasks for each RHS traversal and store the Future object
        final int nonFixatedBits = numberOfColumns - PARTITION_FACTOR;
        GraphTraverser traverser = new GraphTraverser(fileBasedPartitions, keys, relation);
        for (int rhsIndex = 0; rhsIndex < numberOfColumns; rhsIndex++) {
            traverser = traverser.copy().setRHS(rhsIndex);
            for (long baseMask = 0; baseMask < 1L << numberOfColumns; baseMask += 1L << nonFixatedBits) {
                ColumnCollection base = ColumnCollection.fromBits(baseMask);
                final GraphTraverser localTraverser = traverser = traverser.copy().setBase(base);

                // Must not fixate RHS (i.e. RHS is set in LHS)
                if (base.get(rhsIndex))
                    continue;

                System.out.printf("Starting with RHS %d and fixed base %s\n", rhsIndex, base);

                Future<GraphTraverser> future = executorService.submit(() -> {
                    localTraverser.run();
                    found.addAndGet(localTraverser.getDependencies().getCount());
                    return localTraverser;
                });
                futures.add(future);
            }
        }

        // Retrieve the results from each Future
        for (Future<GraphTraverser> future : futures) {
            try {
                traverser = future.get();
                if(!GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES) {
                    traverser.getDependencies().
                            forEach((lhs, rhs) -> rhs.stream().
                                    forEach(rhsIndex -> minimalDependencies.insertMinimalDependency(lhs, rhsIndex)));
                }
                // This will block until the computation is done and return the result
            } catch (InterruptedException | ExecutionException e) {
                // Handle any exceptions that occurred during traversal
                e.printStackTrace();
            }
        }
        if(GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES) {
            traverser.getDependencies().
                    forEach((lhs, rhs) -> rhs.stream().
                            forEach(rhsIndex -> minimalDependencies.insertMinimalDependency(lhs, rhsIndex)));
        }

        // Shutdown the executor service after all tasks are complete
        executorService.shutdown();
        System.out.printf("Found %d deps :)\n", minimalDependencies.getCount());
        System.out.printf("Found is %d :)\n", found.get());

        if(HASH == 0)
            return;

        if (minimalDependencies.hashCode() != HASH) {
            System.out.printf("Deps: %s\n", minimalDependencies);
            throw new RuntimeException("Hashcode is not correct: " + minimalDependencies.hashCode() + " != " + HASH);
        }
        System.out.println("Hashcode is correct :)");

    }

    private ArrayList<ColumnCollection> findUniqueColumnCombinations() {
        ArrayList<ColumnCollection> keys = new ArrayList<>();

        // check each column for uniqueness
        // if a column is unique it's a key for all other columns
        // therefore uniquePartition -> schema - uniquePartition
        for (FileBasedPartition fileBasedPartition : this.fileBasedPartitions) {
            if (fileBasedPartition.isUnique()) {
                ColumnCollection uniquePartitionIndices = fileBasedPartition.getIndices();
                // add unique columns to minimal uniques
                keys.add(uniquePartitionIndices);
            }
        }
        return keys;
    }

    public FunctionalDependencies getDependencies() {
        return this.minimalDependencies;
    }
}