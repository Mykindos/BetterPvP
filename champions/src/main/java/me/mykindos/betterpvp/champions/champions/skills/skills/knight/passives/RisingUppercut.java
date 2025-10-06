package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.RisingUppercutData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RisingUppercut extends Skill implements Listener, CooldownToggleSkill, DamageSkill, MovementSkill {

    /**
     * Tracks each player's current state. This is primarily used so we can update the "slashing" animation in
     * {@link #onUpdate()}.
     */
    private final Map<Player, RisingUppercutData> data = new WeakHashMap<>();

    // <editor-fold defaultstate="collapsed" desc="Config Variables">
    private double radius;
    private double damage;

    // not sure if it's necessary to have this but always gotta account for the edge case!
    private int maxEnemiesCanBeHit;

    /**
     * How long the upward cutting animation of the skill will play for. It's not recommended to change this.
     */
    private double slashAnimationDuration;

    /**
     * After most movement abilities (like Disengage, Seismic Slam, etc.), the player is granted no fall for a limited
     * time (this is so you don't get screwed over by your own movement). The no fall effect for this skill is intended
     * to be short-lived (a few seconds).
     */
    private double noFallDurationInSeconds;

    /**
     * This is used to determine how far the player can be looking away from the enemy for this to still hit. A higher
     * threshold means it's more forgiving. Lower means you have to be looking even more exactly at the target.
     */
    private double fovThreshold;
    // </editor-fold>

    final SoundEffect risingUppercutSwingSFX = new SoundEffect("minecraft", "rising_uppercut_swing");
    final SoundEffect risingUppercutSlashSFX = new SoundEffect("minecraft", "rising_uppercut_slash");

    @Inject
    public RisingUppercut(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Slash nearby enemies hurling",
                "both them and yourself into",
                "the air.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    // entrypoint
    @Override
    public void toggle(Player player, int level) {
        if (data.containsKey(player)) {
            data.get(player).getItemDisplay().remove();
            data.remove(player);
        }

        // Spawns the sword at waist-height (of the player) with the center of the sword being 3 blocks away
        final @NotNull Location swordSpawnLocation = player.getLocation().clone()
                .add(player.getLocation().getDirection().normalize().multiply(3))
                .add(0,1,0);

        final @NotNull ItemDisplay itemDisplay = swordSpawnLocation.getWorld()
                .spawn(swordSpawnLocation, ItemDisplay.class, slashingSword -> {

            slashingSword.setItemStack(new ItemStack(Material.DIAMOND_SWORD));
            slashingSword.setGlowing(false);
            slashingSword.setPersistent(false);

            // This transformation amounts to a big sword that's angled slightly downwards.
            final @NotNull Transformation transformation = slashingSword.getTransformation();
            transformation.getScale().set(3);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(-80));
            transformation.getLeftRotation().rotateLocalY((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalZ((float) Math.toRadians(100));
            slashingSword.setTransformation(transformation);

            slashingSword.setTeleportDuration(1);
            slashingSword.setInterpolationDuration(1);
        });

        // If slashAnimationDuration is 0.3s then this is 300ms
        final long animationDurationInMillis = (long) (slashAnimationDuration * 1000L);

        data.put(player, new RisingUppercutData(
                System.currentTimeMillis(),  // start time of the skill
                animationDurationInMillis,
                itemDisplay,
                swordSpawnLocation
        ));

        // do to enemies in radius
        doDamageToEnemies(player, data.get(player));

        // do to caster
        doUppercutMovement(player, player);
        risingUppercutSwingSFX.play(swordSpawnLocation);
        Particle.BLOCK.builder()
                .count(50)
                .location(player.getLocation())
                .receivers(60)
                .data(Material.IRON_BLOCK.createBlockData())
                .offset(0.5, 0.1, 0.5)
                .spawn();

        final long noFallDurationInMillis = (long) (noFallDurationInSeconds * 1000L);
        championsManager.getEffects().addEffect(player, EffectTypes.NO_FALL, noFallDurationInMillis);
    }

    /**
     * Moves the target, which is `forEntity` directly upwards. The source of this velocity event will be set to
     * the caster parameter.
     */
    private void doUppercutMovement(@NotNull LivingEntity forEntity, @NotNull Player caster) {
        final Vector upwardDirection = new Vector(0, 1, 0);
        final VelocityData velocityData = new VelocityData(
                upwardDirection, 1, false, 0, 1.1, 1.1, true
        );

        UtilVelocity.velocity(forEntity, caster, velocityData, VelocityType.CUSTOM);
    }

    /**
     * Damages enemies within the radius that the player is also looking at. This method also spawns some particles
     * and plays a sound when a hit is confirmed.
     */
    private void doDamageToEnemies(@NotNull Player player, @NotNull RisingUppercutData abilityData) {

        // gonna be honest, idk if these 3 lines are necessary but this is how defensive stance checks fov sooooo
        final @NotNull Vector directionPlayerIsLooking = abilityData.getCastingLocation().getDirection();
        directionPlayerIsLooking.setY(0);
        directionPlayerIsLooking.normalize();

        long delay = 0;
        int enemiesHit = 0;

        for (LivingEntity enemy : UtilEntity.getNearbyEnemies(player, abilityData.getCastingLocation(), radius)) {

            if (enemiesHit > maxEnemiesCanBeHit) break;

            // check if enemy out of fov
            final @NotNull Vector from = UtilVelocity.getTrajectory(player, enemy).normalize();
            if (player.getLocation().getDirection().subtract(from).length() > fovThreshold) continue;
            if (!player.hasLineOfSight(enemy)) return;

            doUppercutMovement(enemy, player);

            UtilServer.runTaskLater(champions, () -> {
                CustomDamageEvent customDamageEvent = new CustomDamageEvent(enemy, player, null,
                        EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName());

                UtilDamage.doCustomDamage(customDamageEvent);

                // Cues
                UtilMessage.message(player, getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s</green>.", enemy.getName(), getName()));
                UtilMessage.message(enemy, getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s</green>.", player.getName(), getName()));

                // Effects
                risingUppercutSlashSFX.play(enemy.getLocation());
                Particle.ELECTRIC_SPARK.builder()
                        .count(40)
                        .location(enemy.getLocation())
                        .receivers(60)
                        .extra(1.0)
                        .offset(0.5, 0.5, 0.5)
                        .spawn();
            }, delay);

            delay += 3L;  // We don't want the slashes to play at the same time; delaying them sounds cooler
            enemiesHit++;
        }
    }

    /**
     * Cleans up {@link #data} and handles the animation of this skill.
     */
    @UpdateEvent
    public void onUpdate() {
        final Iterator<Map.Entry<Player, RisingUppercutData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, RisingUppercutData> entry = iterator.next();
            final Player player = entry.getKey();
            final RisingUppercutData abilityData = entry.getValue();

            final boolean playerNoLongerHasSkill = !hasSkill(player);
            final boolean animationIsComplete = UtilTime.elapsed(abilityData.getStartTime(), abilityData.getAnimationDuration());

            // end animation & remove player from `data`
            if (!player.isOnline() || player.isDead() || playerNoLongerHasSkill || animationIsComplete) {
                abilityData.getItemDisplay().remove();

                Particle.SMOKE.builder()
                        .count(40)
                        .offset(0.5, 0.5, 0.5)
                        .extra(0)
                        .location(abilityData.getItemDisplay().getLocation())
                        .receivers(60)
                        .spawn();

                iterator.remove();
                continue;
            }

            // movement for upward animation
            final @NotNull ItemDisplay itemDisplay = abilityData.getItemDisplay();
            final @NotNull Transformation transformation = itemDisplay.getTransformation();
            final float angleInRadians = (float) Math.toRadians(-5);

            transformation.getLeftRotation().rotateLocalX(transformation.getLeftRotation().x() + angleInRadians);
            itemDisplay.setTransformation(transformation);

            final @NotNull Location newLocation = itemDisplay.getLocation().clone()
                    .add(0, 0.5, 0);
            itemDisplay.teleport(newLocation);

        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public String getName() {
        return "Rising Uppercut";
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 5.0, Double.class);
        damage = getConfig("damage", 4.0, Double.class);
        slashAnimationDuration = getConfig("slashAnimationDuration", 0.7, Double.class);
        noFallDurationInSeconds = getConfig("noFallDurationInSeconds ", 3.0, Double.class);
        fovThreshold = getConfig("fovThreshold", 0.7, Double.class);
        maxEnemiesCanBeHit = getConfig("maxEnemiesCanBeHit", 5, Integer.class);
    }
}
