package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RuleMenu extends AbstractGui implements Windowed {

    public RuleMenu(RuleManager ruleManager, Windowed previous) {
        super(9, 3);

        List<Item> hackingItems = ruleManager.getObjects().values().stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase("hacking"))
                .map(RuleItem::new)
                .map(Item.class::cast)
                .toList();
        ItemProvider hackingProvider = ItemView.builder()
                .displayName(Component.text("Hacking"))
                .material(Material.IRON_SWORD)
                .customModelData(1)
                .build();
        SimpleItem hackingItem = new SimpleItem(hackingProvider, click -> {
            new ViewCollectionMenu("Hacking", hackingItems, this).show(click.getPlayer());
        });

        List<Item> gameplayItems = ruleManager.getObjects().values().stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase("gameplay"))
                .map(RuleItem::new)
                .map(Item.class::cast)
                .toList();
        ItemProvider gameplayProvider = ItemView.builder()
                .displayName(Component.text("Gameplay"))
                .material(Material.ANVIL)
                .customModelData(1)
                .build();
        SimpleItem gameplayItem = new SimpleItem(gameplayProvider, click -> {
            new ViewCollectionMenu("Gameplay", gameplayItems, this).show(click.getPlayer());
        });

        List<Item> chatItems = ruleManager.getObjects().values().stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase("chat"))
                .map(RuleItem::new)
                .map(Item.class::cast)
                .toList();
        ItemProvider chatProvider = ItemView.builder()
                .displayName(Component.text("Chat"))
                .material(Material.WRITABLE_BOOK)
                .customModelData(1)
                .build();
        SimpleItem chatItem = new SimpleItem(chatProvider, click -> {
            new ViewCollectionMenu("Chat", chatItems, this).show(click.getPlayer());
        });

        List<Item> otherItems = ruleManager.getObjects().values().stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase("other"))
                .map(RuleItem::new)
                .map(Item.class::cast)
                .toList();
        ItemProvider otherProvider = ItemView.builder()
                .displayName(Component.text("Other"))
                .material(Material.PAPER)
                .customModelData(1)
                .build();
        SimpleItem otherItem = new SimpleItem(otherProvider, click -> {
            new ViewCollectionMenu("Chat", otherItems, this).show(click.getPlayer());
        });


        Structure structure = new Structure("XXXXXXXXX",
                "XHXGXCXOX",
                "XXXXBXXXX")
                .addIngredient('X', Menu.BACKGROUND_ITEM)
                .addIngredient('B', new BackButton(previous))
                .addIngredient('H', hackingItem)
                .addIngredient('G', gameplayItem)
                .addIngredient('C', chatItem)
                .addIngredient('O', otherItem);

        applyStructure(structure);
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text("Rules");
    }
}
