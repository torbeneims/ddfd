package fdiscovery.approach.runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

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

	private int numberOfColumns;
	private ColumnOrder columnOrder;
	private Stack<Seed> trace;
	private Stack<Seed> seeds;
	private Observations observations;
	private FunctionalDependencies minimalDependencies;
	private FunctionalDependencies maximalNonDependencies;
	private FileBasedPartitions fileBasedPartitions;
	private Dependencies dependencies;
	private NonDependencies nonDependencies;
	private MemoryManagedJoinedPartitions joinedPartitions;

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
			if(hash != -1122082685) {
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
		this.observations = new Observations();
		this.numberOfColumns = table.getNumberOfColumns();
		this.trace = new Stack<>();
		this.seeds = new Stack<>();
		this.minimalDependencies = new FunctionalDependencies();
		this.maximalNonDependencies = new FunctionalDependencies();
		this.dependencies = new Dependencies(this.numberOfColumns);
		this.nonDependencies = new NonDependencies(this.numberOfColumns);
		this.joinedPartitions = new MemoryManagedJoinedPartitions(this.numberOfColumns);
		this.fileBasedPartitions = new FileBasedPartitions(table);
		this.columnOrder = new ColumnOrder(fileBasedPartitions);
		for (int columnIndex = 0; columnIndex < this.numberOfColumns; columnIndex++) {
			ColumnCollection columnIdentifier = new ColumnCollection(this.numberOfColumns);
			columnIdentifier.set(columnIndex);
			this.joinedPartitions.addPartition(this.fileBasedPartitions.get(columnIndex));
		}
	}

	public void run() throws OutOfMemoryError {

		ArrayList<ColumnCollection> keys = new ArrayList<>();

		// check each column for uniqueness
		// if a column is unique it's a key for all other columns
		// therefore uniquePartition -> schema - uniquePartition
		for (FileBasedPartition fileBasedPartition : this.fileBasedPartitions) {
			if (fileBasedPartition.isUnique()) {
				ColumnCollection uniquePartitionIndices = fileBasedPartition.getIndices();
				ColumnCollection RHS = uniquePartitionIndices.complementCopy();
				this.minimalDependencies.put(uniquePartitionIndices, RHS);
				// add unique columns to minimal uniques
				keys.add(uniquePartitionIndices);
			}
		}

		// do this for all RHS
		for (int currentRHSIndex = 0; currentRHSIndex < this.numberOfColumns; currentRHSIndex++) {

			this.dependencies = new Dependencies(numberOfColumns);
			this.nonDependencies = new NonDependencies(numberOfColumns);
			this.trace.clear();
			this.observations.clear();

			for (int lhsIndex = 0; lhsIndex < this.numberOfColumns; lhsIndex++) {
				if (lhsIndex != currentRHSIndex) {
					ColumnCollection lhs = new ColumnCollection(numberOfColumns);
					lhs.set(lhsIndex);
					if (keys.contains(lhs)) {
						this.dependencies.add(lhs);
						this.observations.put(lhs, Observation.MINIMAL_DEPENDENCY);
					}
				}
			}

			ColumnCollection currentRHS = new ColumnCollection(numberOfColumns);
			currentRHS.set(currentRHSIndex);

			// generate seeds
			for (int partitionIndex : columnOrder.getOrderHighDistinctCount(currentRHS.complementCopy())) {
				if (partitionIndex != currentRHSIndex) {
					FileBasedPartition lhsPartition = this.fileBasedPartitions.get(partitionIndex);
					this.seeds.push(new Seed(lhsPartition.getIndices()));
				}
			}

			do {
				while (!seeds.isEmpty()) {
					Seed currentSeed = this.randomTake();
					do {
						ColumnCollection lhsIndices = currentSeed.getIndices();
						Observation observationOfLHS = this.observations.get(currentSeed.getIndices());
						if (observationOfLHS == null) {
							observationOfLHS = this.checkDependencyAndStoreIt(currentSeed, currentRHSIndex);

							// if we couldn't find any dependency that is a
							// subset of the current valid LHS it is minimal
							if (observationOfLHS == Observation.MINIMAL_DEPENDENCY) {
								this.minimalDependencies.addRHSColumn(lhsIndices, currentRHSIndex);
							}
							// if we couldn't find any non-dependency that is
							// superset of the current non-valid LHS it is
							// maximal
							else if (observationOfLHS == Observation.MAXIMAL_NON_DEPENDENCY) {
								this.maximalNonDependencies.addRHSColumn(lhsIndices, currentRHSIndex);
							}
							currentSeed = randomWalkStep(currentSeed, currentRHSIndex);
						} else {
//							System.out.println(String.format("[2]Current [%s]%s\t[%s]", (char) (currentRHSIndex + 65), currentSeed, observationOfLHS));
							if (observationOfLHS.isCandidate()) {
								if (observationOfLHS.isDependency()) {
									Observation updatedDependencyType = this.observations.updateDependencyType(currentSeed.getIndices());
									// System.out.println(String.format("\tupdated:\t%s",
									// updatedDependencyType));
									this.observations.put(lhsIndices, updatedDependencyType);
									if (updatedDependencyType == Observation.MINIMAL_DEPENDENCY) {
										// System.out.println("Add min dependency:\t"
										// + currentSeed);
										this.minimalDependencies.addRHSColumn(lhsIndices, currentRHSIndex);
									}
								} else {
									Observation updatedNonDependencyType = this.observations.updateNonDependencyType(currentSeed.getIndices(), currentRHSIndex);
									this.observations.put(lhsIndices, updatedNonDependencyType);
									// System.out.println(String.format("\tupdated:\t%s",
									// updatedNonDependencyType));
									if (updatedNonDependencyType == Observation.MAXIMAL_NON_DEPENDENCY) {
										this.maximalNonDependencies.addRHSColumn(lhsIndices, currentRHSIndex);
									}
								}
							}
							currentSeed = randomWalkStep(currentSeed, currentRHSIndex);
						}

					} while (currentSeed != null);
				}
				seeds = this.nextSeeds(currentRHSIndex);
			} while (!seeds.isEmpty());
		}
		// System.out.println(String.format("Number partitions:\t%d",
		// this.joinedPartitions.getCount()));
	}

	private Observation checkDependencyAndStoreIt(Seed seed, int currentRHSIndex) {
		if (nonDependencies.isRepresented(seed.getIndices())) {
			// System.out.println("Skip because of nonDependency");
			Observation observationOfLHS = this.observations.updateNonDependencyType(seed.getIndices(), currentRHSIndex);
			this.observations.put(seed.getIndices(), observationOfLHS);
			this.nonDependencies.add(seed.getIndices());
			return observationOfLHS;
		} else if (dependencies.isRepresented(seed.getIndices())) {
			// System.out.println("Skip because of dependency");
			Observation observationOfLHS = this.observations.updateDependencyType(seed.getIndices());
			this.observations.put(seed.getIndices(), observationOfLHS);
			this.dependencies.add(seed.getIndices());
			return observationOfLHS;
		}

		FileBasedPartition currentRHSPartition = this.fileBasedPartitions.get(currentRHSIndex);
		Partition currentLHSPartition = null;
		Partition currentLHSJoinedRHSPartition = null;

		if (seed.isAtomic()) {
			currentLHSPartition = this.joinedPartitions.get(seed.getIndices());
			currentLHSJoinedRHSPartition = new ComposedPartition(currentLHSPartition, currentRHSPartition);
		} else {

			// if we went upwards in the lattice we can build the currentLHS
			// partition directly from the previous partition
			if (seed.getAdditionalColumnIndex() != -1) {
				int additionalColumn = seed.getAdditionalColumnIndex();
				Partition previousLHSPartition = joinedPartitions.get(seed.getBaseIndices());
				if (previousLHSPartition == null) {
					ArrayList<Partition> partitionsToJoin = joinedPartitions.getBestMatchingPartitions(seed.getBaseIndices());
					previousLHSPartition = ComposedPartition.buildPartition(partitionsToJoin);
				}
				FileBasedPartition additionalColumnPartition = this.fileBasedPartitions.get(additionalColumn);
				currentLHSPartition = this.joinedPartitions.get(previousLHSPartition.getIndices().setCopy(additionalColumn));
				if (currentLHSPartition == null) {
					currentLHSPartition = new ComposedPartition(previousLHSPartition, additionalColumnPartition);
					this.joinedPartitions.addPartition(currentLHSPartition);
				}
				currentLHSJoinedRHSPartition = this.joinedPartitions.get(currentLHSPartition.getIndices().setCopy(currentRHSIndex));
				if (currentLHSJoinedRHSPartition == null) {
					currentLHSJoinedRHSPartition = new ComposedPartition(currentLHSPartition, currentRHSPartition);
					this.joinedPartitions.addPartition(currentLHSJoinedRHSPartition);
				}
			} else {
				currentLHSPartition = this.joinedPartitions.get(seed.getIndices());
				if (currentLHSPartition == null) {
					ArrayList<Partition> partitionsToJoin = joinedPartitions.getBestMatchingPartitions(seed.getIndices());
					currentLHSPartition = ComposedPartition.buildPartition(partitionsToJoin);
					this.joinedPartitions.addPartition(currentLHSPartition);
				}
				currentLHSJoinedRHSPartition = this.joinedPartitions.get(currentLHSPartition.getIndices().setCopy(currentRHSIndex));
				if (currentLHSJoinedRHSPartition == null) {
					currentLHSJoinedRHSPartition = new ComposedPartition(currentLHSPartition, currentRHSPartition);
					this.joinedPartitions.addPartition(currentLHSJoinedRHSPartition);
				}
			}
//			this.joinedPartitions.addPartition(currentLHSPartition);
//			this.joinedPartitions.addPartition(currentLHSJoinedRHSPartition);
		}

		if (Partition.representsFD(currentLHSPartition, currentLHSJoinedRHSPartition)) {
			Observation observationOfLHS = this.observations.updateDependencyType(seed.getIndices());
			this.observations.put(seed.getIndices(), observationOfLHS);
			this.dependencies.add(seed.getIndices());
			return observationOfLHS;
		}
		Observation observationOfLHS = this.observations.updateNonDependencyType(seed.getIndices(), currentRHSIndex);
		this.observations.put(seed.getIndices(), observationOfLHS);
		this.nonDependencies.add(seed.getIndices());
		return observationOfLHS;
	}

	private Stack<Seed> nextSeeds(int currentRHSIndex) {
//		System.out.println("Find holes");
		THashSet<ColumnCollection> deps = new THashSet<>();
		ArrayList<ColumnCollection> currentMaximalNonDependencies = maximalNonDependencies.getLHSForRHS(currentRHSIndex);
		HashSet<ColumnCollection> currentMinimalDependencies = new HashSet<>(minimalDependencies.getLHSForRHS(currentRHSIndex));
		ArrayList<ColumnCollection> newDeps = new ArrayList<>(numberOfColumns * deps.size());

		for (ColumnCollection maximalNonDependency : currentMaximalNonDependencies) {
			ColumnCollection complement = maximalNonDependency.setCopy(currentRHSIndex).complement();
			if (deps.isEmpty()) {
				ColumnCollection emptyColumnIndices = new ColumnCollection(numberOfColumns);
				for (int complementColumnIndex : complement.getSetBits()) {
					deps.add(emptyColumnIndices.setCopy(complementColumnIndex));
				}
			} else {
				for (ColumnCollection dep : deps) {
					int[] setBits = complement.getSetBits();
					for (int setBit = 0; setBit < setBits.length; setBit++) {
						newDeps.add(dep.setCopy(setBits[setBit]));
					}
				}
				// minimize newDeps
				ArrayList<ColumnCollection> minimizedNewDeps = minimizeSeeds(newDeps);
				deps.clear();
				deps.addAll(minimizedNewDeps);
				newDeps.clear();
			}
		}
		
		// return only elements that aren't already covered by the minimal
		// dependencies
		Stack<Seed> remainingSeeds = new Stack<>();
		deps.removeAll(currentMinimalDependencies);
		for (ColumnCollection remainingSeed : deps) {
			remainingSeeds.push(new Seed(remainingSeed));
		}

		return remainingSeeds;
	}

	private ArrayList<ColumnCollection> minimizeSeeds(ArrayList<ColumnCollection> seeds) {
		long maxCardinality = 0;
		TLongObjectHashMap<ArrayList<ColumnCollection>> seedsBySize = new TLongObjectHashMap<>(numberOfColumns);
		for (ColumnCollection seed : seeds) {
			long cardinalityOfSeed = seed.cardinality();
			maxCardinality = Math.max(maxCardinality, cardinalityOfSeed);
			seedsBySize.putIfAbsent(cardinalityOfSeed, new ArrayList<ColumnCollection>(seeds.size()/numberOfColumns));
			seedsBySize.get(cardinalityOfSeed).add(seed);
		}

		for (long lowerBound = 1; lowerBound < maxCardinality; lowerBound++) {
			ArrayList<ColumnCollection> lowerBoundSeeds = seedsBySize.get(lowerBound);
			if (lowerBoundSeeds != null) {
				for (long upperBound = maxCardinality; upperBound > lowerBound; upperBound--) {
					ArrayList<ColumnCollection> upperBoundSeeds = seedsBySize.get(upperBound);
					if (upperBoundSeeds != null) {
						for (Iterator<ColumnCollection> lowerIt = lowerBoundSeeds.iterator(); lowerIt.hasNext();) {
							ColumnCollection lowerSeed = lowerIt.next();
							for (Iterator<ColumnCollection> upperIt = upperBoundSeeds.iterator(); upperIt.hasNext();) {
								if (lowerSeed.isSubsetOf(upperIt.next())) {
									upperIt.remove();
								}
							}
						}
					}
				}
			}
		}
		ArrayList<ColumnCollection> minimizedSeeds = new ArrayList<>();
		for (ArrayList<ColumnCollection> seedList : seedsBySize.valueCollection()) {
			for (ColumnCollection seed : seedList) {
				minimizedSeeds.add(seed);
			}
		}
		return minimizedSeeds;
	}

	private Seed randomTake() {
		if (!this.seeds.isEmpty()) {
			return this.seeds.pop();
		}
		return null;
	}

	private Seed randomWalkStep(Seed currentSeed, int currentRHSIndex) {
		Observation observationOfSeed = this.observations.get(currentSeed.getIndices());

		if (observationOfSeed == Observation.CANDIDATE_MINIMAL_DEPENDENCY) {
			THashSet<ColumnCollection> uncheckedSubsets = this.observations.getUncheckedMaximalSubsets(currentSeed.getIndices(), columnOrder);
			THashSet<ColumnCollection> prunedNonDependencySubsets = nonDependencies.getPrunedSupersets(uncheckedSubsets);
			for (ColumnCollection prunedNonDependencySubset : prunedNonDependencySubsets) {
				observations.put(prunedNonDependencySubset, Observation.NON_DEPENDENCY);
			}
			uncheckedSubsets.removeAll(prunedNonDependencySubsets);
			if (uncheckedSubsets.isEmpty() && prunedNonDependencySubsets.isEmpty()) {
				observations.put(currentSeed.getIndices(), Observation.MINIMAL_DEPENDENCY);
				minimalDependencies.addRHSColumn(currentSeed.getIndices(), currentRHSIndex);
			} else if (!uncheckedSubsets.isEmpty()) {
				ColumnCollection notRepresentedUncheckedSubset = uncheckedSubsets.iterator().next();
				if (notRepresentedUncheckedSubset != null) {
					trace.push(currentSeed);
					return new Seed(notRepresentedUncheckedSubset);
				}
			}
		} else if (observationOfSeed == Observation.CANDIDATE_MAXIMAL_NON_DEPENDENCY) {
			THashSet<ColumnCollection> uncheckedSupersets = this.observations.getUncheckedMinimalSupersets(currentSeed.getIndices(), currentRHSIndex, columnOrder);
			THashSet<ColumnCollection> prunedNonDependencySupersets = nonDependencies.getPrunedSupersets(uncheckedSupersets);
			THashSet<ColumnCollection> prunedDependencySupersets = dependencies.getPrunedSubsets(uncheckedSupersets);
			for (ColumnCollection prunedNonDependencySuperset : prunedNonDependencySupersets) {
				observations.put(prunedNonDependencySuperset, Observation.NON_DEPENDENCY);
			}
			for (ColumnCollection prunedDependencySuperset : prunedDependencySupersets) {
				observations.put(prunedDependencySuperset, Observation.DEPENDENCY);
			}
			uncheckedSupersets.removeAll(prunedDependencySupersets);
			uncheckedSupersets.removeAll(prunedNonDependencySupersets);
			if (uncheckedSupersets.isEmpty() && prunedNonDependencySupersets.isEmpty()) {
				observations.put(currentSeed.getIndices(), Observation.MAXIMAL_NON_DEPENDENCY);
				maximalNonDependencies.addRHSColumn(currentSeed.getIndices(), currentRHSIndex);
			} else if (!uncheckedSupersets.isEmpty()) {
				ColumnCollection notRepresentedUncheckedSuperset = uncheckedSupersets.iterator().next();
				if (notRepresentedUncheckedSuperset != null) {
					trace.push(currentSeed);
					int additionalColumn = notRepresentedUncheckedSuperset.removeCopy(currentSeed.getIndices()).nextSetBit(0);
					return new Seed(notRepresentedUncheckedSuperset, additionalColumn);
				}
			}
		}
		if (!this.trace.isEmpty()) {
			Seed nextSeed = this.trace.pop();
			return nextSeed;
		}
		return null;
	}

	public FunctionalDependencies getDependencies() {
		return this.minimalDependencies;
	}
}