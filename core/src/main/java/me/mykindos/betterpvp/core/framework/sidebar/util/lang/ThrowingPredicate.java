package me.mykindos.betterpvp.core.framework.sidebar.util.lang;

@FunctionalInterface
public interface ThrowingPredicate<T, E extends Throwable> {

    boolean test(T t) throws E;
}
