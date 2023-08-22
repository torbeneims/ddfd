package fdiscovery.pruning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

import fdiscovery.approach.runner.GraphTraverser;
import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;

/**
 * Each dependency is stored for every attribute it contains (key).
 */
public class PruneHashSet extends HashMap<ColumnCollection, Collection<ColumnCollection>> implements PruneInterface {

	private static final long serialVersionUID = 8012444410589325434L;

	public PruneHashSet(Relation relation) {
		super(relation.cardinality());
		relation.stream()
				.forEach(columnIndex -> this.put(ColumnCollection.fromIndices(columnIndex), newSecondLayerCollection()));
	}

	@Override
	public void clear() {
		forEach((key, value) -> value.clear());
	}

	@Override
	public void rebalance(Relation relation) {
		boolean rebalancedGroup = false;

		do {
			rebalancedGroup = false;
			ArrayList<ColumnCollection> groupKeys = new ArrayList<>(this.keySet());
			for (ColumnCollection key : groupKeys) {
				if (this.get(key).size() > SPLIT_THRESHOLD) {
					rebalanceGroup(key, relation);
					rebalancedGroup = true;
				}
			}
		} while (rebalancedGroup);
	}

	protected final Collection<ColumnCollection> newSecondLayerCollection() {
		return GraphTraverser.USE_CONCURRENT_FUNCTIONAL_DEPENDENCIES() ?
				new ConcurrentLinkedDeque<>() :
				new HashSet<>();
	}

	@Override
	public void rebalanceGroup(ColumnCollection groupKey, Relation relation) {
		Collection<ColumnCollection> depsOfGroup = this.get(groupKey);
		for (int columnIndex : groupKey.complementCopy(relation).getSetBits(relation)) {
			ColumnCollection newKey = groupKey.setCopy(columnIndex);
			Collection<ColumnCollection> newGroup = newSecondLayerCollection();
			this.put(newKey, newGroup);

			for (ColumnCollection depOfGroup : depsOfGroup) {
				// when splitting a group it cannot contain the key itself
				// because otherwise the group cannot contain any other 
				// element since it would be a superset of the key and be pruned
				// OR
				// when splitting a group it cannot contain the key itself
				// because otherwise all supersets of the key would have 
				// been pruned and it wouldn't need to be split
				if (newKey.isSubsetOf(depOfGroup)) {
					newGroup.add(depOfGroup);
				}
			}
		}
		// remove the old group
		this.remove(groupKey);
	}
}
