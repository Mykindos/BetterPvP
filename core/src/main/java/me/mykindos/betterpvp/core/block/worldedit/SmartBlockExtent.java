package me.mykindos.betterpvp.core.block.worldedit;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Disables editing smart blocks in WorldEdit.
 */
public class SmartBlockExtent extends AbstractDelegateExtent {

    private final WeakReference<World> worldRef;
    private final SmartBlockFactory factory;

    public SmartBlockExtent(Extent extent, World world, SmartBlockFactory factory) {
        super(extent);
        this.factory = factory;
        this.worldRef = new WeakReference<>(world);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        // If the block is a smart block, we don't allow setting it in WorldEdit.
        if (factory.isSmartBlock(adapt(position))) {
            return false;
        }

        // Otherwise, we delegate to the parent extent.
        return super.setBlock(position, block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        // If the block is a smart block, we don't allow setting it in WorldEdit.
        if (factory.isSmartBlock(adapt(BlockVector3.at(x, y, z)))) {
            return false;
        }

        // Otherwise, we delegate to the parent extent.
        return super.setBlock(x, y, z, block);
    }

    @Override
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        // If the region contains smart blocks, we don't allow setting them in WorldEdit.
        Set<BlockVector3> filtered = new HashSet<>();
        for (BlockVector3 position : region) {
            Block blockAtPosition = adapt(position);
            if (!factory.isSmartBlock(blockAtPosition)) {
                filtered.add(position);
            }
        }

        // Otherwise, we delegate to the parent extent.
        return setBlocks(filtered, block);
    }

    @Override
    public int setBlocks(Set<BlockVector3> vset, Pattern pattern) {
        // If the set contains smart blocks, remove them from the set.
        vset.removeIf(position -> factory.isSmartBlock(adapt(position)));

        // Otherwise, we delegate to the parent extent.
        return super.setBlocks(vset, pattern);
    }

    @Override
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        // fixme: doesnt support NBT (we probably don't need it)
        final Mask blockMask = new BlockMask(getExtent(), filter);
        final Mask negated = Masks.negate(new SmartBlockMask(factory, worldRef.get()));
        final Mask union = new MaskUnion(blockMask, negated);

        // Otherwise, we delegate to the parent extent.
        return super.replaceBlocks(region, union, replacement);
    }

    @Override
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        // Masks
        final Mask negated = Masks.negate(new SmartBlockMask(factory, worldRef.get()));
        final Mask union = new MaskUnion(mask, negated);

        // Otherwise, we delegate to the parent extent.
        return super.replaceBlocks(region, union, pattern);
    }

    @Override
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        // Masks
        // fixme: doesnt support NBT (we probably don't need it)
        final Mask blockMask = new BlockMask(getExtent(), filter);
        final Mask negated = Masks.negate(new SmartBlockMask(factory, worldRef.get()));
        final Mask union = new MaskUnion(blockMask, negated);

        // Otherwise, we delegate to the parent extent.
        return super.replaceBlocks(region, union, pattern);
    }

    private Block adapt(BlockVector3 position) {
        final World world = this.worldRef.get();
        if (world == null) {
            return null;
        }
        return BukkitAdapter.adapt(world, position).getBlock();
    }
}
