package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.TabButton;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.TabGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ViewEnemiesButton extends ViewClanCollectionButton {

    private final Clan clan;
    private final Clan viewerClan;

    public ViewEnemiesButton(Clan clan, Windowed parent, Clan viewerClan) {
        super(ItemView.builder()
                        .material(Material.IRON_SWORD)
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .build(),
                "Enemies",
                parent);
        this.clan = clan;
        this.viewerClan = viewerClan;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Sort negative from smallest to biggest
        Comparator<ClanEnemy> negativeComparator = Comparator.comparingDouble(ClanEnemy::getDominance);
        final List<Item> negativeEnemies = clan.getEnemies().stream().sorted(negativeComparator).filter(enemy -> enemy.getDominance() < 0).map(enemy -> {
            final Clan enemyClan = (Clan) enemy.getClan();
            final List<Component> lore = List.of(Component.text("Dominance: ", NamedTextColor.GRAY)
                    .append(Component.text(enemy.getDominance(), NamedTextColor.RED)));
            return (Item) new ViewClanButton(viewerClan, enemyClan, lore);
        }).toList();

        // Sort positive from biggest to smallest
        Comparator<ClanEnemy> positiveComparator = negativeComparator.reversed();
        final List<Item> positiveEnemies = clan.getEnemies().stream().sorted(positiveComparator).filter(enemy -> enemy.getDominance() >= 0).map(enemy -> {
            final Clan enemyClan = (Clan) enemy.getClan();
            final List<Component> lore = List.of(Component.text("Dominance: ", NamedTextColor.GRAY)
                    .append(Component.text("+" + enemy.getDominance(), NamedTextColor.GREEN)));
            return (Item) new ViewClanButton(viewerClan, enemyClan, lore);
        }).toList();

        final ViewCollectionMenu positiveMenu = new ViewCollectionMenu("Enemies", positiveEnemies, parent);
        final ViewCollectionMenu negativeMenu = new ViewCollectionMenu("Enemies", negativeEnemies, parent);

        ItemView positiveBuilder = ItemView.builder()
                .material(Material.GREEN_CONCRETE)
                .displayName(Component.text("Positive", NamedTextColor.GREEN))
                .lore(Component.text("View " + clan.getName() + "'s positive enemies", NamedTextColor.GRAY))
                .build();

        ItemView negativeBuilder = ItemView.builder()
                .material(Material.RED_CONCRETE)
                .displayName(Component.text("Negative", NamedTextColor.RED))
                .lore(Component.text("View " + clan.getName() + "'s negative enemies", NamedTextColor.GRAY))
                .build();

        final TabGui gui = TabGui.normal()
                .setStructure(
                        "# # # - # + # # #",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('-', new TabButton(0, negativeBuilder, negativeBuilder))
                .addIngredient('+', new TabButton(1, positiveBuilder, positiveBuilder))
                .setTabs(List.of(negativeMenu, positiveMenu))
                .build();

        Window.single().setTitle("Enemies").setGui(gui).setViewer(player).open(player);
    }

    @Override
    protected Collection<Clan> getPool() {
        return clan.getEnemies().stream().map(ClanEnemy::getClan).map(Clan.class::cast).toList();
    }
}
