package me.mykindos.betterpvp.progression.profession.skill;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.access.AccessRequirement;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessProvider;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ItemAccessProvider} that gates items behind skill-tree node unlocks.
 *
 * <p>A precomputed {@code itemKeyToNode} map provides O(1) lookup per {@link #evaluate} call.
 * The index is built eagerly on first use (nodes are always loaded before any player can
 * interact with an item) and rebuilt via {@link #rebuildIndex()} whenever
 * {@link ProfessionNodeManager#reload()} runs.</p>
 */
@Singleton
public class SkillTreeAccessProvider implements ItemAccessProvider {

    private static final Set<AccessScope> GATED_SCOPES = Set.of(AccessScope.CRAFT, AccessScope.USE, AccessScope.DAMAGE);
    private static final Key SOURCE = Key.key("progression", "skill_tree");

    private final ProfessionProfileManager professionProfileManager;
    private final ProfessionNodeManager professionNodeManager;

    // Populated from ProfessionNodeManager.getObjects() - rebuilt on reload.
    private volatile Map<Key, ProfessionRecipeNode> itemKeyToNode = Map.of();

    @Inject
    public SkillTreeAccessProvider(ProfessionProfileManager professionProfileManager,
                                   ProfessionNodeManager professionNodeManager) {
        this.professionProfileManager = professionProfileManager;
        this.professionNodeManager = professionNodeManager;
    }

    @Override
    public Optional<AccessRequirement> evaluate(Player player, Key itemKey) {
        if (itemKeyToNode.isEmpty()) {
            rebuildIndex();
        }

        ProfessionRecipeNode node = itemKeyToNode.get(itemKey);
        if (node == null) {
            return Optional.empty();
        }

        boolean satisfied = player != null && isUnlocked(player, node);
        return Optional.of(new AccessRequirement(SOURCE, node.getRequirementLore(), GATED_SCOPES, satisfied));
    }

    /** Rebuilds the item-key → node index from the currently loaded profession nodes. */
    public void rebuildIndex() {
        Map<Key, ProfessionRecipeNode> index = new HashMap<>();
        for (ProfessionNode node : professionNodeManager.getObjects().values()) {
            if (node instanceof ProfessionRecipeNode recipeNode) {
                for (Key key : recipeNode.getRecipes()) {
                    index.put(key, recipeNode);
                }
            }
        }
        itemKeyToNode = Map.copyOf(index);
    }

    /** Returns {@code true} if the player has unlocked the given recipe node in any profession. */
    private boolean isUnlocked(Player player, ProfessionRecipeNode targetNode) {
        return professionProfileManager.getObject(player.getUniqueId().toString())
                .map(profile -> profile.getProfessionDataMap().values().stream()
                        .anyMatch(data -> data.getBuild().getSkillLevel(targetNode) > 0))
                .orElse(false);
    }
}
