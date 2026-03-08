package me.mykindos.betterpvp.core.effects.types.negative;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

public class FrenzyEffect extends VanillaEffectType {

    private static final NamespacedKey GRAVITY_KEY = new NamespacedKey("betterpvp", "frenzy_gravity");
    private static final NamespacedKey REACH_KEY = new NamespacedKey("betterpvp", "frenzy_reach");
    private static final String ASSOCIATED_WITH_KEY = "frenzy_target";
    public static final double REACH = 4.;

    @Override
    public String getName() {
        return "Frenzy";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.BLINDNESS;
    }

    // Teleport around the player
    public static void teleportAround(LivingEntity target, LivingEntity caster) {
        double reach = FrenzyEffect.REACH;
        Location targetCenter = target.getLocation().add(0, target.getHeight() / 2, 0);

        // Random point on unit sphere (uniform distribution)
        double theta = Math.random() * 2 * Math.PI;
        double phi = Math.acos(2 * Math.random() - 1);

        double x = Math.sin(phi) * Math.cos(theta);
        double y = Math.cos(phi);
        double z = Math.sin(phi) * Math.sin(theta);

        // Calculate where to teleport to
        Vector direction = new Vector(x, y, z).multiply(reach);
        final Location teleportLocation = UtilLocation.gracefulRayTrace(target.getLocation(),
                direction,
                reach,
                caster.getBoundingBox(),
                caster::hasLineOfSight).orElse(targetCenter);

        // Get new look vector looking toward the center of the target
        final Vector eyeHeight = new Vector(0, caster.getEyeHeight(), 0);
        Vector lookVector = targetCenter.toVector().subtract(teleportLocation.toVector().add(eyeHeight)).normalize();
        teleportLocation.setDirection(lookVector);

        // Teleport them
        caster.leaveVehicle();
        caster.teleportAsync(teleportLocation).thenRun(() -> {
            caster.setFallDistance(0);
        });
    }

    public static boolean isFrenzy(LivingEntity livingEntity) {
        final AttributeInstance attribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.GRAVITY));
        return attribute.getModifier(GRAVITY_KEY) != null;
    }

    public static LivingEntity getTarget(LivingEntity livingEntity) {
        if (livingEntity.hasMetadata(ASSOCIATED_WITH_KEY)) {
            return livingEntity.getMetadata(ASSOCIATED_WITH_KEY).stream()
                    .map(meta -> Bukkit.getEntity((UUID) Objects.requireNonNull(meta.value())))
                    .filter(Objects::nonNull)
                    .filter(entity -> entity instanceof LivingEntity)
                    .map(entity -> (LivingEntity) entity)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static void setGravity(LivingEntity livingEntity, boolean gravity) {
        final AttributeInstance attribute = livingEntity.getAttribute(Attribute.GRAVITY);
        if (attribute == null) return;

        if (gravity) {
            attribute.removeModifier(GRAVITY_KEY);
        } else {
            attribute.addTransientModifier(new AttributeModifier(GRAVITY_KEY, -attribute.getBaseValue() / 1.05, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private static void setReach(LivingEntity livingEntity, boolean reach) {
        final AttributeInstance attribute = livingEntity.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attribute == null) return;

        if (reach) {
            attribute.addTransientModifier(new AttributeModifier(REACH_KEY, REACH, AttributeModifier.Operation.ADD_NUMBER));
        } else {
            attribute.removeModifier(REACH_KEY);
        }
    }

    private static void setTarget(LivingEntity livingEntity, UUID target) {
        if (target != null) {
            livingEntity.setMetadata(ASSOCIATED_WITH_KEY, new FixedMetadataValue(JavaPlugin.getPlugin(Core.class), target));
        } else {
            livingEntity.removeMetadata(ASSOCIATED_WITH_KEY, JavaPlugin.getPlugin(Core.class));
        }
    }

    public static void clear(LivingEntity livingEntity) {
        setGravity(livingEntity, true);
        setReach(livingEntity, false);
        setTarget(livingEntity, null);
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        super.onReceive(livingEntity, effect);

        // set their gravity
        livingEntity.setVelocity(new Vector());
        setGravity(livingEntity, false);
        final LivingEntity applier = effect.getApplier().get();
        if (applier != null) {
            applier.setVelocity(new Vector());
            setGravity(applier, false);
            setReach(applier, true);
            setTarget(applier, livingEntity.getUniqueId());
        }
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        super.onExpire(livingEntity, effect, notify);

        // give them back gravity
        clear(livingEntity);
        final LivingEntity applier = effect.getApplier().get();
        if (applier != null) clear(applier);
    }

    @Override
    public String getDescription(int level) {
        return "<white>Frenzy " + UtilFormat.getRomanNumeral(level) + "grants <reset><val>" + level + "</val> free melee hits on a target.";
    }

}
