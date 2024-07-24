package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.inventory.gui.TabGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.window.Window;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewEnemiesButton extends ViewClanCollectionButton {

    private final Clan clan;
    private final Clan viewerClan;

    public ViewEnemiesButton(Clan clan, Windowed parent, Clan viewerClan) {
        super(ItemView.builder().material(Material.PAPER).customModelData(7).build(),
                "Enemies", parent);
        this.clan = clan;
        this.viewerClan = viewerClan;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

        HashMap<Clan, ClanEnemy> enemies = new HashMap<>();
        for(ClanEnemy enemy : clan.getEnemies()){
            Clan enemyClan = (Clan) enemy.getClan();
            Optional<ClanEnemy> clanEnemyOptional = enemyClan.getEnemy(clan);
            if(clanEnemyOptional.isPresent()) {
                ClanEnemy clanEnemy = clanEnemyOptional.get();
                if(clanEnemy.getDominance() > 0) {
                    enemies.put((Clan) enemy.getClan(), clanEnemy);
                }
            }
        }

        final List<Item> negativeEnemies = new ArrayList<>();

        // Sort hashmap by comparator
        LinkedHashMap<Clan, ClanEnemy> sortedMap = enemies.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingDouble(ClanEnemy::getDominance).reversed()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));

        sortedMap.forEach((key, value) -> {
            final List<Component> lore = List.of(Component.text("Dominance: ", NamedTextColor.GRAY)
                    .append(Component.text("-" + value.getDominance(), NamedTextColor.RED)));
            negativeEnemies.add(new ViewClanButton(viewerClan, key, lore));
        });


        // Sort positive from biggest to smallest
        Comparator<ClanEnemy> positiveComparator = Comparator.comparingDouble(ClanEnemy::getDominance).reversed();
        final List<Item> positiveEnemies = clan.getEnemies().stream().sorted(positiveComparator).filter(enemy -> enemy.getDominance() > 0).map(enemy -> {
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
