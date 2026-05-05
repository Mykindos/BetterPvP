package me.mykindos.betterpvp.progression.profession.skill.fishing.expertbaiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.access.AccessRequirement;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;

/**
 * Gates the CRAFT scope on bait items behind the {@link ExpertBaiter} skill being unlocked.
 *
 * <p>Bait items declare {@link AccessScope#CRAFT} in their {@code RestrictedAccessComponent};
 * this provider supplies the requirement that satisfies (or denies) that scope.</p>
 */
@Singleton
public class ExpertBaiterAccessProvider implements ItemAccessProvider {

    private static final Key SOURCE = Key.key("progression", "expert_baiter");
    private static final Set<AccessScope> GATED = Set.of(AccessScope.CRAFT);
    private static final Set<String> GATED_KEYS = Set.of("speedy_bait", "event_bait");
    private static final Component LORE = Component.text("Requires ", NamedTextColor.GRAY)
            .append(Component.text("Expert Baiter", NamedTextColor.GREEN));

    private final ExpertBaiter skill;

    @Inject
    public ExpertBaiterAccessProvider(ExpertBaiter skill) {
        this.skill = skill;
    }

    @Override
    public Optional<AccessRequirement> evaluate(Player player, Key itemKey) {
        if (!"progression".equals(itemKey.namespace())) return Optional.empty();
        if (!GATED_KEYS.contains(itemKey.value())) return Optional.empty();

        boolean satisfied = player != null && skill.isUnlocked(player);
        return Optional.of(new AccessRequirement(SOURCE, LORE, GATED, satisfied));
    }
}
