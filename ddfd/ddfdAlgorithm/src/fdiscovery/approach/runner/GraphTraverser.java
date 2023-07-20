package fdiscovery.approach.runner;

import fdiscovery.approach.*;
import fdiscovery.columns.*;
import fdiscovery.general.FunctionalDependencies;
import fdiscovery.partitions.*;
import fdiscovery.pruning.*;
import fdiscovery.pruning.Seed;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.THashSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class GraphTraverser {
    private final Relation relation;

    private final MemoryManagedJoinedPartitions partitions;

    private final ColumnOrder columnOrder;

    private final FunctionalDependencies minimalDependencies;
    private final FunctionalDependencies maximalNonDependencies;
    private final Observations observations;
    private final Dependencies dependencies;
    private final NonDependencies nonDependencies;

    Collection<ColumnCollection> keys;

    private int rhsIndex = -1;

    private Stack<Seed> seeds;


    private static final AtomicLong totalTime = new AtomicLong(0);

    public GraphTraverser(MemoryManagedJoinedPartitions partitions,
                          ColumnOrder columnOrder, Relation relation,
                          ArrayList<ColumnCollection> keys) {
        this.partitions = partitions;
        this.columnOrder = columnOrder;
        this.relation = relation;
        this.minimalDependencies = new FunctionalDependencies();
        keys.forEach(uniquePartition -> minimalDependencies.put(uniquePartition, uniquePartition.complementCopy(relation)));
        this.maximalNonDependencies = new FunctionalDependencies();

        this.observations = new Observations();

        dependencies = new Dependencies(relation);
        nonDependencies = new NonDependencies(relation);

        seeds = new Stack<>();
    }

    public GraphTraverser setRhsIndex(int rhsIndex) {
        this.rhsIndex = rhsIndex;

        dependencies.clear();
        nonDependencies.clear();
        observations.clear();

        addObservationsFromKeys(rhsIndex);

        return this;
    }

    public int traverseGraph(ColumnCollection base) {
        assert relation.get(rhsIndex) || !base.get(rhsIndex) : "RHS must be in relation";

        System.out.printf("Traversing RHS %d on thread %d\n", rhsIndex, Thread.currentThread().getId());


        generateInitialSeeds(base);

        Deque<Seed> trace = new LinkedList<>();

        long startTime = System.currentTimeMillis();

        do {
            while (!seeds.isEmpty()) {
                Seed currentSeed = randomTake();
                do {
                    ColumnCollection lhsIndices = currentSeed.getIndices();
                    Observation observationOfLHS = observations.get(lhsIndices);
                    if (observationOfLHS == null) {
                        observationOfLHS = checkDependencyAndStoreIt(currentSeed, rhsIndex);

                        // if we couldn't find any dependency that is a
                        // subset of the current valid LHS it is minimal
                        if (observationOfLHS == Observation.MINIMAL_DEPENDENCY) {
                            minimalDependencies.addRHSColumn(lhsIndices, rhsIndex);
                        }
                        // if we couldn't find any non-dependency that is
                        // superset of the current non-valid LHS it is
                        // maximal
                        else if (observationOfLHS == Observation.MAXIMAL_NON_DEPENDENCY) {
                            maximalNonDependencies.addRHSColumn(lhsIndices, rhsIndex);
                        }
                    } else {
                        if (observationOfLHS.isCandidate()) {
                            if (observationOfLHS.isDependency()) {
                                Observation updatedDependencyType = observations.updateDependencyType(lhsIndices, relation);
                                observations.put(lhsIndices, updatedDependencyType);
                                if (updatedDependencyType == Observation.MINIMAL_DEPENDENCY) {
                                    minimalDependencies.addRHSColumn(lhsIndices, rhsIndex);
                                }
                            } else {
                                Observation updatedNonDependencyType = observations.updateNonDependencyType(lhsIndices, rhsIndex, relation);
                                observations.put(lhsIndices, updatedNonDependencyType);
                                if (updatedNonDependencyType == Observation.MAXIMAL_NON_DEPENDENCY) {
                                    maximalNonDependencies.addRHSColumn(lhsIndices, rhsIndex);
                                }
                            }
                        }
                    }
                    currentSeed = randomWalkStep(currentSeed, rhsIndex, trace);

                } while (currentSeed != null);
            }
            seeds = this.nextSeeds(rhsIndex, base);
        } while (!seeds.isEmpty());

        final long timeDiff = System.currentTimeMillis() - startTime;
        totalTime.getAndAdd(timeDiff);
        System.out.printf("Finding %d deps (including RHS -> ?) on RHS %d (Thread %d) took %dms, total %dms\n",
                minimalDependencies.getCount(), rhsIndex, Thread.currentThread().getId(), timeDiff, totalTime.get());

        return minimalDependencies.getCount();
    }

    private void generateInitialSeeds(ColumnCollection base) {
        if(!base.isEmpty()) {
            seeds.push(new Seed(base));
            return;
        }
        for (int partitionIndex : columnOrder.getOrderHighDistinctCount(relation.clearCopy(rhsIndex), relation)) {
            assert partitionIndex != rhsIndex : "RHS should not be part of the initial seeds (redundant if)";
            seeds.push(new Seed(base.setCopy(partitionIndex)));
        }
    }

    private void addObservationsFromKeys(int rhsIndex) {
        // TODO Adding observation currently causes death-loop because minimal dependencies may be added that
        //  cannot be traversed
//        relation.clearCopy(rhsIndex).stream().forEach(lhsIndex -> {
//            ColumnCollection lhs = new ColumnCollection(lhsIndex);
//            if (keys.contains(lhs)) {
//                dependencies.add(lhs, relation);
//                observations.put(lhs, Observation.MINIMAL_DEPENDENCY);
//                minimalDependencies.addRHSColumn(lhs, rhsIndex);
//            }
//        });
    }

    private Observation checkDependencyAndStoreIt(Seed seed, int rhsIndex) {
        if (nonDependencies.isRepresented(seed.getIndices())) {
            Observation observationOfLHS = observations.updateNonDependencyType(seed.getIndices(), rhsIndex, relation);
            observations.put(seed.getIndices(), observationOfLHS);
            nonDependencies.add(seed.getIndices(), relation);
            return observationOfLHS;
        } else if (dependencies.isRepresented(seed.getIndices())) {
            Observation observationOfLHS = observations.updateDependencyType(seed.getIndices(), relation);
            observations.put(seed.getIndices(), observationOfLHS);
            dependencies.add(seed.getIndices(), relation); // TODO: this is redundant? Hä ne
            return observationOfLHS;
        }

        Partition RHSPartition = partitions.getAtomicPartition(rhsIndex);
        Partition LHSPartition;
        Partition LHSJoinedRHSPartition;

        if (seed.isAtomic()) {
            LHSPartition = partitions.get(seed.getIndices());
            LHSJoinedRHSPartition = new ComposedPartition(LHSPartition, RHSPartition);
        } else {

            if (seed.getAdditionalColumnIndex() != -1) {
                int additionalColumn = seed.getAdditionalColumnIndex();
                Partition previousLHSPartition = partitions.get(seed.getBaseIndices());
                if (previousLHSPartition == null) {
                    ArrayList<Partition> partitionsToJoin = partitions.getBestMatchingPartitions(seed.getBaseIndices());
                    previousLHSPartition = ComposedPartition.buildPartition(partitionsToJoin);
                }
                Partition additionalColumnPartition = partitions.getAtomicPartition(additionalColumn);
                LHSPartition = partitions.get(previousLHSPartition.getIndices().setCopy(additionalColumn));
                if (LHSPartition == null) {
                    LHSPartition = new ComposedPartition(previousLHSPartition, additionalColumnPartition);
                    partitions.addPartition(LHSPartition);
                }
                LHSJoinedRHSPartition = partitions.get(LHSPartition.getIndices().setCopy(rhsIndex));
                if (LHSJoinedRHSPartition == null) {
                    LHSJoinedRHSPartition = new ComposedPartition(LHSPartition, RHSPartition);
                    partitions.addPartition(LHSJoinedRHSPartition);
                }
            } else {
                LHSPartition = partitions.get(seed.getIndices());
                if (LHSPartition == null) {
                    ArrayList<Partition> partitionsToJoin = partitions.getBestMatchingPartitions(seed.getIndices());
                    LHSPartition = ComposedPartition.buildPartition(partitionsToJoin);
                    partitions.addPartition(LHSPartition);
                }
                LHSJoinedRHSPartition = partitions.get(LHSPartition.getIndices().setCopy(rhsIndex));
                if (LHSJoinedRHSPartition == null) {
                    LHSJoinedRHSPartition = new ComposedPartition(LHSPartition, RHSPartition);
                    partitions.addPartition(LHSJoinedRHSPartition);
                }
            }
//			partitions.addPartition(LHSPartition);
//			partitions.addPartition(LHSJoinedRHSPartition);
        }

        if (Partition.representsFD(LHSPartition, LHSJoinedRHSPartition)) {
            Observation observationOfLHS = observations.updateDependencyType(seed.getIndices(), relation);
            observations.put(seed.getIndices(), observationOfLHS);
            dependencies.add(seed.getIndices(), relation);
            return observationOfLHS;
        }
        Observation observationOfLHS = observations.updateNonDependencyType(seed.getIndices(), rhsIndex, relation);
        observations.put(seed.getIndices(), observationOfLHS);
        nonDependencies.add(seed.getIndices(), relation);
        return observationOfLHS;
    }

    private Stack<Seed> nextSeeds(int rhsIndex, ColumnCollection base) {
//		System.out.println("Find holes");
        Collection<ColumnCollection> deps = new LinkedList<>();
        ArrayList<ColumnCollection> currentMaximalNonDependencies = maximalNonDependencies.getLHSForRHS(rhsIndex);
        HashSet<ColumnCollection> currentMinimalDependencies = new HashSet<>(minimalDependencies.getLHSForRHS(rhsIndex));
        Collection<ColumnCollection> newDeps = new HashSet<>(0 * deps.size());

        // Must not return an empty seed and nothing can be calculated
        if(currentMaximalNonDependencies.isEmpty()) {
            return new Stack<>();
        }

        deps.add(base);

        for (ColumnCollection maximalNonDependency : currentMaximalNonDependencies) {
            ColumnCollection complement = maximalNonDependency.setCopy(rhsIndex).complement(relation);
            for (ColumnCollection dep : deps) {
                for (int bit : complement.getSetBits(relation)) {
                    newDeps.add(dep.setCopy(bit));
                }
            }
            // minimize newDeps
            ArrayList<ColumnCollection> minimizedNewDeps = minimizeSeeds(newDeps);
            deps.clear();
            deps.addAll(minimizedNewDeps);
            newDeps.clear();
        }

        // return only elements that aren't already covered by the minimal
        // dependencies
        Stack<Seed> remainingSeeds = new Stack<>();
        deps.removeAll(currentMinimalDependencies);
        for (ColumnCollection remainingSeed : deps) {
            remainingSeeds.push(new Seed(remainingSeed));
        }

        THashSet<ColumnCollection> prunedSubsets = deps.stream()
                .flatMap(dep -> dependencies.getPrunedSubsets(dep.directSupersets(relation)).stream())
                .collect(Collectors.toCollection(THashSet::new));

        THashSet<ColumnCollection> prunedSupersets = deps.stream()
                .flatMap(dep -> nonDependencies.getPrunedSupersets(dep.directSubsets(relation)).stream())
                .collect(Collectors.toCollection(THashSet::new));


        nonDependencies.getPrunedSupersets(deps);

        Collection<Seed> notPrunedSeeds = deps.stream()
                .filter(seed -> !prunedSubsets.contains(seed) && !prunedSupersets.contains(seed))
                .map(Seed::new)
                .collect(Collectors.toList());

        assert remainingSeeds.isEmpty() || !notPrunedSeeds.isEmpty() : "Only found pruned seeds";

        System.out.printf("Got next seeds: %s\n\tpruned deps: %s\n\tpruned nondeps: %s\n\tnot pruned: %s\n\ton %s\n",
                remainingSeeds,
                prunedSubsets,
                prunedSupersets,
                notPrunedSeeds,
                base);


        return remainingSeeds;
    }

    private ArrayList<ColumnCollection> minimizeSeeds(Collection<ColumnCollection> seeds) {
        long maxCardinality = 0;
        TLongObjectHashMap<ArrayList<ColumnCollection>> seedsBySize = new TLongObjectHashMap<>(relation.numberOfColumns);
        for (ColumnCollection seed : seeds) {
            long cardinalityOfSeed = seed.cardinality();
            maxCardinality = Math.max(maxCardinality, cardinalityOfSeed);
            seedsBySize.putIfAbsent(cardinalityOfSeed, new ArrayList<>(seeds.size() / relation.numberOfColumns));
            seedsBySize.get(cardinalityOfSeed).add(seed);
        }

        for (long lowerBound = 1; lowerBound < maxCardinality; lowerBound++) {
            ArrayList<ColumnCollection> lowerBoundSeeds = seedsBySize.get(lowerBound);
            if(lowerBoundSeeds == null)
                continue;
            for (long upperBound = maxCardinality; upperBound > lowerBound; upperBound--) {
                ArrayList<ColumnCollection> upperBoundSeeds = seedsBySize.get(upperBound);
                if(upperBoundSeeds == null)
                    continue;
                for (ColumnCollection lowerSeed : lowerBoundSeeds) {
                    upperBoundSeeds.removeIf(lowerSeed::isSubsetOf);
                }
            }
        }
        ArrayList<ColumnCollection> minimizedSeeds = new ArrayList<>();
        for (ArrayList<ColumnCollection> seedList : seedsBySize.valueCollection()) {
            minimizedSeeds.addAll(seedList);
        }
        return minimizedSeeds;
    }

    private Seed randomWalkStep(Seed currentSeed, int rhsIndex, Deque<Seed> trace) {
        Observation observationOfSeed = observations.get(currentSeed.getIndices());

        if (observationOfSeed == Observation.CANDIDATE_MINIMAL_DEPENDENCY) {
            THashSet<ColumnCollection> uncheckedSubsets = observations.getUncheckedMaximalSubsets(currentSeed.getIndices(), columnOrder, relation);

            THashSet<ColumnCollection> prunedNonDependencySubsets = nonDependencies.getPrunedSupersets(uncheckedSubsets);
            for (ColumnCollection prunedNonDependencySubset : prunedNonDependencySubsets) {
                observations.put(prunedNonDependencySubset, Observation.NON_DEPENDENCY);
            }
            uncheckedSubsets.removeAll(prunedNonDependencySubsets);

            if (uncheckedSubsets.isEmpty() && prunedNonDependencySubsets.isEmpty()) {
                observations.put(currentSeed.getIndices(), Observation.MINIMAL_DEPENDENCY);
                minimalDependencies.addRHSColumn(currentSeed.getIndices(), rhsIndex);
            } else if (!uncheckedSubsets.isEmpty()) {
                ColumnCollection notRepresentedUncheckedSubset = uncheckedSubsets.iterator().next();
                if (notRepresentedUncheckedSubset != null) {
                    trace.push(currentSeed);
                    return new Seed(notRepresentedUncheckedSubset);
                }
            }
        } else if (observationOfSeed == Observation.CANDIDATE_MAXIMAL_NON_DEPENDENCY) {
            THashSet<ColumnCollection> uncheckedSupersets = observations.getUncheckedMinimalSupersets(currentSeed.getIndices(), rhsIndex, columnOrder, relation);

            THashSet<ColumnCollection> prunedNonDependencySupersets = nonDependencies.getPrunedSupersets(uncheckedSupersets);
            for (ColumnCollection prunedNonDependencySuperset : prunedNonDependencySupersets) {
                observations.put(prunedNonDependencySuperset, Observation.NON_DEPENDENCY);
            }
            uncheckedSupersets.removeAll(prunedNonDependencySupersets);

            THashSet<ColumnCollection> prunedDependencySupersets = dependencies.getPrunedSubsets(uncheckedSupersets);
            for (ColumnCollection prunedDependencySuperset : prunedDependencySupersets) {
                observations.put(prunedDependencySuperset, Observation.DEPENDENCY);
            }
            uncheckedSupersets.removeAll(prunedDependencySupersets);


            if (uncheckedSupersets.isEmpty() && prunedNonDependencySupersets.isEmpty()) {
                observations.put(currentSeed.getIndices(), Observation.MAXIMAL_NON_DEPENDENCY);
                maximalNonDependencies.addRHSColumn(currentSeed.getIndices(), rhsIndex);
            } else if (!uncheckedSupersets.isEmpty()) {
                ColumnCollection notRepresentedUncheckedSuperset = uncheckedSupersets.iterator().next();
                if (notRepresentedUncheckedSuperset != null) {
                    trace.push(currentSeed);
                    int additionalColumn = notRepresentedUncheckedSuperset.removeCopy(currentSeed.getIndices()).nextSetBit(0);
                    return new Seed(notRepresentedUncheckedSuperset, additionalColumn);
                }
            }
        }
        if (!trace.isEmpty()) {
            return trace.pop();
        }
        return null;
    }

    private Seed randomTake() {
        assert !seeds.isEmpty() : "random take must not be called with empty seeds";
        return seeds.pop();
    }

    public FunctionalDependencies getDependencies() {
        return minimalDependencies;
    }
}
