package fdiscovery.approach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import fdiscovery.columns.ColumnCollection;
import fdiscovery.columns.Relation;
import fdiscovery.partitions.FileBasedPartition;
import fdiscovery.partitions.FileBasedPartitions;

// sorts partitions from lowest to highest distinct count
public class ColumnOrder {

	private final int[] order;
	
	public ColumnOrder(FileBasedPartitions fileBasedPartitions) {
		this.order = new int[fileBasedPartitions.size()];
        ArrayList<FileBasedPartition> partitions = new ArrayList<>(fileBasedPartitions);
		Collections.sort(partitions);
//		Collections.sort(partitions, Collections.reverseOrder());
		int orderIndex = 0;
		for (FileBasedPartition partition : partitions) {
			order[orderIndex++] = partition.getIndex();
		}
	}

	public int[] getOrderHighDistinctCount(ColumnCollection columns, Relation relation) {
		int[] result = Arrays.stream(this.order)
				.filter(columns::get)
				.filter(relation::get)
				.toArray();

		assert Arrays.stream(result).allMatch(relation::get) : "Order contains columns not in relation";
		assert Arrays.stream(result).allMatch(columns::get) : "Order contains columns not in columns";

		return result;
	}
	
	public int[] getOrderLowDistinctCount(ColumnCollection columns, Relation relation) {
		int[] columnIndices = columns.getSetBits(relation);
		int[] orderForColumns = new int[columnIndices.length];
		
		int currentOrderIndex = 0;
		for (int i = this.order.length - 1; i >= 0; i--) {
			if (columns.get(this.order[i]) && relation.get(this.order[i])) {
				orderForColumns[currentOrderIndex++] = this.order[i];
			}
		}
		
		return orderForColumns;
	}
}
