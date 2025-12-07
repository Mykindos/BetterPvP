package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class PunishmentMenu extends AbstractGui implements Windowed {

    private final Client target;
    private final String reason;
    private final PunishmentHandler punishmentHandler;

    public PunishmentMenu(Client punisher, Client target, String reason, PunishmentHandler punishmentHandler, Windowed previous) {
        super(9, 5);

        this.target = target;
        this.reason = reason;
        this.punishmentHandler = punishmentHandler;

        List<Item> hackingItems = punishmentHandler.getApplyPunishmentItemList(punisher, "hacking", target, reason, this);

        ItemProvider hackingProvider = ItemView.builder()
                .displayName(Component.text("Hacking"))
                .material(Material.IRON_SWORD)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem hackingItem = new SimpleItem(hackingProvider, click -> {
            new ViewCollectionMenu("Hacking", hackingItems, this).show(click.getPlayer());
        });

        List<Item> gameplayItems = punishmentHandler.getApplyPunishmentItemList(punisher, "gameplay", target, reason, this);

        ItemProvider gameplayProvider = ItemView.builder()
                .displayName(Component.text("Gameplay"))
                .material(Material.ANVIL)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem gameplayItem = new SimpleItem(gameplayProvider, click -> {
            new ViewCollectionMenu("Gameplay", gameplayItems, this).show(click.getPlayer());
        });

        List<Item> chatItems = punishmentHandler.getApplyPunishmentItemList(punisher, "chat", target, reason, this);

        ItemProvider chatProvider = ItemView.builder()
                .displayName(Component.text("Chat"))
                .material(Material.WRITABLE_BOOK)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem chatItem = new SimpleItem(chatProvider, click -> {
            new ViewCollectionMenu("Chat", chatItems, this).show(click.getPlayer());
        });

        List<Item> otherItems = punishmentHandler.getApplyPunishmentItemList(punisher, "other", target, reason, this);

        ItemProvider otherProvider = ItemView.builder()
                .displayName(Component.text("Other"))
                .material(Material.PAPER)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem otherItem = new SimpleItem(otherProvider, click -> {
            new ViewCollectionMenu("Chat", otherItems, this).show(click.getPlayer());
        });

        List<Item> punishmentItems = target.getPunishments().stream()
                .sorted(Comparator.comparingLong(Punishment::getApplyTime).reversed())
                .sorted(Comparator.comparing(Punishment::isActive).reversed())
                .map(punishment -> new PunishmentItem(punishment, punishmentHandler, reason, this))
                .map(Item.class::cast).toList();

        ItemProvider historyProvider = ItemView.builder()
                .displayName(Component.text("Full Punish History"))
                .material(Material.ENCHANTING_TABLE)
                .customModelData(1)
                .lore(UtilMessage.deserialize("<white>Click</white> to view full punish history"))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();

        SimpleItem historyItem = new SimpleItem(historyProvider, click -> {
            new ViewCollectionMenu(target.getName() + "'s Punishment History", punishmentItems, this).show(click.getPlayer());
        });

        Structure structure = new Structure("XXXXXXXXX",
                "XHXGXCXOX",
                "XXXXXXXXX",
                "XPPPPPPPX",
                "XXXXBXXXR")
                .addIngredient('X', Menu.BACKGROUND_ITEM)
                .addIngredient('B', new BackButton(previous))
                .addIngredient('H', hackingItem)
                .addIngredient('G', gameplayItem)
                .addIngredient('C', chatItem)
                .addIngredient('O', otherItem)
                .addIngredient('R', historyItem);

        applyStructure(structure);

        for (int i = 0; i < 7; i++) {
            try {
                setItem(i + 1, 3, punishmentItems.get(i));
            } catch (IndexOutOfBoundsException ignored) {
                break;
            }
        }
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(target.getName() + "'s Punishment Menu");
    }
}
