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

/** The Second door's page: the member picks which of the Barracks' two doors they respawn at. */
public class SecondDoorMenu extends AbstractGui implements Windowed {

    private final SecondDoor door;
    private final SiteKey key;
    private final @Nullable Windowed previous;

    SecondDoorMenu(@NotNull SecondDoor door, @NotNull Player viewer, @NotNull SiteKey key,
                   @Nullable Windowed previous) {
        super(9, 3);
        this.door = door;
        this.key = key;
        this.previous = previous;

        final boolean second = door.picked(key, viewer.getUniqueId());
        setItem(11, option(Material.OAK_DOOR, "clans.camp.upgrade.second_door.main",
                "clans.camp.upgrade.second_door.main.description", !second, false));
        setItem(15, option(Material.SPRUCE_DOOR, "clans.camp.upgrade.second_door.second",
                "clans.camp.upgrade.second_door.second.description", second, true));
        setItem(22, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull SimpleItem option(@NotNull Material material, @NotNull String name, @NotNull String description,
                                       boolean chosen, boolean second) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(material)
                .displayName(Translations.component(name)
                        .color(chosen ? NamedTextColor.GREEN : NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .glow(chosen)
                .lore(Translations.component(description).color(NamedTextColor.GRAY))
                .lore(Component.empty());
        if (chosen) {
            view.lore(Translations.component("clans.camp.upgrade.second_door.chosen").color(NamedTextColor.GREEN));
        } else {
            view.action(ClickActions.ALL, Translations.component("clans.camp.upgrade.second_door.pick"));
        }
        return new SimpleItem(view.build(), click -> {
            if (chosen) {
                return;
            }
            final Player player = click.getPlayer();
            final String problem = door.pick(key, player.getUniqueId(), second);
            UtilMessage.plain(player, Translations.component(problem != null ? problem : "clans.camp.upgrade.second_door.picked",
                            Translations.component(name)).color(NamedTextColor.GRAY));
            new SecondDoorMenu(door, player, key, previous).show(player);
        });
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.second_door.name");
    }
}
