package me.mykindos.betterpvp.clans.world.camp.menu;

import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceKind;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Every structure a camp can build: what it needs, what it costs and how long it takes, and why it cannot be built yet
 * if it cannot. Clicking one that can be built hands over its blueprint.
 */
public class ConstructionMenu extends AbstractGui implements Windowed {

    private final Player viewer;
    private final SiteKey camp;
    private final List<CampStructure> structures;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final BlueprintSessions blueprints;
    private final @Nullable Windowed previous;

    public ConstructionMenu(@NotNull Player viewer, @NotNull SiteKey camp, @NotNull List<CampStructure> structures,
                            @NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                            @NotNull BlueprintSessions blueprints, @Nullable Windowed previous) {
        super(9, 6);
        this.previous = previous;
        this.viewer = viewer;
        this.camp = camp;
        this.structures = structures.stream().sorted(Comparator.comparingInt(CampStructure::getTier)).toList();
        this.construction = construction;
        this.catalogue = catalogue;
        this.blueprints = blueprints;
        populate();
    }

    private void populate() {
        for (int slot = 0; slot < structures.size() && slot < 45; slot++) {
            final CampStructure structure = structures.get(slot);
            setItem(slot, new SimpleItem(view(structure), click -> choose(click.getPlayer(), structure)));
        }
        setItem(49, new BackButton(previous));
        setBackground(Menu.BACKGROUND_ITEM);
    }

    private @NotNull ItemView view(@NotNull CampStructure structure) {
        final Optional<Component> unavailable = construction.unavailable(viewer, camp, structure);
        final StructureStage first = structure.stage(0);
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(structure.getIcon())
                .displayName(structure.getDisplayName().color(unavailable.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD))
                .frameLore(true)
                .lore(structure.getDescription().color(NamedTextColor.GRAY))
                .lore(Component.empty())
                .lore(Translations.component("clans.camp.menu.build.tier", Component.text(structure.getTier()))
                        .color(NamedTextColor.GRAY));

        if (!structure.getRequiredStructures().isEmpty()) {
            final List<Component> names = new ArrayList<>();
            structure.getRequiredStructures().forEach(id -> catalogue.find(id)
                    .ifPresent(required -> names.add(required.getDisplayName())));
            view.lore(Translations.component("clans.camp.menu.build.requires",
                    Component.join(JoinConfiguration.commas(true), names)).color(NamedTextColor.GRAY));
        }
        if (structure.getTier() > 1) {
            final Component hall = catalogue.find(CampConstruction.GREAT_HALL)
                    .map(type -> type.getDisplayName()).orElse(Component.empty());
            view.lore(Translations.component("clans.camp.menu.build.needs_hall", hall,
                    Component.text(structure.getTier())).color(NamedTextColor.GRAY));
        }
        view.lore(Translations.component("clans.camp.menu.build.cost", cost(first.getCost())).color(NamedTextColor.GRAY));
        view.lore(Translations.component("clans.camp.menu.build.time", first.getBuildTime().isZero()
                ? Translations.component("clans.camp.menu.build.instant")
                : Component.text(UtilTime.humanReadableFormat(first.getBuildTime()))).color(NamedTextColor.GRAY));

        view.lore(Component.empty());
        unavailable.ifPresentOrElse(view::lore,
                () -> view.action(ClickActions.ALL, Translations.component("clans.camp.menu.build.action")));
        return view.build();
    }

    private void choose(@NotNull Player player, @NotNull CampStructure structure) {
        final Optional<Component> unavailable = construction.unavailable(player, camp, structure);
        if (unavailable.isPresent()) {
            UtilMessage.message(player, Translations.component("clans.prefix.camp"), unavailable.get());
            return;
        }
        player.getInventory().addItem(blueprints.blueprintFor(structure));
        UtilMessage.message(player, Translations.component("clans.prefix.camp"),
                Translations.component("clans.camp.menu.build.given", structure.getDisplayName()));
        player.closeInventory();
    }

    private static @NotNull Component cost(@NotNull ResourceCost cost) {
        if (cost.isFree()) {
            return Translations.component("clans.camp.menu.build.free");
        }
        final List<Component> parts = new ArrayList<>();
        cost.getAmounts().forEach((resource, amount) -> ResourceKind.byId(resource).ifPresent(kind ->
                parts.add(Translations.component("clans.camp.resource.amount", Component.text(amount), kind.displayName()))));
        return Component.join(JoinConfiguration.commas(true), parts);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("clans.camp.menu.build.title");
    }
}
