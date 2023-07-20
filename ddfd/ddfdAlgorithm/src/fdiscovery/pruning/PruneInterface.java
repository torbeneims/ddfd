package fdiscovery.pruning;

import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;

public interface PruneInterface {

	public static final int SPLIT_THRESHOLD = 1000;

	public void rebalance(Relation relation); // TODO changed interface for relation
	public void rebalanceGroup(ColumnCollection groupKey, Relation relation);

}
