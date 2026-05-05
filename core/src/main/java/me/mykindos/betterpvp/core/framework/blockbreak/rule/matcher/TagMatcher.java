package me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Matches any block whose material is in the given Bukkit {@link Tag}.
 * The set returned by {@link #knownMaterials()} is captured at construction time;
 * Bukkit tags are server-static so this is safe.
 */
public final class TagMatcher implements BlockMatcher {

    private final Tag<Material> tag;
    private final Set<Material> snapshot;

    public TagMatcher(@NotNull Tag<Material> tag) {
        this.tag = tag;
        this.snapshot = Set.copyOf(tag.getValues());
    }

    @Override
    public boolean matches(@NotNull Block block) {
        return tag.isTagged(block.getType());
    }

    @Override
    public @NotNull Set<Material> knownMaterials() {
        return snapshot;
    }
}
