package fdiscovery.partitions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import fdiscovery.columns.ColumnCollection;
import fdiscovery.equivalence.TEquivalence;
import gnu.trove.iterator.TIntIterator;

public abstract class Partition extends TreeSet<TEquivalence> implements Comparable<Partition> {

	private static final long serialVersionUID = 174046028525977844L;
	
	protected ColumnCollection indices;
	protected int numberOfRows;
	protected double error;
	protected double distinctiveness;
//	protected long hashNumber;

	private static final ConcurrentHashMap<ColumnCollection, AtomicInteger> recreationCountPerInstance = new ConcurrentHashMap<>();
	
	public Partition(int columnIndex, int numberOfColumns, int numberOfRows) {
		this.indices = new ColumnCollection(numberOfColumns);
		this.indices.set(columnIndex);
		this.numberOfRows = numberOfRows;
		this.error = -1;
		this.distinctiveness = -1;

		recreationCountPerInstance.computeIfAbsent(this.indices, k -> new AtomicInteger(0)).incrementAndGet();
	}

	/**
	 * Retrieves the recreation counts for each column collection.
	 *
	 * @return a map containing the column collections as keys and the corresponding recreation counts as values
	 */
	public static Map<ColumnCollection, Integer> getRecreationCounts() {
		return recreationCountPerInstance.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> entry.getValue().get()
				));
	}

	protected int[] getProbeTable() {
		int[] probeTable = new int[numberOfRows +1];
		Arrays.fill(probeTable, -1);
		return probeTable;
	}
	
	public Partition(Partition base, Partition additional, boolean trackStatistics) {
		this.indices = base.indices.orCopy(additional.indices);
		this.error = -1;
		this.numberOfRows = base.numberOfRows;
		this.distinctiveness = -1;

		if(trackStatistics)
			recreationCountPerInstance.computeIfAbsent(this.indices, k -> new AtomicInteger(0)).incrementAndGet();
	}

	private void resetProbeTable(int[] probeTable) {
		Arrays.fill(probeTable, -1);
	}
	
	@Override
	public int compareTo(Partition o) {
		if (this.getDistinctiveness() == o.getDistinctiveness()) {
			return this.indices.compareTo(o.indices);
		}
		return Double.valueOf(this.getDistinctiveness()).compareTo(Double.valueOf(o.getDistinctiveness()));
	}
	
	public int getNumberOfRows() {
		return this.numberOfRows;
	}
	
	public ColumnCollection getIndices() {
		return this.indices;
	}
	
	protected double getDistinctiveness() {
		if (this.distinctiveness == -1) {
			double distinctiveness = (double)(this.numberOfRows - this.size())/this.numberOfRows;
			this.distinctiveness = distinctiveness;
		}
		return this.distinctiveness;
	}
	
	public static double estimateDistinctiveness(Partition a, Partition b) {
		return a.getDistinctiveness() + b.getDistinctiveness() - a.getDistinctiveness() * b.getDistinctiveness();
	}
	
	protected double getError() {
		if (this.error == -1) {
			int cumulatedEqClassSizes = 0;
			for (TEquivalence equivalenceGroup : this) {
				cumulatedEqClassSizes += equivalenceGroup.size();
			}
			double error = (double)(cumulatedEqClassSizes - this.size())/this.numberOfRows;
			this.error = error;
		} 
		return this.error;
	}
	
	public static boolean representsFD(Partition base, Partition baseMergedWithRHS) {
		if (base.getError() == baseMergedWithRHS.getError()) {
			return true;
		}
		return false;
	}
	
	public boolean isUnique() {
		return this.size() == 0;
	}
	
	public boolean equals(Partition other) {
		int[] probeTable = getProbeTable();
		int numberOfValues = 0;
		int groupIndex = 0;
		for (TEquivalence equivalenceGroup : this) {
			for (TIntIterator equivalenceGroupIt = equivalenceGroup.iterator(); equivalenceGroupIt.hasNext(); ) {
				probeTable[equivalenceGroupIt.next()] = groupIndex;
				numberOfValues++;
			}
			groupIndex++;
		}
		for (TEquivalence equivalenceGroup : other) {
			groupIndex = -2;
			for (TIntIterator equivalenceGroupIt = equivalenceGroup.iterator(); equivalenceGroupIt.hasNext(); ) {
				int currentGroupIndex = probeTable[equivalenceGroupIt.next()];
				if (groupIndex == -2 || currentGroupIndex == groupIndex) {
					groupIndex = currentGroupIndex;
				} else {
					resetProbeTable(probeTable);
					return false;
				}
				numberOfValues--;
			}
		}
		resetProbeTable(probeTable);
		if (numberOfValues == 0) {
			return true;
		}
		return false;
	}
	
	public String printIndices() {
		StringBuilder outputBuilder = new StringBuilder(this.indices.size());
		
		for (int i=0; i < this.indices.size(); i++) {
			if (this.indices.get(i)) {
				outputBuilder.append("1");
			} else {
				outputBuilder.append("0");
			}
		}
		return outputBuilder.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder outputBuilder = new StringBuilder();
		outputBuilder.append(String.format("[%s]{", this.indices));

		for(TEquivalence equivalenceGroup : this) {
			outputBuilder.append("{");
			for (TIntIterator valueIt=equivalenceGroup.iterator(); valueIt.hasNext(); ) {
				outputBuilder.append(valueIt.next());
				outputBuilder.append(",");
			}
			outputBuilder.append("}");
		}
		outputBuilder.append("}");

		return outputBuilder.toString();
	}
}
