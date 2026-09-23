package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.menu.CampPermissionsMenu;
import me.mykindos.betterpvp.clans.world.camp.menu.ConstructionMenu;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/** What the Steward offers: settlers, hiring, wages, crews, construction and permissions, each in its own menu. */
public class GreatHallMenu extends AbstractGui implements Windowed {

    GreatHallMenu(@NotNull HallMenus menus, @NotNull Player viewer, @NotNull SiteKey key) {
        super(9, 3);
        setItem(10, entry(Material.PLAYER_HEAD, "settlers",
                player -> new SettlerRosterMenu(menus, key, this, 0, 0).show(player)));
        setItem(11, entry(Material.OAK_SIGN, "hiring",
                player -> new HiringBoardMenu(menus, player, key, this).show(player)));
        setItem(12, entry(Material.GOLD_INGOT, "wages",
                player -> new WageFundMenu(menus, player, key, this).show(player)));
        setItem(14, entry(Material.IRON_PICKAXE, "crews",
                player -> menus.getCrews().openJobs(player, this)));
        setItem(15, entry(Material.CRAFTING_TABLE, "construction",
                player -> new ConstructionMenu(player, key, List.copyOf(menus.getStructures().all()),
                        menus.getConstruction(), menus.getCatalogue(), menus.getBlueprints(), this).show(player)));
        setItem(16, entry(Material.WRITABLE_BOOK, "permissions",
                player -> new CampPermissionsMenu(key.getOwnerId(), menus.getPermissions(),
                        menus.isLeader(player, key), CampPermissionsMenu.Page.CONSTRUCTION, this).show(player)));
        setItem(22, new BackButton(null));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull SimpleItem entry(@NotNull Material icon, @NotNull String id,
                                             @NotNull Consumer<Player> open) {
        return new SimpleItem(ItemView.builder()
                .material(icon)
                .displayName(Translations.component("clans.camp.hall." + id + ".name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(Translations.component("clans.camp.hall." + id + ".description").color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.hall.open"))
                .build(), click -> open.accept(click.getPlayer()));
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.hall.title");
    }
}
