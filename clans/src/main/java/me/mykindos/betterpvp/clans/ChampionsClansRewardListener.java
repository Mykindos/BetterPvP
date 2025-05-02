package me.mykindos.betterpvp.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@BPvPListener
public class ChampionsClansRewardListener implements Listener {

    private final ClientManager clientManager;
    private final ItemHandler itemHandler;

    @Inject
    public ChampionsClansRewardListener(ClientManager clientManager, ItemHandler itemHandler) {
        this.clientManager = clientManager;
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onChampionsClansReward(MineplexMessageReceivedEvent event) {
        if(!event.getMessage().getChannel().equalsIgnoreCase("ChampionsWinsReward")) return;
        if(!event.getMessage().getMetadata().containsKey("uuid")) return;

        CompletableFuture<Optional<Client>> uuid = clientManager.search().offline(UUID.fromString(event.getMessage().getMetadata().get("uuid")));
        uuid.thenAcceptAsync(clientOptional -> {
            if(clientOptional.isPresent()) {
                Client client = clientOptional.get();
                RewardBox rewardBox = clientManager.getSqlLayer().getRewardBox(client);
                rewardBox.getContents().add(itemHandler.getItem("dungeons:dungeon_token").getItemStack());
                clientManager.getSqlLayer().updateClientRewards(client, rewardBox);
            }
        });
    }
}
