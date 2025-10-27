package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.StateSkill;
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
import org.jetbrains.annotations.NotNull;

import java.util.Random;

@Singleton
@BPvPListener
public class HoldPosition extends StateSkill implements Listener, BuffSkill {

    public double baseDuration;
    public double durationIncreasePerLevel;
    public int resistanceStrength;
    public int slownessStrength;

    @Inject
    public HoldPosition(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Fortify yourself, gaining",
                "<effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength) + "</effect>, <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> and no",
                "knockback for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.RESISTANCE.getDescription(resistanceStrength)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!activeState.containsKey(player.getUniqueId())) return;  // only disable if they are in the state

        event.setKnockback(false);
    }

    @Override
    public void activate(Player player, int level) {
        super.activate(player, level);

        long duration = (long) (getDuration(level) * 1000);
        championsManager.getEffects().addEffect(player, player, EffectTypes.RESISTANCE, resistanceStrength, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.SLOWNESS, slownessStrength, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(player, player, EffectTypes.NO_SPRINT, duration);
        player.setSprinting(false);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1F, 0.5F);
    }

    @Override
    protected void doOnSuccessfulUpdate(@NotNull Player player) {
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
    protected @NotNull String getActionBarLabel() {
        return "Fortified";
    }

    @Override
    protected double getStateDuration(int level) {
        return getDuration(level);
    }

    @Override
    public String getName() {
        return "Hold Position";
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
        resistanceStrength = getConfig("resistanceStrength", 2, Integer.class);
    }
}
