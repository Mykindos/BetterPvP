package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructure;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.FitCheck;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreview;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreviews;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Workshop upgrade: shows one member a ghost of a structure's next stage where the structure stands, for a while.
 * Each part of the ghost glows green where the next stage fits and red where it would leave the build zones or run
 * into another structure.
 */
@BPvPListener
@Singleton
@Getter(AccessLevel.PACKAGE)
public class SurveyorsTable implements Listener {

    public static final String ID = "surveyors_table";

    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampStore store;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final SchematicService schematics;
    private final FitCheck fitCheck;
    private final GhostPreviews previews;

    private final Map<UUID, Survey> surveys = new HashMap<>();

    @Inject
    public SurveyorsTable(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
                          @NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                          @NotNull SchematicService schematics, @NotNull FitCheck fitCheck,
                          @NotNull GhostPreviews previews) {
        this.upgrades = upgrades;
        this.config = config;
        this.store = store;
        this.construction = construction;
        this.catalogue = catalogue;
        this.schematics = schematics;
        this.fitCheck = fitCheck;
        this.previews = previews;
        upgrades.declare(CampStructures.WORKSHOP, ID, 2);
        upgrades.page(ID, (player, camp, structure, previous) -> open(player, camp, previous));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.WORKSHOP, ID);
    }

    /** Opens the list of structures to survey in camp {@code camp}, if its Workshop has a working table. */
    public void open(@NotNull Player player, @NotNull SiteKey camp, @Nullable Windowed previous) {
        if (!isActive(camp)) {
            tell(player, Translations.component("clans.camp.upgrade.surveyors_table.inactive").color(NamedTextColor.RED));
            return;
        }
        new SurveyorsTableMenu(this, camp, previous).show(player);
    }

    /** The structures in camp {@code camp} that stand in the world and have a stage after the one they are at. */
    @NotNull List<PlacedStructure> surveyable(@NotNull SiteKey camp) {
        return store.cached(camp.getOwnerId())
                .map(Camp::getHolding)
                .map(Holding::getStructures)
                .map(List::copyOf)
                .orElse(List.of())
                .stream()
                .filter(structure -> structure.getCondition() != StructureCondition.NOT_PLACED)
                .filter(structure -> type(structure).filter(type -> type.hasStage(structure.getStage() + 1)).isPresent())
                .toList();
    }

    @NotNull Optional<CampStructure> type(@NotNull PlacedStructure structure) {
        return catalogue.find(structure.getType())
                .filter(CampStructure.class::isInstance)
                .map(CampStructure.class::cast);
    }

    /**
     * Shows {@code player} the next stage of structure {@code id} in camp {@code camp} where it stands, which they must
     * be in to see. Says whether it fits.
     *
     * @return whether the ghost is showing
     */
    public boolean survey(@NotNull Player player, @NotNull SiteKey camp, @NotNull UUID id) {
        if (!isActive(camp)) {
            tell(player, Translations.component("clans.camp.upgrade.surveyors_table.inactive").color(NamedTextColor.RED));
            return false;
        }
        final ConstructionService.Worksite worksite = construction.worksite(player.getWorld())
                .filter(found -> found.getKey().equals(camp))
                .orElse(null);
        if (worksite == null) {
            tell(player, Translations.component("clans.camp.menu.structures.not_in_camp").color(NamedTextColor.RED));
            return false;
        }
        final PlacedStructure structure = worksite.getHolding().find(id).orElse(null);
        final CampStructure type = structure == null ? null : type(structure).orElse(null);
        if (structure == null || type == null) {
            tell(player, Translations.component("core.construction.missing_structure").color(NamedTextColor.RED));
            return false;
        }
        final int next = structure.getStage() + 1;
        if (!type.hasStage(next)) {
            tell(player, Translations.component("core.construction.max_stage").color(NamedTextColor.RED));
            return false;
        }
        final Optional<Schematic> schematic = schematics.load(type.stage(next).getSchematic());
        if (schematic.isEmpty()) {
            tell(player, Translations.component("core.construction.no_build").color(NamedTextColor.RED));
            return false;
        }

        final World world = worksite.getWorld();
        final StructurePosition position = structure.getPosition();
        final Location anchor = position.toLocation(world);
        final SchematicPlacement placement = SchematicPlacement.of(schematic.get(), anchor, position.getQuarterTurns());
        final LongSet clashes = fitCheck.clashes(world, worksite.getHolding(), type, placement, id);

        final GhostPreview preview = previews.open(player, schematic.get());
        preview.show(anchor, position.getQuarterTurns());
        preview.tint(piece -> fits(clashes, anchor.getBlockX(), anchor.getBlockZ(), piece));
        final int seconds = seconds();
        surveys.put(player.getUniqueId(), new Survey(preview, construction.now() + seconds * 1000L));

        final ComponentLike stage = type.stageName(next).color(NamedTextColor.YELLOW);
        if (clashes.isEmpty()) {
            tell(player, Translations.component("clans.camp.upgrade.surveyors_table.fits", stage,
                    Component.text(seconds)).color(NamedTextColor.GREEN));
        } else {
            final Component reason = fitCheck.problem(world, worksite.getHolding(), type, placement, id)
                    .orElseGet(() -> FitCheck.tooClose(type));
            tell(player, Translations.component("clans.camp.upgrade.surveyors_table.clashes", stage,
                    Component.text(seconds), reason.color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
        }
        return true;
    }

    /** How long a survey shows, in seconds. */
    int seconds() {
        return config.upgrade(CampStructures.WORKSHOP, ID)
                .map(numbers -> numbers.setting("preview-seconds", 30))
                .orElse(30);
    }

    /**
     * Whether the column under {@code block} does not clash. Blocks are in blocks from the anchor, and clashing columns
     * are world columns packed with {@link Footprint#pack}.
     */
    static boolean fits(@NotNull LongSet clashes, int anchorX, int anchorZ, @NotNull Schematic.PlacedBlock block) {
        return !clashes.contains(Footprint.pack(anchorX + block.getX(), anchorZ + block.getZ()));
    }

    @UpdateEvent(delay = 1000)
    public void expire() {
        final long now = construction.now();
        surveys.entrySet().removeIf(entry -> {
            if (now < entry.getValue().getUntil()) {
                return false;
            }
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && previews.of(player).filter(open -> open == entry.getValue().getPreview()).isPresent()) {
                previews.close(player);
            }
            return true;
        });
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        surveys.remove(event.getPlayer().getUniqueId());
    }

    private static void tell(@NotNull Player player, @NotNull Component message) {
        UtilMessage.message(player, Translations.component("clans.prefix.camp"), message);
    }

    /** A ghost one player is shown, and when it goes away. */
    @Value
    private static class Survey {
        GhostPreview preview;
        long until;
    }
}
