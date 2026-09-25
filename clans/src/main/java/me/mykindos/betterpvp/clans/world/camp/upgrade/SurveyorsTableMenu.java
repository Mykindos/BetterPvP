package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Surveyor's table's page: the camp's structures that can still grow, each showing its next stage when picked. */
public class SurveyorsTableMenu extends AbstractGui implements Windowed {

    SurveyorsTableMenu(@NotNull SurveyorsTable table, @NotNull SiteKey camp, @Nullable Windowed previous) {
        super(9, 6);
        final List<PlacedStructure> structures = table.surveyable(camp);
        if (structures.isEmpty()) {
            setItem(22, new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Translations.component("clans.camp.upgrade.surveyors_table.none")
                            .color(NamedTextColor.GRAY))
                    .build()));
        }
        for (int slot = 0; slot < structures.size() && slot < 45; slot++) {
            final PlacedStructure structure = structures.get(slot);
            final CampStructure type = table.type(structure).orElseThrow();
            setItem(slot, new SimpleItem(ItemView.builder()
                    .material(type.getIcon())
                    .displayName(type.getDisplayName().color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                    .lore(type.stageName(structure.getStage()).color(NamedTextColor.GRAY))
                    .lore(Translations.component("clans.camp.upgrade.surveyors_table.next",
                            type.stageName(structure.getStage() + 1).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY))
                    .lore(Component.empty())
                    .lore(Translations.component("clans.camp.upgrade.surveyors_table.explain",
                            Component.text(table.seconds())).color(NamedTextColor.GRAY))
                    .action(ClickActions.ALL, Translations.component("clans.camp.upgrade.surveyors_table.survey"))
                    .build(), click -> {
                final Player player = click.getPlayer();
                if (table.survey(player, camp, structure.getId())) {
                    player.closeInventory();
                }
            }));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.upgrade.surveyors_table.name");
    }
}
