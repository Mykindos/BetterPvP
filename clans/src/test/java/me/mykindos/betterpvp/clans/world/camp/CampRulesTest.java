package me.mykindos.betterpvp.clans.world.camp;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceChests;
import me.mykindos.betterpvp.clans.world.camp.resource.ResourceOverflow;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampRulesTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = SiteKey.of(Camps.SITE_ID, CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final ResourceChests chests = mock(ResourceChests.class);
    private final CampConfig config = mock(CampConfig.class);
    private final ClanManager clanManager = mock(ClanManager.class);
    private CampResources resources;
    private CampPermissions permissions;
    private CampConstruction construction;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(config.defaultPermissions()).thenReturn(Map.of(
                ClanMember.MemberRank.LEADER, EnumSet.allOf(ConstructionAction.class),
                ClanMember.MemberRank.RECRUIT, EnumSet.of(ConstructionAction.CLAIM)));
        resources = new CampResources(store, chests);
        permissions = new CampPermissions(clanManager, store, config);
        construction = new CampConstruction(store, resources, permissions, config,
                new ResourceOverflow(store, resources, config), mock(ConstructionService.class));
    }

    @Test
    void spendingAndRefundingMoveTheBalance() {
        camp.getResources().put("wood", 50);

        assertTrue(resources.canAfford(SITE, ResourceCost.of(Map.of("wood", 50))));
        assertFalse(resources.canAfford(SITE, ResourceCost.of(Map.of("wood", 51))));

        resources.spend(SITE, ResourceCost.of(Map.of("wood", 30)));
        assertEquals(20, camp.getResource("wood"));
        resources.refund(SITE, ResourceCost.of(Map.of("wood", 5, "iron", 2)));
        assertEquals(25, camp.getResource("wood"));
        assertEquals(2, camp.getResource("iron"));
        verify(store, atLeastOnce()).changed(CLAN);
    }

    @Test
    void aHigherTierWaitsForTheGreatHall() {
        final StructureType forge = type("forge", 2);

        assertTrue(construction.blocked(SITE, camp.getHolding(), forge, 0).isPresent(), "no hall at all");

        final PlacedStructure hall = new PlacedStructure(UUID.randomUUID(), CampConstruction.GREAT_HALL,
                new StructurePosition(), StructureCondition.ACTIVE);
        camp.getHolding().getStructures().add(hall);
        assertTrue(construction.blocked(SITE, camp.getHolding(), forge, 0).isPresent(), "a first-stage hall is tier 1");

        hall.setStage(1);
        assertTrue(construction.blocked(SITE, camp.getHolding(), forge, 0).isEmpty());
        assertTrue(construction.blocked(SITE, camp.getHolding(), type("storehouse", 1), 0).isEmpty(),
                "tier 1 never waits");
    }

    @Test
    void demolishingAChestDropsWhatNoLongerFitsLargestFirst() {
        camp.getResources().put("wood", 300);
        camp.getResources().put("stone", 100);
        when(chests.capacity(any())).thenReturn(250);

        construction.contents().forEach(contents -> contents.drop(SITE,
                new PlacedStructure(UUID.randomUUID(), "storehouse", new StructurePosition(), StructureCondition.ACTIVE),
                mock(Location.class)));

        assertEquals(150, camp.getResource("wood"), "150 over, all of it taken from the larger balance");
        assertEquals(100, camp.getResource("stone"));
    }

    @Test
    void ranksDoWhatTheDefaultsAllowUntilTheClanChangesIt() {
        final Player leader = member(ClanMember.MemberRank.LEADER);
        final Player recruit = member(ClanMember.MemberRank.RECRUIT);

        assertTrue(construction.allows(leader, SITE, ConstructionAction.DEMOLISH));
        assertTrue(construction.allows(recruit, SITE, ConstructionAction.CLAIM));
        assertFalse(construction.allows(recruit, SITE, ConstructionAction.BUILD));

        permissions.set(CLAN, ClanMember.MemberRank.RECRUIT, ConstructionAction.BUILD, true);
        assertTrue(construction.allows(recruit, SITE, ConstructionAction.BUILD));
        assertEquals(Set.of(ConstructionAction.CLAIM), config.defaultPermissions().get(ClanMember.MemberRank.RECRUIT),
                "changing a camp never touches the defaults");
    }

    @Test
    void outsidersMayDoNothing() {
        final Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        final Clan clan = mock(Clan.class);
        when(clan.getMemberByUUID(any(UUID.class))).thenReturn(Optional.empty());
        when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(clan));

        assertFalse(construction.allows(stranger, SITE, ConstructionAction.CLAIM));
    }

    @Test
    void alliesDoWhatTheAllyRowAllows() {
        final UUID allyId = UUID.randomUUID();
        final Player ally = mock(Player.class);
        when(ally.getUniqueId()).thenReturn(allyId);
        final ClanMember allyMember = mock(ClanMember.class);
        when(allyMember.getUuid()).thenReturn(allyId);
        final IClan allied = mock(IClan.class);
        when(allied.getMembers()).thenReturn(List.of(allyMember));
        final Clan owner = mock(Clan.class);
        when(owner.getMemberByUUID(any(UUID.class))).thenReturn(Optional.empty());
        when(owner.getAlliances()).thenReturn(List.of(new ClanAlliance(allied, false)));
        when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(owner));
        when(config.defaultAllyActions()).thenReturn(Set.of());
        final Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());

        assertFalse(construction.allows(ally, SITE, ConstructionAction.CLAIM));
        assertFalse(permissions.mayOpenContainers(ally, CLAN));

        permissions.setAlly(CLAN, ConstructionAction.CLAIM, true);
        permissions.setAllyContainers(CLAN, true);

        assertTrue(construction.allows(ally, SITE, ConstructionAction.CLAIM));
        assertTrue(permissions.mayOpenContainers(ally, CLAN));
        assertFalse(construction.allows(stranger, SITE, ConstructionAction.CLAIM), "only allies get the Ally row");
        assertFalse(permissions.mayOpenContainers(stranger, CLAN));
    }

    private Player member(ClanMember.MemberRank rank) {
        final UUID id = UUID.randomUUID();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        final ClanMember member = mock(ClanMember.class);
        when(member.getRank()).thenReturn(rank);
        final Clan clan = clanManager.getClanById(CLAN).orElseGet(() -> {
            final Clan created = mock(Clan.class);
            when(clanManager.getClanById(CLAN)).thenReturn(Optional.of(created));
            return created;
        });
        when(clan.getMemberByUUID(id)).thenReturn(Optional.of(member));
        return player;
    }

    private static StructureType type(String id, int tier) {
        final StructureType type = mock(StructureType.class);
        when(type.getId()).thenReturn(id);
        when(type.getTier()).thenReturn(tier);
        return type;
    }
}
