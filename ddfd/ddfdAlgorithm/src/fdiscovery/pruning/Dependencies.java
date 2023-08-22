package fdiscovery.pruning;

import java.util.Collection;
import java.util.Iterator;

import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;
import gnu.trove.set.hash.THashSet;

public class Dependencies extends PruneHashSet {

	private static final long serialVersionUID = 6853361532152708964L;

	public Dependencies(Relation relation) {
		super(relation);
	}

	public THashSet<ColumnCollection> getPrunedSubsets(Iterable<ColumnCollection> subsets) {
		THashSet<ColumnCollection> prunedSubsets = new THashSet<>();
		for (ColumnCollection subset : subsets) {
			if (this.isRepresented(subset)) {
				prunedSubsets.add(subset);
			}
		}
		return prunedSubsets;
	}
	
	public boolean isRepresented(ColumnCollection value) {
		for (ColumnCollection keyForGroup : this.keySet()) {
			if (keyForGroup.isSubsetOf(value)) {
				for (ColumnCollection dependency : this.get(keyForGroup)) {
					// prune supersets of dependencies
					if (value.isSupersetOf(dependency)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void add(ColumnCollection newEntry, Relation relation) {
		outer: for (ColumnCollection key : this.keySet()) {
			if (key.isSubsetOf(newEntry)) {
				Collection<ColumnCollection> depsForKey = this.get(key);

				boolean doInsert = depsForKey.stream().noneMatch(newEntry::isSupersetOf);
				if (doInsert)
					depsForKey.add(newEntry);
				depsForKey.removeIf(newEntry::isProperSubsetOf);
//				for (Iterator<ColumnCollection> depIt = depsForKey.iterator(); depIt.hasNext(); ) {
//					ColumnCollection dep = depIt.next();
//					if (newEntry.isSupersetOf(dep)) {
//						continue outer;
//					}
//					if (newEntry.isSubsetOf(dep)) {
//						depIt.remove();
//					}
//				}
				depsForKey.add(newEntry);
			}
		}
		this.rebalance(relation);
	}
}
