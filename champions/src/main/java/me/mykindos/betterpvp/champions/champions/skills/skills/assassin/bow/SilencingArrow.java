package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class SilencingArrow extends PrepareArrowSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    @Inject
    public SilencingArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Silencing Arrow";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will <effect>Silence</effect> your",
                "target for <val>" + (baseDuration + (level * durationIncreasePerLevel)) + "</val> seconds, making them",
                "unable to use any non-passive skills",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        if (!(target instanceof Player damagee)) return;
        championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (long) ((baseDuration + (level * durationIncreasePerLevel)) * 1000L));
        if (championsManager.getEffects().hasEffect(damagee, EffectType.IMMUNETOEFFECTS)) {
            UtilMessage.simpleMessage(damager, getClassType().getName(), "<alt>" + damagee.getName() + "</alt> is immune to your silence!");
        }
    }

    @Override
    public void displayTrail(Location location) {
        location.getWorld().spawnParticle(Particle.SPELL, location, 1, 0.1, 0.1, 0.1, 0);
    }


    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
    }
}
