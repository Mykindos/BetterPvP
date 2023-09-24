package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class StaticLazer extends ChannelSkill implements InteractSkill, CooldownSkill, EnergySkill {

    // Percentage (0 -> 1)
    private final WeakHashMap<Player, LazerData> charging = new WeakHashMap<>();

    private double baseCharge;
    private double baseDamage;
    private double baseRange;
    private double energyPerSecond;

    @Inject
    public StaticLazer(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Static Lazer";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Hold right click with a Sword to channel",
                "",
                "Charge static electricity and",
                "release right click to fire a lazer",
                "",
                "Charges <val>" + getChargePerSecond(level) + "%</val> per second,",
                "dealing up to <val>" + getDamage(level) + "</val> damage and traveling up to",
                "<val>" + getRange(level) + "</val> blocks",
                "",
                "Taking damage cancels charge",
                "",
                "Cooldown: <val>" + getCooldown(level),
                "Energy: <val>" + getEnergyPerSecond(level)
        };
    }

    private float getEnergyPerSecond(int level) {
        return (float) energyPerSecond;
    }

    private float getRange(int level) {
        return (float) baseRange + 10 * (level - 1);
    }

    private float getDamage(int level) {
        return (float) baseDamage + 2 * (level - 1);
    }

    private double getChargePerSecond(int level) {
        return (float) baseCharge + (10 * (level - 1)); // Increment of 10% per level
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public float getEnergy(int level) {
        return energy;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * 0.5;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseCharge = getConfig("baseCharge", 40.0, Double.class);
        baseDamage = getConfig("baseDamage", 6.0, Double.class);
        baseRange = getConfig("baseRange", 20.0, Double.class);
        energyPerSecond = getConfig("energyPerSecond", 24.0, Double.class);
    }

    @Override
    public void activate(Player player, int level) {
        charging.put(player, new LazerData(level));
    }

    private void showCharge(Player player, LazerData charge) {
        // Action bar
        int green = (int) Math.round(charge.getCharge() * 15);
        int red = 15 - green;

        String msg = "<green><bold>" + "\u258B".repeat(Math.max(0, green)) + "<red><bold>" + "\u258B".repeat(Math.max(0, red));
        final Component bar = MiniMessage.miniMessage().deserialize(msg);
        player.sendActionBar(bar);

        // Sound
        if (!UtilTime.elapsed(charge.getLastSound(), 150)) {
            return;
        }

        player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f + (0.5f * (float) charge.getCharge()));
        charge.setLastSound(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void cancelCooldown(PlayerUseSkillEvent event) {
        if (event.getSkill() == this && charging.containsKey(event.getPlayer())) {
            event.setCancelled(true); // Cancel cooldown or ability use if they're charging to allow them to release
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework) {
            final Boolean key = firework.getPersistentDataContainer().get(new NamespacedKey(champions, "no-damage"), PersistentDataType.BOOLEAN);
            if (key != null && key) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageReceived(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        if (hasSkill(player) && charging.containsKey(player)) {
            charging.remove(player);
            // Cues
            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> was interrupted.", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.6f, 1.2f);
        }
    }

    private void shoot(Player player, float charge, int level) {
        // Cooldown
        championsManager.getCooldowns().add(player, getName(), getCooldown(level), showCooldownFinished());

        final float range = getRange(level);
        final Vector direction = player.getEyeLocation().getDirection();
        final Location start = player.getEyeLocation().add(direction);

        float xDelta = 0;
        while (xDelta < range) {
            xDelta += 0.2f;
            Location point = start.clone().add(direction.clone().multiply(xDelta));
            final Block block = point.getBlock();
            final BoundingBox hitbox = BoundingBox.of(point, 0.5, 0.5, 0.5);

            // Check for entity and block collision
            final List<LivingEntity> nearby = UtilEntity.getNearbyEnemies(player, point, 1.8);
            final boolean collideEnt = !nearby.isEmpty();
            final boolean collideBlock = UtilBlock.solid(block) && UtilBlock.doesBoundingBoxCollide(hitbox, block);
            if (collideEnt || collideBlock) {
                impact(player, point, level, charge);
                return;
            }

            // Particle
            Particle.FIREWORKS_SPARK.builder().extra(0).location(point).receivers(60, true).spawn();
        }

        impact(player, start.add(direction.clone().multiply(range)), level, charge);
    }

    private void impact(Player player, Location point, int level, float charge) {
        // Particles
        Particle.EXPLOSION_NORMAL.builder().location(point).receivers(60, true).extra(0).spawn();

        Firework firework = point.getWorld().spawn(point, Firework.class);
        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.setPower(1);
        final FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.WHITE)
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .build();
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        firework.getPersistentDataContainer().set(new NamespacedKey(champions, "no-damage"), PersistentDataType.BOOLEAN, true);
        firework.detonate();

        // Damage people
        final float damage = getDamage(level) * charge;
        final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, point, 4);
        for (LivingEntity enemy : enemies) {
            UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
        }

        // Cues
        UtilMessage.message(player, getClassType().getName(), "You fired <alt>%s %s</alt>.", getName(), level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f + player.getExp(), 1.75f - charge);
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            LazerData charge = charging.get(player);
            if (player != null) {
                int level = getLevel(player);

                // Remove if they no longer have the skill
                if (level <= 0) {
                    iterator.remove();
                    continue;
                }

                // Check if they still are blocking and charge
                if (player.isHandRaised() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond(level) / 20, true)) {
                    championsManager.getCooldowns().removeCooldown(player, getName(), true);

                    // Check for sword hold status
                    if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        iterator.remove(); // Otherwise, remove
                    }

                    charge.tick();
                    // Cues
                    showCharge(player, charge);
                    continue;
                }

                if (UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                    shoot(player, (float) charge.getCharge(), level);
                    charging.remove(player);
                }
            }
        }
    }

    @Data
    private class LazerData {

        private long lastSound = 0;
        private long lastMessage = 0;
        private double charge = 0; // 0 -> 1
        private final int level;

        public void tick() {
            // Divide over 100 to get multiplication factor since it's in 100% scale for display
            final double chargeToGive = getChargePerSecond(level) / 100;
            this.charge = Math.min(1, this.charge + (chargeToGive / 20));
        }

    }
}
