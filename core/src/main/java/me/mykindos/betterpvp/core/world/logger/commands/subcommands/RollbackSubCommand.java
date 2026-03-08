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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS_METADATA;

@CustomLog
@SubCommand(WorldLoggerCommand.class)
public class RollbackSubCommand extends Command {

    private final Core core;
    private final WorldLogHandler worldLogHandler;
    private final Database database;
    private static final Pattern timePattern = Pattern.compile("(\\d+)([smhd])");

    @Inject
    public RollbackSubCommand(Core core, WorldLogHandler worldLogHandler, Database database) {
        this.core = core;
        this.worldLogHandler = worldLogHandler;
        this.database = database;
    }

    @Override
    public String getName() {
        return "rollback";
    }

    @Override
    public String getDescription() {
        return "Roll back block changes within a radius and time period";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "World Logger", "Usage: /wl rollback radius:<radius> time:<time> [player:<player>]");
            UtilMessage.message(player, "World Logger", "Example: /wl rollback radius:5 time:3h player:Mykindos");
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
                    UtilMessage.message(player, "World Logger", "Invalid radius value. Must be a number.");
                    return;
                }
            } else if (arg.startsWith("time:")) {
                timeString = arg.substring(5);
            } else if (arg.startsWith("player:")) {
                targetPlayer = arg.substring(7);
            }
        }

        if (radius < 0) {
            UtilMessage.message(player, "World Logger", "You must specify a valid radius.");
            return;
        }

        if (timeString == null) {
            UtilMessage.message(player, "World Logger", "You must specify a time period.");
            return;
        }

        // Parse time string (e.g., 3h, 30m, 1d)
        long timeInMillis = parseTimeString(timeString);
        if (timeInMillis <= 0) {
            UtilMessage.message(player, "World Logger", "Invalid time format. Use format like 3h, 30m, 1d.");
            return;
        }

        // Calculate the time threshold
        Instant timeThreshold = Instant.now().minus(timeInMillis, ChronoUnit.MILLIS);

        // Get player location
        final Location playerLocation = player.getLocation();
        final int finalRadius = radius;
        final String finalTargetPlayer = targetPlayer;

        UtilMessage.message(player, "World Logger", "Starting rollback process...");

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            var query = ctx.selectQuery();

            query.addFrom(WORLD_LOGS);
            if (finalTargetPlayer != null) {
                query.addJoin(WORLD_LOGS_METADATA, WORLD_LOGS.ID.eq(WORLD_LOGS_METADATA.LOG_ID));
                query.addConditions(WORLD_LOGS_METADATA.META_KEY.eq("PlayerName")
                        .and(WORLD_LOGS_METADATA.META_VALUE.eq(finalTargetPlayer)));
            }

            query.addConditions(WORLD_LOGS.REALM.eq(Core.getCurrentRealm()));
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

            try {
                List<WorldLog> worldLogs = new ArrayList<>();
                query.fetch().forEach(worldLogRecord -> {
                    String world = worldLogRecord.get(WORLD_LOGS.WORLD);
                    int x = worldLogRecord.get(WORLD_LOGS.BLOCK_X);
                    int y = worldLogRecord.get(WORLD_LOGS.BLOCK_Y);
                    int z = worldLogRecord.get(WORLD_LOGS.BLOCK_Z);
                    String action = worldLogRecord.get(WORLD_LOGS.ACTION);
                    String material = worldLogRecord.get(WORLD_LOGS.MATERIAL);

                    WorldLog log = WorldLog.builder()
                            .world(world)
                            .blockX(x)
                            .blockY(y)
                            .blockZ(z)
                            .action(WorldLogAction.valueOf(action))
                            .material(material)
                            .build();
                    worldLogs.add(log);
                });

                if (worldLogs.isEmpty()) {
                    UtilMessage.message(player, "World Logger", "No blocks found to roll back.");
                    return;
                }

                // Process the rollback
                processRollback(worldLogs);

                UtilMessage.message(player, "World Logger", "Rollback completed.");

            } catch (Exception ex) {
                UtilMessage.simpleMessage(player, "World Logger", "Failed to rollback blocks");
                log.error("Failed to rollback blocks", ex).submit();
            }
        });

    }

    private void processRollback(List<WorldLog> logs) {

        UtilServer.runTask(core, () -> {
            for (WorldLog log : logs) {
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
                if (log.getAction().equals(WorldLogAction.BLOCK_PLACE.name())) {
                    // If a block was placed, we remove it (set to AIR)
                    location.getBlock().setType(Material.AIR);
                } else if (log.getAction().equals(WorldLogAction.BLOCK_BREAK.name())) {
                    // If a block was broken, we restore it
                    if (log.getMaterial() != null) {
                        location.getBlock().setType(Material.valueOf(log.getMaterial()));
                        // Set the block type to the original material
                    }
                }
                // Other action types can be handled as needed
            }
        });


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
