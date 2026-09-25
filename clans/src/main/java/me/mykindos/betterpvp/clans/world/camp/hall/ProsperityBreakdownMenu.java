package me.mykindos.betterpvp.clans.world.camp.hall;

import me.mykindos.betterpvp.clans.world.camp.settler.prosperity.ProsperityFactors;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Prosperity breakdown's page: the camp's Prosperity, and one entry for each factor with what it adds. */
public class ProsperityBreakdownMenu extends AbstractGui implements Windowed {

    ProsperityBreakdownMenu(@NotNull ProsperityFactors factors, @Nullable Windowed previous) {
        super(9, 4);

        final ItemView.ItemViewBuilder total = ItemView.builder()
                .material(Material.EMERALD)
                .displayName(Translations.component("clans.camp.upgrade.prosperity_breakdown.name")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .lore(Translations.component("clans.camp.prosperity.value",
                        Component.text(factors.getTotal(), NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
        if (factors.getFactors().isEmpty()) {
            total.lore(Translations.component("clans.camp.upgrade.prosperity_breakdown.empty")
                    .color(NamedTextColor.GRAY));
        }
        setItem(4, new SimpleItem(total.build()));

        final List<ProsperityFactors.Factor> shown = factors.getFactors();
        final int first = 22 - Math.min(shown.size(), 7) / 2;
        for (int i = 0; i < shown.size() && i < 7; i++) {
            setItem(first + i, new SimpleItem(factor(shown.get(i))));
        }

        setItem(31, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static @NotNull ItemView factor(@NotNull ProsperityFactors.Factor factor) {
        final Material material;
        final Component name;
        switch (factor.getKind()) {
            case SETTLERS -> {
                material = Material.PLAYER_HEAD;
                name = Translations.component("clans.camp.upgrade.prosperity_breakdown.settlers",
                        Component.text((int) factor.getDetail()),
                        factor.getRarity() == null ? Component.empty() : factor.getRarity().displayName());
            }
            case MORALE -> {
                material = Material.CAKE;
                name = Translations.component("clans.camp.upgrade.prosperity_breakdown.morale",
                        signed(factor.getDetail()));
            }
            default -> {
                material = Material.WRITABLE_BOOK;
                name = Translations.component("clans.camp.upgrade.prosperity_breakdown.chronicler",
                        Component.text("+" + Math.round(factor.getDetail() * 100) + "%"));
            }
        }
        return ItemView.builder()
                .material(material)
                .displayName(name.color(NamedTextColor.YELLOW))
                .lore(Translations.component("clans.camp.upgrade.prosperity_breakdown.adds", signed(factor.getAmount()))
                        .color(NamedTextColor.GRAY))
                .build();
    }

    /** A whole number with its sign, green when it raises Prosperity and red when it lowers it. */
    private static @NotNull Component signed(double amount) {
        final long rounded = Math.round(amount);
        if (rounded == 0) {
            return Component.text("0", NamedTextColor.WHITE);
        }
        return rounded > 0 ? Component.text("+" + rounded, NamedTextColor.GREEN)
                : Component.text(String.valueOf(rounded), NamedTextColor.RED);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.prosperity_breakdown.name");
    }
}
