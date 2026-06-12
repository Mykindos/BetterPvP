package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                .displayName(Translations.component("core.menu.punishment.button.hacking.name"))
                .material(Material.IRON_SWORD)
                .customModelData(0)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem hackingItem = new SimpleItem(hackingProvider, click -> {
            new ViewCollectionMenu("Hacking", hackingItems, this).show(click.getPlayer());
        });

        List<Item> gameplayItems = punishmentHandler.getApplyPunishmentItemList(punisher, "gameplay", target, reason, this);

        ItemProvider gameplayProvider = ItemView.builder()
                .displayName(Translations.component("core.menu.punishment.button.gameplay.name"))
                .material(Material.ANVIL)
                .customModelData(0)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem gameplayItem = new SimpleItem(gameplayProvider, click -> {
            new ViewCollectionMenu("Gameplay", gameplayItems, this).show(click.getPlayer());
        });

        List<Item> chatItems = punishmentHandler.getApplyPunishmentItemList(punisher, "chat", target, reason, this);

        ItemProvider chatProvider = ItemView.builder()
                .displayName(Translations.component("core.menu.punishment.button.chat.name"))
                .material(Material.WRITABLE_BOOK)
                .customModelData(0)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem chatItem = new SimpleItem(chatProvider, click -> {
            new ViewCollectionMenu("Chat", chatItems, this).show(click.getPlayer());
        });

        List<Item> otherItems = punishmentHandler.getApplyPunishmentItemList(punisher, "other", target, reason, this);

        ItemProvider otherProvider = ItemView.builder()
                .displayName(Translations.component("core.menu.punishment.button.other.name"))
                .material(Material.PAPER)
                .customModelData(0)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
        SimpleItem otherItem = new SimpleItem(otherProvider, click -> {
            new ViewCollectionMenu("Other", otherItems, this).show(click.getPlayer());
        });

        List<Item> punishmentItems = target.getPunishments().stream()
                .sorted(Comparator.comparingLong(Punishment::getApplyTime).reversed())
                .sorted(Comparator.comparing(Punishment::isActive).reversed())
                .map(punishment -> new PunishmentItem(
                        punishment,
                        punishmentHandler,
                        punishment.getPunisher() != null ? punishment.getPunisher().toString() : null,
                        punishment.getRevoker() != null ? punishment.getRevoker().toString() : null,
                        true,
                        reason,
                        this))
                .map(Item.class::cast).toList();

        ItemProvider historyProvider = ItemView.builder()
                .displayName(Translations.component("core.menu.punishment.button.history.name"))
                .material(Material.ENCHANTING_TABLE)
                .customModelData(1)
                .lore(Translations.component("core.menu.punishment.button.history.lore.1").color(NamedTextColor.WHITE))
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
        return Translations.component("core.menu.punishment.title", Component.text(target.getName()));
    }
}
