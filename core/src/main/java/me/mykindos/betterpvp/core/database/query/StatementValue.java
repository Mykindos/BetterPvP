package me.mykindos.betterpvp.core.database.query;

import lombok.Setter;

public abstract class StatementValue<T> {

    @Setter
    private T value;

    public StatementValue(T value){
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    /**
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/Types.html">Types</a>
     * @return SQL Type of the object
     */
    public abstract int getType();
}
