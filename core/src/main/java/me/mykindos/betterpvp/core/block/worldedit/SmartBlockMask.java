package me.mykindos.betterpvp.core.block.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.ref.WeakReference;

public class SmartBlockMask extends AbstractMask {

    private final SmartBlockFactory smartBlockFactory;
    private final WeakReference<World> worldRef;

    public SmartBlockMask(SmartBlockFactory smartBlockFactory, World world) {
        this.smartBlockFactory = smartBlockFactory;
        this.worldRef = new WeakReference<>(world);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        final World world = worldRef.get();
        if (world == null) {
            return false; // World reference is no longer valid
        }

        Block block = BukkitAdapter.adapt(world, vector).getBlock();
        return smartBlockFactory.isSmartBlock(block);
    }

    @Override
    public Mask copy() {
        return new SmartBlockMask(smartBlockFactory, worldRef.get());
    }
}
