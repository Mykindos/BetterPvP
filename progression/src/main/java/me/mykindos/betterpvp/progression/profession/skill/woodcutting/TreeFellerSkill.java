package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.CooldownProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.PerkActivator;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

@Singleton
public class TreeFellerSkill extends WoodcuttingProgressionSkill implements CooldownProgressionSkill {

    @Getter
    private final CooldownManager cooldownManager;
    private double cooldown;
    private double cooldownDecreasePerLevel;

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
                "",
                "Cooldown: <green>" + getCooldown(level)
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
        return (cooldown - (cooldownDecreasePerLevel * level));
    }

    @Override
    public void whenPlayerUsesSkill(Player player, int level) {
        if (cooldownManager.use(player, getName(), getCooldown(level), true, true,
                false, this::shouldDisplayActionBar, getPriority())) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 2.0f, 1.0f);
            UtilMessage.simpleMessage(player, getProgressionTree(), "You used <alt>" + getName() + "</alt> " + level);
        }
    }

    @Override
    public PerkActivator getActivator() {
        return PerkActivator.AXE;
    }

    public void whenPlayerCantUseSkill(Player player) {
        UtilMessage.simpleMessage(player, getProgressionTree(),
                "You cannot use <alt>" + getName() + "</alt> for <alt>" + cooldownManager.getAbilityRecharge(player, getName()).getRemaining() + "</alt> seconds");
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        cooldown = getConfig("cooldown", 20.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Tactician"};
        return new ProgressionSkillDependency(dependencies, 10);
    }
}
