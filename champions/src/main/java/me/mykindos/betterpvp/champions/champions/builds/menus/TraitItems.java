package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The trait tile shared by the class selector and the build editor, so a trait reads identically wherever it
 * is shown.
 */
@UtilityClass
public class TraitItems {

    public static ItemView of(Trait trait) {
        List<Component> lore = new ArrayList<>();
        lore.add(Translations.component("champions.menu.trait.innate").color(NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.empty());
        Arrays.stream(trait.getDescription(trait.getTraitLevel()))
                .map(line -> line.colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .forEach(lore::add);

        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.PAPER)
                .itemModel(trait.getIcon())
                .displayName(trait.getDisplayName().color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD))
                .lore(lore)
                .flag(ItemFlag.HIDE_ATTRIBUTES);

        if (trait.getTags() != null) {
            builder.prelore(trait.getTags());
        }

        return builder.hideAdditionalTooltip(true).frameLore(true).build();
    }

    /**
     * The placeholder for a trait slot this role does not fill.
     */
    public static ItemView empty() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/gui/classes/x_mark"))
                .displayName(Translations.component("champions.menu.trait.empty").color(NamedTextColor.DARK_GRAY))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .build();
    }
}
