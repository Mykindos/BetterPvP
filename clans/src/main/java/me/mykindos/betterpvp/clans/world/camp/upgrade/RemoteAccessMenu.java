package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.storage.CampChest;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Remote access page: every item chest in the camp, each opening its real inventory when clicked. */
public class RemoteAccessMenu extends AbstractGui implements Windowed {

    RemoteAccessMenu(@NotNull RemoteAccess access, @NotNull Player viewer, @NotNull SiteKey key,
                     @Nullable Windowed previous) {
        super(9, 6);
        final StorehouseChests chests = access.getChests();
        final List<CampChest> found = access.chests(viewer, key);
        if (found.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.storage.no_chests").color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int i = 0; i < found.size() && i < 45; i++) {
            final CampChest chest = found.get(i);
            setItem(i, new SimpleItem(ItemView.builder()
                    .material(Material.CHEST)
                    .displayName(chests.name(chest).color(NamedTextColor.YELLOW))
                    .frameLore(true)
                    .lore(StorehouseChests.position(chest).color(NamedTextColor.DARK_GRAY))
                    .action(ClickActions.ALL, Translations.component("clans.camp.hall.open"))
                    .build(), click -> {
                        final String problem = access.open(click.getPlayer(), key, chest);
                        if (problem != null) {
                            UtilMessage.message(click.getPlayer(), Translations.component("clans.prefix.camp"),
                                    Translations.component(problem).color(NamedTextColor.RED));
                        }
                    }));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.remote_access.name");
    }
}
