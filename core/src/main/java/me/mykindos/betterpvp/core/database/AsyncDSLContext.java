package me.mykindos.betterpvp.core.database;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Wraps DSLContext operations to run asynchronously on a background thread.
 * Eliminates boilerplate CompletableFuture.runAsync() calls in repositories.
 */
public class AsyncDSLContext {
    /**
     * -- GETTER --
     * Get the underlying Configuration.
     */
    @Getter
    private final IDatabaseConnection connection;
    private final Executor executor;

    public AsyncDSLContext(IDatabaseConnection connection, Executor executor) {
        this.connection = connection;
        this.executor = executor;
    }

    /**
     * Executes a query asynchronously and returns a CompletableFuture.
     *
     * @param query The query function to execute
     * @param <T>   The return type
     * @return CompletableFuture with the query result
     */
    public <T> CompletableFuture<T> executeAsync(Function<DSLContext, T> query) {
        return CompletableFuture.supplyAsync(() -> {
            DSLContext ctx = DSL.using(connection.getDataSource(TargetDatabase.GLOBAL), SQLDialect.POSTGRES);
            return query.apply(ctx);

        }, executor).exceptionally(ex -> {
            throw new RuntimeException("Async query execution failed", ex);
        });
    }

    /**
     * Executes a void query asynchronously.
     *
     * @param query The query consumer to execute
     * @return CompletableFuture that completes when done
     */
    public CompletableFuture<Void> executeAsyncVoid(Consumer<DSLContext> query) {
        return CompletableFuture.runAsync(() -> {
            DSLContext ctx = DSL.using(connection.getDataSource(TargetDatabase.GLOBAL), SQLDialect.POSTGRES);
            query.accept(ctx);

        }, executor).exceptionally(ex -> {
            throw new RuntimeException("Async query execution failed", ex);
        });
    }

}