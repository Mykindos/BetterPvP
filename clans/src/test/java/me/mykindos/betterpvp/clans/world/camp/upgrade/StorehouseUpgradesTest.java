package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceChests;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorehouseUpgradesTest {

    private static final long CLAN = 5;

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final ClanManager clanManager = mock(ClanManager.class);
    private final Clan clan = mock(Clan.class);
    private RankLockbox lockbox;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(clan));
        when(clan.getMemberByUUID(any(UUID.class))).thenReturn(Optional.empty());
        lockbox = new RankLockbox(new CampUpgrades(store), store, clanManager);
    }

    private Player member(ClanMember.MemberRank rank) {
        final UUID id = UUID.randomUUID();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        final ClanMember member = mock(ClanMember.class);
        when(member.getRank()).thenReturn(rank);
        when(clan.getMemberByUUID(id)).thenReturn(Optional.of(member));
        return player;
    }

    @Test
    void sharesSplitEvenlyWithTheRemainderUpFront() {
        assertArrayEquals(new int[]{4, 3, 3}, TallyBoard.shares(10, 3));
        assertArrayEquals(new int[]{0, 0}, TallyBoard.shares(0, 2));
        assertArrayEquals(new int[0], TallyBoard.shares(10, 0));
    }

    @Test
    void itemNamesMatchIgnoringCase() {
        assertTrue(ItemFinder.matches("Iron Ingot", " iron "));
        assertTrue(ItemFinder.matches("RAW IRON", "raw ir"));
        assertFalse(ItemFinder.matches("Oak Log", "stone"));
        assertFalse(ItemFinder.matches("Oak Log", "  "));
    }

    @Test
    void lockboxOpensForLeadersAndAdminsByDefault() {
        assertEquals(EnumSet.of(ClanMember.MemberRank.LEADER, ClanMember.MemberRank.ADMIN), lockbox.ranks(CLAN));
        assertTrue(lockbox.mayOpen(member(ClanMember.MemberRank.ADMIN), CLAN));
        assertFalse(lockbox.mayOpen(member(ClanMember.MemberRank.MEMBER), CLAN));
        assertFalse(lockbox.mayOpen(mock(Player.class), CLAN));
    }

    @Test
    void onlyTheLeaderChangesWhoOpensIt() {
        final Player admin = member(ClanMember.MemberRank.ADMIN);
        assertEquals("clans.camp.upgrade.rank_lockbox.leader_only",
                lockbox.toggle(admin, CLAN, ClanMember.MemberRank.MEMBER));

        final Player leader = member(ClanMember.MemberRank.LEADER);
        assertNull(lockbox.toggle(leader, CLAN, ClanMember.MemberRank.MEMBER));
        assertNull(lockbox.toggle(leader, CLAN, ClanMember.MemberRank.ADMIN));
        assertEquals(Set.of(ClanMember.MemberRank.LEADER, ClanMember.MemberRank.MEMBER), lockbox.ranks(CLAN));
        assertTrue(lockbox.mayOpen(member(ClanMember.MemberRank.MEMBER), CLAN));
        assertFalse(lockbox.mayOpen(admin, CLAN));
    }

    @Test
    void theLeaderCanNeverBeLockedOut() {
        final Player leader = member(ClanMember.MemberRank.LEADER);
        assertNull(lockbox.toggle(leader, CLAN, ClanMember.MemberRank.LEADER));
        assertTrue(lockbox.mayOpen(leader, CLAN));
    }

    @Test
    void everyStorehouseUpgradeIsDeclaredAtItsStage() {
        final CampUpgrades upgrades = new CampUpgrades(store);
        new RankLockbox(upgrades, store, clanManager);
        final StorehouseChests chests = mock(StorehouseChests.class);
        new SortingTable(upgrades, chests, clanManager);
        new TallyBoard(upgrades, store, mock(ResourceChests.class), mock(CampResources.class), chests);
        new ItemFinder(upgrades, chests);
        new RemoteAccess(upgrades, chests, mock(CampPermissions.class));

        assertEquals(List.of(new CampUpgrades.Declared(RankLockbox.ID, 0),
                        new CampUpgrades.Declared(SortingTable.ID, 0),
                        new CampUpgrades.Declared(TallyBoard.ID, 0),
                        new CampUpgrades.Declared(ItemFinder.ID, 1),
                        new CampUpgrades.Declared(RemoteAccess.ID, 2)),
                upgrades.declared(CampStructures.STOREHOUSE));
        assertTrue(upgrades.page(SortingTable.ID).isPresent());
        assertTrue(upgrades.page(RemoteAccess.ID).isPresent());
    }
}
