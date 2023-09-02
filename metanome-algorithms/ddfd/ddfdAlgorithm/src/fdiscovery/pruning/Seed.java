package fdiscovery.pruning;

import fdiscovery.columns.ColumnCollection;

import java.io.Serializable;

public class Seed implements Serializable {

	private final ColumnCollection indices;
	private final int additionalColumnIndex;
	
	public Seed(ColumnCollection indices, int additionalColumnIndex) {
		this.indices = indices.setCopy(additionalColumnIndex);
		this.additionalColumnIndex = additionalColumnIndex;
	}
	
	public Seed(ColumnCollection indices) {
		this.indices = indices;
		this.additionalColumnIndex = -1;
	}
	
	public boolean isAtomic() {
		return this.indices.cardinality() == 1;
	}
	
	public ColumnCollection getBaseIndices() {
		return this.indices.removeColumnCopy(additionalColumnIndex);
	}
	
	public ColumnCollection getIndices() {
		return this.indices;
	}

	public int getAdditionalColumnIndex() {
		return this.additionalColumnIndex;
	}
	
	public String toString() {
		StringBuilder outputBuilder = new StringBuilder();
		outputBuilder.append(String.format("Seed: [%s]", this.indices));
		return outputBuilder.toString();
	}
}
