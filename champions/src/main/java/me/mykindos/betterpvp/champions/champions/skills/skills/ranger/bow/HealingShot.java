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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class HealingShot extends PrepareArrowSkill {

    double baseDuration;

    double increaseDurationPerLevel;

    double cooldownDecreasePerLevel;

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
                "Shoot an arrow that gives <effect>Regeneration III</effect>",
                "to allies hit for <val>" + getDuration(level) + "</val> seconds",
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
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;
        int level = getLevel(damager);
        if (level > 0) {
            onHit(damager, cde.getDamagee(), level);
            arrows.remove(arrow);
            cde.addReason(getName());
            event.setCancelled(true);
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        if (target instanceof Player damagee) {
            if (UtilPlayer.isPlayerFriendly(damager, damagee)) {
                damagee.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (getDuration(level) * 20), 2 ));
            }
        }
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        arrows.add(arrow);
        arrow.setFireTicks(200);
    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(255, 0, 0).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level * cooldownDecreasePerLevel));
    }

    public double getDuration(int level) {
        return baseDuration + (level * increaseDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 6.0, Double.class);
        increaseDurationPerLevel = getConfig("increasePerLevel", 1.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
    }
}
