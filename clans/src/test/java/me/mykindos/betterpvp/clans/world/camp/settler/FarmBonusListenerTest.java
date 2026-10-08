package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FarmBonusListenerTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camps camps = mock(Camps.class);
    private final ZoneManager zones = mock(ZoneManager.class);
    private final FarmWorkplace farm = mock(FarmWorkplace.class);
    private final World world = mock(World.class);
    private final Location at = new Location(world, 3, 64, 7);
    private final Ageable crop = mock(Ageable.class);
    private final BlockState state = mock(BlockState.class);
    private final FarmBonusListener listener = new FarmBonusListener(camps, zones, farm);

    @BeforeEach
    void setUp() {
        when(camps.clanOf(world)).thenReturn(OptionalLong.of(CLAN));
        when(zones.hasTagAt(at, CampGrounds.FARM)).thenReturn(true);
        when(state.getBlockData()).thenReturn(crop);
        when(state.getLocation()).thenReturn(at);
        when(crop.getMaximumAge()).thenReturn(7);
    }

    private void grow(int age) {
        when(crop.getAge()).thenReturn(age);
        listener.onGrow(new BlockGrowEvent(mock(Block.class), state));
    }

    @Test
    void ac22_aFarmCropGrowsAnExtraStageForEveryWholeShareOfGrowth() {
        when(farm.growth(SITE)).thenReturn(1.0);
        grow(3);
        verify(crop).setAge(4);
        verify(state).setBlockData(crop);
    }

    @Test
    void ac22_aCropNeverGrowsPastFullyGrown() {
        when(farm.growth(SITE)).thenReturn(1.0);
        grow(7);
        verify(crop).setAge(7);
    }

    @Test
    void ac22_cropsOffTheFarmOrWithNoBonusGrowNormally() {
        when(farm.growth(SITE)).thenReturn(0.0);
        grow(3);
        verify(crop, never()).setAge(anyInt());

        when(farm.growth(SITE)).thenReturn(1.0);
        when(zones.hasTagAt(at, CampGrounds.FARM)).thenReturn(false);
        grow(3);
        verify(crop, never()).setAge(anyInt());

        when(zones.hasTagAt(at, CampGrounds.FARM)).thenReturn(true);
        when(camps.clanOf(world)).thenReturn(OptionalLong.empty());
        grow(3);
        verify(crop, never()).setAge(anyInt());
    }

    private ItemStack harvest(int age) {
        when(crop.getAge()).thenReturn(age);
        final ItemStack dropped = mock(ItemStack.class);
        final ItemStack extra = mock(ItemStack.class);
        when(dropped.clone()).thenReturn(extra);
        final Item item = mock(Item.class);
        when(item.getItemStack()).thenReturn(dropped);
        listener.onHarvest(new BlockDropItemEvent(mock(Block.class), state, mock(Player.class),
                new ArrayList<>(List.of(item))));
        return extra;
    }

    @Test
    void ac23_aRipeFarmCropDropsOneMoreOfWhatItGave() {
        when(farm.extraDrop(SITE)).thenReturn(1.0);
        final ItemStack extra = harvest(7);
        verify(extra).setAmount(1);
        verify(world).dropItemNaturally(any(Location.class), eq(extra));
    }

    @Test
    void ac23_unripeCropsCropsOffTheFarmAndNoChanceDropNothingMore() {
        when(farm.extraDrop(SITE)).thenReturn(1.0);
        harvest(6);
        when(zones.hasTagAt(at, CampGrounds.FARM)).thenReturn(false);
        harvest(7);
        when(zones.hasTagAt(at, CampGrounds.FARM)).thenReturn(true);
        when(farm.extraDrop(SITE)).thenReturn(0.0);
        harvest(7);

        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
