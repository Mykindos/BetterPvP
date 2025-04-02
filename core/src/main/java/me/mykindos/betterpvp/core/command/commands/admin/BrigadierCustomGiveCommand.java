package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Singleton
@CustomLog
public class BrigadierCustomGiveCommand extends BrigadierCommand {
    private final ItemHandler itemHandler;
    private final UUIDManager uuidManager;
    @Inject
    public BrigadierCustomGiveCommand(ClientManager clientManager, ItemHandler itemHandler, UUIDManager uuidManager) {
        super(clientManager);
        this.itemHandler = itemHandler;
        this.uuidManager = uuidManager;
    }

    @Override
    public String getName() {
        return "customgive";
    }

    @Override
    public String getDescription() {
        return "Give a custom item";
    }

    /**
     * Define the command, using normal rank based permissions
     *
     * @return the builder for the command
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .then(IBrigadierCommand.argument("players", ArgumentTypes.players())
                        .then(IBrigadierCommand.argument("item", BPvPArgumentTypes.bPvPItem())
                                .executes(context -> {
                                    final Player player = getPlayerFromSender(context);
                                    final List<Player> targets = context.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                    final BPvPItem item = context.getArgument("item", BPvPItem.class);
                                    doGive(player, targets, item, 1);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(IBrigadierCommand.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            final Player player = getPlayerFromSender(context);
                                            final List<Player> targets = context.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                            final BPvPItem item = context.getArgument("item", BPvPItem.class);
                                            final Integer amount = context.getArgument("amount", Integer.class);
                                            doGive(player, targets, item, amount);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                );
    }

    private void doGive(final @NotNull Player giver, final @NotNull List<Player> targets, final @NotNull BPvPItem item, int amount) {
        //todo handle more than 1 stack per player
        amount = Math.min(amount, item.getItemStack().getMaxStackSize());
        for (Player target : targets) {
            ItemStack itemStack = item.getItemStack(amount);
            itemHandler.updateNames(itemStack);
            ItemMeta itemMeta = itemStack.getItemMeta();
            UUIDItem uuidItem = null;

            if (itemMeta != null) {
                PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
                if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
                    uuidItem = uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING)))).orElse(null);
                }
            }
            if (uuidItem != null) {
                log.info("{} spawned and gave ({}) to {}", giver.getName(), uuidItem.getUuid(), target.getName())
                        .setAction("ITEM_SPAWN").addClientContext(giver).addClientContext(target, true).addItemContext(uuidItem).submit();

            }
            target.getInventory().addItem(itemStack);
            target.give(itemStack);
        }
        doFeedback(giver, targets, item, amount);
    }

    private void doFeedback(final @NotNull Player giver, final @NotNull List<Player> targets, final @NotNull BPvPItem item, final int amount) {
        Component message;
        if (targets.size() <= 3 && !targets.isEmpty()) {
            List<String> names = targets.stream()
                    .map(target -> "<yellow>" + target.getName() + "</yellow>")
                    .toList();
            message = UtilMessage.deserialize("<yellow>%s</yellow> gave %s [<green>%s</green>] x<green>%s</green>",
                    giver.getName(), UtilFormat.getConjunctiveString(names), item.getIdentifier(), amount);

        } else {
            message = UtilMessage.deserialize("<yellow>%s</yellow> gave <green>%s</green> players [<green>%s</green>] x<green>%s</green>",
                    giver.getName(), targets.size(), item.getIdentifier(), amount);
        }
        clientManager.sendMessageToRank("Core", message, Rank.HELPER);
    }


}
