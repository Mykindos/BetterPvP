package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

@Singleton
@BPvPListener
public class Wreath extends Skill implements InteractSkill, CooldownSkill {

    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private int slowStrength;
    private double healthPerEnemyHit;

    @Inject
    public Wreath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wreath";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to cast",
                "",
                "Release a barrage of teeth that",
                "deal <val>" + String.format("%.2f", getDamage(level)) + "</val> damage and apply <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength + 1) + "</effect>",
                "to their target for <stat>" + getSlowDuration(level) + "</stat> seconds.",
                "",
                "For each enemy hit, restore <val>" + healthPerEnemyHit + "</val> health.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }


    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    private void processPlayerAction(Player player, int level) {

        final Location startPos = player.getLocation().clone();
        final Vector vector = player.getLocation().clone().getDirection().normalize().multiply(1);
        vector.setY(0);
        final Location loc = player.getLocation().subtract(0, 1, 0).add(vector);

        final BukkitTask runnable = new BukkitRunnable() {

            @Override
            public void run() {
                loc.add(vector);
                if ((!UtilBlock.airFoliage(loc.getBlock()))
                        && UtilBlock.solid(loc.getBlock())) {

                    loc.add(0.0D, 1.0D, 0.0D);
                    if ((!UtilBlock.airFoliage(loc.getBlock()))
                            && UtilBlock.solid(loc.getBlock())) {

                        cancel();
                        return;
                    }

                }

                if (loc.getBlock().getType().name().contains("DOOR")) {
                    cancel();
                    return;
                }

                if ((loc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType() == Material.AIR)) {
                    loc.add(0.0D, -1.0D, 0.0D);
                }

                if (loc.distance(startPos) > 20) {
                    cancel();
                }

                EvokerFangs fangs = (EvokerFangs) player.getWorld().spawnEntity(loc, EntityType.EVOKER_FANGS);
                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, fangs.getLocation(), 1.5)) {
                    CustomDamageEvent dmg = new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, getName());
                    UtilDamage.doCustomDamage(dmg);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getSlowDuration(level) * 20), slowStrength));
                    UtilPlayer.health(player, healthPerEnemyHit);
                }

            }

        }.runTaskTimer(champions, 0, 1);

        UtilServer.runTaskLater(champions, runnable::cancel, 60);


    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        processPlayerAction(player, level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 1.8f);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Override
    public void loadSkillConfig() {
        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);

        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.66, Double.class);

        healthPerEnemyHit = getConfig("healthPerEnemyHit", 1.0, Double.class);

        slowStrength = getConfig("slowStrength", 1, Integer.class);
    }
}
