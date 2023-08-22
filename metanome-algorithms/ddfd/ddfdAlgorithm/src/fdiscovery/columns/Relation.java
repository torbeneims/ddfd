package fdiscovery.columns;

public class Relation extends ColumnCollection {

    public int numberOfColumns;
    public ColumnCollection base;

    public Relation(int numberOfColumns, ColumnCollection base) {
        this.numberOfColumns = numberOfColumns;
        this.base = base;
        this.set(0, numberOfColumns, true);
    }

    public Relation(int numberOfColumns) {
        this(numberOfColumns, new ColumnCollection());
    }

    public Relation clearCopy(int bitIndex) {
        Relation copy = (Relation)this.clone();
        copy.clear(bitIndex);

        return copy;
    }

    public Relation copy() {
        return (Relation)this.clone();
    }
}
