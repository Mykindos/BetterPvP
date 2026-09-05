package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.core.database.Database;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.jooq.impl.DSL.*;

/**
 * Persistence for {@code personal_mine_points}: which blocks of a per-player node each player has personally mined out,
 * and how far down each has gone.
 * <p>
 * In the database rather than in a server-local cache because spawn may be sharded — a player who hops shards has to
 * find the mine exactly as they left it, and a file in one shard's data folder is invisible to the other. The same
 * reason the point is stored at all: a respawn timer a player can reset by relogging is not a timer.
 * <p>
 * One row per depleted point, so a respawn is a single {@code DELETE} and two shards touching different blocks never
 * contend over the same row. Uses the dynamic jOOQ DSL, so no generated classes are needed for this table.
 */
@Singleton
@CustomLog
public class PersonalMineRepository {

    private static final long DB_TIMEOUT_SECONDS = 3;
    private static final String TABLE = "personal_mine_points";

    private final Database database;

    @Inject
    public PersonalMineRepository(@NotNull Database database) {
        this.database = database;
    }

    /**
     * Everything {@code client} has depleted in one node. Read once when the player enters the node; an empty result
     * (the common case, for a player who has never mined there) means they see it whole.
     */
    public CompletableFuture<List<Point>> load(long client, @NotNull String world, @NotNull String node) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            final Result<Record> records = ctx.select()
                    .from(table(name(TABLE)))
                    .where(field(name("client"), Long.class).eq(client))
                    .and(field(name("world"), String.class).eq(world))
                    .and(field(name("node"), String.class).eq(node))
                    .fetch();
            final List<Point> points = new ArrayList<>(records.size());
            for (Record record : records) {
                points.add(new Point(
                        record.get(field(name("x"), Integer.class)),
                        record.get(field(name("y"), Integer.class)),
                        record.get(field(name("z"), Integer.class)),
                        record.get(field(name("stage"), String.class)),
                        record.get(field(name("mined_at"), Long.class))));
            }
            return points;
        }).orTimeout(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Failed to load personal mine points for client {} in node {}", client, node, ex).submit();
            return List.of();
        });
    }

    /**
     * Records a point as depleted, or moves an already-depleted one further down its chain. {@code minedAt} is left at
     * its original value on a later stage: the respawn is measured from when the block was first broken out of its
     * intact state, not from the last time it was hit.
     */
    public CompletableFuture<Void> save(long client, @NotNull String world, @NotNull String node,
                                        @NotNull Point point) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(table(name(TABLE)),
                        field(name("client"), Long.class),
                        field(name("world"), String.class),
                        field(name("node"), String.class),
                        field(name("x"), Integer.class),
                        field(name("y"), Integer.class),
                        field(name("z"), Integer.class),
                        field(name("stage"), String.class),
                        field(name("mined_at"), Long.class))
                .values(client, world, node, point.getX(), point.getY(), point.getZ(),
                        point.getStage(), point.getMinedAtMs())
                .onConflict(field(name("client"), Long.class),
                        field(name("world"), String.class),
                        field(name("x"), Integer.class),
                        field(name("y"), Integer.class),
                        field(name("z"), Integer.class))
                .doUpdate()
                .set(field(name("stage"), String.class), point.getStage())
                .execute()).exceptionally(ex -> {
            log.error("Failed to save personal mine point for client {} at {},{},{}",
                    client, point.getX(), point.getY(), point.getZ(), ex).submit();
            return null;
        });
    }

    /** Drops one point, because it has respawned for this player. */
    public CompletableFuture<Void> delete(long client, @NotNull String world, int x, int y, int z) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(table(name(TABLE)))
                .where(field(name("client"), Long.class).eq(client))
                .and(field(name("world"), String.class).eq(world))
                .and(field(name("x"), Integer.class).eq(x))
                .and(field(name("y"), Integer.class).eq(y))
                .and(field(name("z"), Integer.class).eq(z))
                .execute()).exceptionally(ex -> {
            log.error("Failed to delete personal mine point for client {} at {},{},{}", client, x, y, z, ex).submit();
            return null;
        });
    }

    /**
     * Drops rows last mined before {@code cutoffMs}. Everything this old has long since respawned; the rows survive
     * only for players who never came back to have it handed to them, so this is what stops the table growing without
     * bound.
     *
     * @return the number of rows removed
     */
    public CompletableFuture<Integer> pruneOlderThan(long cutoffMs) {
        return database.getAsyncDslContext().executeAsync(ctx -> ctx.deleteFrom(table(name(TABLE)))
                .where(field(name("mined_at"), Long.class).lt(cutoffMs))
                .execute()).exceptionally(ex -> {
            log.error("Failed to prune personal mine points older than {}", cutoffMs, ex).submit();
            return 0;
        });
    }

    /**
     * One depleted point. {@code stage} is the material this player currently sees there — a point can sit part way
     * down its chain — and {@code minedAtMs} is when they first broke it out of its intact state.
     */
    @Value
    public static class Point {
        int x;
        int y;
        int z;
        String stage;
        long minedAtMs;
    }
}
