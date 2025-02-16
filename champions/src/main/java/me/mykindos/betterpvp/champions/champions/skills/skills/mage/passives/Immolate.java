package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
import me.mykindos.betterpvp.core.utilities.UtilEntity;
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

    @Getter
    private double fireTickDuration;
    @Getter
    private double fireTrailDuration;
    private int strengthLevel;
    private int speedStrength;

    @Inject
    public Immolate(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Immolate";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Ignite yourself in flaming fury, gaining",
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> and <effect>Strength " + UtilFormat.getRomanNumeral(strengthLevel) + "</effect>",
                "",
                "You leave a trail of fire, which",
                "ignites enemies for <val>" + getFireTickDuration() + "</val> seconds",
                "",
                "Uses <val>" + getEnergyStartCost() + "</val> energy on activation",
                "Energy / Second: <val>" + getEnergy(),
                "",
                "While active, you are also immune to fire damage",
                "",
                EffectTypes.STRENGTH.getDescription(strengthLevel),
        };
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
        if (championsManager.getEnergy().use(player, getName(), getEnergyStartCost(), false)) {
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
        ThrowableItem throwableItem = new ThrowableItem(this, fire, player, getName(), (long) (getFireTrailDuration() * 1000L));
        throwableItem.setRemoveInWater(true);
        championsManager.getThrowables().addThrowable(throwableItem);

        fire.setVelocity(new Vector((Math.random() - 0.5D) / 3.0D, Math.random() / 3.0D, (Math.random() - 0.5D) / 3.0D));

        World world = player.getWorld();
        Location location = player.getLocation();
        int particleCount = 10;
        world.spawnParticle(Particle.FLAME, location, particleCount, 0.5, 0.5, 0.5, 0.05);

    }

    private boolean doImmolate(Player player) {

        if (!hasSkill(player)) {
            return false;
        } else if (!championsManager.getEnergy().use(player, getName(), getEnergy() / 20, true)) {
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
        UtilEntity.setFire(hit, thrower, (long) (1000L * getFireTickDuration()));
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
    public float getEnergy() {
        return (float) energy;
    }


    @Override
    public void loadSkillConfig() {
        fireTickDuration = getConfig("fireTickDuration", 4.0, Double.class);
        fireTrailDuration = getConfig("fireTrailDuration", 2.0, Double.class);
        speedStrength = getConfig("speedStrength", 1, Integer.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
    }
}
