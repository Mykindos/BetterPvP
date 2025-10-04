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

    private double radius;
    private double damage;
    private double velocityStrength;
    private double slashAnimationDuration;

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
            data.get(player).getSlashingSword().remove();
            data.remove(player);
        }

        final @NotNull Location location = player.getLocation().clone()
                .add(0,1,0);
        location.add(player.getLocation().getDirection().normalize().multiply(2.5));

        final @NotNull ItemDisplay sword = location.getWorld().spawn(location, ItemDisplay.class, slashingSword -> {

            // todo: you could make it based on the weapon they used to activate... that'd be cool
            slashingSword.setItemStack(new ItemStack(Material.DIAMOND_SWORD));
            slashingSword.setGlowing(false);
            slashingSword.setPersistent(false);

            Transformation transformation = slashingSword.getTransformation();
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
        data.put(player, new RisingUppercutData(System.currentTimeMillis(), animationDurationInMillis, sword, player.getLocation().clone()));

        doDamageToEnemies(player, data.get(player));
        Vector vec = new Vector(0, 1, 0);
        VelocityData velocityData = new VelocityData(vec, 1, false, 0, 1.1, 1.1, true);
        UtilVelocity.velocity(player, player, velocityData, VelocityType.CUSTOM);

        new SoundEffect("minecraft", "rising_uppercut_swing").play(player.getLocation());

        championsManager.getEffects().addEffect(player, EffectTypes.NO_FALL, 3000);
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<Map.Entry<Player, RisingUppercutData>> iterator = data.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, RisingUppercutData> entry = iterator.next();
            final Player player = entry.getKey();
            final RisingUppercutData abilityData = entry.getValue();

            if (!player.isOnline()) {
                iterator.remove();
                abilityData.getSlashingSword().remove();  // todo: make it linger for like 100ms-200ms
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                abilityData.getSlashingSword().remove();  // todo: make it linger for like 100ms-200ms
                iterator.remove();
                continue;
            }

            // animation is over
            if (UtilTime.elapsed(abilityData.getStartTimeInMillis(), abilityData.getAnimationDurationInMillis())) {
                iterator.remove();
                abilityData.getSlashingSword().remove();  // todo: make it linger for like 100ms-200ms
                continue;
            }

            // continue animation
            var slashingSword = abilityData.getSlashingSword();
            Transformation transformation = slashingSword.getTransformation();
            transformation.getLeftRotation().rotateLocalX(transformation.getLeftRotation().x() + ((float) Math.toRadians(-5)));
            slashingSword.setTransformation(transformation);
            slashingSword.teleport(slashingSword.getLocation().clone().add(0,1.25,0));

        }
    }

    private void doDamageToEnemies(@NotNull Player player, @NotNull RisingUppercutData abilityData) {
        final @NotNull Vector directionPlayerIsLooking = abilityData.getCastingLocation().getDirection();
        directionPlayerIsLooking.setY(0);
        directionPlayerIsLooking.normalize();

        long delay = 0;

        for (LivingEntity enemy : UtilEntity.getNearbyEnemies(player, abilityData.getCastingLocation(), radius)) {
            Vector from = UtilVelocity.getTrajectory(player, enemy);
            from.normalize();
            if (player.getLocation().getDirection().subtract(from).length() > 0.7D) continue;  // out of fov

            UtilServer.runTaskLater(champions, () -> {
                new SoundEffect("minecraft", "rising_uppercut_slash").play(enemy.getLocation());

                CustomDamageEvent customDamageEvent = new CustomDamageEvent(enemy, player, null,
                        EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName());
                UtilDamage.doCustomDamage(customDamageEvent);


                Particle.ELECTRIC_SPARK.builder()
                        .count(40)
                        .location(enemy.getLocation())
                        .receivers(60)
                        .extra(1.0)
                        .offset(0.5, 0.5, 0.5)
                        .spawn();
            }, delay);

            delay += 3L;

            Vector vec = new Vector(0, 1, 0);
            VelocityData velocityData = new VelocityData(vec, 1, false, 0, 1.1, 1.1, true);
            UtilVelocity.velocity(enemy, null, velocityData, VelocityType.CUSTOM);
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
        velocityStrength = getConfig("velocityStrength", 2.0, Double.class);
        slashAnimationDuration = getConfig("slashAnimationDuration", 0.3, Double.class);
    }
}
