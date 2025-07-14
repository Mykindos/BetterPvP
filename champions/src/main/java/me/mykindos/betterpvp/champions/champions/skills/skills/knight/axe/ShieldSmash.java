package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

@Singleton
@BPvPListener
public class ShieldSmash extends Skill implements InteractSkill, CooldownSkill, Listener, CrowdControlSkill {

    private double multiplier;
    private double multiplierPerLevel;
    private double entityKickbackMultiplier;
    private double entityKickbackMultiplierPerLevel;
    private double blockKickbackMultiplier;
    private double blockKickbackMultiplierPerLevel;
    @Getter
    private double range;
    @Getter
    private double fov;

    @Inject
    public ShieldSmash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Shield Smash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Smash your shield into an enemy,",
                "dealing " + getValueString(this::getKnockbackMultiplier, level, 100, "%", 0) + " knockback and knocking",
                "you back in the opposite direction.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getBlockKickbackMultiplier(int level) {
        return blockKickbackMultiplier + (blockKickbackMultiplierPerLevel * (level - 1));
    }

    public double getEntityKickbackMultiplier(int level) {
        return entityKickbackMultiplier + (entityKickbackMultiplierPerLevel * (level - 1));
    }

    public double getKnockbackMultiplier(int level) {
        return multiplier + (multiplierPerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            event.setKnockback(false);
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean activate(Player player, int level) {
        final Location bashLocation = player.getLocation().add(0, 0.8, 0);
        bashLocation.add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5));

        // Visual Cues
        Collection<Player> receivers = player.getWorld().getNearbyPlayers(player.getLocation(), 60);
        Particle.CLOUD.builder().extra(0.05f).count(6).location(bashLocation).receivers(receivers).spawn();
        Particle.EXPLOSION.builder().extra(0).count(1).location(bashLocation).receivers(receivers).spawn();

        // Skill
        Vector direction = player.getLocation().getDirection();
        direction.setY(Math.max(0, direction.getY())); // Prevents downwards knockback

        final double strength = getKnockbackMultiplier(level);
        final List<KeyValue<LivingEntity, EntityProperty>> bashed = UtilEntity.getNearbyEntities(player, bashLocation, getRange(), EntityProperty.ALL);
        boolean hit = false;
        for (KeyValue<LivingEntity, EntityProperty> bashedEntry : bashed) {
            final LivingEntity ent = bashedEntry.getKey();

            if (!player.hasLineOfSight(ent.getLocation()) && !player.hasLineOfSight(ent.getEyeLocation())) {
                continue; // Skip entities not in line of sight
            }

            // Get angle from player to entity
            final double angle = Math.toDegrees(player.getLocation().getDirection()
                    .angle(player.getLocation().toVector().subtract(player.getLocation().toVector())));
            if (angle > getFov() / 2) {
                continue; // Skip entities not in front of us
            }

            // Add velocity and damage
            hit = true;
            VelocityData velocityData = new VelocityData(direction, strength, false, 0, 0.3, 0.8 + 0.25, true);
            UtilVelocity.velocity(ent, player, velocityData, VelocityType.KNOCKBACK_CUSTOM);
            UtilDamage.doDamage(new DamageEvent(ent, player, null, new VanillaDamageCause(EntityDamageEvent.DamageCause.FALL), 0.0, getName()));

            // Cancel fall damage if they're friendly
            if (bashedEntry.getValue() == EntityProperty.FRIENDLY && ent instanceof Player friendly) {
                championsManager.getEffects().addEffect(friendly, EffectTypes.NO_FALL, 10_000);
            }

            // Inform them
            UtilMessage.simpleMessage(ent, "Skill", "<alt2>%s</alt2> hit you with <alt>%s</alt>.", player.getName(), getName());
        }

        if (hit) { // entity hit
            final VelocityData data = new VelocityData(player.getLocation().getDirection().multiply(-1),
                    getEntityKickbackMultiplier(level),
                    0,
                    1.0,
                    true);
            UtilVelocity.velocity(player, player, data);
        }

        if (!hit) {
            final RayTraceResult trace = player.rayTraceBlocks(getRange());
            if (trace != null && trace.getHitBlock() != null) { // block hit
                final VelocityData data = new VelocityData(player.getLocation().getDirection().multiply(-1),
                        getBlockKickbackMultiplier(level),
                        0,
                        1.0,
                        true);
                UtilVelocity.velocity(player, player, data);
                hit = true; // we hit a block
            }
        }

        // Result indicator
        if (hit) {
            player.getWorld().playSound(bashLocation, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.9f);
        } else {
            UtilMessage.simpleMessage(player, "Skill", "You missed <alt>%s</alt>.", getName());
        }
        return true;
    }

    @EventHandler
    public void onShieldCheck(RightClickEvent event) {
        Player player = event.getPlayer();
        if (hasSkill(player) && !this.championsManager.getCooldowns().hasCooldown(player, getName())) {
            if (isHolding(event.getPlayer())) {
                event.setBlockingItem(ItemStack.of(Material.SHIELD));
            }
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        multiplier = getConfig("multiplier", 1.6, Double.class);
        multiplierPerLevel = getConfig("multiplierPerLevel", 0.2, Double.class);
        entityKickbackMultiplier = getConfig("entityKickbackMultiplier", 0.7, Double.class);
        entityKickbackMultiplierPerLevel = getConfig("entityKickbackMultiplierPerLevel", 0.2, Double.class);
        blockKickbackMultiplier = getConfig("blockKickbackMultiplier", 2.0, Double.class);
        blockKickbackMultiplierPerLevel = getConfig("blockKickbackMultiplierPerLevel", 0.5, Double.class);
        range = getConfig("range", 3.0, Double.class);
        fov = getConfig("fov", 90.0, Double.class);
    }
}