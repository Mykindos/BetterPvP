package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampRespawn;
import me.mykindos.betterpvp.clans.world.camp.CampRespawnEvent;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.site.PlayerWhereabouts;
import me.mykindos.betterpvp.core.world.site.Places;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteLandings;
import me.mykindos.betterpvp.core.world.site.Whereabouts;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BarracksUpgradesTest {

    private static final long CLAN = 11;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final CampUpgrades upgrades = new CampUpgrades(store);
    private final ClanMember ada = member("Ada");
    private final ClanMember bram = member("bram");
    private final ClanMember cleo = member("Cleo");
    private SecondDoor door;
    private PlacedStructure barracks;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        door = new SecondDoor(store, upgrades, mock(ConstructionService.class), mock(StructureCatalogue.class),
                mock(StructureShapes.class), new SiteLandings(), mock(CampRespawn.class));
        barracks = new PlacedStructure(UUID.randomUUID(), CampStructures.BARRACKS,
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        barracks.setStage(1);
        barracks.getUpgrades().put(1, SecondDoor.ID);
        camp.getHolding().getStructures().add(barracks);
    }

    @Test
    void theBarracksOffersEachAtItsStage() {
        new CasualtyBoard(mock(Clans.class), mock(ClanManager.class), upgrades, mock(CasualtyStore.class),
                mock(Places.class), mock(DamageLogManager.class));
        new MusterRoll(mock(Clans.class), mock(ClanManager.class), mock(ClientManager.class),
                mock(PlayerWhereabouts.class), upgrades);
        new InfirmaryCot(mock(Clans.class), mock(ClanManager.class), mock(CampConfig.class), upgrades,
                mock(EffectManager.class));

        assertEquals(List.of(
                        new CampUpgrades.Declared(CasualtyBoard.ID, 0),
                        new CampUpgrades.Declared(SecondDoor.ID, 1),
                        new CampUpgrades.Declared(MusterRoll.ID, 1),
                        new CampUpgrades.Declared(InfirmaryCot.ID, 1)),
                upgrades.declared(CampStructures.BARRACKS));
        assertTrue(upgrades.page(CasualtyBoard.ID).isPresent());
        assertTrue(upgrades.page(MusterRoll.ID).isPresent());
        assertTrue(upgrades.page(SecondDoor.ID).isPresent());
    }

    @Test
    void theBoardListsTheNewestDeathFirstThenMembersWhoHaveNotDied() {
        final Map<UUID, Casualty> deaths = Map.of(
                ada.getUuid(), death(ada, 100),
                cleo.getUuid(), death(cleo, 300));

        assertEquals(List.of(cleo, ada, bram), CasualtyBoard.order(List.of(ada, bram, cleo), deaths));
    }

    @Test
    void membersWithoutDeathsAreListedByName() {
        assertEquals(List.of(ada, bram, cleo), CasualtyBoard.order(List.of(cleo, bram, ada), Map.of()));
    }

    @Test
    void theRollListsOnlineMembersFirstThenTheMostRecentlySeen() {
        final Whereabouts here = new Whereabouts("clans-1", Component.text("Camp"), null, 0, 64, 0);
        final Map<UUID, Whereabouts> online = Map.of(cleo.getUuid(), here);
        final Map<UUID, Long> seen = Map.of(ada.getUuid(), 50L, bram.getUuid(), 900L);

        assertEquals(List.of(cleo, bram, ada), MusterRoll.order(List.of(ada, bram, cleo), online, seen));
    }

    @Test
    void aMemberPicksTheSecondDoorAndBack() {
        final UUID member = UUID.randomUUID();
        assertFalse(door.picked(SITE, member));

        assertNull(door.pick(SITE, member, true));
        assertTrue(door.picked(SITE, member));
        assertEquals(Set.of(member), camp.getSecondDoor());

        assertNull(door.pick(SITE, member, false));
        assertFalse(door.picked(SITE, member));
    }

    @Test
    void theDoorCannotBePickedWhileTheBarracksIsBroken() {
        barracks.setCondition(StructureCondition.DISABLED);

        assertEquals("clans.camp.upgrade.second_door.inactive", door.pick(SITE, UUID.randomUUID(), true));
    }

    @Test
    void membersWhoPickedTheSecondDoorTravelToItsLanding() {
        final Player player = mock(Player.class);
        final UUID member = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(member);
        door.pick(SITE, member, true);

        final CampRespawnEvent event = new CampRespawnEvent(player, SITE, null, null, CampRespawn.LANDING);
        door.onCampRespawn(event);

        assertEquals(SecondDoor.LANDING, event.getLanding());
        assertNull(event.getSpot());
    }

    @Test
    void membersWhoKeptTheMainDoorAreLeftAlone() {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        final CampRespawnEvent event = new CampRespawnEvent(player, SITE, null, null, CampRespawn.LANDING);
        door.onCampRespawn(event);

        assertEquals(CampRespawn.LANDING, event.getLanding());
    }

    private static ClanMember member(String name) {
        return new ClanMember(UUID.randomUUID(), ClanMember.MemberRank.MEMBER, name);
    }

    private static Casualty death(ClanMember member, long at) {
        return new Casualty(member.getUuid(), new Whereabouts("clans-1", Component.text("Camp"), null, 0, 64, 0),
                null, at);
    }
}
