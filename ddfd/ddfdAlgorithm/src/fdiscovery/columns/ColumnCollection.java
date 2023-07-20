package fdiscovery.columns;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.stream.Collectors;

public class ColumnCollection extends BitSet implements Comparable<BitSet> {

	private static final long serialVersionUID = -5256272139963505719L;

	public ColumnCollection() {}

	public ColumnCollection(int numberOfColumns) {
		super(numberOfColumns);
	}

	public static ColumnCollection fromIndices(int ...indices) {
		ColumnCollection columnCollection = new ColumnCollection(indices.length);
		Arrays.stream(indices).forEach(columnCollection::set);
		return columnCollection;
	}

	/**
	 * Creates a new ColumnCollection from a bitmap.
	 *
	 * @param bitmap the bitmap representing the bits to include in the ColumnCollection
	 * @return the ColumnCollection containing the bits from the bitmap
	 */
	public static ColumnCollection fromBits(long bitmap) {
		ColumnCollection columnCollection = new ColumnCollection();
		for (int i = 0; (bitmap << i) > 0; i++) {
			if ((bitmap & (1L << i)) != 0) {
				columnCollection.set(i);
			}
		}
		return columnCollection;
	}

	private static int getFormatStringWidth(int numberOfColumns) {
		return (int) Math.ceil(Math.log10(numberOfColumns));
	}

	/**
	 * Returns the bits inside this collection, aligning to the relation view.
	 */
	public int[] getSetBits(Relation relation) {
		return this.stream().filter(relation::get).toArray();
	}

	/**
	 * Returns all set bits. Only use this method if the column collection already aligns to the relation view i.e.
	 * all statically set bits should be returned.
	 * Statically set bits are set bits that are not part of the relation.
	 */
	@Deprecated
	public int[] getSetBits() {
		return this.stream().toArray();
	}

	/**
	 * Returns all direct supersets according to the relation view. If in doubt, pass a full relation.
	 */
	public Collection<ColumnCollection> directSupersets(Relation relation) {
		return this.complementCopy(relation).stream()
				.filter(relation::get)
				.mapToObj(this::setCopy)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all direct subsets according to the relation view. If in doubt, pass a full relation.
	 */
	public Collection<ColumnCollection> directSubsets(Relation relation) {
		return this.stream()
				.filter(relation::get)
				.mapToObj(this::clearCopy)
				.collect(Collectors.toList());
	}

	public boolean isAtomic() {
		return this.cardinality() == 1;
	}

	public int cardinality(Relation relation) {
		return this.stream().filter(relation::get).toArray().length;
	}
	
	public ColumnCollection addColumn(int columnIndex) {
		ColumnCollection copy = (ColumnCollection) this.clone();
		copy.set(columnIndex);
		
		return copy;
	}
	
	public ColumnCollection andCopy(ColumnCollection other) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.and(other);
		
		return copy;
	}
	
	public ColumnCollection clearCopy(int bitIndex) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.clear(bitIndex);
		
		return copy;
	}

	public ColumnCollection clearAllCopy(Relation relation) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		relation.stream().forEach(copy::clear);

		return copy;
	}
	
	public ColumnCollection removeCopy(ColumnCollection other) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.andNot(other);
		
		return copy;
	}
	
	public ColumnCollection orCopy(ColumnCollection other) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.or(other);

		return copy;
	}
	
	public ColumnCollection setCopy(int index) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.set(index);
		
		return copy;
	}
	
	public ColumnCollection xorCopy(ColumnCollection other) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		copy.xor(other);
		
		return copy;
	}

	public ColumnCollection complementCopy(Relation relation) {
		ColumnCollection copy = (ColumnCollection)this.clone();
		relation.stream().forEach(copy::flip);

		return copy;
	}

	public ColumnCollection complement(Relation relation) {
		relation.stream().forEach(this::flip);
		return this;
	}
	
	public boolean isSubsetOf(ColumnCollection other) {
		return this.unionCount(other) == other.cardinality();
	}
	
	public boolean isSupersetOf(ColumnCollection other) {
		return this.unionCount(other) == this.cardinality();

	}
	
	public boolean isProperSubsetOf(ColumnCollection other) {
		long cardinality = this.cardinality();
		long otherCardinality = other.cardinality();
		if (cardinality != otherCardinality) {
			if (this.unionCount(other) == otherCardinality) {
				return true;
			}
		}
		return false;
	}

	
	public boolean isProperSupersetOf(ColumnCollection other) {
		long cardinality = this.cardinality();
		long otherCardinality = other.cardinality();
		if (cardinality != otherCardinality) {
			if (this.unionCount(other) == cardinality) {
				return true;
			}
		}
		return false;
	}

	public int unionCount(ColumnCollection other) {
		ColumnCollection union = (ColumnCollection) this.clone();
		union.or(other);
		return union.cardinality();
	}

	public long getMostRightBit() {
		return super.stream()
				.reduce((acc, value) -> value)
				.orElse(-1);
	}
	
	public ColumnCollection removeColumnCopy(int columnIndex) {
		ColumnCollection copy = (ColumnCollection) this.clone();
		copy.clear(columnIndex);
		
		return copy;
	}

	@Override
	public int compareTo(BitSet other) {
		ColumnCollection copy = (ColumnCollection) this.clone();
		copy.xor(other);
		int lowestBit = copy.nextSetBit(0);
		if (lowestBit == -1) {
			return 0;
		} else if (this.get(lowestBit)) {
			return -1;
		} else {
			return 1;
		}
	}
	
	public String toString() {
		StringBuilder outputBuilder = new StringBuilder();
		if (this.cardinality() > 0) {
			for (int columnIndex : this.getSetBits()) {
				outputBuilder.append(String.format("%0" + getFormatStringWidth((int)this.getMostRightBit()) + "d,", Integer.valueOf(columnIndex)));

			}
		} else {
			outputBuilder.append("emptyset");
		}
		
		return outputBuilder.toString();
	}
	
	public void remove(ColumnCollection other) {
		this.andNot(other);
	}

	public static int intersectionCount(ColumnCollection set1, ColumnCollection set2) {
		ColumnCollection intersection = (ColumnCollection) set1.clone();
		intersection.and(set2);
		return intersection.cardinality();
	}
}
