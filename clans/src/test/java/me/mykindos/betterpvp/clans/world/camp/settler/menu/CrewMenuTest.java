package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.ConstructionSite;
import me.mykindos.betterpvp.core.world.construction.ConstructionSites;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.construction.Worksite;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.settler.crew.CrewService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.click;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreMentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreText;
import static me.mykindos.betterpvp.clans.testing.Messages.mentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.named;
import static me.mykindos.betterpvp.clans.testing.Messages.text;
import static me.mykindos.betterpvp.clans.testing.Messages.told;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.view;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.views;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrewMenuTest {

    private static final SiteKey SITE = Camps.keyFor(42L);
    private static final long MINUTE = 60_000;

    private final CrewMenus menus = mock(CrewMenus.class);
    private final StructureStatusTracker tracker = mock(StructureStatusTracker.class);
    private final CrewService crews = mock(CrewService.class);
    private final CrewRule rule = mock(CrewRule.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final StructureCatalogue catalogue = mock(StructureCatalogue.class);
    private final ConstructionSites sites = mock(ConstructionSites.class);
    private final SettlerSite site = mock(SettlerSite.class);
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);
    private final Windowed previous = mock(Windowed.class);
    private final Holding holding = new Holding();
    private final Worksite worksite = new Worksite(SITE, mock(ConstructionSite.class), holding, world);
    private final Roster roster = new Roster();

    private MockedConstruction<BackButton> backButtons;

    @BeforeAll
    static void load() {
        MenuProbe.load();
    }

    @BeforeEach
    void setUp() {
        backButtons = mockConstruction(BackButton.class);
        when(menus.getTracker()).thenReturn(tracker);
        when(menus.getCrews()).thenReturn(crews);
        when(menus.getRule()).thenReturn(rule);
        when(menus.getSettlers()).thenReturn(settlers);
        when(menus.getCatalogue()).thenReturn(catalogue);
        when(catalogue.find(any())).thenReturn(Optional.empty());
        when(settlers.site(SITE)).thenReturn(Optional.of(site));
        when(settlers.roster(SITE)).thenReturn(Optional.of(roster));
        when(site.allows(any(), eq(SITE), eq(SettlerAction.ASSIGN))).thenReturn(true);
        when(site.builderStats(eq(SITE), any(), any(), any(), anyList()))
                .thenReturn(Optional.of(new BuilderStats(2, 1.0, 0.5, CampProfessions.MASON, Set.of())));
        when(crews.isFree(any())).thenAnswer(invocation -> {
            final Settler settler = invocation.getArgument(0);
            return settler.getAssignment() == null && settler.hasProfession(CampProfessions.BUILDER);
        });
        when(rule.crew(eq(SITE), any())).thenReturn(List.of());
        when(player.getWorld()).thenReturn(world);
    }

    @AfterEach
    void tearDown() {
        backButtons.close();
    }

    private PlacedStructure structure(Job job) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), "workshop",
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        structure.setJob(job);
        holding.getStructures().add(structure);
        return structure;
    }

    private static Job job(long minutes) {
        return Job.start(JobKind.BUILD, Duration.ofMinutes(minutes), ResourceCost.NONE, 1, 0);
    }

    private static Settler builder(String name, String assignment) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName(name);
        settler.setRarity(SettlerRarity.RARE);
        settler.setProfession(CampProfessions.BUILDER);
        settler.setSpecialty(CampProfessions.MASON);
        settler.setAssignment(assignment);
        return settler;
    }

    private CrewMenu crewMenu(PlacedStructure structure) {
        return new CrewMenu(menus, player, worksite, structure, previous);
    }

    /** Built from exactly these dependencies, so {@code CrewMenus} taking anything more fails the tests that use it (AC33). */
    private CrewMenus realMenus() {
        return spy(MenuProbe.buildExactly(CrewMenus.class, sites, tracker, crews, rule, settlers, catalogue));
    }

    @Test
    void ac12_outsideACampTheCrewMenusRefuse() {
        when(sites.worksite(world)).thenReturn(Optional.empty());

        realMenus().openJobs(player, previous);

        assertTrue(told(player, "clans.settler.crew.not_in_camp"));
    }

    @Test
    void ac12_aStructureWithNoJobOpensTheJobsList() {
        when(sites.worksite(world)).thenReturn(Optional.of(worksite));
        final PlacedStructure idle = structure(null);
        final CrewMenus real = realMenus();
        doNothing().when(real).openJobs(any(), any());

        real.openCrew(player, idle.getId(), previous);
        real.openCrew(player, UUID.randomUUID(), previous);

        verify(real, times(2)).openJobs(player, previous);
    }

    @Test
    void ac13_theJobsListShowsUnfinishedJobsAndOpensTheirCrew() {
        when(tracker.now()).thenReturn(2 * MINUTE);
        final PlacedStructure running = structure(job(10));
        final Job held = job(10);
        held.hold("crew", 0);
        final PlacedStructure paused = structure(held);
        structure(job(1));
        structure(null);

        final CrewJobsMenu jobs = new CrewJobsMenu(menus, player, worksite, previous);

        final List<ItemView> listed = views(jobs).stream()
                .filter(item -> loreMentions(item, "clans.settler.crew.job.build"))
                .toList();
        assertEquals(2, listed.size(), "running and paused, not finished or idle");
        assertTrue(listed.stream().allMatch(item -> item.getMaterial() == Material.BRICKS));
        assertTrue(listed.stream().allMatch(item -> !item.getClickActions().isEmpty()));

        click(jobs, 0, player, ClickType.LEFT);
        click(jobs, 1, player, ClickType.LEFT);
        verify(menus).openCrew(player, running.getId(), previous);
        verify(menus).openCrew(player, paused.getId(), previous);
    }

    @Test
    void ac13_withNoJobsTheListSaysNothingIsBeingBuilt() {
        structure(null);

        final CrewJobsMenu jobs = new CrewJobsMenu(menus, player, worksite, previous);

        assertEquals(Material.BARRIER, named(jobs, "clans.settler.crew.no_jobs").getMaterial());
    }

    @Test
    void ac14_theSummaryShowsWorkforceSpeedAndWhatIsMissing() {
        final Job held = job(10);
        held.hold("crew", 0);
        final PlacedStructure waiting = structure(held);
        when(rule.threshold(waiting, held)).thenReturn(4);
        when(rule.workforce(SITE, waiting, held)).thenReturn(1);
        when(rule.speed(SITE, waiting, held)).thenReturn(1.5);

        final ItemView waitingItem = view(crewMenu(waiting), 4).orElseThrow();
        assertEquals("workshop", text(waitingItem.getDisplayName()));
        assertTrue(loreMentions(waitingItem, "clans.settler.crew.job.build"));
        final Component workforce = line(waitingItem, "clans.settler.crew.workforce");
        assertEquals(NamedTextColor.RED, workforce.color());
        assertTrue(text(workforce).contains("1") && text(workforce).contains("4"));
        assertTrue(text(line(waitingItem, "clans.settler.crew.speed")).contains("1.50"));
        assertTrue(text(line(waitingItem, "clans.settler.crew.waiting")).contains("3"));

        final Job runningJob = job(10);
        final PlacedStructure running = structure(runningJob);
        when(rule.threshold(running, runningJob)).thenReturn(4);
        when(rule.workforce(SITE, running, runningJob)).thenReturn(4);
        final ItemView met = view(crewMenu(running), 4).orElseThrow();
        assertEquals(NamedTextColor.GREEN, line(met, "clans.settler.crew.workforce").color());
        assertTrue(loreMentions(met, "clans.settler.crew.done_in"));
        assertFalse(loreMentions(met, "clans.settler.crew.waiting"));
    }

    @Test
    void ac1_aJobHeldForAnotherReasonShowsPausedWithNoWorkforceNeeded() {
        final Job sieged = job(10);
        sieged.hold("siege", 0);
        final PlacedStructure structure = structure(sieged);
        when(rule.threshold(structure, sieged)).thenReturn(4);
        when(rule.workforce(SITE, structure, sieged)).thenReturn(1);

        final ItemView crewItem = view(crewMenu(structure), 4).orElseThrow();
        final ItemView jobsItem = view(new CrewJobsMenu(menus, player, worksite, previous), 0).orElseThrow();

        for (ItemView item : List.of(crewItem, jobsItem)) {
            assertTrue(loreMentions(item, "clans.camp.upgrade.job_board.paused"));
            assertFalse(loreMentions(item, "clans.settler.crew.waiting"));
            assertFalse(loreMentions(item, "clans.settler.crew.done_in"));
        }
    }

    @Test
    void ac1_aJobHeldByItsCrewAmongOtherReasonsShowsTheWorkforceItNeeds() {
        final Job held = job(10);
        held.hold("siege", 0);
        held.hold(CrewRule.ID, 0);
        final PlacedStructure structure = structure(held);
        when(rule.threshold(structure, held)).thenReturn(4);
        when(rule.workforce(SITE, structure, held)).thenReturn(1);

        final ItemView crewItem = view(crewMenu(structure), 4).orElseThrow();
        final ItemView jobsItem = view(new CrewJobsMenu(menus, player, worksite, previous), 0).orElseThrow();

        for (ItemView item : List.of(crewItem, jobsItem)) {
            assertTrue(text(line(item, "clans.settler.crew.waiting")).contains("3"));
            assertFalse(loreMentions(item, "clans.camp.upgrade.job_board.paused"));
        }
    }

    private static Component line(ItemView view, String key) {
        return view.getLore().stream().filter(line -> mentions(line, key)).findFirst()
                .orElseThrow(() -> new AssertionError("no lore line " + key));
    }

    @Test
    void ac15_theCrewShowsEachBuilderWithTagsAndWhatItAdds() {
        final Job job = job(10);
        final PlacedStructure structure = structure(job);
        final Settler member = builder("Aldric Tanner", structure.getId().toString());
        roster.getSettlers().add(member);
        when(rule.crew(SITE, job)).thenReturn(List.of(member));

        final ItemView shown = view(crewMenu(structure), 11).orElseThrow();

        assertEquals("Aldric Tanner", text(shown.getDisplayName()));
        assertEquals(SettlerRarity.RARE.getColor(), shown.getDisplayName().color());
        final String tags = text(shown.getLore().getFirst());
        assertTrue(tags.contains(text(SettlerTags.rarity(SettlerRarity.RARE))));
        assertTrue(tags.contains(text(SettlerTags.role(CampProfessions.MASON).orElseThrow())));
        final String brings = text(line(shown, "clans.settler.crew.brings"));
        assertTrue(brings.contains("2") && brings.contains("1.00"));
        verify(site).builderStats(SITE, member, structure, job, List.of(member));
    }

    @Test
    void ac16_freeBuildersAreListedWithWhatTheyWouldAdd() {
        final Job job = job(10);
        final PlacedStructure structure = structure(job);
        final Settler member = builder("Aldric Tanner", structure.getId().toString());
        final Settler free = builder("Mirel of the Fens", null);
        final Settler busy = builder("Oswin Greyhand", UUID.randomUUID().toString());
        final Settler farmer = builder("Tamsin Reed", null);
        farmer.setProfession(CampProfessions.FARMER);
        roster.getSettlers().addAll(List.of(member, free, busy, farmer));
        when(rule.crew(SITE, job)).thenReturn(List.of(member));

        final CrewMenu menu = crewMenu(structure);

        final ItemView listed = view(menu, 27).orElseThrow();
        assertEquals("Mirel of the Fens", text(listed.getDisplayName()));
        assertTrue(loreMentions(listed, "clans.settler.crew.brings"));
        assertFalse(view(menu, 28).isPresent(), "busy Builders and other professions are not free");
        verify(site).builderStats(SITE, free, structure, job, List.of(member, free));
    }

    @Test
    void ac16_withNoFreeBuildersTheMenuSaysSo() {
        final CrewMenu menu = crewMenu(structure(job(10)));

        assertEquals(Material.BARRIER, named(menu, "clans.settler.crew.none_free").getMaterial());
    }

    @Test
    void ac17_clickingAFreeBuilderJoinsItForThePlayer() {
        final PlacedStructure structure = structure(job(10));
        final Settler free = builder("Mirel of the Fens", null);
        roster.getSettlers().add(free);

        final CrewMenu menu = crewMenu(structure);
        assertFalse(view(menu, 27).orElseThrow().getClickActions().isEmpty());
        click(menu, 27, player, ClickType.LEFT);

        verify(menus).join(player, structure.getId(), free.getId(), previous);
    }

    @Test
    void ac17_joiningTellsARefusalAndReopensTheCrew() {
        final PlacedStructure structure = structure(job(10));
        final UUID settler = UUID.randomUUID();
        when(crews.join(player, world, structure.getId(), settler))
                .thenReturn(SettlerResult.refused("core.settler.crew.full"));
        final CrewMenus real = realMenus();
        doNothing().when(real).openCrew(any(), any(), any());

        real.join(player, structure.getId(), settler, previous);

        assertTrue(told(player, "core.settler.crew.full"));
        verify(real).openCrew(player, structure.getId(), previous);
    }

    @Test
    void ac17_aRankThatCannotAssignCannotAddBuilders() {
        when(site.allows(player, SITE, SettlerAction.ASSIGN)).thenReturn(false);
        final PlacedStructure structure = structure(job(10));
        roster.getSettlers().add(builder("Mirel of the Fens", null));

        final CrewMenu menu = crewMenu(structure);
        final ItemView listed = view(menu, 27).orElseThrow();
        assertTrue(loreMentions(listed, "clans.settler.card.not_allowed"));
        assertTrue(listed.getClickActions().isEmpty());
        click(menu, 27, player, ClickType.LEFT);

        verify(menus, never()).join(any(), any(), any(), any());
    }

    @Test
    void ac18_backOpensTheJobsList() {
        final CrewMenu menu = crewMenu(structure(job(10)));

        click(menu, "clans.settler.crew.back", player);

        verify(menus).openJobs(player, previous);
    }

    @Test
    void ac14_theJobNameFollowsTheJobKind() {
        final Job repair = Job.start(JobKind.REPAIR, Duration.ofMinutes(5), ResourceCost.NONE, 1, 0);
        assertTrue(CrewJobsMenu.jobName(repair) instanceof TranslatableComponent);
        assertTrue(loreText(view(crewMenu(structure(repair)), 4).orElseThrow()).contains("clans.settler.crew.job.repair"));
    }

    @Test
    void ac16_crewMembersAreNotListedAsFree() {
        final Job job = job(10);
        final PlacedStructure structure = structure(job);
        final Settler member = builder("Aldric Tanner", structure.getId().toString());
        roster.getSettlers().add(member);
        when(rule.crew(SITE, job)).thenReturn(List.of(member));

        final CrewMenu menu = crewMenu(structure);

        assertEquals(Material.BARRIER, named(menu, "clans.settler.crew.none_free").getMaterial());
    }
}
