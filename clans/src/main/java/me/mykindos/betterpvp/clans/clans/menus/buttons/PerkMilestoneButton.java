package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.leveling.ClanPerk;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Displays a single {@link ClanPerk} milestone in the {@link me.mykindos.betterpvp.clans.clans.menus.PerkMenu}.
 * Unlocked perks show their real icon; locked perks show a barrier with "Unlocks at level N" lore.
 */
public class PerkMilestoneButton extends SimpleItem {

    public PerkMilestoneButton(ClanPerk perk, long clanLevel) {
        super(build(perk, clanLevel));
    }

    private static ItemView build(ClanPerk perk, long clanLevel) {
        boolean unlocked = perk.hasPerk(clanLevel);

        Component name = Component.text(perk.getName(), unlocked ? NamedTextColor.GREEN : NamedTextColor.RED)
                .append(Component.text(" [Lvl " + perk.getMinimumLevel() + "]", NamedTextColor.DARK_GRAY));

        List<Component> lore = Arrays.asList(perk.getDescription());

        ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(unlocked ? perk.getIcon().getMaterial() : Material.BARRIER)
                .displayName(name)
                .lore(lore)
                .frameLore(true);

        if (!unlocked) {
            builder.lore(Component.empty());
            builder.lore(Component.text("Unlocks at level " + perk.getMinimumLevel(), NamedTextColor.GRAY)
                    .decorate(TextDecoration.ITALIC));
        }

        return builder.build();
    }

}
