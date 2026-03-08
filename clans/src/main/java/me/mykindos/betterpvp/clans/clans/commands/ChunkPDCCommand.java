package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class ChunkPDCCommand extends Command {

    private final ClanManager clanManager;

    @Inject
    public ChunkPDCCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public String getName() {
        return "chunkpdc";
    }

    @Override
    public String getDescription() {
        return "Assigns PDC to all clan chunks";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        AtomicInteger count = new AtomicInteger();
        clanManager.getObjects().values().forEach(clan -> {
            clan.getTerritory().forEach(clanTerritory -> {
                Chunk chunk = clanTerritory.getWorldChunk();
                PersistentDataContainer persistentDataContainer = chunk.getPersistentDataContainer();
                persistentDataContainer.set(ClansNamespacedKeys.CLAN, PersistentDataType.LONG, clan.getId());
                persistentDataContainer.remove(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY);
                count.getAndIncrement();
            });
        });

        UtilMessage.simpleMessage(player, "Command", "Loaded PDC data for <yellow>%d</yellow> chunks", count.get());
    }
}
