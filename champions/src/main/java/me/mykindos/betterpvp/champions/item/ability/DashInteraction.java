package me.mykindos.betterpvp.champions.item.ability;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Input;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class DashInteraction extends CooldownInteraction implements DisplayedInteraction {

    private final EffectManager effectManager;
    private final Config config;

    private double invulnerabilitySeconds = 0.5;
    private double strength = 1.0;
    private double energy = 12.0;

    public DashInteraction(CooldownManager cooldownManager, EffectManager effectManager, Config config) {
        super("dodge", cooldownManager);
        this.effectManager = effectManager;
        this.config = config;
    }

    public void loadConfig() {
        this.strength = config.getConfig("strength", 1.0, Double.class);
        this.invulnerabilitySeconds = config.getConfig("invulnerability-seconds", 0.5, Double.class);
        this.energy = config.getConfig("energy", 12.0, Double.class);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Dodge");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Quickly dash horizontally to dodge incoming attacks.");
    }

    @Override
    public double getCooldown() {
        return 0;
    }

    @Override
    public double getEnergyCost() {
        return energy;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final LivingEntity entity = actor.getEntity();
        final Vector direction;
        if (entity instanceof Player player) {
            final Input input = player.getCurrentInput();
            double x = 0;
            double z = 0;

            if (input.isForward()) z += 1;
            if (input.isBackward()) z -= 1;
            if (input.isRight()) x -= 1;
            if (input.isLeft()) x += 1;

            // No input means zero vector
            final Vector inputDirection = new Vector(x, 0, z);
            if (inputDirection.lengthSquared() != 0) {
                // Rotate by player yaw to world space
                direction = inputDirection;
                direction.rotateAroundY(Math.toRadians(-player.getLocation().getYaw()));
                direction.normalize();
            } else {
                direction = entity.getEyeLocation().getDirection().setY(0).normalize();
            }
        } else {
            direction = entity.getEyeLocation().getDirection().setY(0).normalize();
        }

        boolean success = UtilVelocity.velocity(entity, null, new VelocityData(
                direction,
                strength,
                true,
                0.01,
                0,
                0.01,
                false,
                true
        ));
        new SoundEffect(Sound.ITEM_TRIDENT_THROW, 2F, 1F).play(entity.getLocation());
        new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 0.3F).play(entity.getLocation());

        if (success) {
            UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
                if (!entity.isValid()) {
                    return false;
                }
                if (run >= 3) {
                    entity.setVelocity(new Vector());
                }
                Particle.FLAME.builder()
                        .count(3)
                        .extra(0)
                        .offset(0.2, 0.01, 0.2)
                        .location(entity.getLocation())
                        .receivers(60)
                        .spawn();
                return true;
            }, 4, 1L);
        }

        effectManager.addEffect(actor.getEntity(), EffectTypes.INVULNERABLE, (long) (invulnerabilitySeconds * 1000L));
        return success ? InteractionResult.Success.ADVANCE : new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED);
    }
}
