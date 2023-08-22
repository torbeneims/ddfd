package fdiscovery.approach.runner;

import fdiscovery.approach.*;
import fdiscovery.columns.*;
import fdiscovery.general.FunctionalDependencies;
import fdiscovery.partitions.*;
import fdiscovery.pruning.*;
import fdiscovery.pruning.Seed;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.THashSet;

import javax.ws.rs.core.Link;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GraphTraverser implements Callable<GraphTraverser>, Serializable {

    /* Always shared data */
    private transient final FileBasedPartitions fileBasedPartitions;
    private final ColumnOrder columnOrder;
    private final Collection<ColumnCollection> keys;

    /* Optionally shared data */
    private MemoryManagedJoinedPartitions partitions;
    private FunctionalDependencies minimalDependencies;
    private FunctionalDependencies maximalNonDependencies;

    // ========== RHS LOCAL ==========
    private int rhsIndex = -1;
    private Observations observations;
    private Dependencies dependencies;
    private NonDependencies nonDependencies;

    // ========== SPACE-PARTITIONING LOCAL ==========
    private ColumnCollection base;
    private Relation relation;
    private AtomicReference<ConcurrentLinkedDeque<Seed>> seeds;
    private volatile boolean isFinished = false;

    /* Other data */
    @Deprecated
    private static final AtomicLong totalTime = new AtomicLong(0);
    protected static final ConcurrentLinkedDeque<Long> jobTimes = new ConcurrentLinkedDeque<>();

    private static final AtomicInteger jobCount = new AtomicInteger(0);

    /* Settings */
    /** Whether to share partitions over RHSs */
    public static boolean SHARE_PARTITIONS = false;
    /** Whether to use concurrent partitions even if they are not shared */
    public static boolean FORCE_CONCURRENT_PARTITIONS = false;
    public static boolean FORCE_CONCURRENT_FUNCTIONAL_DEPENDENCIES = false;
    /** Whether to share minimalDependencies and maximalNonDependencies over RHSs <br/>
     *  Careful: Make sure all traversers have finished before accessing minimalDependencies or maximalNonDependencies */
    public static boolean SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES = false;
    /** Whether to share observations between space-partitioning runs */
    public static boolean SHARE_OBSERVATIONS = false;
    /** Whether to share dependencies and nonDependencies (for pruning) between space-partitioning runs */
    public static boolean SHARE_FUNCTIONAL_DEPENDENCIES = false;
    /** Whether to share the relation between different bases (disable when dynamically changing space partitioning)  */
    public static boolean SHARE_RELATION = false;

    public static boolean VERBOSE = false;


    public GraphTraverser(FileBasedPartitions fileBasedPartitions, Collection<ColumnCollection> keys, Relation relation) {
        this.fileBasedPartitions = fileBasedPartitions;
        this.columnOrder = new ColumnOrder(fileBasedPartitions);
        this.keys = keys;

        this.partitions = createJoinedPartitions();
        this.minimalDependencies = new FunctionalDependencies();
        keys.forEach(uniquePartition -> minimalDependencies.put(uniquePartition, uniquePartition.complementCopy(relation)));
        this.maximalNonDependencies = new FunctionalDependencies();

        this.relation = relation;
    }

    private GraphTraverser(FileBasedPartitions fileBasedPartitions, ColumnOrder columnOrder, Collection<ColumnCollection> keys) {
        this.fileBasedPartitions = fileBasedPartitions;
        this.columnOrder = columnOrder;
        this.keys = keys;
    }


    static boolean USE_CONCURRENT_PARTITIONS() {
        return SHARE_PARTITIONS || FORCE_CONCURRENT_PARTITIONS || DDFDMiner.PARTITION_FACTOR > 0 || DDFDMiner.TRAVERSERS_PER_RHS > 1;
    }
    public static boolean USE_CONCURRENT_FUNCTIONAL_DEPENDENCIES() {
        return FORCE_CONCURRENT_FUNCTIONAL_DEPENDENCIES ||
                SHARE_FUNCTIONAL_DEPENDENCIES ||
                DDFDMiner.TRAVERSERS_PER_RHS > 1;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Deprecated
    public GraphTraverser setRhsIndex(int rhsIndex) {
        this.rhsIndex = rhsIndex;

        dependencies.clear();
        nonDependencies.clear();
        observations.clear();

        addObservationsFromKeys(rhsIndex);

        return this;
    }

    /**
     * Create a new Traverser with all data shared
     */
    public GraphTraverser copy() {
        GraphTraverser copy = new GraphTraverser(fileBasedPartitions, columnOrder, keys);

        copy.partitions = partitions;
        copy.minimalDependencies = minimalDependencies;
        copy.maximalNonDependencies = maximalNonDependencies;

        copy.rhsIndex = rhsIndex;
        copy.observations = observations;
        copy.dependencies = dependencies;
        copy.nonDependencies = nonDependencies;

        copy.relation = relation;
        copy.base = base;

        copy.seeds = seeds;

        return copy;
    }

    public GraphTraverser setRHS(int newRhsIndex) {
        /* Optionally shared data */
        if(!SHARE_PARTITIONS) {
            partitions = createJoinedPartitions();
        }

        /* Necessary resets */
        this.rhsIndex = newRhsIndex;
        this.dependencies = new Dependencies(relation);
        this.nonDependencies = new NonDependencies(relation);
        this.observations = new Observations();
        this.addObservationsFromKeys(newRhsIndex);

        return this;
    }

    public GraphTraverser setBase(ColumnCollection base) {
        /* Optionally shared data */
        if(!SHARE_OBSERVATIONS) {
            this.observations = new Observations();
            this.addObservationsFromKeys(rhsIndex);
        }
        if(!SHARE_FUNCTIONAL_DEPENDENCIES) {
            this.dependencies = new Dependencies(relation);
            this.nonDependencies = new NonDependencies(relation);
        }
        if(!SHARE_INTEREST_FUNCTIONAL_DEPENDENCIES) {
            /* Must not be shared over bases because of hole finding */
            this.minimalDependencies = new FunctionalDependencies();
            keys.forEach(uniquePartition ->
                    this.minimalDependencies.put(uniquePartition, uniquePartition.complementCopy(relation)));
            this.maximalNonDependencies = new FunctionalDependencies();
        }

        /* Necessary resets */
        this.base = base;
        this.seeds = new AtomicReference<>(new ConcurrentLinkedDeque<>());
        generateInitialSeeds(base);

        /* Copy relation if it can dynamically change */
        if(!SHARE_RELATION) {
            this.relation = relation.copy();
        }
        return this;
    }

    private MemoryManagedJoinedPartitions createJoinedPartitions() {
        return USE_CONCURRENT_PARTITIONS() ?
                new ConcurrentMemoryManagedJoinedPartitions(fileBasedPartitions) :
                new MemoryManagedJoinedPartitions(fileBasedPartitions);
    }

    /**
     * Traverses the graph and returns a copy of the GraphTraverser object with the updated data.
     *
     * @return this
     */
    @Override
    public GraphTraverser call() {
        assert relation.get(rhsIndex) || !base.get(rhsIndex) : "RHS must be in relation";

        System.out.printf("Traversing rhs %d on thread %d (%d/%d job(s) running)\n", rhsIndex, Thread.currentThread().getId(), jobCount.incrementAndGet(), DDFDMiner.THREADS);

        Deque<Seed> trace = new LinkedList<>();

        long startTime = System.currentTimeMillis();

        // for locking check
        Stack<Seed> prevSeeds = null;
        while(seeds.get() != null) {
            for (Seed currentSeed = randomTake(); currentSeed != null; currentSeed = randomTake()) {
                for (;currentSeed != null; currentSeed = randomWalkStep(currentSeed, rhsIndex, trace)) {
                    assert base.isSubsetOf(currentSeed.getIndices()) : "Seed must be a superset of the base";
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
                }
            }
            prevSeeds = this.getNextSeeds(rhsIndex, base, prevSeeds);
        }

        final long timeDiff = System.currentTimeMillis() - startTime;
        totalTime.getAndAdd(timeDiff);
        jobTimes.add(timeDiff);
        if(VERBOSE)
            System.out.printf("Finding %d deps (including RHS -> ?) on RHS %d (Thread %d) took %dms, total %dms\n",
                minimalDependencies.getCount(), rhsIndex, Thread.currentThread().getId(), timeDiff, totalTime.get());

        System.out.printf("Terminating. Running jobs: %d\n", jobCount.decrementAndGet());
        return this;
    }

    private boolean hasNextSeed() {
        ConcurrentLinkedDeque<Seed> localSeeds = seeds.get();
        return localSeeds != null && !localSeeds.isEmpty();
    }

    private void generateInitialSeeds(ColumnCollection base) {
        if(!base.isEmpty()) {
            seeds.get().push(new Seed(base));
            return;
        }
        for (int partitionIndex : columnOrder.getOrderHighDistinctCount(relation.clearCopy(rhsIndex), relation)) {
            assert partitionIndex != rhsIndex : "RHS should not be part of the initial seeds (redundant if)";
            seeds.get().push(new Seed(base.setCopy(partitionIndex)));
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

    private Stack<Seed> getNextSeeds(int rhsIndex, ColumnCollection base, Stack<Seed> prevSeeds) {
        ConcurrentLinkedDeque<Seed> oldSeeds = seeds.get();
        // Early return, a concurrent traverser found that no more seeds exist
        if(oldSeeds == null) {
            return prevSeeds;
        }

        ConcurrentLinkedDeque<Seed> newSeeds = new ConcurrentLinkedDeque<>();
        assert base != null : "Base must not be null";
//		System.out.println("Find holes");
        Collection<ColumnCollection> deps = new LinkedList<>();
        ArrayList<ColumnCollection> currentMaximalNonDependencies = maximalNonDependencies.getLHSForRHS(rhsIndex);
        HashSet<ColumnCollection> currentMinimalDependencies = new HashSet<>(minimalDependencies.getLHSForRHS(rhsIndex));
        Collection<ColumnCollection> newDeps = new HashSet<>(0 * deps.size());

        // Must not return an empty seed and nothing can be calculated
        if(currentMaximalNonDependencies.isEmpty()) {
            seeds.compareAndSet(oldSeeds, null);
            return prevSeeds;
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
        deps.removeAll(currentMinimalDependencies);
        for (ColumnCollection remainingSeed : deps) {
            newSeeds.push(new Seed(remainingSeed));
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

        assert newSeeds.isEmpty() || !notPrunedSeeds.isEmpty() : "Only found pruned seeds";

        if(VERBOSE) {
            System.out.printf("Got next seeds[0] (of size %d): %s\ton %s, thread %d\n",
                    newSeeds.size(),
                    newSeeds.isEmpty() ? "[]" : newSeeds.peek() + ": " + observations.get(newSeeds.peek().getIndices()) + "(" + newSeeds.size() + ")",
                    base,
                    Thread.currentThread().getId());
            if(!newSeeds.isEmpty() && observations.get(newSeeds.peek().getIndices()) == Observation.DEPENDENCY)
                System.out.printf("directSubsets: %s\n", newSeeds.peek().getIndices().directSubsets(relation).stream().map(cc -> cc + ": " + observations.get(cc)).collect(Collectors.toList()));
        }

        if(newSeeds.isEmpty()) {
            newSeeds = null;
        }
        seeds.compareAndSet(oldSeeds, newSeeds);


        if(newSeeds == null) return prevSeeds;
        assert prevSeeds == null ||
                !new HashSet<>(newSeeds).containsAll(prevSeeds) :
                "Stuck at generating seeds: " + newSeeds.size() + " vs " + prevSeeds.size();
        prevSeeds = new Stack<>();
        prevSeeds.addAll(newSeeds);
        return prevSeeds;
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
        ConcurrentLinkedDeque<Seed> localSeeds = this.seeds.get();
        if(localSeeds == null)
            return null;
        return localSeeds.poll();
    }

    public FunctionalDependencies getDependencies() {
        return minimalDependencies;
    }
}
