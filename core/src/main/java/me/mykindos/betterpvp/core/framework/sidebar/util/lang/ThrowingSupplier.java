package me.mykindos.betterpvp.core.framework.sidebar.util.lang;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {

    T get() throws E;
}
