package fdiscovery.pruning;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import fdiscovery.approach.ColumnOrder;
import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;
import gnu.trove.set.hash.THashSet;

public class Observations extends ConcurrentHashMap<ColumnCollection, Observation> {

	private static final long serialVersionUID = 2932117192054503664L;

	public THashSet<ColumnCollection> getUncheckedMaximalSubsets(ColumnCollection lhs, ColumnOrder order, Relation relation) {
		THashSet<ColumnCollection> uncheckedMaximalSubsets = new THashSet<>();
		
//		if (lhs.cardinality() > 2) {
			for (int columnIndex : order.getOrderHighDistinctCount(lhs, relation)) {
				ColumnCollection subsetIndices = lhs.removeColumnCopy(columnIndex);
				if (!this.containsKey(subsetIndices)) {
					uncheckedMaximalSubsets.add(subsetIndices);
				}
			}
//		}
		return uncheckedMaximalSubsets;
	}

	public THashSet<ColumnCollection> getUncheckedMinimalSupersets(ColumnCollection lhs, int rhsIndex, ColumnOrder order, Relation relation) {
		THashSet<ColumnCollection> uncheckedMinimalSupersets = new THashSet<>();
		
		for (int columnIndex : order.getOrderLowDistinctCount(lhs.setCopy(rhsIndex).complement(relation), relation)) {
			ColumnCollection supersetIndices = lhs.setCopy(columnIndex);
			if (!this.containsKey(supersetIndices)) {
				uncheckedMinimalSupersets.add(supersetIndices);
			}
		}
		return uncheckedMinimalSupersets;
	}

	public Observation updateDependencyType(ColumnCollection lhs, Relation relation) {
		if (lhs.cardinality() > 1) {
			boolean foundUncheckedSubset = false;
			for (ColumnCollection subset : lhs.directSubsets(relation)) {
				Observation observationOfSubset = this.get(subset);
				if (observationOfSubset == null) {
					foundUncheckedSubset = true;
				} else if (observationOfSubset.isDependency()) {
					return Observation.DEPENDENCY;
				}
			}
			if (foundUncheckedSubset) {
				return Observation.CANDIDATE_MINIMAL_DEPENDENCY;
			}
		}
		return Observation.MINIMAL_DEPENDENCY;
	}

	public Observation updateNonDependencyType(ColumnCollection lhs, int rhsIndex, Relation relation) {
		boolean foundUncheckedSuperset = false;
		for (ColumnCollection superset : lhs.directSupersets(relation.clearCopy(rhsIndex))) {
			Observation observationOfSuperset = this.get(superset);
			if (observationOfSuperset == null) {
				foundUncheckedSuperset = true;
			} else if (observationOfSuperset.isNonDependency()) {
				return Observation.NON_DEPENDENCY;
			}
		}
		if (foundUncheckedSuperset) {
			return Observation.CANDIDATE_MAXIMAL_NON_DEPENDENCY;
		}

		return Observation.MAXIMAL_NON_DEPENDENCY;
	}
}
