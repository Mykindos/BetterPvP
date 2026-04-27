package me.mykindos.betterpvp.core.framework.blockbreak.rule.preset;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A named, reusable {@link BlockMatcher}. Statically declared groups
 * (see {@link BlockGroups}) act as canonical building blocks for tool rules.
 */
@Getter
public final class BlockGroup implements BlockMatcher {

    private final String id;
    private final BlockMatcher delegate;

    public BlockGroup(@NotNull String id, @NotNull BlockMatcher delegate) {
        this.id = id;
        this.delegate = delegate;
    }

    @Override
    public boolean matches(@NotNull Block block) {
        return delegate.matches(block);
    }

    @Override
    public @NotNull Set<Material> knownMaterials() {
        return delegate.knownMaterials();
    }
}
