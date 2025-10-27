package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.StateSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

@Singleton
@BPvPListener
public class ExcessiveForce extends StateSkill implements Listener, OffensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;

    @Inject
    public ExcessiveForce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Excessive Force";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "For the next " + getValueString(this::getDuration, level) + " seconds",
                "your attacks deal standard knockback to enemies",
                "",
                "Does not ignore anti-knockback abilities",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    /**
     * The duration of the excessive force ability for the given level.
     */
    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    // entrypoint
    @Override
    public void activate(Player player, int level) {
        super.activate(player, level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1.7f);
    }

    /**
     * This listener's purpose is to override the default no-knockback behavior of assassin's melee attacks.
     * <p>
     * Note: This might also affect other skills that set no-knockback, but currently there are none.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void setKnockback(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (activeState.containsKey(damager.getUniqueId())) {
                event.setKnockback(true);
            }
        }
    }

    @Override
    protected @NotNull String getActionBarLabel() {
        return "Dealing Knockback";
    }

    @Override
    protected double getStateDuration(int level) {
        return getDuration(level);

    }
    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationPerLevel", 0.5, Double.class);
    }
}
