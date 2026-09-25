package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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

import java.util.function.BiFunction;

/** The Deputy Steward's page: place it where the member stands, or dismiss it. */
public class DeputyStewardMenu extends AbstractGui implements Windowed {

    DeputyStewardMenu(@NotNull DeputySteward deputy, @NotNull SiteKey key, @Nullable Windowed previous) {
        super(9, 3);
        final boolean placed = deputy.isPlaced(key);
        setItem(4, new SimpleItem(ItemView.builder()
                .material(Material.ARMOR_STAND)
                .displayName(Translations.component("clans.camp.upgrade.deputy_steward.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .lore(Translations.component(placed ? "clans.camp.upgrade.deputy_steward.placed"
                        : "clans.camp.upgrade.deputy_steward.not_placed")
                        .color(placed ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .build()));
        setItem(11, button(Material.LODESTONE, "place", key, deputy::place, "clans.camp.upgrade.deputy_steward.place.done"));
        setItem(15, button(Material.BARRIER, "dismiss", key, deputy::dismiss,
                "clans.camp.upgrade.deputy_steward.dismiss.done"));
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull SimpleItem button(@NotNull Material icon, @NotNull String id, @NotNull SiteKey key,
                                              @NotNull BiFunction<Player, SiteKey, String> action,
                                              @NotNull String done) {
        final String prefix = "clans.camp.upgrade.deputy_steward." + id;
        return new SimpleItem(ItemView.builder()
                .material(icon)
                .displayName(Translations.component(prefix + ".name").color(NamedTextColor.YELLOW))
                .lore(Translations.component(prefix + ".description").color(NamedTextColor.GRAY))
                .action(ClickActions.ALL, Translations.component(prefix + ".name"))
                .build(), click -> {
                    final Player player = click.getPlayer();
                    final String problem = action.apply(player, key);
                    UtilMessage.plain(player, Translations.component(problem == null ? done : problem)
                                    .color(problem == null ? NamedTextColor.GREEN : NamedTextColor.RED));
                    player.closeInventory();
                });
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.deputy_steward.name");
    }
}
