package me.mykindos.betterpvp.core.client.rewards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientSQLLayer;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

@Singleton

public class RewardsCommand extends Command {

    private final Core core;
    private final ClientSQLLayer clientSQLLayer;
    private final ItemHandler itemHandler;

    @Inject
    public RewardsCommand(Core core, ClientSQLLayer clientSQLLayer, ItemHandler itemHandler) {
        this.core = core;
        this.clientSQLLayer = clientSQLLayer;
        this.itemHandler = itemHandler;
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
        CompletableFuture.runAsync(() -> {
            RewardBox rewardBox = clientSQLLayer.getRewardBox(client);

            UtilServer.runTask(core, () -> {
                new GuiRewardBox(rewardBox, itemHandler, null).show(player).addCloseHandler(() -> {
                    JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ClientSQLLayer.class).updateClientRewards(client, rewardBox);
                });
            });
        });
    }
}
