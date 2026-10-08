package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.Trait;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.click;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreMentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.loreText;
import static me.mykindos.betterpvp.clans.testing.Messages.mentions;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.named;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.slotNamed;
import static me.mykindos.betterpvp.clans.testing.Messages.told;
import static me.mykindos.betterpvp.clans.world.camp.settler.menu.MenuProbe.view;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateMenuTest {

    private static final SiteKey SITE = Camps.keyFor(42L);
    private static final String HIRE = "clans.settler.recruit.hire";
    private static final String REJECT = "clans.settler.recruit.turn_away";

    private final SettlerCards cards = mock(SettlerCards.class);
    private final CampRecruitment recruitment = mock(CampRecruitment.class);
    private final ProfessionRegistry professions = new ProfessionRegistry();
    private final TraitRegistry traits = new TraitRegistry();
    private final Player player = mock(Player.class);
    private final List<Windowed> boards = new ArrayList<>();
    private final Supplier<Windowed> previous = () -> {
        final Windowed board = mock(Windowed.class);
        boards.add(board);
        return board;
    };
    private final SettlerCandidate candidate = new SettlerCandidate(settler(), 15_000, 0);

    private MockedConstruction<BackButton> backButtons;

    @BeforeAll
    static void load() {
        MenuProbe.load();
    }

    @BeforeEach
    void setUp() {
        backButtons = mockConstruction(BackButton.class);
        new CampProfessions(professions);
        traits.register(Trait.builder().id("haggler").key("clans.settler.trait.haggler")
                .group(TraitGroup.SITE_WIDE).build());
        when(cards.getRecruitment()).thenReturn(recruitment);
        when(cards.getProfessions()).thenReturn(professions);
        when(cards.getTraits()).thenReturn(traits);
        when(cards.allows(player, SITE, SettlerAction.HIRE)).thenReturn(true);
        when(recruitment.price(SITE, candidate)).thenReturn(4_500L);
        when(recruitment.hire(any(), any(), any())).thenReturn(SettlerResult.done(candidate.getSettler()));
        when(recruitment.turnAway(any(), any(), any())).thenReturn(SettlerResult.done(candidate.getSettler()));
    }

    @AfterEach
    void tearDown() {
        backButtons.close();
    }

    private static Settler settler() {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Aldric Tanner");
        settler.setRarity(SettlerRarity.UNCOMMON);
        settler.setProfession(CampProfessions.BUILDER);
        settler.setTraits(List.of("haggler"));
        return settler;
    }

    private void assertWentBackToAFreshMenu() {
        assertEquals(2, boards.size(), "one menu for Back and a fresh one on leaving");
        verify(boards.getFirst(), never()).show(player);
        verify(boards.getLast()).show(player);
    }

    private void assertStayed() {
        boards.forEach(board -> verify(board, never()).show(player));
    }

    private CandidateMenu menu(Supplier<Windowed> back) {
        return new CandidateMenu(cards, player, SITE, candidate, back);
    }

    @Test
    void ac19_aCandidateShowsWhoItIsWhatItCostsAndWhatItDoes() {
        final CandidateMenu menu = menu(previous);

        final ItemView identity = view(menu, 4).orElseThrow();
        assertTrue(loreMentions(identity, "clans.settler.recruit.price"));
        assertTrue(loreText(identity).contains("4,500"), "the price after the camp's discounts");
        assertFalse(loreMentions(identity, "clans.settler.recruit.waits"), "no expiry, no expiry line");
        assertTrue(mentions(view(menu, 13).orElseThrow().getDisplayName(),
                professions.find(CampProfessions.BUILDER).orElseThrow().getKey()));
        assertTrue(slotNamed(menu, "clans.settler.trait.haggler.name").isPresent());
    }

    @Test
    void ac19_aFreeCandidateAndOneThatExpires() {
        when(recruitment.price(SITE, candidate)).thenReturn(0L);
        candidate.setExpiresAt(System.currentTimeMillis() + 3_600_000);

        final ItemView identity = view(menu(previous), 4).orElseThrow();

        assertTrue(loreMentions(identity, "clans.settler.recruit.free"));
        assertTrue(loreMentions(identity, "clans.settler.recruit.waits"));
    }

    @Test
    void ac20_hiringThanksThePlayerAndGoesBack() {
        click(menu(previous), HIRE, player);

        verify(recruitment).hire(player, SITE, candidate.getSettler().getId());
        verify(cards).tell(eq(player), argThat(message -> mentions(message, "clans.settler.recruit.hired")));
        assertWentBackToAFreshMenu();
    }

    @Test
    void ac20_hiringWithNowhereToGoBackCloses() {
        click(menu(null), HIRE, player);

        verify(player).closeInventory();
    }

    @Test
    void ac20_aRefusedHireTellsTheReasonAndStays() {
        final SettlerResult broke = SettlerResult.refused("clans.settler.recruit.cannot_afford");
        when(recruitment.hire(any(), any(), any())).thenReturn(broke);

        click(menu(previous), HIRE, player);

        verify(cards).tell(player, broke.getReason());
        assertStayed();
        verify(player, never()).closeInventory();
    }

    @Test
    void ac21_rejectingTurnsItAwayAndGoesBack() {
        click(menu(previous), REJECT, player);

        verify(recruitment).turnAway(player, SITE, candidate.getSettler().getId());
        assertWentBackToAFreshMenu();
    }

    @Test
    void ac21_aRefusedRejectionTellsTheReasonAndStays() {
        final SettlerResult refused = SettlerResult.refused("clans.settler.recruit.members_only");
        when(recruitment.turnAway(any(), any(), any())).thenReturn(refused);

        click(menu(previous), REJECT, player);

        verify(cards).tell(player, refused.getReason());
        assertStayed();
    }

    @Test
    void ac22_withoutTheHirePermissionBothButtonsDoNothing() {
        when(cards.allows(player, SITE, SettlerAction.HIRE)).thenReturn(false);
        final CandidateMenu menu = menu(previous);

        for (String button : List.of(HIRE, REJECT)) {
            final ItemView view = named(menu, button);
            assertTrue(loreMentions(view, "clans.settler.card.not_allowed"));
            assertTrue(view.getClickActions().isEmpty());
            click(menu, button, player);
        }

        verify(recruitment, never()).hire(any(), any(), any());
        verify(recruitment, never()).turnAway(any(), any(), any());
    }

    @Test
    void ac23_aCandidateThatHasGoneSaysSo() {
        final CampRecruitment real = mock(CampRecruitment.class);
        when(real.find(eq(SITE), any())).thenReturn(Optional.empty());
        final SettlerCards cardsWithRecruitment = MenuProbe.build(SettlerCards.class, real);

        cardsWithRecruitment.openCandidate(player, SITE, UUID.randomUUID(), previous);

        assertTrue(told(player, "clans.settler.recruit.gone"));
    }
}
