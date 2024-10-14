package me.mykindos.betterpvp.core.logging.menu.button.type;

public interface IRefreshButton {
    void setRefresh(java.util.function.Supplier<java.util.concurrent.CompletableFuture<Boolean>> refresh);
}
