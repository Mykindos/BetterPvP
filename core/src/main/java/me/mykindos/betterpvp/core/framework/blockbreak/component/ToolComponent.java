package me.mykindos.betterpvp.core.framework.blockbreak.component;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.preset.BlockGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Item component that defines how a tool interacts with blocks: which blocks it
 * can break, and at what speed.
 * <p>
 * Conflict policy: two rules in the same component <b>must not share any material</b>
 * in their static {@code knownMaterials()} set. {@link #addRule(BlockBreakRule)}
 * fails fast if it would introduce overlap. This makes resolution deterministic —
 * for any given block, at most one rule can apply.
 * <p>
 * Not serialized: tool components are added programmatically by item builders
 * (see e.g. {@code RunedPickaxe}) and do not survive item save/load on their own.
 */
public class ToolComponent extends AbstractItemComponent implements LoreComponent {

    public static final String KEY = "tool";

    private final List<BlockBreakRule> rules = new ArrayList<>();

    public ToolComponent() {
        super(KEY);
    }

    /**
     * Adds a rule. Throws {@link IllegalArgumentException} if the new rule's
     * matcher overlaps any existing rule's matcher.
     */
    public ToolComponent addRule(@NotNull BlockBreakRule rule) {
        Preconditions.checkNotNull(rule, "rule");
        for (BlockBreakRule existing : rules) {
            if (rule.matcher().overlaps(existing.matcher())) {
                final Set<Material> mineKnown = rule.matcher().knownMaterials();
                final Set<Material> existingKnown = existing.matcher().knownMaterials();
                throw new IllegalArgumentException(
                        "Rule conflict: new rule overlaps existing rule on at least one material. "
                        + "New=" + mineKnown + " existing=" + existingKnown);
            }
        }
        rules.add(rule);
        return this;
    }

    /**
     * @return the (unique, by construction) rule matching this block, if any.
     */
    public Optional<BlockBreakRule> resolve(@NotNull Block block) {
        for (BlockBreakRule r : rules) {
            if (r.matcher().matches(block)) return Optional.of(r);
        }
        return Optional.empty();
    }

    public List<BlockBreakRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    @Override
    public ToolComponent copy() {
        final ToolComponent copy = new ToolComponent();
        // rules are immutable values; safe to share refs
        copy.rules.addAll(this.rules);
        return copy;
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<Component> lines = new ArrayList<>();
        for (BlockBreakRule r : rules) {
            if (r.matcher() instanceof BlockGroup blockGroup) {
                final String type = blockGroup.getId();
                final int breakSpeed = r.properties().getBreakSpeed();
                final Component component = Component.empty()
                        .append(Component.text("Break speed (", NamedTextColor.GRAY))
                        .append(Component.text(type, NamedTextColor.GRAY))
                        .append(Component.text("): ", NamedTextColor.GRAY))
                        .append(Component.text(breakSpeed, NamedTextColor.GREEN));
                lines.add(component);
            }
        }

        if (lines.isEmpty()) {
            // get the highest break speed
            final int maxSpeed = rules.stream()
                    .mapToInt(r -> r.properties().getBreakSpeed())
                    .max()
                    .orElse(0);
            final Component component = Component.empty()
                    .append(Component.text("Break speed: ", NamedTextColor.GRAY))
                    .append(Component.text(maxSpeed, NamedTextColor.GREEN));
            lines.add(component);
        }

        return lines;
    }

    @Override
    public int getRenderPriority() {
        return Integer.MAX_VALUE - 100;
    }
}
