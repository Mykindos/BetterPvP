package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SkyforgedAscent extends CooldownInteraction implements DisplayedInteraction, Listener {

    @EqualsAndHashCode.Include
    private double velocity;
    @EqualsAndHashCode.Include
    private double cooldown; // Seconds
    @EqualsAndHashCode.Include
    private int speedAmplifier;
    @EqualsAndHashCode.Include
    private double speedDuration; // Seconds
    private final EffectManager effectManager;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;

    public SkyforgedAscent(EffectManager effectManager, CooldownManager cooldownManager, ItemFactory itemFactory, ClientManager clientManager) {
        super("skyforged_ascent", cooldownManager);
        this.effectManager = effectManager;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(Champions.class));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Skyforged Ascent");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Throw the weapon to ascend skyward, riding its divine force, granting you a burst of speed.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();

        // Set them to riptide mode
        UtilVelocity.velocity(entity, entity, new VelocityData(
                entity.getLocation().getDirection(),
                velocity,
                0,
                10.0,
                true
        ));
        effectManager.addEffect(entity, EffectTypes.NO_FALL, 5_000L);

        // Give speed effect
        effectManager.addEffect(entity, EffectTypes.SPEED, speedAmplifier, (long) (speedDuration * 1000L));

        // Cues
        new SoundEffect(Sound.ITEM_TRIDENT_THUNDER, 2f, 0.5f).play(entity.getLocation());
        new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_2, 0f, 1f).broadcast(entity);
        Particle.CLOUD.builder()
                .extra(0.2)
                .count(20)
                .location(entity.getLocation())
                .receivers(60)
                .spawn();

        // Spawn circle with star of DUST particles under the player
        // Create circle particles
        Location center = entity.getEyeLocation().add(entity.getLocation().getDirection().multiply(2.5));
        Particle particle = Particle.END_ROD;
        final double radius = 2.5; // Radius of the circle
        final double pitch = Math.toRadians(entity.getLocation().getPitch() + 90);
        final double yaw = Math.toRadians(-entity.getLocation().getYaw());

        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * radius; // 2 block radius
            double z = Math.sin(angle) * radius;

            Vector particleLoc = new Vector(x, 0, z);
            particleLoc.rotateAroundX(pitch);
            particleLoc.rotateAroundY(yaw);
            particleLoc.add(center.toVector());
            particle.builder()
                    .location(particleLoc.toLocation(center.getWorld()))
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }

        // Create star particles (5-pointed star)
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72); // 72 degrees between points
            double x = Math.cos(angle) * radius; // Slightly larger radius for star
            double z = Math.sin(angle) * radius;

            Vector starPoint = new Vector(x, 0, z);
            starPoint.rotateAroundX(pitch);
            starPoint.rotateAroundY(yaw);
            starPoint.add(center.toVector());
            particle.builder()
                    .location(starPoint.toLocation(center.getWorld()))
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }

        // Create inner star points (connecting lines)
        for (int i = 0; i < 5; i++) {
            double angle1 = Math.toRadians(i * 72);
            double angle2 = Math.toRadians((i + 2) * 72); // Connect to point 2 steps away

            double x1 = Math.cos(angle1) * radius;
            double z1 = Math.sin(angle1) * radius;
            double x2 = Math.cos(angle2) * radius;
            double z2 = Math.sin(angle2) * radius;

            // Spawn particles along the line between star points
            for (int j = 0; j <= 10; j++) {
                double t = j / 10.0;
                double x = x1 + (x2 - x1) * t;
                double z = z1 + (z2 - z1) * t;

                Vector linePoint = new Vector(x, 0, z);
                linePoint.rotateAroundX(pitch);
                linePoint.rotateAroundY(yaw);
                linePoint.add(center.toVector());
                particle.builder()
                        .location(linePoint.toLocation(center.getWorld()))
                        .extra(0)
                        .receivers(60)
                        .spawn();
            }
        }

        // Consume durability
        if (itemStack != null && entity instanceof Player player) {
            UtilItem.damageItem(player, itemStack, 1);
        }
        return InteractionResult.Success.ADVANCE;
    }
}
