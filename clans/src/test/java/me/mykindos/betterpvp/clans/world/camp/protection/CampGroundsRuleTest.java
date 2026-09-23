package me.mykindos.betterpvp.clans.world.camp.protection;

import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.world.zone.ZoneActionContext;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampGroundsRuleTest {

    private static final long CLAN = 7L;

    private final Camps camps = mock(Camps.class);
    private final CampPermissions permissions = mock(CampPermissions.class);
    private final ClientManager clientManager = mock(ClientManager.class, RETURNS_DEEP_STUBS);
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);

    @BeforeEach
    void setUp() {
        when(player.getWorld()).thenReturn(world);
        when(camps.clanOf(world)).thenReturn(OptionalLong.of(CLAN));
    }

    @Test
    void nobodyChangesTheLand() {
        when(camps.isMember(player, world)).thenReturn(true);
        final CampGroundsRule rule = new CampGroundsRule(camps, permissions, clientManager, false);

        assertEquals(Event.Result.DENY, rule.evaluate(context(ZoneInteraction.BREAK, block(Material.WHEAT))));
        assertEquals(Event.Result.DENY, rule.evaluate(context(ZoneInteraction.PLACE, block(Material.STONE))));
    }

    @Test
    void membersPlantAndHarvestCropsInTheFarm() {
        when(camps.isMember(player, world)).thenReturn(true);
        final CampGroundsRule rule = new CampGroundsRule(camps, permissions, clientManager, true);

        assertEquals(Event.Result.ALLOW, rule.evaluate(context(ZoneInteraction.BREAK, block(Material.WHEAT))));
        assertEquals(Event.Result.ALLOW, rule.evaluate(context(ZoneInteraction.PLACE, block(Material.CARROTS))));
        assertEquals(Event.Result.DENY, rule.evaluate(context(ZoneInteraction.BREAK, block(Material.DIRT))));
    }

    @Test
    void visitorsCannotFarm() {
        final CampGroundsRule rule = new CampGroundsRule(camps, permissions, clientManager, true);

        assertEquals(Event.Result.DENY, rule.evaluate(context(ZoneInteraction.BREAK, block(Material.WHEAT))));
    }

    @Test
    void containersOpenOnlyForThoseTheCampAllows() {
        final CampGroundsRule rule = new CampGroundsRule(camps, permissions, clientManager, false);
        final Block chest = block(Material.CHEST);
        when(chest.getState()).thenReturn(mock(Chest.class));

        assertEquals(Event.Result.DENY, rule.evaluate(context(ZoneInteraction.INTERACT, chest)));

        when(permissions.mayOpenContainers(player, CLAN)).thenReturn(true);
        assertEquals(Event.Result.DEFAULT, rule.evaluate(context(ZoneInteraction.INTERACT, chest)));
    }

    @Test
    void staffAreLeftAlone() {
        when(clientManager.search().online(player).isAdministrating()).thenReturn(true);
        final CampGroundsRule rule = new CampGroundsRule(camps, permissions, clientManager, false);

        assertEquals(Event.Result.DEFAULT, rule.evaluate(context(ZoneInteraction.BREAK, block(Material.STONE))));
    }

    private ZoneActionContext context(ZoneInteraction interaction, Block block) {
        return ZoneActionContext.builder().player(player).interaction(interaction).block(block).build();
    }

    private Block block(Material type) {
        final Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.getBlockData()).thenReturn(mock(BlockData.class));
        return block;
    }
}
