package fdiscovery.approach.runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import fdiscovery.columns.Relation;
import org.apache.commons.cli.CommandLine;


import fdiscovery.approach.ColumnOrder;
import fdiscovery.columns.ColumnCollection;
import fdiscovery.general.CLIParserMiner;
import fdiscovery.general.ColumnFiles;
import fdiscovery.general.FunctionalDependencies;
import fdiscovery.general.Miner;
import fdiscovery.partitions.ComposedPartition;
import fdiscovery.partitions.FileBasedPartition;
import fdiscovery.partitions.FileBasedPartitions;
import fdiscovery.partitions.MemoryManagedJoinedPartitions;
import fdiscovery.partitions.Partition;
import fdiscovery.preprocessing.SVFileProcessor;
import fdiscovery.pruning.Dependencies;
import fdiscovery.pruning.NonDependencies;
import fdiscovery.pruning.Observation;
import fdiscovery.pruning.Observations;
import fdiscovery.pruning.Seed;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.THashSet;

public class DFDMiner extends Miner implements Runnable {

    private final int numberOfColumns;
    private FunctionalDependencies minimalDependencies;
    private FileBasedPartitions fileBasedPartitions;

    public static void main(String[] args) {
        createColumDirectory();

        File source = new File(DFDMiner.input);
        SVFileProcessor inputFileProcessor = null;
        try {
            long timeStart = System.currentTimeMillis();

            inputFileProcessor = new SVFileProcessor(source);
            inputFileProcessor.init();
            System.out.println("Delimiter:\t" + inputFileProcessor.getDelimiter());
            System.out.println("Columns:\t" + inputFileProcessor.getNumberOfColumns());
            System.out.println("Rows:\t" + inputFileProcessor.getNumberOfRows());
            inputFileProcessor.createColumnFiles();
            DFDMiner dfdRunner = new DFDMiner(inputFileProcessor);

            dfdRunner.run();
            System.out.println(String.format("Number of dependencies:\t%d", Integer.valueOf(dfdRunner.minimalDependencies.getCount())));
            long timeFindFDs = System.currentTimeMillis();
            System.out.println(dfdRunner.getDependencies());
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

    public DFDMiner(SVFileProcessor table) throws OutOfMemoryError {
        this.numberOfColumns = table.getNumberOfColumns();
        this.minimalDependencies = new FunctionalDependencies();
        this.fileBasedPartitions = new FileBasedPartitions(table);
    }

    public void run() throws OutOfMemoryError {

        ArrayList<ColumnCollection> keys = findUniqueColumnCombinations();

        ColumnOrder columnOrder = new ColumnOrder(fileBasedPartitions);
        MemoryManagedJoinedPartitions joinedPartitions = new MemoryManagedJoinedPartitions(fileBasedPartitions);
        Relation relation = new Relation(numberOfColumns);


        final int partitionFactor = 0;
        for (int i = 0; i < partitionFactor; i++) {
            relation.clear(numberOfColumns - 1 - i);
        }
        System.out.printf("Partition factor is %d, relation is %s\n", partitionFactor, relation);

        // Create an ExecutorService with a thread pool size equal to the number of RHS
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<Future<GraphTraverser>> futures = new ArrayList<>();

        AtomicInteger found = new AtomicInteger(0);

        // Submit tasks for each RHS traversal and store the Future object
        final int nonFixatedBits = numberOfColumns - partitionFactor;
        for (int rhsIndex = 0; rhsIndex < numberOfColumns; rhsIndex++) {
            for (long baseMask = 0; baseMask < 1L << numberOfColumns; baseMask += 1L << nonFixatedBits) {
                // Capture the for lambda expression context


                GraphTraverser traverser = new GraphTraverser(joinedPartitions, columnOrder, relation, keys);
                traverser.setRhsIndex(rhsIndex);
                ColumnCollection base = ColumnCollection.fromBits(baseMask);

                // Must not fixate RHS (i.e. RHS is set in LHS)
                if (base.get(rhsIndex))
                    continue;

                System.out.printf("Starting with RHS %d and fixed base %s\n", rhsIndex, base);

                Future<GraphTraverser> future = executorService.submit(() -> {
                    found.addAndGet(traverser.traverseGraph(base));
                    return traverser;
                });
                futures.add(future);
            }
        }

        // Retrieve the results from each Future
        for (Future<GraphTraverser> future : futures) {
            try {
                GraphTraverser traverser = future.get();
                traverser.getDependencies().
                        forEach((lhs, rhs) -> rhs.stream().
                                forEach(rhsIndex -> minimalDependencies.insertMinimalDependency(lhs, rhsIndex)));
                // This will block until the computation is done and return the result
            } catch (InterruptedException | ExecutionException e) {
                // Handle any exceptions that occurred during traversal
                e.printStackTrace();
            }
        }

        // Shutdown the executor service after all tasks are complete
        executorService.shutdown();
        System.out.printf("Found %d deps :)\n", minimalDependencies.getCount());
        System.out.printf("Found is %d :)\n", found.get());

        if (minimalDependencies.hashCode() != -1122082685) {
            System.out.printf("Deps: %s\n", minimalDependencies);
            throw new RuntimeException("Hashcode is not correct");
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