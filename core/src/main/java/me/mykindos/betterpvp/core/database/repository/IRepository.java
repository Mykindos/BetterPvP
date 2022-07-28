package me.mykindos.betterpvp.core.database.repository;

import java.util.List;

public interface IRepository<T> {

    List<T> getAll();

    void save(T object);
}
