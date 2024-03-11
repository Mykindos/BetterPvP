package me.mykindos.betterpvp.core.framework.sidebar.util.lang;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T t) throws E;
   
}
