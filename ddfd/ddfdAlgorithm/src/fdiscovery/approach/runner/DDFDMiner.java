package fdiscovery.approach.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    public static int TRAVERSERS_PER_RHS = 1;
    public static String FILE = DDFDMiner.input;
    public static int HASH = 0;
    public static long RHS_IGNORE_MAP = 0;

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
                    case "--forceconcurrentfunctionaldependencies":
                    case "-f":
                        GraphTraverser.FORCE_CONCURRENT_FUNCTIONAL_DEPENDENCIES = Boolean.parseBoolean(args[++i]);
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
                    case "--traversersperrhs":
                    case "-j":
                        TRAVERSERS_PER_RHS = Integer.parseInt(args[++i]);
                        break;
                    case "--rhsignoremap":
                        RHS_IGNORE_MAP = Long.parseLong(args[++i], 16);
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
        System.out.println("FORCE_CONCURRENT_FUNCTIONAL_DEPENDENCIES: " + GraphTraverser.FORCE_CONCURRENT_FUNCTIONAL_DEPENDENCIES);
        System.out.println("SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES: " + GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES);
        System.out.println("SHARE_OBSERVATIONS: " + GraphTraverser.SHARE_OBSERVATIONS);
        System.out.println("SHARE_FUNCTIONAL_DEPENDENCIES: " + GraphTraverser.SHARE_FUNCTIONAL_DEPENDENCIES);
        System.out.println("SHARE_RELATION: " + GraphTraverser.SHARE_RELATION);
        System.out.println("THREADS: " + THREADS);
        System.out.println("PARTITION_FACTOR: " + PARTITION_FACTOR);
        System.out.println("TRAVERSERS_PER_RHS: " + TRAVERSERS_PER_RHS);
        System.out.println("input: " + FILE);
        System.out.println("hash: " + HASH + (HASH == 0 ? " (ignored)" : " (checked)"));
        System.out.println();
        System.out.println("USE_CONCURRENT_PARTITIONS(): " + GraphTraverser.USE_CONCURRENT_PARTITIONS());
        System.out.println("USE_CONCURRENT_FUNCTIONAL_DEPENDENCIES(): " + GraphTraverser.USE_CONCURRENT_FUNCTIONAL_DEPENDENCIES());
        System.out.format("RHS_IGNORE_MAP: %h\n", RHS_IGNORE_MAP);
    }

    public DDFDMiner(SVFileProcessor table) throws OutOfMemoryError {
        this.numberOfColumns = table.getNumberOfColumns();
        this.minimalDependencies = new FunctionalDependencies();
        this.fileBasedPartitions = new FileBasedPartitions(table);
    }

    public void run() throws OutOfMemoryError {

        ArrayList<ColumnCollection> keys = findUniqueColumnCombinations();

        // Create an ExecutorService with a thread pool size equal to the number of RHS
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Future<GraphTraverser>> futures = new ArrayList<>();


        assert TRAVERSERS_PER_RHS > 0 : "Traversers needed";
        for (int i = 0; i < TRAVERSERS_PER_RHS; i++) {
            int finalI = i;
            getTraversers(keys).forEach(traverser -> {
                Future<GraphTraverser> future = executorService.submit(traverser);

                // Further traversers hold redundant information
                if(finalI == 0)
                    futures.add(future);
            });
        }

        retrieveResults(futures);

        // Shutdown the executor service after all tasks are complete
        executorService.shutdown();
        System.out.printf("Found %d deps :)\n", minimalDependencies.getCount());

        checkHash();
    }

    /**
     * Submit tasks for each RHS traversal and store the Future object
     */
    private Collection<GraphTraverser> getTraversers(ArrayList<ColumnCollection> keys) {
        Relation relation = setupRelation();
        Collection<GraphTraverser> traversers = new ArrayList<>();

        final int nonFixatedBits = numberOfColumns - PARTITION_FACTOR;
        GraphTraverser traverser = new GraphTraverser(fileBasedPartitions, keys, relation);
        for (int rhsIndex = 0; rhsIndex < numberOfColumns; rhsIndex++) {
            if(RHS_IGNORE_MAP != 0 && (RHS_IGNORE_MAP & (1 << rhsIndex)) != 0)
                continue;
            traverser = traverser.copy().setRHS(rhsIndex);
            for (long baseMask = 0; baseMask < 1L << numberOfColumns; baseMask += 1L << nonFixatedBits) {
                ColumnCollection base = ColumnCollection.fromBits(baseMask);

                traverser = traverser.copy().setBase(base);

                // Must not fixate RHS (i.e. RHS is set in every possible LHS)
                // TODO For some reason, traverser still finds 15 -> 15 with 15 notin relation
                if (base.get(rhsIndex))
                    continue;

                System.out.printf("Starting with RHS %d and fixed base %s\n", rhsIndex, base);
                traversers.add(traverser);
            }
        }

        return traversers;
    }

    public static class PrioritizedTask implements Runnable, Comparable<PrioritizedTask> {
        private final Runnable task;
        private final int priority;

        public PrioritizedTask(Runnable task, int priority) {
            this.task = task;
            this.priority = priority;
        }

        public <T> PrioritizedTask(Callable<T> task, int priority, Consumer<T> then) {
            this.task = () -> {
                try {
                    then.accept(task.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            this.priority = priority;
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public int compareTo(PrioritizedTask other) {
            return Integer.compare(other.priority, this.priority);
        }
    }

    private Relation setupRelation() {
        Relation relation = new Relation(numberOfColumns);

        for (int i = 0; i < PARTITION_FACTOR; i++) {
            relation.clear(numberOfColumns - 1 - i);
        }
        System.out.printf("Partition factor is %d, relation is %s\n", PARTITION_FACTOR, relation);
        return relation;
    }

    private void checkHash() {
        if(HASH == 0)
            return;

        if (minimalDependencies.hashCode() != HASH) {
            System.out.printf("Deps: %s\n", minimalDependencies);
            throw new RuntimeException("Hashcode is not correct: " + minimalDependencies.hashCode() + " != " + HASH);
        }
        System.out.println("Hashcode is correct :)");
    }

    private void retrieveResults(List<Future<GraphTraverser>> futures) {
        try {
            GraphTraverser traverser = null;
            for (Future<GraphTraverser> future : futures) {
                // This will block until the computation is done and return the result
                traverser = future.get();

                if (!GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES) {
                    traverser.getDependencies().
                            forEach((lhs, rhs) -> rhs.stream().
                                    forEach(rhsIndex -> minimalDependencies.insertMinimalDependency(lhs, rhsIndex)));
                }
            }
            if (GraphTraverser.SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES) {
                traverser.getDependencies().
                        forEach((lhs, rhs) -> rhs.stream().
                                forEach(rhsIndex -> minimalDependencies.insertMinimalDependency(lhs, rhsIndex)));
            }
        } catch(ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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