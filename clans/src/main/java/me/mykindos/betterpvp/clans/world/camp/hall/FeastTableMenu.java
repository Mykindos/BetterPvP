package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.upgrade.FeastTable;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
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

/** The Feast table's page: whether a feast is laid and for how long, what food is worth, and laying a new one. */
public class FeastTableMenu extends AbstractGui implements Windowed {

    private final HallMenus menus;
    private final FeastTable feast;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    FeastTableMenu(@NotNull HallMenus menus, @NotNull FeastTable feast, @NotNull Player viewer, @NotNull SiteKey key,
                   @Nullable Windowed previous) {
        super(9, 3);
        this.menus = menus;
        this.feast = feast;
        this.key = key;
        this.previous = previous;

        final long left = feast.timeLeft(key);
        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.CAKE)
                .displayName(Translations.component("clans.camp.upgrade.feast_table.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .lore(left > 0
                        ? Translations.component("clans.camp.upgrade.feast_table.laid_for", time(left))
                        .color(NamedTextColor.GREEN)
                        : Translations.component("clans.camp.upgrade.feast_table.none").color(NamedTextColor.GRAY))
                .build()));

        setItem(11, new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .displayName(Translations.component("clans.camp.upgrade.feast_table.values")
                        .color(NamedTextColor.YELLOW))
                .lore(values())
                .build()));

        setItem(15, new SimpleItem(ItemView.builder()
                .material(Material.BREAD)
                .displayName(Translations.component("clans.camp.upgrade.feast_table.lay.name")
                        .color(NamedTextColor.YELLOW))
                .lore(Translations.component("clans.camp.upgrade.feast_table.lay.description",
                        Component.text(feast.cost(), NamedTextColor.WHITE),
                        Component.text("+" + feast.bonus(), NamedTextColor.GREEN),
                        time(feast.length().toMillis())).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.camp.upgrade.feast_table.carried",
                        Component.text(feast.carried(viewer), NamedTextColor.WHITE),
                        Component.text(feast.cost(), NamedTextColor.WHITE)).color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.feast_table.lay.action"))
                .build(), click -> {
                    final Player player = click.getPlayer();
                    final String problem = feast.lay(player, key);
                    if (problem != null) {
                        menus.refuse(player, problem);
                    } else {
                        menus.confirm(player, "clans.camp.upgrade.feast_table.laid",
                                Component.text("+" + feast.bonus(), NamedTextColor.WHITE),
                                time(feast.length().toMillis()));
                    }
                    new FeastTableMenu(menus, feast, player, key, previous).show(player);
                }));

        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull List<Component> values() {
        final List<Component> lines = new ArrayList<>();
        feast.values().forEach((item, points) -> {
            final Material material = Material.matchMaterial(item);
            final Component name = material == null ? Component.text(item)
                    : Component.translatable(material.translationKey());
            lines.add(Translations.component("clans.camp.upgrade.feast_table.value", name.color(NamedTextColor.WHITE),
                    Component.text(points, NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
        });
        return lines;
    }

    private static @NotNull Component time(long millis) {
        return Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(1000, millis))),
                NamedTextColor.WHITE);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.feast_table.name");
    }
}
