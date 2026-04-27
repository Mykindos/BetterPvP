package me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

@EqualsAndHashCode
public final class MaterialSetMatcher implements BlockMatcher {

    private final Set<Material> materials;

    public MaterialSetMatcher(@NotNull Set<Material> materials) {
        this.materials = materials.isEmpty()
                ? EnumSet.noneOf(Material.class)
                : EnumSet.copyOf(materials);
    }

    public static MaterialSetMatcher of(Material... materials) {
        final Set<Material> set = EnumSet.noneOf(Material.class);
        for (Material m : materials) set.add(m);
        return new MaterialSetMatcher(set);
    }

    @Override
    public boolean matches(@NotNull Block block) {
        return materials.contains(block.getType());
    }

    @Override
    public @NotNull Set<Material> knownMaterials() {
        return Set.copyOf(materials);
    }
}
