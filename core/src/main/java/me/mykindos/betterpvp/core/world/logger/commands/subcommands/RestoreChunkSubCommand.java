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
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.mykindos.betterpvp.core.database.jooq.Tables.WORLD_LOGS;

@CustomLog
@SubCommand(WorldLoggerCommand.class)
public class RestoreChunkSubCommand extends Command {

    private final Core core;
    private final WorldLogHandler worldLogHandler;
    private final Database database;

    @Inject
    public RestoreChunkSubCommand(Core core, WorldLogHandler worldLogHandler, Database database) {
        this.core = core;
        this.worldLogHandler = worldLogHandler;
        this.database = database;
    }

    @Override
    public String getName() {
        return "restorechunk";
    }

    @Override
    public String getDescription() {
        return "Restore all block changes in the current chunk to their last known state";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Chunk chunk = player.getLocation().getChunk();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;

        UtilMessage.message(player, "World Logger", "Starting chunk restore process for chunk at " + chunk.getX() + ", " + chunk.getZ() + "...");

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            var query = ctx.selectQuery();

            query.addFrom(WORLD_LOGS);
            query.addConditions(WORLD_LOGS.REALM.eq(Core.getCurrentRealm().getId()));
            query.addConditions(WORLD_LOGS.WORLD.eq(player.getWorld().getName()));
            query.addConditions(WORLD_LOGS.BLOCK_X.ge(minX));
            query.addConditions(WORLD_LOGS.BLOCK_X.le(maxX));
            query.addConditions(WORLD_LOGS.BLOCK_Z.ge(minZ));
            query.addConditions(WORLD_LOGS.BLOCK_Z.le(maxZ));
            query.addConditions(WORLD_LOGS.ACTION.in(
                    WorldLogAction.BLOCK_PLACE.name(),
                    WorldLogAction.BLOCK_BREAK.name()
            ));

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
                    UtilMessage.message(player, "World Logger", "No logs found for this chunk.");
                    return;
                }

                // Process the restore
                processRestore(worldLogs);

                UtilMessage.message(player, "World Logger", "Chunk restore completed.");

            } catch (Exception ex) {
                UtilMessage.simpleMessage(player, "World Logger", "Failed to restore chunk");
                log.error("Failed to restore chunk", ex).submit();
            }
        });
    }

    private void processRestore(List<WorldLog> logs) {
        // Group by location to only apply the latest change
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

                    // Ensure chunk is loaded
                    if (!location.getChunk().isLoaded()) {
                        location.getChunk().load();
                    }

                    if (log.getAction().equals(WorldLogAction.BLOCK_PLACE.name())) {
                        if (log.getMaterial() != null) {
                            try {
                                location.getBlock().setType(Material.valueOf(log.getMaterial()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } else if (log.getAction().equals(WorldLogAction.BLOCK_BREAK.name())) {
                        location.getBlock().setType(Material.AIR);
                    }
                }

                if (index >= latestLogs.size()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(core, 0L, 1L);
    }
}
