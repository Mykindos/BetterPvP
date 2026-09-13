package me.mykindos.betterpvp.clans.world.camp;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.site.Admission;
import me.mykindos.betterpvp.core.world.site.Party;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteOwners;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("What the site framework is told about a clan's camp")
class CampsTest {

    private static final long OWNER = 7L;
    private static final long ALLY = 8L;

    private final UUID member = UUID.randomUUID();
    private final UUID allyMember = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();

    private ClanManager clanManager;
    private CampStore store;
    private Camps camps;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        final Clan ally = clan(ALLY, allyMember);
        final Clan owner = clan(OWNER, member);
        final List<ClanAlliance> alliances = List.of(alliance(ally));
        when(owner.getAlliances()).thenReturn(alliances);

        clanManager = mock(ClanManager.class);
        when(clanManager.getClanById(any())).thenReturn(Optional.empty());
        when(clanManager.getClanById(OWNER)).thenReturn(Optional.of(owner));
        when(clanManager.getClanById(ALLY)).thenReturn(Optional.of(ally));

        store = mock(CampStore.class);
        when(store.cached(anyLong())).thenReturn(Optional.empty());

        camps = new Camps(clanManager, store, mock(SiteInstances.class), new SiteOwners());

        final Field skins = Camps.class.getDeclaredField("skinFolder");
        skins.setAccessible(true);
        skins.set(camps, "templates/camps/");
    }

    private Clan clan(long id, UUID... members) {
        final Clan clan = mock(Clan.class);
        when(clan.getId()).thenReturn(id);

        final List<ClanMember> roster = new ArrayList<>();
        for (UUID uuid : members) {
            final ClanMember clanMember = mock(ClanMember.class);
            when(clanMember.getUuid()).thenReturn(uuid);
            roster.add(clanMember);
        }

        when(clan.getMembers()).thenReturn(roster);
        when(clan.getAlliances()).thenReturn(List.of());
        return clan;
    }

    private ClanAlliance alliance(Clan clan) {
        final ClanAlliance alliance = mock(ClanAlliance.class);
        when(alliance.getClan()).thenReturn(clan);
        return alliance;
    }

    private boolean admits(UUID... party) {
        final Admission rule = Admission.byName("clan");
        assertNotNull(rule, "registering the rule is what makes a camp reachable at all");
        return rule.admits(Camps.keyFor(OWNER), Set.of(), Party.of(party[0], Set.of(party)));
    }

    @Test
    @DisplayName("a camp is named by its clan")
    void aCampIsNamedByItsClan() {
        assertEquals(SiteKey.of("camp", OWNER), Camps.keyFor(OWNER));
    }

    @Test
    @DisplayName("a member of the clan is let in")
    void aMemberIsLetIn() {
        assertTrue(admits(member));
    }

    @Test
    @DisplayName("an ally brought along is let in, so a captain can sail home with one")
    void anAllyIsLetIn() {
        assertTrue(admits(member, allyMember));
    }

    @Test
    @DisplayName("a crew carrying anybody else is turned away as one")
    void anOutsiderTurnsTheWholeCrewAway() {
        assertFalse(admits(member, outsider));
    }

    @Test
    @DisplayName("a camp whose clan no longer exists lets nobody in")
    void aCampWithNoClanLetsNobodyIn() {
        final Admission rule = Admission.byName("clan");
        assertNotNull(rule);
        assertFalse(rule.admits(SiteKey.of("camp", 999L), Set.of(), Party.solo(member)));
    }

    @Test
    @DisplayName("a player with no clan has no camp")
    void aPlayerWithNoClanHasNoCamp() {
        final Player player = mock(Player.class);
        when(clanManager.getClanByPlayer(player)).thenReturn(Optional.empty());

        assertEquals(OptionalLong.empty(), camps.ownerOf(player));
    }

    @Test
    @DisplayName("a player's camp is their clan's")
    void aPlayersCampIsTheirClans() {
        final Player player = mock(Player.class);
        final Optional<Clan> owner = clanManager.getClanById(OWNER);
        when(clanManager.getClanByPlayer(player)).thenReturn(owner);

        assertEquals(OptionalLong.of(OWNER), camps.ownerOf(player));
    }

    @Test
    @DisplayName("a camp whose record has not been read chooses nothing, rather than the default")
    void anUnreadRecordChoosesNothing() {
        assertTrue(camps.templateFor(Camps.keyFor(OWNER)).isEmpty(),
                "answering here would rebuild a camp from the wrong template");
    }

    @Test
    @DisplayName("a camp built to a skin is built from that skin's template")
    void aSkinNamesTheTemplate() {
        when(store.cached(OWNER)).thenReturn(Optional.of(new Camp("shore", List.of())));

        assertEquals(Optional.of("templates/camps/shore"), camps.templateFor(Camps.keyFor(OWNER)));
    }

    @Test
    @DisplayName("a camp with no skin chosen takes whatever the site is configured with")
    void noSkinLeavesTheSiteDefault() {
        when(store.cached(OWNER)).thenReturn(Optional.of(new Camp()));

        assertTrue(camps.templateFor(Camps.keyFor(OWNER)).isEmpty());
    }
}
