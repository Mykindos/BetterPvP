package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class HealingShot extends PrepareArrowSkill {

    double baseDuration;

    double increaseDurationPerLevel;

    int regenerationStrength;

    @Inject
    public HealingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Healing Shot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength + 1) + "</effect>",
                "to allies hit for <val>" + getDuration(level) + "</val> seconds",
                "and cleanse them of all negative effects",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
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

    //Code from PrepareArrowSkill. For this skill, we need to use PreCustomDamageEvent as it effects targets we cannot damage
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;
        int level = getLevel(damager);
        if (level > 0) {
            onHit(damager, cde.getDamagee(), level, event);
            arrows.remove(arrow);
            cde.addReason(getName());
        }
    }

    public void onHit(Player damager, LivingEntity target, int level) {
        return;
    }

    public void onHit(Player damager, LivingEntity target, int level, PreCustomDamageEvent event) {
        if (target instanceof Player damagee) {
            if (UtilPlayer.isPlayerFriendly(damager, damagee)) {

                damagee.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (getDuration(level) * 20), regenerationStrength));

                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5F);

                championsManager.getEffects().addEffect(damagee, EffectType.IMMUNETOEFFECTS, 1);
                UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>", damagee.getName(), getName(), level));
                if (!damager.equals(damagee)) {
                    UtilMessage.message(damagee, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
                }
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void displayTrail(Location location) {
        Particle.HEART.builder().location(location).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + (level * increaseDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        increaseDurationPerLevel = getConfig("increasePerLevel", 1.0, Double.class);

        regenerationStrength = getConfig("regenerationStrength", 2, Integer.class);
    }
}
