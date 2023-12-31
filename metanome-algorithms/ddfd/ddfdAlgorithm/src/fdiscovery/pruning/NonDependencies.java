package fdiscovery.pruning;

import java.util.Collection;
import java.util.Iterator;

import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;
import gnu.trove.set.hash.THashSet;

public class NonDependencies extends PruneHashSet {

	private static final long serialVersionUID = 3160579586722511675L;

	public NonDependencies(Relation relation) {
		super(relation);
	}
	
	public THashSet<ColumnCollection> getPrunedSupersets(Iterable<ColumnCollection> supersets) {
		THashSet<ColumnCollection> prunedSupersets = new THashSet<>();
		for (ColumnCollection superset : supersets) {
			if (this.isRepresented(superset)) {
				prunedSupersets.add(superset);
			}
		}
		return prunedSupersets;
	}
	
	public boolean isRepresented(ColumnCollection value) {
		acquire();

		for (ColumnCollection keyForGroup : this.keySet()) {
			if (keyForGroup.isSubsetOf(value)) {
				for (ColumnCollection nonDependency : this.get(keyForGroup)) {
					// prune subsets of non-dependencies
					if (value.isSubsetOf(nonDependency)) {
						release();
						return true;
					}
				}
			}
		}
		release();
		return false;
	}
	
	public boolean findSupersetOf(ColumnCollection value) {
		acquire();
		for (ColumnCollection keyForGroup : this.keySet()) {
			if (keyForGroup.isSubsetOf(value)) {
				for (ColumnCollection valueInGroup : this.get(keyForGroup)) {
					if (valueInGroup.isSupersetOf(value)) {
						release();
						return true;
					}
				}
			}
		}

		release();
		return false;
	}

	public void add(ColumnCollection newEntry, Relation relation) {
		acquire();
		outer: for (ColumnCollection key : this.keySet()) {
			if (key.isSubsetOf(newEntry)) {
				Collection<ColumnCollection> depsForKey = this.get(key);
				for (Iterator<ColumnCollection> nonDepIt = depsForKey.iterator(); nonDepIt.hasNext(); ) {
					ColumnCollection nonDep = nonDepIt.next();
					if (newEntry.isSubsetOf(nonDep)) {
						continue outer;
					}
					if (newEntry.isSupersetOf(nonDep)) {
						nonDepIt.remove();
					}
				}
				depsForKey.add(newEntry);
			}
		}

		release();
		this.rebalance(relation);
	}
}
