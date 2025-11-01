package me.mykindos.betterpvp.core.client.rewards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@CustomLog
public class RewardsCommand extends Command {

    private final Core core;
    private final ClientSQLLayer clientSQLLayer;
    private final ItemHandler itemHandler;
    private final CooldownManager cooldownManager;

    @Inject
    public RewardsCommand(Core core, ClientSQLLayer clientSQLLayer, ItemHandler itemHandler, CooldownManager cooldownManager) {
        this.core = core;
        this.clientSQLLayer = clientSQLLayer;
        this.itemHandler = itemHandler;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "rewards";
    }

    @Override
    public String getDescription() {
        return "Open the rewards menu";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(cooldownManager.use(player, "Rewards", 60 * 5, true, false)) {
            CompletableFuture.runAsync(() -> {
                RewardBox rewardBox = clientSQLLayer.getRewardBox(client.getUniqueId());

                UtilServer.runTask(core, () -> {
                    new GuiRewardBox(rewardBox, itemHandler, null).show(player).addCloseHandler(() -> {
                        JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientSQLLayer.class).updateClientRewards(client.getUniqueId(), rewardBox)
                                .whenComplete((unused, throwable) -> {
                                    if (throwable != null) {
                                        log.error("Failed to update client rewards for {}", throwable, client.getName());
                                        return;
                                    }

                                    rewardBox.setLocked(false);
                                });
                    });
                });
            });
        } else {
            UtilMessage.simpleMessage(player, "Rewards", "You have checked your rewards recently, please wait a bit before checking again.");
        }
    }
}
