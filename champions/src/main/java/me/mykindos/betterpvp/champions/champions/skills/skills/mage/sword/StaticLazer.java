package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChargeSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class StaticLazer extends ChargeSkill implements InteractSkill, EnergyChannelSkill, CooldownSkill, OffensiveSkill, AreaOfEffectSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final DisplayObject<Component> actionBarComponent = ChargeData.getActionBar(this, charging);
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
                "Charges " + getValueString(this::getChargePerSecond, level, 100, "%", 0) + " per second,",
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
        charging.put(player, new ChargeData((float) getChargePerSecond(level)));
    }

    public boolean use(Player player, ChargeData charge, int level) {
        // Cooldown
        final float chargePercent = charge.getCharge();
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
                return true;
            }

            if (collideEnt || collideBlock ) {
                impact(player, point, level, chargePercent);
                return true;
            }

            // Particle
            Particle.FIREWORK.builder().extra(0).location(point).receivers(60, true).spawn();
        }

        impact(player, start.add(direction.clone().multiply(range)), level, chargePercent);
        return true;
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
        firework.getPersistentDataContainer().set(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN, true);
        firework.detonate(); // Triggers an EntityDamageEvent, not an EntityDamageByEntityEvent

        // Damage people
        final double damage = getDamage(level) * charge;
        final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, point, explosionRadius);
        for (LivingEntity enemy : enemies) {
            if (enemy.hasLineOfSight(point)) {
                UtilDamage.doDamage(new DamageEvent(enemy,
                        player,
                        null,
                        new SkillDamageCause(this).withKnockback(true),
                        damage,
                        getName()));
            }
        }

        // Cues
        UtilMessage.message(player, getClassType().getName(), "You fired <alt>%s %s</alt>.", getName(), level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f + player.getExp(), 1.75f - charge);
    }


    public TickBehavior getTickBehavior(Player player, ChargeData chargeData, int level) {
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        if (isHolding(player) && gamer.isHoldingRightClick() && championsManager.getEnergy().use(player, getName(), getEnergyPerSecond(level) / 20, true)) {
            return TickBehavior.TICK;
        }
        return TickBehavior.USE;
    }

}
