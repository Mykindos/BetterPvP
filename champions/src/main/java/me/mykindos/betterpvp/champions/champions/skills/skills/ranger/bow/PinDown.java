package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class PinDown extends PrepareArrowSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    @Inject
    public PinDown(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Pin Down";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to activate",
                "",
                "Quickly launch an arrow that gives enemies",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect> for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + durationIncreasePerLevel * level;
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

        if (championsManager.getCooldowns().use(player, getName(), getCooldown(level), true, true, true, false)) {
            UtilInventory.remove(player, Material.ARROW, 1);

            Arrow proj = player.launchProjectile(Arrow.class);
            arrows.add(proj);

            proj.setVelocity(player.getLocation().getDirection().multiply(1.6D));
            player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
            player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration(level) * 20), slownessStrength));
        championsManager.getEffects().addEffect(target, EffectType.NO_JUMP, (long) getDuration(level) * 100);
        Bukkit.broadcastMessage("[debug]");

    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(128, 0, 128).count(3).extra(0).receivers(60, true).spawn();
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
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
    }
}
