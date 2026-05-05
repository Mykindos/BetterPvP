package me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockMatcher;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Boolean-algebra composition of child matchers.
 * <p>
 * {@link #knownMaterials()} is a static over-approximation:
 * <ul>
 *   <li>AND: intersection of children</li>
 *   <li>OR / XOR: union of children</li>
 *   <li>NOT: all materials minus the child (single child only)</li>
 * </ul>
 */
public final class CompositeMatcher implements BlockMatcher {

    private final MatcherOp op;
    private final List<BlockMatcher> children;

    private CompositeMatcher(MatcherOp op, List<BlockMatcher> children) {
        this.op = op;
        this.children = children;
    }

    public static CompositeMatcher and(BlockMatcher... matchers) {
        Preconditions.checkArgument(matchers.length >= 1, "AND needs at least one child");
        return new CompositeMatcher(MatcherOp.AND, List.of(matchers));
    }

    public static CompositeMatcher or(BlockMatcher... matchers) {
        Preconditions.checkArgument(matchers.length >= 1, "OR needs at least one child");
        return new CompositeMatcher(MatcherOp.OR, List.of(matchers));
    }

    public static CompositeMatcher xor(BlockMatcher a, BlockMatcher b) {
        return new CompositeMatcher(MatcherOp.XOR, List.of(a, b));
    }

    public static CompositeMatcher not(BlockMatcher matcher) {
        return new CompositeMatcher(MatcherOp.NOT, List.of(matcher));
    }

    @Override
    public boolean matches(@NotNull Block block) {
        return switch (op) {
            case AND -> {
                for (BlockMatcher c : children) if (!c.matches(block)) yield false;
                yield true;
            }
            case OR -> {
                for (BlockMatcher c : children) if (c.matches(block)) yield true;
                yield false;
            }
            case XOR -> children.get(0).matches(block) ^ children.get(1).matches(block);
            case NOT -> !children.get(0).matches(block);
        };
    }

    @Override
    public @NotNull Set<Material> knownMaterials() {
        return switch (op) {
            case AND -> {
                final Set<Material> acc = EnumSet.copyOf(children.get(0).knownMaterials());
                for (int i = 1; i < children.size(); i++) acc.retainAll(children.get(i).knownMaterials());
                yield Set.copyOf(acc);
            }
            case OR, XOR -> {
                final Set<Material> acc = EnumSet.noneOf(Material.class);
                for (BlockMatcher c : children) acc.addAll(c.knownMaterials());
                yield Set.copyOf(acc);
            }
            case NOT -> {
                // Over-approximate: NOT(X) could match anything not in X.
                final Set<Material> all = EnumSet.allOf(Material.class);
                all.removeAll(children.get(0).knownMaterials());
                yield Set.copyOf(all);
            }
        };
    }
}
