package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.treefellercooldown;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.interaction.TreeFellerCooldownModifier;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;

@Singleton
@NodeId("tree_feller_cooldown")
public class TreeFellerCooldownAttribute implements IProfessionAttribute, TreeFellerCooldownModifier {

    private final ProfessionProfileManager profileManager;

    @Inject
    public TreeFellerCooldownAttribute(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public String getName() {
        return "Tree Feller cooldown";
    }

    @Override
    public String getDescription() {
        return "Tree Feller cooldown reduction";
    }

    @Override
    public String getOperation() {
        return "s";
    }

    /**
     * Returns the total flat cooldown reduction (in seconds) granted by this attribute for the player.
     */
    public double getCooldownReduction(Player player) {
        return IProfessionAttribute.computeValue(player, "Woodcutting", this, profileManager);
    }

    @Override
    public double getEffectiveCooldown(Player player, double baseCooldown) {
        double reduction = getCooldownReduction(player);
        return Math.max(1.0, baseCooldown - reduction);
    }
}
