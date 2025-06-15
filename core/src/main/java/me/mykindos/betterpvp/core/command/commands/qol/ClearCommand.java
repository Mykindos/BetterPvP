package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.UUIDProperty;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Singleton
@CustomLog
public class ClearCommand extends Command {

    private final ItemFactory itemFactory;
    private final ItemRegistry registry;
    private final ClientManager clientManager;

    @Inject
    public ClearCommand(ItemFactory itemFactory, ItemRegistry registry, ClientManager clientManager) {
        this.itemFactory = itemFactory;
        this.registry = registry;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear your inventory";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            doClear(player, player);
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                doClear(player, target);
            }
        } else {
            UtilMessage.message(player, "CLear", "Usage: /clear [name], use /minecraft:clear for advanced usage");
        }
    }

    private Map<ItemInstance, UUID> getUUIDItems(Player target) {
        final List<ItemInstance> items = itemFactory.fromArray(target.getInventory().getContents());
        final Map<ItemInstance, UUID> map = new HashMap<>();
        for (ItemInstance item : items) {
            final Optional<UUIDProperty> uuidCompt = item.getComponent(UUIDProperty.class);
            uuidCompt.ifPresent(uuidProperty -> map.put(item, uuidProperty.getUniqueId()));
        }
        return map;
    }

    private void doClear(Player runner, Player target) {
        Map<ItemInstance, UUID> uuidItems = getUUIDItems(target);
        Component successFeedback = UtilMessage.deserialize("<yellow>%s</yellow> cleared <yellow>%s</yellow>'s inventory",
                runner.getName(), target.getName());
        if (uuidItems.isEmpty()) {
            target.getInventory().clear();
            clientManager.sendMessageToRank("Clear", successFeedback, Rank.TRIAL_MOD);
            return;
        }

        new ConfirmationMenu("Inventory has UUIDItems, confirm clear", (success) -> {
            if (Boolean.TRUE.equals(success)) {
                getUUIDItems(target).forEach((item, uuid) -> {
                    log.info("{} cleared ({}) from {}'s inventory",
                            runner.getName(), uuid, target.getName())
                            .setAction("ITEM_CLEAR")
                            .addClientContext(runner)
                            .addItemContext(registry, item)
                            .addClientContext(target, true)
                            .submit();
                });
                target.getInventory().clear();
                clientManager.sendMessageToRank("Clear", successFeedback, Rank.TRIAL_MOD);
            }
        }).show(runner);
    }

    @Override
    public String getArgumentType(int args) {
        if (args == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }
}
