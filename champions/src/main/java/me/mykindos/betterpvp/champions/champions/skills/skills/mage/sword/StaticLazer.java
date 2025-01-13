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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

    private double charge;
    private double damage;
    private double range;
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
    public String[] getDescription() {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "Charge static electricity and",
                "release right click to fire a lazer",
                "",
                "Charges <val>" + UtilFormat.formatNumber(getChargePerSecond(), 0) + "</val> per second,",
                "dealing up to <val>" + getDamage() + "</val> damage and",
                "traveling up to <val>" + getRange() + "</val> blocks",
                "",
                "Cooldown: <val>" + getCooldown(),
                "Energy: <val>" + getEnergyPerSecond()
        };
    }

    private float getEnergyPerSecond() {
        return (float) energy;
    }

    private float getRange() {
        return (float) range;
    }

    private double getDamage() {
        return damage;
    }

    private float getChargePerSecond() {
        return (float) charge; // Increment of 10% per level
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public float getEnergy() {
        return (float) energy;
    }

    @Override
    public double getCooldown() {
        return cooldown;
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
        charge = getConfig("charge", 40.0, Double.class);
        damage = getConfig("damage", 2.0, Double.class);
        range = getConfig("range", 15.0, Double.class);
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
    public void activate(Player player) {
        charging.put(player, new ChargeData(getChargePerSecond() / 100));
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

    private void shoot(Player player, float charge) {
        // Cooldown
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(),
                true,
                true,
                isCancellable(),
                this::shouldDisplayActionBar);

        final float range = getRange();
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
            if (block.getBlockData() instanceof Openable openable && !openable.isOpen()) {
                return;
            }

            if (collideEnt || collideBlock) {
                impact(player, point, charge);
                return;
            }

            // Particle
            Particle.FIREWORK.builder().extra(0).location(point).receivers(60, true).spawn();
        }

        impact(player, start.add(direction.clone().multiply(range)), charge);
    }

    private void impact(Player player, Location point, float charge) {
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
        final double damage = getDamage() * charge;
        final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, point, explosionRadius);
        for (LivingEntity enemy : enemies) {
            if (enemy.hasLineOfSight(point)) {
                UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
            }
        }

        // Cues
        UtilMessage.message(player, getClassType().getName(), "You fired <alt>%s</alt>.", getName());
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
            if (!hasSkill(player)) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond() / 20, true)) {
                charge.tick();
                charge.tickSound(player);
                continue;
            }

            shoot(player, charge.getCharge());
            iterator.remove();
        }
    }

}
