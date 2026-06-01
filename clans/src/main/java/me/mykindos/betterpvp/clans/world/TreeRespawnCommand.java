package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Admin command for manually queuing chunks for tree-respawn evaluation.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code /treerespawn} – queues the chunk the sender is currently standing in.</li>
 *   <li>{@code /treerespawn <chunkX> <chunkZ>} – queues a specific chunk by its chunk
 *       coordinates (the chunk must already be loaded).</li>
 *   <li>{@code /treerespawn status} – shows the current queue size.</li>
 * </ul>
 *
 * <p>Required rank defaults to {@code ADMIN} (set in {@code command.treerespawn.requiredRank}
 * inside the Clans plugin config).
 */
@Singleton
public class TreeRespawnCommand extends Command {

    private final TreeRespawnManager treeRespawnManager;

    @Inject
    public TreeRespawnCommand(TreeRespawnManager treeRespawnManager) {
        this.treeRespawnManager = treeRespawnManager;
    }

    @Override
    public String getName() {
        return "treerespawn";
    }

    @Override
    public String getDescription() {
        return "Queues a chunk for tree-respawn evaluation";
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) return "POSITION_X"; // chunkX suggestion
        if (argCount == 2) return "POSITION_Z"; // chunkZ suggestion
        return ICommand.ArgumentType.NONE.name();
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
        if (world == null) {
            UtilMessage.simpleMessage(player, "TreeRespawn", "Main world not found.");
            return;
        }

        // /treerespawn status
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            UtilMessage.simpleMessage(player, "TreeRespawn",
                    "Queue size: <yellow>%d</yellow> | Enabled: <yellow>%s</yellow>",
                    treeRespawnManager.getQueueSize(),
                    treeRespawnManager.isEnabled());
            return;
        }

        // /treerespawn <chunkX> <chunkZ>
        if (args.length == 2) {
            int chunkX;
            int chunkZ;
            try {
                chunkX = Integer.parseInt(args[0]);
                chunkZ = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                UtilMessage.simpleMessage(player, "TreeRespawn",
                        "Usage: <white>/treerespawn [chunkX chunkZ]</white>");
                return;
            }

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                UtilMessage.simpleMessage(player, "TreeRespawn",
                        "Chunk <yellow>%d, %d</yellow> is not loaded. Only loaded chunks can be queued.",
                        chunkX, chunkZ);
                return;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            queueAndReport(player, chunk);
            return;
        }

        // /treerespawn  (no args – use the player's current chunk)
        if (args.length == 0) {
            if (!player.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
                UtilMessage.simpleMessage(player, "TreeRespawn",
                        "You must be in the main world to queue your current chunk.");
                return;
            }
            queueAndReport(player, player.getChunk());
            return;
        }

        UtilMessage.simpleMessage(player, "TreeRespawn",
                "Usage: <white>/treerespawn [chunkX chunkZ | status]</white>");
    }

    private void queueAndReport(Player player, Chunk chunk) {
        int sizeBefore = treeRespawnManager.getQueueSize();
        treeRespawnManager.queueChunk(chunk);
        int sizeAfter = treeRespawnManager.getQueueSize();

        if (sizeAfter > sizeBefore) {
            UtilMessage.simpleMessage(player, "TreeRespawn",
                    "Chunk <yellow>%d, %d</yellow> queued for tree-respawn evaluation. Queue size: <yellow>%d</yellow>.",
                    chunk.getX(), chunk.getZ(), sizeAfter);
        } else {
            UtilMessage.simpleMessage(player, "TreeRespawn",
                    "Chunk <yellow>%d, %d</yellow> was <red>not queued</red> "
                            + "(already queued, claimed, queue full, or system disabled).",
                    chunk.getX(), chunk.getZ());
        }
    }
}


