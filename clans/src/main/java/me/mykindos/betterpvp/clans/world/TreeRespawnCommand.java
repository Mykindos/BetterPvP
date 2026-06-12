package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        return "clans.command.tree-respawn.description";
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
            UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.no-world");
            return;
        }

        // /treerespawn status
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.status",
                    Component.text(treeRespawnManager.getQueueSize(), NamedTextColor.YELLOW),
                    Component.text(treeRespawnManager.isEnabled(), NamedTextColor.YELLOW));
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
                UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.usage");
                return;
            }

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.chunk-not-loaded",
                        Component.text(chunkX, NamedTextColor.YELLOW), Component.text(chunkZ, NamedTextColor.YELLOW));
                return;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            queueAndReport(player, chunk);
            return;
        }

        // /treerespawn  (no args – use the player's current chunk)
        if (args.length == 0) {
            if (!player.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
                UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.not-main-world");
                return;
            }
            queueAndReport(player, player.getChunk());
            return;
        }

        UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.usage");
    }

    private void queueAndReport(Player player, Chunk chunk) {
        int sizeBefore = treeRespawnManager.getQueueSize();
        treeRespawnManager.queueChunk(chunk);
        int sizeAfter = treeRespawnManager.getQueueSize();

        if (sizeAfter > sizeBefore) {
            UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.queued",
                    Component.text(chunk.getX(), NamedTextColor.YELLOW), Component.text(chunk.getZ(), NamedTextColor.YELLOW),
                    Component.text(sizeAfter, NamedTextColor.YELLOW));
        } else {
            UtilMessage.message(player, "clans.prefix", "clans.command.tree-respawn.not-queued",
                    Component.text(chunk.getX(), NamedTextColor.YELLOW), Component.text(chunk.getZ(), NamedTextColor.YELLOW));
        }
    }
}


