package me.mykindos.betterpvp.core.database.repository;

import java.util.ArrayList;
import java.util.List;

public interface IRepository<T> {

    default List<T> getAll() {
        return new ArrayList<>();
    }

    default void save(T object) {

    }
}
