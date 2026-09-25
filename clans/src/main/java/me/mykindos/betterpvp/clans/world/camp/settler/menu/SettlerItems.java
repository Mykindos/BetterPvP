package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** The items that show who a settler is, shared by its card and by a candidate's. */
public final class SettlerItems {

    private SettlerItems() {
    }

    /** Name in its rarity's colour, its rarity, and where it came from. */
    public static @NotNull ItemView.ItemViewBuilder identity(@NotNull Settler settler) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(Material.NAME_TAG)
                .displayName(Component.text(settler.getName(), settler.getRarity().getColor()))
                .frameLore(true)
                .lore(settler.getRarity().displayName());
        if (settler.getHistory() != null) {
            final ComponentLike[] args = settler.getHistoryArgs().stream()
                    .map(Component::text)
                    .toArray(ComponentLike[]::new);
            view.lore(Translations.component(settler.getHistory(), args).color(NamedTextColor.GRAY));
        }
        return view;
    }

    /** Its profession and specialty, or that it has none and wanders. */
    public static @NotNull ItemView.ItemViewBuilder profession(@NotNull Settler settler,
                                                              @Nullable Profession profession) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(profession == null ? Material.LEATHER_BOOTS : Material.IRON_PICKAXE)
                .hideAdditionalTooltip(true)
                .displayName((profession == null
                        ? Translations.component("clans.settler.card.no_profession")
                        : Translations.component(profession.getKey())).color(NamedTextColor.YELLOW))
                .frameLore(true);
        if (profession != null && settler.getSpecialty() != null) {
            view.lore(Translations.component(profession.specialtyKey(settler.getSpecialty())).color(NamedTextColor.GRAY));
        }
        if (profession == null) {
            view.lore(Translations.component("clans.settler.card.wanders").color(NamedTextColor.GRAY));
        }
        return view;
    }

    /** One trait, what it does and when. */
    public static @NotNull ItemView trait(@NotNull Trait trait) {
        final String group = trait.getGroup().name().toLowerCase(Locale.ROOT);
        return ItemView.builder()
                .material(trait.isTradeOff() ? Material.REDSTONE : Material.GLOWSTONE_DUST)
                .displayName(Translations.component(trait.nameKey())
                        .color(trait.isTradeOff() ? NamedTextColor.GOLD : NamedTextColor.AQUA))
                .frameLore(true)
                .lore(Translations.component(trait.descriptionKey()).color(NamedTextColor.GRAY))
                .lore(Translations.component("clans.settler.card.group." + group).color(NamedTextColor.DARK_GRAY))
                .build();
    }
}
