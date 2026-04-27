package me.mykindos.betterpvp.progression.profession.mining.item.interaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.projectile.RiftPickaxeProjectile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ChainThrowInteraction extends CooldownInteraction implements DisplayedInteraction {

    @EqualsAndHashCode.Include
    private double cooldown;
    @EqualsAndHashCode.Include
    private double aliveTime;    // seconds
    @EqualsAndHashCode.Include
    private double speed;        // blocks/tick
    @EqualsAndHashCode.Include
    private double explosionInterval; // seconds
    @EqualsAndHashCode.Include
    private int explosionRadius;
    @EqualsAndHashCode.Include
    private double oreChance;
    @EqualsAndHashCode.Include
    private int maxBounces;
    @EqualsAndHashCode.Include
    private boolean allowRecall;

    private final BaseItem heldItem;
    private final ItemFactory itemFactory;
    private final Map<Player, RiftPickaxeProjectile> projectiles = new HashMap<>();

    // Tracks invocations that should bypass the cooldown apply in then() because they were
    // recall presses, not throws. Populated in doExecute, drained in then(). The TTL is a
    // safety net so an entry never lingers if then() is skipped for any reason.
    private final Cache<UUID, Boolean> recallInvocations = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    @Setter
    private Supplier<Material> oreSupplier = () -> null;

    public ChainThrowInteraction(CooldownManager cooldownManager, ItemFactory itemFactory, BaseItem heldItem,
                                  double cooldown, double aliveTime, double speed,
                                  double explosionInterval, int explosionRadius, double oreChance,
                                  int maxBounces, boolean allowRecall) {
        super("Chain Throw", cooldownManager);
        this.heldItem = heldItem;
        this.itemFactory = itemFactory;
        this.cooldown = cooldown;
        this.aliveTime = aliveTime;
        this.speed = speed;
        this.explosionInterval = explosionInterval;
        this.explosionRadius = explosionRadius;
        this.oreChance = oreChance;
        this.maxBounces = maxBounces;
        this.allowRecall = allowRecall;

        Progression plugin = JavaPlugin.getPlugin(Progression.class);
        UtilServer.runTaskTimer(plugin, this::tickProjectiles, 0L, 1L);
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        // Recall must short-circuit BEFORE the cooldown gate in CooldownInteraction#doExecute,
        // otherwise the second press is rejected with FailReason.COOLDOWN and recall never fires.
        // Skipped entirely when allowRecall is disabled — the projectile then only ends via
        // expiry, max-bounces auto-recall, or being marked for removal.
        if (allowRecall && actor.isPlayer()) {
            final Player caster = (Player) actor.getEntity();
            final RiftPickaxeProjectile existing = projectiles.get(caster);
            if (existing != null) {
                existing.recall();
                recallInvocations.put(caster.getUniqueId(), Boolean.TRUE);
                return InteractionResult.Success.ADVANCE;
            }
        }
        return super.doExecute(actor, context, itemInstance, itemStack);
    }

    @Override
    public void then(@NotNull InteractionActor actor,
                     @NotNull InteractionContext context,
                     @NotNull InteractionResult result,
                     @Nullable ItemInstance itemInstance,
                     @Nullable ItemStack itemStack) {
        // Skip cooldown application when this invocation was a recall — only the throw should
        // start the cooldown. We still want sibling/event hooks via super, just not the
        // CooldownInteraction#then path that calls cooldownManager.use().
        if (actor.isPlayer()) {
            final UUID id = actor.getEntity().getUniqueId();
            if (recallInvocations.getIfPresent(id) != null) {
                recallInvocations.invalidate(id);
                return;
            }
        }
        super.then(actor, context, result, itemInstance, itemStack);
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor,
                                                            @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance,
                                                            @Nullable ItemStack itemStack) {
        if (!actor.isPlayer() || itemInstance == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        Player caster = (Player) actor.getEntity();

        // Spawn new projectile
        final long aliveMillis = (long) (aliveTime * 1000);
        final long explosionIntervalMillis = (long) (explosionInterval * 1000);

        // Get the display item from the item instance
        final ItemStack displayItem = itemInstance.createItemStack();

        final RiftPickaxeProjectile projectile = new RiftPickaxeProjectile(
                caster,
                0.3,
                caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(0.5)),
                aliveMillis,
                displayItem,
                explosionRadius,
                explosionIntervalMillis,
                oreChance,
                maxBounces,
                oreSupplier
        );

        // speed * 20 converts blocks/tick to blocks/second for the velocity vector
        projectile.redirect(caster.getLocation().getDirection().multiply(speed * 20));
        projectiles.put(caster, projectile);

        return InteractionResult.Success.ADVANCE;
    }

    public void tickProjectiles() {
        final Iterator<Map.Entry<Player, RiftPickaxeProjectile>> iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, RiftPickaxeProjectile> entry = iterator.next();
            final Player caster = entry.getKey();
            final RiftPickaxeProjectile projectile = entry.getValue();

            // Remove if player is offline or dead
            if (caster == null || !caster.isOnline() || caster.isDead()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            // Remove if expired or marked for removal
            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            projectile.tick();
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Chain Throw");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Hurl the pickaxe forward, detonating the path. Returns after 4 seconds or on impact with an unbreakable surface.");
    }
}
