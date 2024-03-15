package me.mykindos.betterpvp.core.framework.updater;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

@Data
public class UpdateEventContainer implements Comparable<UpdateEventContainer> {

    private final Object instance;
    private final Method method;
    private final UpdateEvent updateEvent;

    @Override
    public int compareTo(@NotNull UpdateEventContainer o) {
        return Integer.compare(o.getUpdateEvent().priority(), this.getUpdateEvent().priority());
    }
}
