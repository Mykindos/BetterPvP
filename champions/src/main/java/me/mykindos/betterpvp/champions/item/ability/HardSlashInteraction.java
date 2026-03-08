package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.combat.InteractionDamageCause;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.selector.entity.EntityFilters;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.EntityOrigin;
import me.mykindos.betterpvp.core.utilities.model.selector.shape.ShapeEntitySelector;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HardSlashInteraction extends CooldownInteraction implements DisplayedInteraction {

    private final Config config;

    private double damage = 5.0;
    private double radius = 4.5;
    private double knockbackStrength = 2.0;
    private double fireSeconds = 2.0;
    private double cooldown = 8.0;

    public HardSlashInteraction(CooldownManager cooldownManager, Config config) {
        super("hard_slash", cooldownManager);
        this.config = config;
    }

    public void loadConfig() {
        this.damage = config.getConfig("damage", 5.0, Double.class);
        this.radius = config.getConfig("radius", 4.5, Double.class);
        this.knockbackStrength = config.getConfig("knockback-strength", 2.0, Double.class);
        this.fireSeconds = config.getConfig("fire-seconds", 2.0, Double.class);
        this.cooldown = config.getConfig("cooldown", 8.0, Double.class);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Hard Slash");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Deal damage to nearby enemies and knock them back with a fiery slash.");
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        final Location center = actor.getEntity().getLocation();

        // Play flame particles
        final List<Location> circumference = UtilLocation.getCircumference(center, 0.5, 15);
        final List<ParticleBuilder> flameParticles = new ArrayList<>();
        for (Location location : circumference) {
            Vector flameDirection = location.clone().subtract(center).toVector();
            final ParticleBuilder particles = Particle.FLAME.builder()
                    .count(0)
                    .extra(1.2)
                    .offset(flameDirection.getX(), 0, flameDirection.getZ())
                    .location(center)
                    .receivers(60);
            flameParticles.add(particles);
        }

        UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
            flameParticles.forEach(ParticleBuilder::spawn);
            return true;
        }, 3, 1L);

        // Play redstone ring
        final List<Location> ringLocations = UtilLocation.getCircumference(center, radius, 50);
        final List<ParticleBuilder> redstoneParticles = new ArrayList<>();
        for (Location location : ringLocations) {
            final ParticleBuilder particles = Particle.DUST.builder()
                    .count(1)
                    .color(Color.fromRGB(255, 176, 41))
                    .location(location)
                    .receivers(60);
            redstoneParticles.add(particles);
        }

        UtilServer.repeatTask(JavaPlugin.getPlugin(Champions.class), run -> {
            redstoneParticles.forEach(ParticleBuilder::spawn);
            return true;
        }, 2, 1L);

        // Sounds
        new SoundEffect(Sound.ITEM_TRIDENT_THROW, 2f, 1f).play(center);
        new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 2f, 1f).play(center);
        new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_1, 0.3f, 0.8f).play(center);
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, 0.3f, 0.8f).play(center);

        // Do damage
        final ShapeEntitySelector selector = ShapeEntitySelector.sphere(new EntityOrigin(actor.getEntity(), true), radius);
        selector.withFilter(EntityFilters.combatEnemies())
                .select()
                .forEach(target -> onSelect(actor, context, target));
        return InteractionResult.Success.ADVANCE;
    }

    private void onSelect(@NotNull InteractionActor actor, @NotNull InteractionContext context, LivingEntity livingEntity) {
        UtilDamage.doDamage(new DamageEvent(
                livingEntity,
                actor.getEntity(),
                actor.getEntity(),
                new InteractionDamageCause(this).withCategory(DamageCauseCategory.MELEE),
                damage,
                "Hard Slash"
        ));

        UtilEntity.setFire(livingEntity, actor.getEntity(), (long) (fireSeconds * 1000));

        final Vector direction = livingEntity.getLocation().subtract(actor.getLocation()).toVector();
        UtilVelocity.velocity(livingEntity, actor.getEntity(), new VelocityData(
                direction,
                knockbackStrength,
                false,
                0.0,
                0.2,
                0.4,
                true,
                true
        ));
    }
}
