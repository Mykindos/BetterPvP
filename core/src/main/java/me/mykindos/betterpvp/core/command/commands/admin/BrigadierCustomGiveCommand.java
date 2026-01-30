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
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@CustomLog
public class BrigadierCustomGiveCommand extends BrigadierCommand {
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    @Inject
    public BrigadierCustomGiveCommand(ClientManager clientManager, ItemFactory itemFactory, ItemRegistry itemRegistry) {
        super(clientManager);
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public String getName() {
        return "give";
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
                        .then(IBrigadierCommand.argument("item", BPvPArgumentTypes.customItemType())
                                .executes(context -> {
                                    final Player player = getPlayerFromSender(context);
                                    final List<Player> targets = context.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                    final BaseItem item = context.getArgument("item", BaseItem.class);
                                    doGive(player, targets, item, 1);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(IBrigadierCommand.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            final Player player = getPlayerFromSender(context);
                                            final List<Player> targets = context.getArgument("players", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                            final BaseItem item = context.getArgument("item", BaseItem.class);
                                            final Integer amount = context.getArgument("amount", Integer.class);
                                            doGive(player, targets, item, amount);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                );
    }

    private void doGive(final @NotNull Player giver, final @NotNull List<Player> targets, final @NotNull BaseItem baseItem, final int amount) {
        //todo handle more than 1 stack per player
        for (Player target : targets) {
            int toGive = amount;
            while (toGive > 0) {
                final ItemInstance instance = itemFactory.create(baseItem);
                final ItemStack itemStack = instance.createItemStack();
                final int stackSize = itemStack.getMaxStackSize();
                int giveAmount = Math.min(toGive, stackSize);
                itemStack.setAmount(giveAmount);
                UtilItem.insert(target, itemStack);

                // Give Uncommon+ rarities a UUID
                if (instance.getRarity().isImportant()) {
                    Optional<UUIDProperty> component = instance.getComponent(UUIDProperty.class);
                    component.ifPresent(uuidProperty ->
                            log.info("{} spawned and gave ({}) to {}", giver.getName(), uuidProperty.getUniqueId(), target.getName())
                                    .setAction("ITEM_SPAWN")
                                    .addClientContext(giver)
                                    .addClientContext(target, true)
                                    .addItemContext(itemRegistry, instance)
                                    .submit());
                }

                toGive -= giveAmount;
            }
        }
        doFeedback(giver, targets, baseItem, amount);
    }

    private void doFeedback(final @NotNull Player giver, final @NotNull List<Player> targets, final @NotNull BaseItem item, final int amount) {
        Component message;
        final NamespacedKey key = Objects.requireNonNull(itemRegistry.getKey(item));
        if (targets.size() <= 3 && !targets.isEmpty()) {
            List<String> names = targets.stream()
                    .map(target -> "<yellow>" + target.getName() + "</yellow>")
                    .toList();
            message = UtilMessage.deserialize("<yellow>%s</yellow> gave %s [<green>%s</green>] x<green>%s</green>",
                    giver.getName(), UtilFormat.getConjunctiveString(names), key.toString(), amount);

        } else {
            message = UtilMessage.deserialize("<yellow>%s</yellow> gave <green>%s</green> players [<green>%s</green>] x<green>%s</green>",
                    giver.getName(), targets.size(), key.toString(), amount);
        }
        clientManager.sendMessageToRank("Core", message, Rank.HELPER);
    }


}
