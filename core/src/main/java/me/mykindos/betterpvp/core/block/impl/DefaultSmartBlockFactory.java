package me.mykindos.betterpvp.core.block.impl;

import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import java.util.Optional;

public class DefaultSmartBlockFactory implements SmartBlockFactory {
    @Override
    public Optional<SmartBlockInstance> from(Location location) {
        return Optional.empty();
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        return Optional.empty();
    }

    @Override
    public Optional<SmartBlockInstance> load(Block block) {
        return Optional.empty();
    }

    @Override
    public boolean isSmartBlock(Block block) {
        return false;
    }

    @Override
    public boolean isSmartBlock(Location location) {
        return false;
    }

    @Override
    public boolean isSmartBlock(Entity entity) {
        return false;
    }

    @Override
    public BlockData createBlockData(SmartBlock type) {
        return Material.STONE.createBlockData();
    }
}
