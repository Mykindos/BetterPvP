package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;

@Singleton
@BPvPListener
public class Immolate extends ActiveToggleSkill implements EnergySkill, ThrowableListener, FireSkill, BuffSkill {

    private double baseFireTickDuration;
    private double fireTickDurationIncreasePerLevel;
    private double baseFireTrailDuration;
    private double fireTrailDurationIncreasePerLevel;
    private int speedStrength;
    private int strengthLevel;

    @Inject
    public Immolate(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Immolate";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Ignite yourself in flaming fury, gaining",
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect>, <effect>Strength " + UtilFormat.getRomanNumeral(strengthLevel) + "</effect> and <effect>Fire Resistance",
                "",
                "You leave a trail of fire, which",
                "ignites enemies for " + getValueString(this::getFireTickDuration, level) + " seconds",
                "",
                "Uses " + getValueString(this::getEnergyStartCost, level) + " energy on activation",
                "Energy / Second: " + getValueString(this::getEnergy, level),
                "",
                EffectTypes.STRENGTH.getDescription(strengthLevel)

        };
    }

    public double getFireTickDuration(int level) {
        return baseFireTickDuration + ((level-1) * fireTickDurationIncreasePerLevel);
    }

    public double getFireTrailDuration(int level) {
        return baseFireTrailDuration + ((level-1) * fireTrailDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }


    @Override
    public boolean process(Player player) {

        HashMap<String, Long> updateCooldowns = updaterCooldowns.get(player.getUniqueId());

        if (updateCooldowns.getOrDefault("audio", 0L) < System.currentTimeMillis()) {
            audio(player);
            updateCooldowns.put("audio", System.currentTimeMillis() + 1000);
        }

        if (updateCooldowns.getOrDefault("fire", 0L) < System.currentTimeMillis()) {
            fire(player);
            updateCooldowns.put("fire", System.currentTimeMillis() + 100);
        }

        return doImmolate(player);
    }

    @Override
    public void toggleActive(Player player) {
        if (championsManager.getEnergy().use(player, getName(), getEnergyStartCost(player.getLevel()), false)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "Immolate: <green>On");
        } else {
            cancel(player);
        }
    }

    @Override
    public void cancel(Player player, String reason) {
        super.cancel(player, reason);

        championsManager.getEffects().removeEffect(player, EffectTypes.SPEED, getName());
        championsManager.getEffects().removeEffect(player, EffectTypes.FIRE_RESISTANCE, getName());
        championsManager.getEffects().removeEffect(player, EffectTypes.STRENGTH, getName());

    }


    private void audio(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3F, 0.0F);
    }

    private void fire(Player player) {

        Item fire = player.getWorld().dropItem(player.getLocation().add(0.0D, 0.5D, 0.0D), new ItemStack(Material.BLAZE_POWDER));
        int level = getLevel(player);
        ThrowableItem throwableItem = new ThrowableItem(this, fire, player, getName(), (long) (getFireTrailDuration(level) * 1000L));
        championsManager.getThrowables().addThrowable(throwableItem);

        fire.setVelocity(new Vector((Math.random() - 0.5D) / 3.0D, Math.random() / 3.0D, (Math.random() - 0.5D) / 3.0D));

        World world = player.getWorld();
        Location location = player.getLocation();
        int particleCount = 10;
        world.spawnParticle(Particle.FLAME, location, particleCount, 0.5, 0.5, 0.5, 0.05);

    }

    private boolean doImmolate(Player player) {

        int level = getLevel(player);
        if (level <= 0) {
            return false;
        } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 20, true)) {
            return false;
        } else if (championsManager.getEffects().hasEffect(player, EffectTypes.SILENCE) && !canUseWhileSilenced()) {
            return false;
        } else {
            championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, 1250, true);
            championsManager.getEffects().addEffect(player, EffectTypes.FIRE_RESISTANCE, getName(), 1, 1250, true);
            championsManager.getEffects().addEffect(player, EffectTypes.STRENGTH, getName(), strengthLevel, 1250, true);
        }

        return true;

    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (!(thrower instanceof Player damager)) return;
        if (hit.getFireTicks() > 0) return;

        //LogManager.addLog(e.getCollision(), damager, "Immolate", 0);
        int level = getLevel(damager);
        hit.setFireTicks((int) (getFireTickDuration(level) * 20));
    }

    @EventHandler
    public void Combust(EntityCombustEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }


    @Override
    public void loadSkillConfig() {
        baseFireTickDuration = getConfig("baseFireTickDuration", 4.0, Double.class);
        fireTickDurationIncreasePerLevel = getConfig("fireTickDurationIncreasePerLevel", 0.0, Double.class);
        baseFireTrailDuration = getConfig("baseFireTrailDuration", 2.0, Double.class);
        fireTrailDurationIncreasePerLevel = getConfig("fireTrailDurationIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 1, Integer.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
    }
}
