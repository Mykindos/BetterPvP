package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
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

    private final ClientManager clientManager;

    public PunishmentMenu(Client target, String reason, ClientManager clientManager, RuleManager ruleManager, Windowed previous) {
        super(9, 5);

        this.target = target;
        this.reason = reason;
        this.clientManager = clientManager;

        List<Item> hackingItems = ruleManager.getApplyPunishmentItemList("hacking", target, reason);

        ItemProvider hackingProvider = ItemView.builder()
                .displayName(Component.text("Hacking"))
                .material(Material.IRON_SWORD)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
        SimpleItem hackingItem = new SimpleItem(hackingProvider, click -> {
            new ViewCollectionMenu("Hacking", hackingItems, this).show(click.getPlayer());
        });

        List<Item> gameplayItems = ruleManager.getApplyPunishmentItemList("gameplay", target, reason);

        ItemProvider gameplayProvider = ItemView.builder()
                .displayName(Component.text("gameplay"))
                .material(Material.ANVIL)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
        SimpleItem gameplayItem = new SimpleItem(gameplayProvider, click -> {
            new ViewCollectionMenu("Gameplay", gameplayItems, this).show(click.getPlayer());
        });

        List<Item> chatItems = ruleManager.getApplyPunishmentItemList("chat", target, reason);

        ItemProvider chatProvider = ItemView.builder()
                .displayName(Component.text("Chat"))
                .material(Material.WRITABLE_BOOK)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
        SimpleItem chatItem = new SimpleItem(chatProvider, click -> {
            new ViewCollectionMenu("Chat", chatItems, this).show(click.getPlayer());
        });

        List<Item> otherItems = ruleManager.getApplyPunishmentItemList("other", target, reason);

        ItemProvider otherProvider = ItemView.builder()
                .displayName(Component.text("Other"))
                .material(Material.PAPER)
                .customModelData(1)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();
        SimpleItem otherItem = new SimpleItem(otherProvider, click -> {
            new ViewCollectionMenu("Chat", otherItems, this).show(click.getPlayer());
        });


        Structure structure = new Structure("XXXXXXXXX",
                "XHXGXCXOX",
                "XXXXXXXXX",
                "XPPPPPPPX",
                "XXXXBXXXX")
                .addIngredient('X', Menu.BACKGROUND_ITEM)
                .addIngredient('B', new BackButton(previous))
                .addIngredient('H', hackingItem)
                .addIngredient('G', gameplayItem)
                .addIngredient('C', chatItem)
                .addIngredient('O', otherItem);

        applyStructure(structure);



    }

    private void setPunishments() {
        List<Item> punishments = target.getPunishments().stream()
                .sorted(Comparator.comparingLong(Punishment::getExpiryTime).reversed())
                .map(punishment -> new PunishmentItem(punishment, clientManager, reason, this))
                .map(Item.class::cast).toList();

        for (int i = 0; i < 7; i++) {
            try {
                setItem(i + 1, 4, punishments.get(i));
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
        return Component.text("Rules");
    }
}
