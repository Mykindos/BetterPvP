package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

public class PerkMenu extends AbstractGui implements Windowed {

    private final @NotNull Clan clan;
    private final @Nullable Windowed previous;

    public PerkMenu(@NotNull Clan clan, @Nullable Windowed previous) {
        super(9, 3);
        this.clan = clan;
        this.previous = previous;
        populate();
    }

    private void populate() {
        final Iterator<ClanPerk> iterator = ClanPerkManager.getInstance().getPerksSortedByLevel().iterator();
        int slot = 0;
        while (iterator.hasNext()) {
            final ClanPerk perk = iterator.next();
            final boolean owns = perk.hasPerk(clan);
            final Component name = Component.text(perk.getName(), owns ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.text(" (Level ", NamedTextColor.GRAY))
                    .append(Component.text(perk.getMinimumLevel(), NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY));

            final ItemView perkItem = ItemView.builder()
                    .material(owns ? perk.getIcon().getMaterial() : Material.BARRIER)
                    .displayName(name)
                    .lore(Arrays.stream(perk.getDescription()).toList())
                    .frameLore(true)
                    .build();

            setItem(slot, new SimpleItem(perkItem));
            slot++;
        }

        if (previous != null) {
            setItem(26, new BackButton(previous));
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Perks for " + clan.getName());
    }
}
