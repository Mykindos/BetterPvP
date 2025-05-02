package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@BPvPListener
@CustomLog
public class ChampionsClansRewardListener implements Listener {

    private final Clans clans;
    private final ClientManager clientManager;
    private final ItemHandler itemHandler;

    @Inject
    public ChampionsClansRewardListener(Clans clans, ClientManager clientManager, ItemHandler itemHandler) {
        this.clans = clans;
        this.clientManager = clientManager;
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onChampionsClansReward(MineplexMessageReceivedEvent event) {
        if (!event.getMessage().getChannel().equalsIgnoreCase("ChampionsWinsReward")) return;
        if (!event.getMessage().getMetadata().containsKey("uuid")) return;

        CompletableFuture.runAsync(() -> {
            ItemStack itemStack = itemHandler.getItem("dungeons:dungeon_token").getItemStack();

            String uuid = event.getMessage().getMetadata().get("uuid");
            CompletableFuture<Optional<Client>> clientOptionalFuture = clientManager.search().offline(UUID.fromString(uuid));
            clientOptionalFuture.thenAcceptAsync(clientOptional -> {
                if (clientOptional.isPresent()) {
                    Client client = clientOptional.get();
                    RewardBox rewardBox = clientManager.getSqlLayer().getRewardBox(client);
                    rewardBox.getContents().add(itemStack);
                    clientManager.getSqlLayer().updateClientRewards(client, rewardBox);
                }
            }).exceptionally(ex -> {
                log.error("Failed to fetch client for uuid: " + uuid, ex).submit();
                return null;
            });
        }, Bukkit.getScheduler().getMainThreadExecutor(clans)).exceptionally(ex -> {
            log.error("Error while executing champions clans reward listener", ex).submit();
            return null;
        });

    }
}
