package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class StunningShot extends PrepareArrowSkill implements DebuffSkill, OffensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    
    @Inject
    public StunningShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Stunning Shot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that <effect>Stuns</effect>",
                "anyone hit for <val>" + getValueString(this::getDuration, level) + "</val> seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.STUN.getDescription(0)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level-1));
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        if(!(target instanceof Player player)) return;
        UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>.", player.getName(), getName(), level));
        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
        championsManager.getEffects().addEffect(player, EffectTypes.STUN, (long) (getDuration(level) * 1000));
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SCULK_CHARGE_POP)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 0.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.25, Double.class);
    }
}
