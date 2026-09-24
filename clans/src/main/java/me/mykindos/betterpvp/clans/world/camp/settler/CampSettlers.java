package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerCards;
import me.mykindos.betterpvp.clans.world.camp.upgrade.ToolRack;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.settler.crew.CrewLimits;
import me.mykindos.betterpvp.core.world.settler.morale.MoraleModel;
import me.mykindos.betterpvp.core.world.settler.wage.CoinAccount;
import me.mykindos.betterpvp.core.world.settler.wage.WageModel;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Everything settlers need from a camp: the roster is kept on the camp record, the Great Hall's stage decides how many
 * settlers it can have and how many of each profession can work, and structures such as the Workshop add to that.
 * Settlers gather at the Great Hall, Farmers work the farm, and Builders work at the structure they are assigned to,
 * bringing what {@link CampBuilders} says they do.
 */
@Singleton
public class CampSettlers implements SettlerSite {

    /** The point in a Great Hall build settlers gather around. */
    public static final String HOME_POINT = "settler_home";
    /** The point in any structure's build where a settler assigned to it stands to work. */
    public static final String WORK_POINT = "settler_work";

    private final CampStore store;
    private final SettlerConfig config;
    private final CampPermissions permissions;
    private final StructureShapes shapes;
    private final SettlerCards cards;
    private final CampBuilders builders;
    private final CampResources resources;
    private final CampWageFund wageFund;
    private final CampMorale morale;
    private final ToolRack toolRack;

    @Inject
    public CampSettlers(@NotNull CampStore store, @NotNull SettlerConfig config, @NotNull SettlerService service,
                        @NotNull CampPermissions permissions, @NotNull StructureShapes shapes,
                        @NotNull SettlerCards cards, @NotNull CampBuilders builders,
                        @NotNull CampResources resources, @NotNull CampWageFund wageFund,
                        @NotNull CampMorale morale,
                        @NotNull CampProfessions professions,
                        @NotNull CampTraits traits, @NotNull ToolRack toolRack) {
        this.store = store;
        this.config = config;
        this.permissions = permissions;
        this.shapes = shapes;
        this.cards = cards;
        this.builders = builders;
        this.resources = resources;
        this.wageFund = wageFund;
        this.morale = morale;
        this.toolRack = toolRack;
        service.register(Camps.SITE_ID, this);
    }

    @Override
    public @NotNull Optional<Roster> roster(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId()).map(Camp::getRoster);
    }

    @Override
    public void changed(@NotNull SiteKey site) {
        store.changed(site.getOwnerId());
    }

    @Override
    public int populationCap(@NotNull SiteKey site) {
        return config.population(hallStage(site));
    }

    @Override
    public @NotNull OptionalInt workingCap(@NotNull SiteKey site, @NotNull String profession) {
        return config.workingCap(profession).map(cap -> {
            final Holding holding = holding(site);
            int total = SettlerConfig.byStage(cap.getByHallStage(), finishedStage(holding, CampConstruction.GREAT_HALL));
            for (Map.Entry<String, Integer> bonus : cap.getPerStageOf().entrySet()) {
                total += (finishedStage(holding, bonus.getKey()) + 1) * bonus.getValue();
            }
            return OptionalInt.of(total);
        }).orElseGet(OptionalInt::empty);
    }

    @Override
    public boolean allows(@NotNull Player player, @NotNull SiteKey site, @NotNull SettlerAction action) {
        return permissions.allows(player, site.getOwnerId(), action);
    }

    /** Its profession's look, or the default one while that look's model or skin is not installed. */
    @Override
    public @NotNull SettlerLook look(@NotNull SiteKey site, @NotNull Settler settler) {
        final SettlerLook look = config.look(settler.getProfession(), settler.getRarity());
        final boolean installed = ModelEngineAPI.getBlueprint(look.getModel()) != null
                && (look.getSkin() == null || ModelEngineAPI.getBlueprint(look.getSkin()) != null);
        return installed ? look : config.defaultLook();
    }

    /** The Great Hall's {@code settler_home} point, or just above where the Great Hall stands. */
    @Override
    public @NotNull Optional<Location> home(@NotNull SiteKey site, @NotNull World world, @NotNull RegionIndex regions) {
        return holding(site).ofType(CampConstruction.GREAT_HALL).stream()
                .filter(structure -> structure.getCondition() != StructureCondition.NOT_PLACED)
                .findFirst()
                .map(hall -> standOn(world, hall, HOME_POINT));
    }

    /** The middle of the first farm for Farmers, and a structure's {@code settler_work} point for anything else. */
    @Override
    public @NotNull Optional<Location> workplace(@NotNull SiteKey site, @NotNull World world,
                                                 @NotNull RegionIndex regions, @NotNull String workplace) {
        if (workplace.equals(CampGrounds.FARM)) {
            return regions.find(CampGrounds.FARM, CuboidRegion.class).stream().findFirst().map(farm -> {
                final Location min = farm.getMin();
                final Location max = farm.getMax();
                return new Location(world, (min.getX() + max.getX()) / 2 + 0.5, min.getY() + 1,
                        (min.getZ() + max.getZ()) / 2 + 0.5);
            });
        }

        final UUID structureId;
        try {
            structureId = UUID.fromString(workplace);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return holding(site).find(structureId).map(structure -> standOn(world, structure, WORK_POINT));
    }

    @Override
    public void interact(@NotNull Player player, @NotNull SiteKey site, @NotNull Settler settler) {
        cards.open(player, site, settler.getId());
    }

    @Override
    public @NotNull Optional<BuilderStats> builderStats(@NotNull SiteKey site, @NotNull Settler settler,
                                                        @NotNull PlacedStructure structure, @NotNull Job job,
                                                        @NotNull List<Settler> crew) {
        if (!settler.hasProfession(CampProfessions.BUILDER)) {
            return Optional.empty();
        }
        return Optional.of(toolRack.apply(site, builders.stats(settler, job, crew)));
    }

    @Override
    public @NotNull CrewLimits crewLimits(@NotNull SiteKey site) {
        return config.getCrewLimits();
    }

    /** Frugal and Patcher crews hand back part of what the job cost. */
    @Override
    public void crewFinished(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Job job,
                             @NotNull List<Settler> crew) {
        final double refund = builders.refund(job, crew);
        if (refund > 0) {
            resources.refund(site, job.getSpent().share(refund));
        }
    }

    @Override
    public @NotNull Optional<WageModel> wageModel(@NotNull SiteKey site) {
        return Optional.of(config.getWageModel());
    }

    @Override
    public @NotNull Optional<CoinAccount> wageFund(@NotNull SiteKey site) {
        return Optional.of(wageFund);
    }

    /** Greedy settlers ask for more. It is a trade-off's cost, so rarity does not change it. */
    @Override
    public double wageMultiplier(@NotNull SiteKey site, @NotNull Settler settler) {
        return settler.hasTrait(CampTraits.GREEDY) ? 1 + config.trait(CampTraits.GREEDY, "wage", 0.25) : 1;
    }

    @Override
    public @NotNull Duration strikeLimit(@NotNull SiteKey site) {
        return config.getStrikeLimit();
    }

    @Override
    public @NotNull Optional<SettlerTable> table(@NotNull SiteKey site) {
        return Optional.of(config.getTable());
    }

    @Override
    public @NotNull Optional<MoraleModel> moraleModel(@NotNull SiteKey site) {
        return Optional.of(morale);
    }

    private @NotNull Location standOn(@NotNull World world, @NotNull PlacedStructure structure, @NotNull String point) {
        return shapes.point(world, structure, point)
                .orElseGet(() -> structure.getPosition().toLocation(world).add(0.5, 1, 0.5));
    }

    private @NotNull Holding holding(@NotNull SiteKey site) {
        return store.cached(site.getOwnerId()).map(Camp::getHolding).orElseGet(Holding::new);
    }

    private int hallStage(@NotNull SiteKey site) {
        return finishedStage(holding(site), CampConstruction.GREAT_HALL);
    }

    /** The highest stage a finished structure of {@code type} stands at, counting from 0, or -1 for none. */
    static int finishedStage(@NotNull Holding holding, @NotNull String type) {
        return holding.ofType(type).stream()
                .filter(structure -> structure.getCondition() != StructureCondition.UNDER_CONSTRUCTION
                        && structure.getCondition() != StructureCondition.NOT_PLACED)
                .mapToInt(PlacedStructure::getStage)
                .max()
                .orElse(-1);
    }
}
