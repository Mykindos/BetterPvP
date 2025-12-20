package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.Core;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Snowflake ID Generator based on Twitter's Snowflake algorithm.
 * <p>
 * ID Structure (64 bits):
 * - 41 bits: timestamp (milliseconds since epoch)
 * - 10 bits: server ID (derived from current server name hash)
 * - 5 bits: season ID (derived from current season hash)
 * - 8 bits: sequence number (for IDs generated in the same millisecond)
 * <p>
 * This allows for:
 * - ~69 years of timestamps
 * - 1024 unique servers
 * - 32 unique seasons
 * - 256 IDs per millisecond per server/season combination
 */
public class SnowflakeIdGenerator {

    // Custom epoch (e.g., January 1, 2020 00:00:00 UTC)
    private static final long CUSTOM_EPOCH = 1577836800000L;

    private static final long TIMESTAMP_BITS = 41;
    private static final long SERVER_BITS = 10;
    private static final long SEASON_BITS = 5;
    private static final long SEQUENCE_BITS = 8;

    private static final long MAX_SERVER_ID = (1L << SERVER_BITS) - 1;
    private static final long MAX_SEASON_ID = (1L << SEASON_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private static final long SERVER_SHIFT = SEQUENCE_BITS;
    private static final long SEASON_SHIFT = SEQUENCE_BITS + SERVER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + SERVER_BITS + SEASON_BITS;

    private final long serverId;
    private final long seasonId;

    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public SnowflakeIdGenerator() {
        this.serverId = generateServerId(Core.getCurrentServer().getId() + "");
        this.seasonId = generateSeasonId(Core.getCurrentRealm().getSeason() + "");
    }

    /**
     * Generates a unique Snowflake ID.
     *
     * @return A unique 64-bit ID
     * @throws IllegalStateException if clock moves backwards
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        long lastTs = lastTimestamp.get();

        if (timestamp < lastTs) {
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds",
                            lastTs - timestamp)
            );
        }

        if (timestamp == lastTs) {
            // Same millisecond - increment sequence
            long seq = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (seq == 0) {
                // Sequence exhausted, wait for next millisecond
                timestamp = waitNextMillis(lastTs);
            }
            sequence.set(seq);
        } else {
            // New millisecond - reset sequence
            sequence.set(0);
        }

        lastTimestamp.set(timestamp);

        return ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (seasonId << SEASON_SHIFT)
                | (serverId << SERVER_SHIFT)
                | sequence.get();
    }

    /**
     * Generates a Snowflake ID as a String.
     *
     * @return A unique ID as a String
     */
    public String nextIdAsString() {
        return String.valueOf(nextId());
    }

    /**
     * Parses a Snowflake ID and extracts its components.
     *
     * @param id The Snowflake ID to parse
     * @return A SnowflakeInfo object containing the parsed components
     */
    public static SnowflakeInfo parse(long id) {
        long timestamp = ((id >> TIMESTAMP_SHIFT) & ((1L << TIMESTAMP_BITS) - 1)) + CUSTOM_EPOCH;
        long seasonId = (id >> SEASON_SHIFT) & MAX_SEASON_ID;
        long serverId = (id >> SERVER_SHIFT) & MAX_SERVER_ID;
        long sequence = id & MAX_SEQUENCE;

        return new SnowflakeInfo(timestamp, serverId, seasonId, sequence);
    }

    /**
     * Waits until the next millisecond.
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * Generates a server ID from the server name using a hash function.
     */
    private long generateServerId(String serverName) {
        if (serverName == null || serverName.equals("unknown")) {
            return 0;
        }
        return serverName.hashCode() & MAX_SERVER_ID;
    }

    /**
     * Generates a season ID from the season name using a hash function.
     */
    private long generateSeasonId(String seasonName) {
        if (seasonName == null || seasonName.equals("unknown")) {
            return 0;
        }
        return seasonName.hashCode() & MAX_SEASON_ID;
    }

    /**
     * Information extracted from a Snowflake ID.
     */
    public record SnowflakeInfo(long timestamp, long serverId, long seasonId, long sequence) {

        @Override
        public @NotNull String toString() {
            return String.format("SnowflakeInfo{timestamp=%d, serverId=%d, seasonId=%d, sequence=%d}",
                    timestamp, serverId, seasonId, sequence);
        }
    }
}