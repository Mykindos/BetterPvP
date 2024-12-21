package me.mykindos.betterpvp.core.logging.menu.button.type;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IRefreshButton {
    void setRefresh(Supplier<CompletableFuture<Boolean>> refresh);
}
