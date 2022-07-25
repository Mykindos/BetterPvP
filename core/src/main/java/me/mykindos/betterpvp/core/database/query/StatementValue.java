package me.mykindos.betterpvp.core.database.query;

public abstract class StatementValue<T> {

    private T value;

    public StatementValue(T value){
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    /**
     * https://docs.oracle.com/javase/8/docs/api/java/sql/Types.html
     * @return SQL Type of the object
     */
    public abstract int getType();
}
