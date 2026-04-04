package me.mykindos.betterpvp.core.client.rewards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.server.CrossServerMessageService;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
public class RewardsTestCommand extends Command {

    private final ClientSQLLayer clientSQLLayer;
    private final ItemFactory itemFactory;
    private final CrossServerMessageService crossServerMessageService;

    @Inject
    public RewardsTestCommand(ClientSQLLayer clientSQLLayer, ItemFactory itemFactory,
                              CrossServerMessageService crossServerMessageService) {
        this.clientSQLLayer = clientSQLLayer;
        this.itemFactory = itemFactory;
        this.crossServerMessageService = crossServerMessageService;
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
        final BaseItem baseItem = Objects.requireNonNull(itemFactory.getItemRegistry().getItem("dungeons:dungeon_token"));
        ItemStack itemStack = itemFactory.create(baseItem).createItemStack();
        CompletableFuture.runAsync(() -> {
            crossServerMessageService.broadcast(ServerMessage.builder()
                    .channel("ChampionsWinsReward")
                    .message("TEST")
                    .metadata("uuid", player.getUniqueId().toString())
                    .build());

            RewardBox rewardBox = clientSQLLayer.getRewardBox(client);
            rewardBox.getContents().add(itemStack);
            clientSQLLayer.updateClientRewards(client, rewardBox);
        }).exceptionally(ex -> {
            log.info("Error while executing rewards command", ex).submit();
            return null;
        });
    }
}
