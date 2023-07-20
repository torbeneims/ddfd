package fdiscovery.partitions;

import fdiscovery.columns.ColumnCollection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentMemoryManagedJoinedPartitions extends MemoryManagedJoinedPartitions {

    public ConcurrentMemoryManagedJoinedPartitions(int numberOfColumns) {
        super(numberOfColumns);
    }

    @Override
    protected Map<ColumnCollection, Partition> createPartitionMap() {
        return new ConcurrentHashMap<ColumnCollection, Partition>();
    }
}
