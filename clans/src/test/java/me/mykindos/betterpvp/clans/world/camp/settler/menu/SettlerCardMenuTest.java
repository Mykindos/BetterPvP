package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.clans.world.camp.settler.CampMorale;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.wage.Payroll;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.click;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreMentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreText;
import static me.mykindos.betterpvp.clans.testing.Messages.mentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.named;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.slotNamed;
import static me.mykindos.betterpvp.clans.testing.Messages.sent;
import static me.mykindos.betterpvp.clans.testing.Messages.text;
import static me.mykindos.betterpvp.clans.testing.Messages.told;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.view;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettlerCardMenuTest {

    private static final SiteKey SITE = Camps.keyFor(42L);
    private static final String TAKE_OFF_CREW = "clans.settler.card.take_off_crew";

    private final SettlerCards cards = mock(SettlerCards.class);
    private final SettlerService service = mock(SettlerService.class);
    private final CrewMenus crews = mock(CrewMenus.class);
    private final Payroll payroll = mock(Payroll.class);
    private final CampMorale morale = mock(CampMorale.class);
    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final TraitRegistry traits = new TraitRegistry();
    private final Player player = mock(Player.class);
    private final Windowed previous = mock(Windowed.class);

    private MockedConstruction<BackButton> backButtons;

    @BeforeAll
    static void menus() {
        MenuProbe.load();
    }

    @AfterEach
    void tearDown() {
        backButtons.close();
    }

    @BeforeEach
    void setUp() {
        backButtons = mockConstruction(BackButton.class);
        new CampProfessions(professions);
        traits.register(Trait.builder().id("tireless").key("clans.settler.trait.tireless")
                .group(TraitGroup.BUILDER).build());
        traits.register(Trait.builder().id("greedy").key("clans.settler.trait.greedy")
                .group(TraitGroup.BUILDER).tradeOff(true).build());
        when(cards.getService()).thenReturn(service);
        when(cards.getProfessions()).thenReturn(professions);
        when(cards.getTraits()).thenReturn(traits);
        when(cards.getCrews()).thenReturn(crews);
        when(cards.getPayroll()).thenReturn(payroll);
        when(cards.getMorale()).thenReturn(morale);
        when(cards.allows(any(), any(), any())).thenReturn(true);
        when(morale.leaveBelow()).thenReturn(-40);
        when(morale.leaveAfter()).thenReturn(Duration.ofHours(48));
        when(morale.dismissalPenalty()).thenReturn(10);
        when(morale.dismissalFade()).thenReturn(Duration.ofHours(48));
        when(service.assign(any(Player.class), any(), any(), any())).thenAnswer(done());
        when(service.unassign(any(Player.class), any(), any())).thenAnswer(done());
        when(service.dismiss(any(Player.class), any(), any())).thenAnswer(done());
    }

    private static Answer<SettlerResult> done() {
        return invocation -> SettlerResult.done(new Settler());
    }

    private static Settler settler(String profession) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Tamsin Reed");
        settler.setRarity(SettlerRarity.RARE);
        settler.setProfession(profession);
        return settler;
    }

    private SettlerCardMenu card(Settler settler) {
        return new SettlerCardMenu(cards, player, SITE, settler, previous);
    }

    private ItemView professionItem(SettlerCardMenu card) {
        return view(card, 11).orElseThrow();
    }

    @Test
    void ac1_theTopItemShowsNameRarityTagAndHistory() {
        final Settler settler = settler(null);
        settler.setHistory("clans.settler.history.dungeon");
        settler.setHistoryArgs(List.of("the Sunken Keep"));

        final ItemView identity = view(card(settler), 4).orElseThrow();

        assertEquals("Tamsin Reed", text(identity.getDisplayName()));
        assertEquals(SettlerRarity.RARE.getColor(), identity.getDisplayName().color());
        assertEquals(text(SettlerTags.rarity(SettlerRarity.RARE)), text(identity.getLore().get(0)));
        assertTrue(mentions(identity.getLore().get(1), "clans.settler.history.dungeon"));
        assertTrue(text(identity.getLore().get(1)).contains("the Sunken Keep"));

        settler.setHistory(null);
        assertEquals(1, view(card(settler), 4).orElseThrow().getLore().size(), "no history, no history line");
    }

    @Test
    void ac2_theProfessionItemShowsProfessionSpecialtyAndWage() {
        final Settler mason = settler(CampProfessions.BUILDER);
        mason.setSpecialty(CampProfessions.MASON);
        when(payroll.hourly(SITE, mason)).thenReturn(1199.2);

        final ItemView builder = professionItem(card(mason));
        assertTrue(mentions(builder.getDisplayName(), professions.find(CampProfessions.BUILDER).orElseThrow().getKey()));
        assertEquals(text(SettlerTags.role(CampProfessions.MASON).orElseThrow()), text(builder.getLore().get(0)));
        assertTrue(loreMentions(builder, "clans.settler.card.wage"));
        assertTrue(loreText(builder).contains("1,200"), "the wage is rounded up");

        final ItemView farmer = professionItem(card(settler(CampProfessions.FARMER)));
        assertFalse(loreMentions(farmer, "clans.settler.card.wage"), "no wage, no wage line");

        final ItemView wanderer = professionItem(card(settler(null)));
        assertTrue(mentions(wanderer.getDisplayName(), "clans.settler.card.no_profession"));
        assertTrue(loreMentions(wanderer, "clans.settler.card.wanders"));
    }

    @Test
    void ac2_aSpecialtyWithNoTagShowsItsName() {
        professions.register(Profession.construction("weaver", "clans.settler.profession.weaver", List.of("loom")));
        final Settler settler = settler("weaver");
        settler.setSpecialty("loom");

        assertTrue(loreMentions(professionItem(card(settler)), "clans.settler.profession.weaver.specialty.loom"));
    }

    @Test
    void ac3_theMoraleItemIsColouredAndWarnsWhileUnhappy() {
        final Settler settler = settler(null);
        settler.setMorale(12);
        assertEquals(NamedTextColor.GREEN, moraleValue(settler).color());
        settler.setMorale(-5);
        assertEquals(NamedTextColor.RED, moraleValue(settler).color());
        settler.setMorale(0);
        assertEquals(NamedTextColor.GRAY, moraleValue(settler).color());
        assertFalse(loreMentions(view(card(settler), 13).orElseThrow(), "clans.settler.card.unhappy"));

        settler.setUnhappySince(1);
        final ItemView unhappy = view(card(settler), 13).orElseThrow();
        assertTrue(loreMentions(unhappy, "clans.settler.card.unhappy"));
        assertTrue(loreText(unhappy).contains("-40"));
    }

    private Component moraleValue(Settler settler) {
        final ItemView item = view(card(settler), 13).orElseThrow();
        assertTrue(mentions(item.getDisplayName(), "clans.settler.card.morale"));
        return ((TranslatableComponent) item.getDisplayName()).arguments().getFirst().asComponent();
    }

    @Test
    void ac4_theWorkItemShowsStateAndWhereItWorks() {
        final Settler farmer = settler(CampProfessions.FARMER);
        assertTrue(mentions(view(card(farmer), 15).orElseThrow().getDisplayName(), "clans.settler.card.state.idle"));
        assertTrue(view(card(farmer), 15).orElseThrow().getLore().isEmpty());

        farmer.setAssignment(CampGrounds.FARM);
        farmer.setState(SettlerState.WORKING);
        final ItemView working = view(card(farmer), 15).orElseThrow();
        assertTrue(mentions(working.getDisplayName(), "clans.settler.card.state.working"));
        assertTrue(loreMentions(working, "clans.settler.card.workplace.farm"));

        final Settler builder = settler(CampProfessions.BUILDER);
        builder.setAssignment(UUID.randomUUID().toString());
        builder.setState(SettlerState.STRIKING);
        final ItemView striking = view(card(builder), 15).orElseThrow();
        assertTrue(mentions(striking.getDisplayName(), "clans.settler.card.state.striking"));
        assertTrue(loreMentions(striking, "clans.settler.card.workplace.construction"));
    }

    @Test
    void ac5_eachKnownTraitShowsOnceTradeOffsInGold() {
        final Settler settler = settler(CampProfessions.BUILDER);
        settler.setTraits(List.of("tireless", "greedy", "forgotten"));
        final SettlerCardMenu card = card(settler);

        final ItemView tireless = named(card, "clans.settler.trait.tireless.name");
        assertEquals(NamedTextColor.AQUA, tireless.getDisplayName().color());
        assertEquals(Material.GLOWSTONE_DUST, tireless.getMaterial());
        assertTrue(loreMentions(tireless, "clans.settler.trait.tireless.description"));
        assertTrue(loreMentions(tireless, "clans.settler.card.group.builder"));

        final ItemView greedy = named(card, "clans.settler.trait.greedy.name");
        assertEquals(NamedTextColor.GOLD, greedy.getDisplayName().color());
        assertEquals(Material.REDSTONE, greedy.getMaterial());

        assertEquals(2, MenuProbe.views(card).stream()
                .filter(item -> item.getMaterial() == Material.REDSTONE || item.getMaterial() == Material.GLOWSTONE_DUST)
                .count(), "an unknown trait is skipped");
    }

    @Test
    void ac6_aFarmerIsSentToItsWorkplaceAndCalledBack() {
        final Settler farmer = settler(CampProfessions.FARMER);
        final SettlerCardMenu idle = card(farmer);
        assertFalse(slotNamed(idle, "clans.settler.card.unassign").isPresent());
        click(idle, "clans.settler.card.assign_farm", player);
        verify(service).assign(player, SITE, farmer.getId(), CampGrounds.FARM);
        verify(cards).after(eq(player), eq(SITE), eq(farmer.getId()), any(), eq(previous));

        farmer.setAssignment(CampGrounds.FARM);
        final SettlerCardMenu working = card(farmer);
        assertFalse(slotNamed(working, "clans.settler.card.assign_farm").isPresent());
        click(working, "clans.settler.card.unassign", player);
        verify(service).unassign(player, SITE, farmer.getId());
    }

    @Test
    void ac7_aBuildersCrewsButtonOpensItsCrewOrTheJobs() {
        final Settler builder = settler(CampProfessions.BUILDER);
        final SettlerCardMenu free = card(builder);
        click(free, "clans.settler.card.crews", player);
        verify(crews).openJobs(player, free);

        final UUID structure = UUID.randomUUID();
        builder.setAssignment(structure.toString());
        final SettlerCardMenu busy = card(builder);
        click(busy, "clans.settler.card.crews", player);
        verify(crews).openCrew(player, structure, busy);

        final SettlerCardMenu wanderer = card(settler(null));
        assertFalse(slotNamed(wanderer, "clans.settler.card.crews").isPresent());
        assertFalse(slotNamed(wanderer, "clans.settler.card.assign_farm").isPresent());
        assertFalse(slotNamed(wanderer, "clans.settler.card.unassign").isPresent());
    }

    @Test
    void ac8_anAssignedBuilderCanBeTakenOffItsCrew() {
        final Settler builder = settler(CampProfessions.BUILDER);
        builder.setAssignment(UUID.randomUUID().toString());

        click(card(builder), TAKE_OFF_CREW, player);

        verify(service).unassign(player, SITE, builder.getId());
        verify(cards).after(eq(player), eq(SITE), eq(builder.getId()), any(), eq(previous));
    }

    @Test
    void ac8_takingABuilderOffARunningJobShowsTheJobRunningRefusal() {
        final Settler builder = settler(CampProfessions.BUILDER);
        final String job = UUID.randomUUID().toString();
        builder.setAssignment(job);
        final Roster roster = new Roster();
        roster.getSettlers().add(builder);
        final SettlerSite site = mock(SettlerSite.class);
        when(site.roster(SITE)).thenReturn(Optional.of(roster));
        when(site.allows(any(), any(), any())).thenReturn(true);
        when(site.jobRunning(SITE, builder)).thenReturn(true);
        final SettlerService realService = new SettlerService(professions);
        realService.register(SITE.getSiteId(), site);
        final SettlerCards real = spy(MenuProbe.build(SettlerCards.class, realService, professions, traits, morale,
                payroll, crews));
        doNothing().when(real).open(player, SITE, builder.getId(), previous);

        click(new SettlerCardMenu(real, player, SITE, builder, previous), TAKE_OFF_CREW, player);

        assertTrue(told(player, "core.settler.job_running"));
        assertEquals(job, builder.getAssignment(), "the Builder stays on its job");
        verify(real).open(player, SITE, builder.getId(), previous);
    }

    @Test
    void ac8_aBuilderOnNoJobHasNoTakeOffButton() {
        assertFalse(slotNamed(card(settler(CampProfessions.BUILDER)), TAKE_OFF_CREW).isPresent());
        assertFalse(slotNamed(card(settler(CampProfessions.FARMER)), TAKE_OFF_CREW).isPresent());
    }

    @Test
    void ac9_dismissWarnsAndNeedsAShiftClick() {
        final Settler settler = settler(null);
        final SettlerCardMenu card = card(settler);
        final int slot = slotNamed(card, "clans.settler.card.dismiss").orElseThrow();
        final ItemView dismiss = view(card, slot).orElseThrow();
        assertTrue(loreMentions(dismiss, "clans.settler.card.dismiss_warning"));
        assertTrue(loreText(dismiss).contains("10"));

        click(card, slot, player, ClickType.LEFT);
        verify(service, never()).dismiss(any(Player.class), any(), any());

        click(card, slot, player, ClickType.SHIFT_LEFT);
        verify(service).dismiss(player, SITE, settler.getId());
        verify(player).closeInventory();
        verify(cards).tell(eq(player), argThat(message -> mentions(message, "clans.settler.card.dismissed")));
    }

    @Test
    void ac9_aRefusedDismissalTellsTheReasonAndReopens() {
        final Settler settler = settler(CampProfessions.BUILDER);
        final SettlerResult refused = SettlerResult.refused("core.settler.job_running");
        when(service.dismiss(player, SITE, settler.getId())).thenReturn(refused);
        final SettlerCardMenu card = card(settler);

        click(card, slotNamed(card, "clans.settler.card.dismiss").orElseThrow(), player, ClickType.SHIFT_LEFT);

        verify(cards).after(player, SITE, settler.getId(), refused, previous);
        verify(player, never()).closeInventory();
    }

    @Test
    void ac10_aRankWithoutTheActionSeesGrayButtonsThatDoNothing() {
        when(cards.allows(any(), any(), any())).thenReturn(false);
        final Settler farmer = settler(CampProfessions.FARMER);
        final SettlerCardMenu card = card(farmer);

        final ItemView farm = named(card, "clans.settler.card.assign_farm");
        assertEquals(NamedTextColor.GRAY, farm.getDisplayName().color());
        assertTrue(loreMentions(farm, "clans.settler.card.not_allowed"));
        assertTrue(farm.getClickActions().isEmpty());
        click(card, "clans.settler.card.assign_farm", player);

        final int dismiss = slotNamed(card, "clans.settler.card.dismiss").orElseThrow();
        assertTrue(loreMentions(view(card, dismiss).orElseThrow(), "clans.settler.card.not_allowed"));
        click(card, dismiss, player, ClickType.SHIFT_LEFT);

        verifyNoInteractions(service);
        verify(cards, never()).after(any(), any(), any(), any(), any());
        verify(player, never()).closeInventory();
    }

    @Test
    void ac10_buttonsAskForTheirOwnAction() {
        when(cards.allows(player, SITE, SettlerAction.ASSIGN)).thenReturn(false);
        when(cards.allows(player, SITE, SettlerAction.DISMISS)).thenReturn(true);
        final SettlerCardMenu card = card(settler(CampProfessions.FARMER));

        assertTrue(loreMentions(named(card, "clans.settler.card.assign_farm"), "clans.settler.card.not_allowed"));
        assertFalse(loreMentions(named(card, "clans.settler.card.dismiss"), "clans.settler.card.not_allowed"));
    }

    @Test
    void ac10_takingOffACrewNeedsTheAssignPermission() {
        when(cards.allows(player, SITE, SettlerAction.ASSIGN)).thenReturn(false);
        final Settler builder = settler(CampProfessions.BUILDER);
        builder.setAssignment(UUID.randomUUID().toString());
        final SettlerCardMenu card = card(builder);

        final ItemView takeOff = named(card, TAKE_OFF_CREW);
        assertEquals(NamedTextColor.GRAY, takeOff.getDisplayName().color());
        assertTrue(loreMentions(takeOff, "clans.settler.card.not_allowed"));
        assertTrue(takeOff.getClickActions().isEmpty());
        click(card, TAKE_OFF_CREW, player);

        verify(service, never()).unassign(any(Player.class), any(), any());
        verify(cards, never()).after(any(), any(), any(), any(), any());
    }

    @Test
    void ac11_aSuccessAndARefusalBothReopenTheCard() {
        final UUID settlerId = UUID.randomUUID();
        final SettlerCards real = spy(MenuProbe.build(SettlerCards.class));
        doNothing().when(real).open(player, SITE, settlerId, previous);

        real.after(player, SITE, settlerId, SettlerResult.done(new Settler()), previous);
        assertTrue(sent(player).isEmpty(), "a success says nothing");
        verify(real).open(player, SITE, settlerId, previous);

        real.after(player, SITE, settlerId, SettlerResult.refused("core.settler.job_running"), previous);
        assertTrue(told(player, "core.settler.job_running"));
        verify(real, times(2)).open(player, SITE, settlerId, previous);
    }

    @Test
    void ac11_aRefusalIsToldAndACardForAGoneSettlerDoesNotOpen() {
        final SettlerService realService = mock(SettlerService.class);
        when(realService.roster(SITE)).thenReturn(Optional.of(new Roster()));
        final SettlerCards real = MenuProbe.build(SettlerCards.class, realService, professions, traits,
                mock(CampRecruitment.class), morale, payroll, crews);

        real.after(player, SITE, UUID.randomUUID(), SettlerResult.refused("core.settler.job_running"), null);

        assertTrue(told(player, "core.settler.job_running"));
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void ac11_actionsAreAllowedOnlyWhereTheSiteIsLoaded() {
        final SettlerService realService = mock(SettlerService.class);
        when(realService.site(SITE)).thenReturn(Optional.empty());
        final SettlerCards real = MenuProbe.build(SettlerCards.class, realService);

        assertFalse(real.allows(player, SITE, SettlerAction.ASSIGN));
    }
}
