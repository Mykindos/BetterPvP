package me.mykindos.betterpvp.core.client.rewards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.mineplex.MineplexMessage;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageSentEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
public class RewardsTestCommand extends Command {

    private final ClientSQLLayer clientSQLLayer;
    private final ItemHandler itemHandler;

    @Inject
    public RewardsTestCommand(ClientSQLLayer clientSQLLayer, ItemHandler itemHandler) {
        this.clientSQLLayer = clientSQLLayer;
        this.itemHandler = itemHandler;
    }

    @Override
    public String getName() {
        return "rewardstest";
    }

    @Override
    public String getDescription() {
        return "Open the rewards menu";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        ItemStack itemStack = itemHandler.getItem("dungeons:dungeon_token").getItemStack();
        CompletableFuture.runAsync(() -> {
            if (Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
                UtilServer.callEvent(new MineplexMessageSentEvent("BetterPvP", MineplexMessage.builder()
                        .channel("ChampionsWinsReward").message("TEST").metadata("uuid", player.getUniqueId().toString()).build()));
            } else {
                RewardBox rewardBox = clientSQLLayer.getRewardBox(client);
                rewardBox.getContents().add(itemStack);

                clientSQLLayer.updateClientRewards(client, rewardBox);
            }
        }).exceptionally(ex -> {
            log.info("Error while executing rewards command", ex).submit();
            return null;
        });
    }
}
