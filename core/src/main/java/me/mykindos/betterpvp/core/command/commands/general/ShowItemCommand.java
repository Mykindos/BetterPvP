package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.renderer.LorePages;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class ShowItemCommand extends Command {

    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final Core core;

    @Inject
    public ShowItemCommand(ItemFactory itemFactory, CooldownManager cooldownManager, Core core) {
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.core = core;
    }

    @Override
    public String getName() {
        return "showitem";
    }

    @Override
    public String getDescription() {
        return "Sends the current held item to chat";
    }

    @Override
    public List<String> processTabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(player.getInventory().getItemInMainHand());
            if (instanceOpt.isPresent()) {
                final int pages = LorePages.visiblePages(instanceOpt.get()).size();
                final List<String> completions = new ArrayList<>();
                for (int page = 1; page <= pages; page++) {
                    final String value = String.valueOf(page);
                    if (value.startsWith(args[0])) {
                        completions.add(value);
                    }
                }
                return completions;
            }
        }
        return super.processTabComplete(sender, args);
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!cooldownManager.use(player, getName(), 15d, false, true)){
            UtilMessage.message(player, "Command", "You must wait some time between using this command again");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(itemStack);
        final Component name;
        if (instanceOpt.isPresent()) {
            final ItemInstance instance = instanceOpt.get();
            name = instance.getBaseItem().getItemNameRenderer().createName(instance);

            final int page;
            if (args.length > 0) {
                final List<Integer> visiblePages = LorePages.visiblePages(instance);
                try {
                    // 1-based argument, clamped onto the item's visible pages
                    final int parsedPage = Integer.parseInt(args[0]) - 1;
                    page = visiblePages.get(parsedPage);
                } catch (NumberFormatException e) {
                    UtilMessage.message(player, "Command", "Page must be a number.");
                    return;
                } catch (IndexOutOfBoundsException e) {
                    UtilMessage.message(player, "Command", "Page must be between 1 and " + visiblePages.size() + ".");
                    return;
                }
            } else {
                page = LorePages.mostRelevant(instance);
            }
            itemStack = instance.getView().get(null, page);
        } else {
            if (itemStack.getItemMeta().hasDisplayName()) {
                name = Objects.requireNonNull(itemStack.getItemMeta().displayName());
            } else {
                name = Objects.requireNonNullElse(itemStack.getData(DataComponentTypes.ITEM_NAME),
                        Component.translatable(itemStack.getType().translationKey()));
            }
        }

        Component messageComponent = Component.text("I am currently holding [", NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false)
                .append(Objects.requireNonNull(name).hoverEvent(itemStack.asHoverEvent()))
                .append(Component.text("]", NamedTextColor.WHITE));
        UtilServer.runTaskAsync(core, () -> UtilServer.callEvent(new ChatSentEvent(player, client.getGamer().getChatChannel(), UtilMessage.deserialize("<yellow>%s:</yellow>"), messageComponent)));    }
}
