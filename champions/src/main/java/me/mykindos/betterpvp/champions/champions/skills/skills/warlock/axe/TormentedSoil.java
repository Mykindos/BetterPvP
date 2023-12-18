package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Singleton
@BPvPListener
public class TormentedSoil extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final List<Torment> tormentList = new ArrayList<>();

    private double baseDuration;

    private double durationIncreasePerLevel;
    private double baseRange;

    private double rangeIncreasePerLevel;

    private double baseDamageIncrease;
    private double damageIncreasePerLevel;

    @Inject
    public TormentedSoil(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Tormented Soul";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Corrupt the earth around you, creating a ring that",
                "debuffs enemies within it for <stat>" + getDuration(level) + "</stat> seconds.",
                "Players within the ring take <stat>" + (getDamageIncrease(level) * 100) + "%</stat> more damage.",
                "",
                "Range: <val>" + getRange(level) + "</val> blocks.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getRange(int level) {
        return baseRange + level * rangeIncreasePerLevel;
    }

    public double getDamageIncrease(int level) {
        return baseDamageIncrease + level * damageIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        for (Torment torment : tormentList) {
            if (!torment.getLocation().getWorld().equals(event.getDamagee().getLocation().getWorld())) {
                return;
            }
            for (LivingEntity target : UtilEntity.getNearbyEnemies(torment.getCaster(), torment.getLocation(), getRange(torment.getLevel()))) {
                if (target.equals(event.getDamagee())) {
                    event.setDamage(event.getDamage() * (1 + getDamageIncrease(torment.getLevel())));
                    return;
                }
            }
        }
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        ListIterator<Torment> it = tormentList.listIterator();
        while (it.hasNext()) {
            Torment torment = it.next();

            if (UtilTime.elapsed(torment.getCastTime(), (long) (getDuration(torment.getLevel()) * 1000)) || torment.getCaster() == null) {
                it.remove();
                continue;
            }

            double size = getRange(torment.getLevel());
            int particles = 50;
            Location loc = torment.getLocation().clone();
            for (int i = 0; i < particles; i++) {
                double angle, x, z;
                angle = 2 * Math.PI * i / particles;
                x = Math.cos(angle) * size;
                z = Math.sin(angle) * size;

                loc.add(x, 0, z);

                if (UtilBlock.airFoliage(loc.getBlock()) && !loc.getBlock().isLiquid()) {
                    loc.subtract(0, 1, 0);
                }
                if (UtilBlock.solid(loc.getBlock())) {
                    loc.add(0, 1, 0);
                }


                Particle.END_ROD.builder().location(loc).receivers(30).extra(0).spawn();

                loc.subtract(x, 0, z);

            }
        }

    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level * cooldownDecreasePerLevel);
    }


    @Override
    public void activate(Player player, int level) {
        Location loc = player.getLocation().clone();
        if (!UtilBlock.solid(loc.getBlock())) {
            for (int i = 0; i < 10; i++) {
                loc.subtract(0, 1, 0);
                if (UtilBlock.solid(loc.getBlock())) {
                    break;
                }
            }
        }
        tormentList.add(new Torment(player, loc.add(0, 0.5, 0), level));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 2f, 1.3f);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 7.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        baseRange = getConfig("baseRange", 5.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 0.5, Double.class);
        baseDamageIncrease = getConfig("baseDamageIncrease", 0.33, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
    }

    @Data
    private static class Torment {

        private final Player caster;
        private final Location location;
        private final int level;
        private long castTime = System.currentTimeMillis();

    }
}
