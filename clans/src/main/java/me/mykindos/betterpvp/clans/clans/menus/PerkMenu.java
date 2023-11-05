package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.buttons.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PerkMenu extends Menu {

    private final @NotNull Clan clan;
    private final @Nullable Menu previous;

    public PerkMenu(Player player, Clan clan, @Nullable Menu previous) {
        super(player, 27, Component.text("Perks for " + clan.getName()));
        this.clan = clan;
        this.previous = previous;
        populate();
        construct();
    }

    public PerkMenu(Player player, Clan clan) {
        this(player, clan, null);
    }

    private void populate() {
        final Iterator<ClanPerk> iterator = ClanPerkManager.getInstance().getPerksSortedByLevel().iterator();
        int slot = 0;
        while (iterator.hasNext()) {
            final ClanPerk perk = iterator.next();
            final boolean owns = perk.hasPerk(clan);
            final ItemStack icon = owns ? perk.getIcon() : new ItemStack(Material.BARRIER);
            final Component name = Component.text(perk.getName(), owns ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.text(" (Level ", NamedTextColor.GRAY))
                    .append(Component.text(perk.getMinimumLevel(), NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY));

            List<Component> lore = new ArrayList<>();
            lore.add(UtilMessage.DIVIDER);
            lore.add(Component.empty());
            lore.addAll(List.of(perk.getDescription()));
            lore.add(Component.empty());
            lore.add(UtilMessage.DIVIDER);

            final Button button = new Button(slot, icon, name, lore);
            addButton(button);

            slot++;
        }

        if (previous != null) {
            final BackButton back = new BackButton(26, new ItemStack(Material.ARROW), previous);
            addButton(back);
        }

        fillEmpty(Menu.BACKGROUND);
    }

}
