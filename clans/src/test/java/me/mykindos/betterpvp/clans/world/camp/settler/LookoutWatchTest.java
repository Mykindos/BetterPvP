package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LookoutWatchTest {

    private static final long CLAN = 42;
    private static final long OTHER_CLAN = 43;

    private final Camps camps = mock(Camps.class);
    private final ClanManager clanManager = mock(ClanManager.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final World campWorld = mock(World.class);
    private final World otherCamp = mock(World.class);
    private final Player online = mock(Player.class);
    private final Player offline = mock(Player.class);
    private final Roster roster = new Roster();
    private final Roster otherRoster = new Roster();
    private MockedStatic<Bukkit> bukkit;
    private LookoutWatch watch;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        when(camps.clanOf(campWorld)).thenReturn(OptionalLong.of(CLAN));
        when(camps.clanOf(otherCamp)).thenReturn(OptionalLong.of(OTHER_CLAN));
        when(settlers.roster(Camps.keyFor(CLAN))).thenReturn(Optional.of(roster));
        when(settlers.roster(Camps.keyFor(OTHER_CLAN))).thenReturn(Optional.of(otherRoster));

        clan(CLAN, online, offline);
        clan(OTHER_CLAN, online);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenAnswer(invocation -> {
            final UUID id = invocation.getArgument(0);
            return id.equals(online.getUniqueId()) ? online : null;
        });
        when(online.getUniqueId()).thenReturn(UUID.randomUUID());
        when(offline.getUniqueId()).thenReturn(UUID.randomUUID());

        watch = new LookoutWatch(camps, clanManager, settlers, new CampWideTraits(ShippedSettlers.config()));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private void clan(long id, Player... members) {
        final Clan clan = mock(Clan.class);
        final List<ClanMember> list = new ArrayList<>();
        for (Player member : members) {
            final ClanMember clanMember = mock(ClanMember.class);
            when(clanMember.getUuid()).thenAnswer(invocation -> member.getUniqueId());
            list.add(clanMember);
        }
        when(clan.getMembers()).thenReturn(list);
        when(clanManager.getClanById(id)).thenReturn(Optional.of(clan));
    }

    private static void lookout(Roster roster) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(SettlerRarity.RARE);
        settler.setTraits(new ArrayList<>(List.of(CampTraits.LOOKOUT)));
        roster.getSettlers().add(settler);
    }

    private Player arrive(World world, boolean member) {
        final Player visitor = mock(Player.class);
        when(visitor.getUniqueId()).thenReturn(UUID.randomUUID());
        when(visitor.getName()).thenReturn("Raider");
        when(visitor.getWorld()).thenReturn(world);
        when(camps.isMember(visitor, world)).thenReturn(member);
        watch.onArrive(new PlayerChangedWorldEvent(visitor, mock(World.class)));
        return visitor;
    }

    private void arrive(Player visitor, World world) {
        when(visitor.getWorld()).thenReturn(world);
        when(camps.isMember(visitor, world)).thenReturn(false);
        watch.onArrive(new PlayerChangedWorldEvent(visitor, mock(World.class)));
    }

    @Test
    void ac32_aLookoutWarnsOnlineMembersWhenAnOutsiderArrives() {
        lookout(roster);
        arrive(campWorld, false);
        verify(online).sendMessage(any(Component.class));
        verify(offline, never()).sendMessage(any(Component.class));
    }

    @Test
    void ac32_membersAndCampsWithoutALookoutWarnNobody() {
        arrive(campWorld, false);
        verify(online, never()).sendMessage(any(Component.class));

        lookout(roster);
        arrive(campWorld, true);
        verify(online, never()).sendMessage(any(Component.class));
    }

    @Test
    void ac33_theSameVisitorIsReportedOnceInAWhilePerCamp() {
        lookout(roster);
        lookout(otherRoster);
        final Player visitor = arrive(campWorld, false);
        arrive(visitor, campWorld);
        verify(online, times(1)).sendMessage(any(Component.class));

        arrive(visitor, otherCamp);
        verify(online, times(2)).sendMessage(any(Component.class));

        arrive(campWorld, false);
        verify(online, times(3)).sendMessage(any(Component.class));
    }
}
