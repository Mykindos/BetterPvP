package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class Immolate extends ActiveToggleSkill implements EnergySkill, ThrowableListener {

    private double baseFireTickDuration;
    private double fireTickDurationIncreasePerLevel;
    private double baseFireTrailDuration;
    private double fireTrailDurationIncreasePerLevel;
    private int speedStrength;
    private int strengthLevel;
    private double energyDecreasePerLevel;

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
                "<effect>Speed " + UtilFormat.getRomanNumeral(speedStrength + 1) + "</effect>, <effect>Strength " + UtilFormat.getRomanNumeral(strengthLevel) + " </effect> and <effect>Fire Resistance",
                "",
                "You leave a trail of fire, which",
                "ignites enemies for <stat>" + getFireTickDuration(level) + "</stat> seconds",
                "",
                "Energy / Second: <val>" + getEnergy(level)

        };
    }

    public double getFireTickDuration(int level) {
        return baseFireTickDuration + level * fireTickDurationIncreasePerLevel;
    }

    public double getFireTrailDuration(int level) {
        return baseFireTrailDuration + level * fireTrailDurationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @EventHandler
    public void Combust(EntityCombustEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }


    @UpdateEvent(delay = 1000)
    public void audio() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.3F, 0.0F);
            }

        }
    }

    @UpdateEvent(delay = 125)
    public void fire() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Item fire = player.getWorld().dropItem(player.getLocation().add(0.0D, 0.5D, 0.0D), new ItemStack(Material.BLAZE_POWDER));
                int level = getLevel(player);
                ThrowableItem throwableItem = new ThrowableItem(this, fire, player, getName(), (long) (getFireTrailDuration(level) * 1000L));
                championsManager.getThrowables().addThrowable(throwableItem);

                fire.setVelocity(new Vector((Math.random() - 0.5D) / 3.0D, Math.random() / 3.0D, (Math.random() - 0.5D) / 3.0D));
            }
        }
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (!(thrower instanceof Player damager)) return;
        if (hit.getFireTicks() > 0) return;

        //LogManager.addLog(e.getCollision(), damager, "Immolate", 0);
        int level = getLevel(damager);
        hit.setFireTicks((int) (getFireTickDuration(level) * 20));
    }

    @UpdateEvent
    public void checkActive() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                int level = getLevel(player);
                if (level <= 0) {
                    iterator.remove();
                    sendState(player, false);
                } else if (!championsManager.getEnergy().use(player, getName(), getEnergy(level) / 5, true)) {
                    iterator.remove();
                    sendState(player, false);
                } else if (championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                    iterator.remove();
                    sendState(player, false);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, speedStrength));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 25, 0));
                    championsManager.getEffects().addEffect(player, EffectType.STRENGTH, strengthLevel, 1250L);
                }
            } else {
                iterator.remove();
            }
        }
    }

    @UpdateEvent(delay = 100)
    public void createFireParticles() {
        for (UUID uuid : active) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                World world = player.getWorld();
                Location location = player.getLocation();
                int particleCount = 10;
                world.spawnParticle(Particle.FLAME, location, particleCount, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void toggle(Player player, int level) {
        if (active.contains(player.getUniqueId())) {

            active.remove(player.getUniqueId());

            if (!UtilPlayer.hasPotionEffect(player, PotionEffectType.SPEED, strengthLevel + 1)) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            championsManager.getEffects().removeEffect(player, EffectType.STRENGTH);
            sendState(player, false);
        } else {
            if (championsManager.getEnergy().use(player, getName(), 10, false)) {
                active.add(player.getUniqueId());
                sendState(player, true);
            }
        }
    }

    private void sendState(Player player, boolean state) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Immolate: %s", state ? "<green>On" : "<red>Off");
    }

    @Override
    public void loadSkillConfig() {
        baseFireTickDuration = getConfig("baseFireTickDuration", 4.0, Double.class);
        fireTickDurationIncreasePerLevel = getConfig("fireTickDurationIncreasePerLevel", 0.0, Double.class);
        baseFireTrailDuration = getConfig("baseFireTrailDuration", 2.0, Double.class);
        fireTrailDurationIncreasePerLevel = getConfig("fireTrailDurationIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 0, Integer.class);
        strengthLevel = getConfig("strengthLevel", 1, Integer.class);
        energyDecreasePerLevel = getConfig("energyDecreasePerLevel", 1.0, Double.class);
    }

}
