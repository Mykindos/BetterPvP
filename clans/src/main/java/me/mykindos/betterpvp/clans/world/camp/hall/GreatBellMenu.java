package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.upgrade.GreatBell;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** The Great bell's page: whether its lift is still on the settlers, when it can ring again, and ringing it. */
public class GreatBellMenu extends AbstractGui implements Windowed {

    GreatBellMenu(@NotNull HallMenus menus, @NotNull GreatBell bell, @NotNull SiteKey key,
                  @NotNull PlacedStructure hall, @Nullable Windowed previous) {
        super(9, 3);

        final List<Component> status = new ArrayList<>();
        final long lift = bell.liftLeft(key);
        if (lift > 0) {
            status.add(Translations.component("clans.camp.upgrade.great_bell.lifted",
                    Component.text("+" + bell.bonus(), NamedTextColor.GREEN), time(lift)).color(NamedTextColor.GREEN));
        }
        final long ready = bell.readyIn(key);
        status.add(ready > 0
                ? Translations.component("clans.camp.upgrade.great_bell.cooldown", time(ready))
                .color(NamedTextColor.RED)
                : Translations.component("clans.camp.upgrade.great_bell.ready").color(NamedTextColor.GREEN));
        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.BELL)
                .displayName(Translations.component("clans.camp.upgrade.great_bell.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(status)
                .build()));

        setItem(13, new SimpleItem(ItemView.builder()
                .material(ready > 0 ? Material.GRAY_DYE : Material.BELL)
                .displayName(Translations.component("clans.camp.upgrade.great_bell.ring.name")
                        .color(ready > 0 ? NamedTextColor.GRAY : NamedTextColor.YELLOW))
                .frameLore(true)
                .lore(Translations.component("clans.camp.upgrade.great_bell.ring.description",
                        Component.text("+" + bell.bonus(), NamedTextColor.GREEN), time(bell.length().toMillis()))
                        .color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.great_bell.ring.action"))
                .build(), click -> {
                    final Player player = click.getPlayer();
                    final String problem = bell.ring(player, key, hall);
                    if (problem != null) {
                        menus.tell(player, problem);
                    }
                    new GreatBellMenu(menus, bell, key, hall, previous).show(player);
                }));

        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull Component time(long millis) {
        return Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(1000, millis))),
                NamedTextColor.WHITE);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.great_bell.name");
    }
}
