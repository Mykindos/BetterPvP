package me.mykindos.betterpvp.core.world.logger.commands.subcommands;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
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

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Statement.StatementBuilder builder = Statement.builder()
                .queryBase("SELECT wl.* FROM world_logs wl")
                .forceIndex("world_logs_location_index");

        // Add player filter if specified
        if (finalTargetPlayer != null) {
            builder = builder.join("world_logs_metadata", Statement.JoinType.INNER, "wlm1", "wl.id", "wlm1.LogId")
                    .where("wlm1.MetaKey", "=", StringStatementValue.of("PlayerName"))
                    .where("wlm1.MetaValue", "=", StringStatementValue.of(finalTargetPlayer));
        }

        builder = builder.where("Server", "=", IntegerStatementValue.of(Core.getCurrentServer()))
                .where("Season", "=", IntegerStatementValue.of(Core.getCurrentSeason()))
                .where("World", "=", StringStatementValue.of(playerLocation.getWorld().getName()))
                .where("BlockX", ">=", IntegerStatementValue.of(playerLocation.getBlockX() - finalRadius))
                .where("BlockX", "<=", IntegerStatementValue.of(playerLocation.getBlockX() + finalRadius))
                .where("BlockY", ">=", IntegerStatementValue.of(playerLocation.getBlockY() - finalRadius))
                .where("BlockY", "<=", IntegerStatementValue.of(playerLocation.getBlockY() + finalRadius))
                .where("BlockZ", ">=", IntegerStatementValue.of(playerLocation.getBlockZ() - finalRadius))
                .where("BlockZ", "<=", IntegerStatementValue.of(playerLocation.getBlockZ() + finalRadius))
                .whereOrSameColumn("Action", "=", List.of(StringStatementValue.of(WorldLogAction.BLOCK_PLACE.name()),
                        StringStatementValue.of(WorldLogAction.BLOCK_BREAK.name())))
                .where("Time", ">=", new TimestampStatementValue(timeThreshold))
                .orderBy("Time", Statement.SortOrder.DESCENDING);


        Statement statement = builder.build();

        database.executeQuery(statement, TargetDatabase.GLOBAL).thenAccept(resultSet -> {
           try {
               List<WorldLog> worldLogs = new ArrayList<>();
               while(resultSet.next()) {
                   String world = resultSet.getString(3);
                   int x = resultSet.getInt(4);
                   int y = resultSet.getInt(5);
                   int z = resultSet.getInt(6);
                   String action = resultSet.getString(7);
                   String material = resultSet.getString(8);

                   WorldLog log = WorldLog.builder().world(world).blockX(x).blockY(y).blockZ(z).action(WorldLogAction.valueOf(action)).material(material).build();
                   worldLogs.add(log);
               }

               resultSet.close();

               if (worldLogs.isEmpty()) {
                   UtilMessage.message(player, "World Logger", "No blocks found to roll back.");
                   return;
               }

               // Process the rollback
               processRollback(worldLogs);

               UtilMessage.message(player, "World Logger", "Rollback completed.");

           } catch (SQLException e) {
               throw new RuntimeException(e);
           }
        }).exceptionally(ex -> {
            UtilMessage.simpleMessage(player, "World Logger", "Failed to rollback blocks");
            log.error("Failed to rollback blocks", ex).submit();
            return null;
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
