package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.client.gamer.Gamer;

public interface IDisplayQueue<T> {
    void add(int priority, T component);

    void remove(T component);

    void clear();

    boolean hasElementsQueued();

    void show(Gamer gamer);

    void cleanUp();
}
