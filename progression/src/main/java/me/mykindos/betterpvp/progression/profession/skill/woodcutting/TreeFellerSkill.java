package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.CooldownProgressionSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

@Singleton
public class TreeFellerSkill extends WoodcuttingProgressionSkill implements CooldownProgressionSkill {

    @Getter
    private final CooldownManager cooldownManager;

    @Inject
    public TreeFellerSkill(Progression progression, CooldownManager cooldownManager) {
        super(progression);
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "Tree Feller";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Cut down an entire tree by chopping a single log",
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_AXE;
    }

    @Override
    public ItemFlag getFlag() {
        return ItemFlag.HIDE_ATTRIBUTES;
    }

    @Override
    public double getCooldown(int level) {
        return 2*level;
    }

    @Override
    public void whenPlayerUsesSkill(Player player, int level) {
        if (cooldownManager.use(player, getName(), getCooldown(level), true)) {
            UtilMessage.simpleMessage(player, getProgressionTree(), "You used <alt>" + getName() + "</alt> " + level);
        }
    }

    public void whenPlayerCantUseSkill(Player player) {
        UtilMessage.simpleMessage(player, getProgressionTree(),
                "You must wait <alt>" + cooldownManager.getAbilityRecharge(player, getName()).getRemaining() + "</alt> seconds");
    }
}
