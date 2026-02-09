package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.BoomerangProjectile;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Heavensplitter extends AbstractInteraction implements Listener, DisplayedInteraction {

    @EqualsAndHashCode.Include
    private float hitbox; // Hitbox radius for collision detection
    @EqualsAndHashCode.Include
    private double damage; // Damage dealt by the trident
    @EqualsAndHashCode.Include
    private double impactVelocity; // velocity at which the target is hit
    @EqualsAndHashCode.Include
    private double velocity; // Velocity at which the trident flies
    @EqualsAndHashCode.Include
    private double airTime; // Seconds
    private final BaseItem heldItem;
    private final ItemFactory itemFactory;
    private final Map<Player, BoomerangProjectile> projectiles = new HashMap<>();

    public Heavensplitter(BaseItem heldItem, ItemFactory itemFactory) {
        super("heavensplitter");
        this.heldItem = heldItem;
        this.itemFactory = itemFactory;
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(Champions.class));
        UtilServer.runTaskTimer(JavaPlugin.getPlugin(Champions.class), this::tickProjectiles, 0L, 1L);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Heavensplitter");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Throw the weapon, summon the power of Thor, and deal damage to enemies in its path.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        LivingEntity entity = actor.getEntity();
        if (!(entity instanceof Player caster)) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        // Player already has a projectile, summon back
        if (projectiles.containsKey(caster)) {
            final BoomerangProjectile projectile = projectiles.get(caster);
            projectile.recall();
            projectile.playRedirectSound();
            return InteractionResult.Success.ADVANCE;
        }

        // Spawn projectile
        final BoomerangProjectile projectile = new BoomerangProjectile(
                "Heavensplitter",
                caster,
                hitbox,
                caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(0.5)),
                (long) (airTime * 1000),
                damage,
                impactVelocity,
                itemInstance,
                this
        );
        projectile.redirect(caster.getLocation().getDirection().multiply(velocity));
        projectile.playRedirectSound();
        projectiles.put(caster, projectile);

        // Consume durability
        // Run later because bug idk
        if (itemStack != null) {
            UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> UtilItem.damageItem(caster, itemStack, 1));
        }
        return InteractionResult.Success.ADVANCE;
    }

    public void tickProjectiles() {
        final Iterator<Map.Entry<Player, BoomerangProjectile>> iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, BoomerangProjectile> entry = iterator.next();
            final Player caster = entry.getKey();
            final BoomerangProjectile projectile = entry.getValue();
            // Remove if player is offline or dead
            if (caster == null || !caster.isOnline() || caster.isDead()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            // Check if the projectile is expired or marked for removal
            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            // Update the projectile's position
            projectile.tick();
        }
    }
}
