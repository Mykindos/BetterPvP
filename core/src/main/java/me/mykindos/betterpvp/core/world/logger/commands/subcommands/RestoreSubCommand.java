package me.mykindos.betterpvp.core.world.logger.commands.subcommands;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import me.mykindos.betterpvp.core.world.logger.WorldLogAction;
import me.mykindos.betterpvp.core.world.logger.WorldLogHandler;
import me.mykindos.betterpvp.core.world.logger.commands.WorldLoggerCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS_METADATA;

@CustomLog
@SubCommand(WorldLoggerCommand.class)
public class RestoreSubCommand extends Command {

    private final Core core;
    private final WorldLogHandler worldLogHandler;
    private final Database database;
    private static final Pattern timePattern = Pattern.compile("(\\d+)([smhd])");

    @Inject
    public RestoreSubCommand(Core core, WorldLogHandler worldLogHandler, Database database) {
        this.core = core;
        this.worldLogHandler = worldLogHandler;
        this.database = database;
    }

    @Override
    public String getName() {
        return "restore";
    }

    @Override
    public String getDescription() {
        return "core.command.restore.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.usage");
            UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.example");
            return;
        }

        int radius = -1;
        String timeString = null;
        String targetPlayer = null;

        // Parse arguments
        for (String arg : args) {
            if (arg.startsWith("radius:")) {
                try {
                    radius = Integer.parseInt(arg.substring(7));
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.radius.invalid");
                    return;
                }
            } else if (arg.startsWith("time:")) {
                timeString = arg.substring(5);
            } else if (arg.startsWith("player:")) {
                targetPlayer = arg.substring(7);
            }
        }

        if (radius < 0) {
            UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.radius.required");
            return;
        }

        if (timeString == null) {
            UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.time.required");
            return;
        }

        // Parse time string (e.g., 3h, 30m, 1d)
        long timeInMillis = parseTimeString(timeString);
        if (timeInMillis <= 0) {
            UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.time.invalid");
            return;
        }

        // Calculate the time threshold
        Instant timeThreshold = Instant.now().minus(timeInMillis, ChronoUnit.MILLIS);

        // Get player location
        final Location playerLocation = player.getLocation();
        final int finalRadius = radius;
        final String finalTargetPlayer = targetPlayer;

        UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.start");

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            var query = ctx.selectQuery();

            query.addFrom(WORLD_LOGS);
            if (finalTargetPlayer != null) {
                query.addJoin(WORLD_LOGS_METADATA, WORLD_LOGS.ID.eq(WORLD_LOGS_METADATA.LOG_ID));
                query.addConditions(WORLD_LOGS_METADATA.META_KEY.eq("PlayerName")
                        .and(WORLD_LOGS_METADATA.META_VALUE.eq(finalTargetPlayer)));
            }

            query.addConditions(WORLD_LOGS.REALM.eq(Core.getCurrentRealm().getId()));
            query.addConditions(WORLD_LOGS.WORLD.eq(playerLocation.getWorld().getName()));
            query.addConditions(WORLD_LOGS.BLOCK_X.ge(playerLocation.getBlockX() - finalRadius));
            query.addConditions(WORLD_LOGS.BLOCK_X.le(playerLocation.getBlockX() + finalRadius));
            query.addConditions(WORLD_LOGS.BLOCK_Y.ge(playerLocation.getBlockY() - finalRadius));
            query.addConditions(WORLD_LOGS.BLOCK_Y.le(playerLocation.getBlockY() + finalRadius));
            query.addConditions(WORLD_LOGS.BLOCK_Z.ge(playerLocation.getBlockZ() - finalRadius));
            query.addConditions(WORLD_LOGS.BLOCK_Z.le(playerLocation.getBlockZ() + finalRadius));
            query.addConditions(WORLD_LOGS.ACTION.in(
                    WorldLogAction.BLOCK_PLACE.name(),
                    WorldLogAction.BLOCK_BREAK.name()
            ));
            query.addConditions(WORLD_LOGS.TIME.ge(timeThreshold.getEpochSecond()));

            // Order by time ascending to replay history
            query.addOrderBy(WORLD_LOGS.TIME.asc());

            try {
                List<WorldLog> worldLogs = new ArrayList<>();
                query.fetch().forEach(worldLogRecord -> {
                    String world = worldLogRecord.get(WORLD_LOGS.WORLD);
                    int x = worldLogRecord.get(WORLD_LOGS.BLOCK_X);
                    int y = worldLogRecord.get(WORLD_LOGS.BLOCK_Y);
                    int z = worldLogRecord.get(WORLD_LOGS.BLOCK_Z);
                    String action = worldLogRecord.get(WORLD_LOGS.ACTION);
                    String material = worldLogRecord.get(WORLD_LOGS.MATERIAL);
                    long time = worldLogRecord.get(WORLD_LOGS.TIME);

                    WorldLog log = WorldLog.builder()
                            .world(world)
                            .blockX(x)
                            .blockY(y)
                            .blockZ(z)
                            .action(WorldLogAction.valueOf(action))
                            .material(material)
                            .time(Instant.ofEpochSecond(time))
                            .build();
                    worldLogs.add(log);
                });

                if (worldLogs.isEmpty()) {
                    UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.none");
                    return;
                }

                // Process the restore
                processRestore(worldLogs);

                UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.completed");

            } catch (Exception ex) {
                UtilMessage.message(player, "core.prefix.command", "core.command.worldlogger.restore.failed");
                log.error("Failed to restore blocks", ex).submit();
            }
        });

    }

    private void processRestore(List<WorldLog> logs) {
        // Group by location to only apply the latest change if multiple changes occurred at the same spot
        // Although replaying all in order works too, taking the last state is more efficient.
        Map<String, WorldLog> latestLogsMap = new HashMap<>();
        for (WorldLog log : logs) {
            String key = log.getWorld() + ":" + log.getBlockX() + ":" + log.getBlockY() + ":" + log.getBlockZ();
            latestLogsMap.put(key, log);
        }

        List<WorldLog> latestLogs = new ArrayList<>(latestLogsMap.values());
        int blocksPerTick = 20;

        new org.bukkit.scheduler.BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                for (int i = 0; i < blocksPerTick && index < latestLogs.size(); i++) {
                    WorldLog log = latestLogs.get(index++);
                    Location location = new Location(
                            Bukkit.getWorld(log.getWorld()),
                            log.getBlockX(),
                            log.getBlockY(),
                            log.getBlockZ()
                    );

                    // Skip if the chunk is not loaded
                    if (!location.getChunk().isLoaded()) {
                        location.getChunk().load();
                    }

                    // Process based on action type
                    // Restore means re-applying what happened.
                    if (log.getAction().equals(WorldLogAction.BLOCK_PLACE.name())) {
                        // If a block was placed, we place it back
                        if (log.getMaterial() != null) {
                            try {
                                location.getBlock().setType(Material.valueOf(log.getMaterial()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } else if (log.getAction().equals(WorldLogAction.BLOCK_BREAK.name())) {
                        // If a block was broken, we make it AIR (since it was broken)
                        location.getBlock().setType(Material.AIR);
                    }
                }

                if (index >= latestLogs.size()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(core, 0L, 1L);
    }

    private long parseTimeString(String timeString) {
        Matcher matcher = timePattern.matcher(timeString);
        if (matcher.matches()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            return switch (unit) {
                case "s" -> value * 1000L; // seconds
                case "m" -> value * 60 * 1000L; // minutes
                case "h" -> value * 60 * 60 * 1000L; // hours
                case "d" -> value * 24 * 60 * 60 * 1000L; // days
                default -> -1;
            };
        }
        return -1;
    }
}
