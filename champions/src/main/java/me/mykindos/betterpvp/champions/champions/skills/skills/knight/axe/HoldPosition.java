package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

@Singleton
@BPvPListener
public class HoldPosition extends Skill implements InteractSkill, CooldownSkill, Listener, BuffSkill {

    @Getter
    public double duration;
    public int resistanceStrength;
    public int slownessStrength;

    @Inject
    public HoldPosition(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hold Position";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Hold your position, gaining",
                "<effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength) + "</effect>, <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> and no",
                "knockback for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.RESISTANCE.getDescription(resistanceStrength)
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            event.setKnockback(false);
        }
    }


    @Override
    public void activate(Player player) {
        long duration = (long) (getDuration() * 1000);
        championsManager.getEffects().addEffect(player, player, EffectTypes.RESISTANCE, resistanceStrength, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.SLOWNESS, slownessStrength, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.NO_SPRINT, duration);
        player.setSprinting(false);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1F, 0.5F);

        long durationTicks = (long) (getDuration() * 20);
        new BukkitRunnable() {
            long ticksRun = 0;

            @Override
            public void run() {
                if (ticksRun > durationTicks || !hasSkill(player)) {
                    UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s</green> has ended.", getName()));
                    this.cancel();
                    return;
                }

                spawnMobSpellParticles(player);
                ticksRun++;
            }
        }.runTaskTimer(champions, 0, 1);
    }

    private void spawnMobSpellParticles(Player player) {
        Location loc = player.getLocation();
        Random random = UtilMath.RANDOM;
        for (int i = 0; i < 5; i++) {
            double x = loc.getX() + (random.nextDouble() - 0.5) * 0.9;
            double y = loc.getY() + (0.25 + (random.nextDouble() - 0.5) * 0.9);
            double z = loc.getZ() + (random.nextDouble() - 0.5) * 0.9;
            player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, new Location(loc.getWorld(), x, y, z), 0, 0.5, 0.5, 0.5, 0, org.bukkit.Color.BLACK);
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void loadSkillConfig() {
        duration = getConfig("duration", 4.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
        resistanceStrength = getConfig("resistanceStrength", 2, Integer.class);
    }
}
