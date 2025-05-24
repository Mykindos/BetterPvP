package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.client.gamer.Gamer;

public interface IDisplayQueue<T> {
    void add(int priority, T element);

    void remove(T element);

    void clear();

    boolean hasElementsQueued();

    void show(Gamer gamer);

    void cleanUp();
}
