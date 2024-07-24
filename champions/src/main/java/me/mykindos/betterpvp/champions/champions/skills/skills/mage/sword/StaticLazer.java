package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
public class StaticLazer extends ChannelSkill implements InteractSkill, EnergyChannelSkill, CooldownSkill, OffensiveSkill, AreaOfEffectSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);

    private double baseCharge;
    private double chargeIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseRange;
    private double rangeIncreasePerLevel;
    private double collisionRadius;
    private double explosionRadius;

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
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Charge static electricity and",
                "release right click to fire a lazer",
                "",
                "Charges " + getValueString(this::getChargePerSecond, level, 1, "%", 0) + " per second,",
                "dealing up to " + getValueString(this::getDamage, level) + " damage and",
                "traveling up to " + getValueString(this::getRange, level) + " blocks",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Energy: " + getValueString(this::getEnergyPerSecond, level)
        };
    }

    private float getEnergyPerSecond(int level) {
        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    private float getRange(int level) {
        return (float) (baseRange + rangeIncreasePerLevel * (level - 1));
    }

    private double getDamage(int level) {
        return baseDamage + damageIncreasePerLevel * (level - 1);
    }

    private float getChargePerSecond(int level) {
        return (float) (baseCharge + (chargeIncreasePerLevel * (level - 1))); // Increment of 10% per level
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public float getEnergy(int level) {
        return (float) (energy - energyDecreasePerLevel * (level - 1));
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
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
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    @Override
    public void loadSkillConfig() {
        baseCharge = getConfig("baseCharge", 40.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 10.0, Double.class);
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.75, Double.class);
        baseRange = getConfig("baseRange", 15.0, Double.class);
        rangeIncreasePerLevel = getConfig("rangeIncreasePerLevel", 4.5, Double.class);
        collisionRadius = getConfig("collisionRadius", 1.8, Double.class);
        explosionRadius = getConfig("explosionRadius", 3.5, Double.class);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void activate(Player player, int level) {
        charging.put(player, new ChargeData(getChargePerSecond(level) / 100));
    }

    // This doesnt work anyway
    //@EventHandler
    //public void onDamage(CustomDamageEvent event) {
    //    if (event.getDamager() instanceof Firework firework) {
    //        final Boolean key = firework.getPersistentDataContainer().get(new NamespacedKey(champions, "no-damage"), PersistentDataType.BOOLEAN);
    //        if (key != null && key) {
    //            event.setCancelled(true);
    //        }
    //    }
    //}

    private void shoot(Player player, float charge, int level) {
        // Cooldown
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);

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
            final List<LivingEntity> nearby = UtilEntity.getNearbyEnemies(player, point, collisionRadius);
            final boolean collideEnt = !nearby.isEmpty();
            final boolean collideBlock = UtilBlock.solid(block) && UtilBlock.doesBoundingBoxCollide(hitbox, block);

            // Cheap fix
            if(block.getBlockData() instanceof Openable openable && !openable.isOpen()) {
                return;
            }

            if (collideEnt || collideBlock ) {
                impact(player, point, level, charge);
                return;
            }

            // Particle
            Particle.FIREWORK.builder().extra(0).location(point).receivers(60, true).spawn();
        }

        impact(player, start.add(direction.clone().multiply(range)), level, charge);
    }

    private void impact(Player player, Location point, int level, float charge) {
        // Particles
        Particle.EXPLOSION.builder().location(point).receivers(60, true).extra(0).spawn();

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
        firework.detonate(); // Triggers an EntityDamageEvent, not an EntityDamageByEntityEvent

        // Damage people
        final double damage = getDamage(level) * charge;
        final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, point, explosionRadius);
        for (LivingEntity enemy : enemies) {
            if (enemy.hasLineOfSight(point)) {
                UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
            }
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
            ChargeData charge = charging.get(player);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond(level) / 20, true)) {
                charge.tick();
                charge.tickSound(player);
                continue;
            }

            shoot(player, charge.getCharge(), level);
            iterator.remove();
        }
    }

}
