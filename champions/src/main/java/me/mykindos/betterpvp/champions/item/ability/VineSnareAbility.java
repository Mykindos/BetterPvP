package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.VineSnareProjectile;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class VineSnareAbility extends AbstractInteraction implements DisplayedInteraction, Listener {

    @EqualsAndHashCode.Exclude
    private final BaseItem baseItem;
    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;
    @EqualsAndHashCode.Exclude
    private final List<VineSnareProjectile> projectiles = new ArrayList<>();
    private double hitboxSize;
    private double speed;
    private long aliveTime;
    private int entangleAmplifier;
    private double entangleSeconds;

    public VineSnareAbility(Champions champions, ItemFactory itemFactory, BaseItem baseItem, EffectManager effectManager) {
        super("vine_snare");
        this.itemFactory = itemFactory;
        this.baseItem = baseItem;
        this.effectManager = effectManager;
        Bukkit.getPluginManager().registerEvents(this, champions);
        UtilServer.runTaskTimer(champions, this::processProjectiles, 0L, 1L);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Vine Snare");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Bolts release vines that ensnare enemies and anchor them to the ground.");
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        // This is a passive ability triggered by bow shooting
        return InteractionResult.Success.ADVANCE;
    }

    private void processProjectiles() {
        final Iterator<VineSnareProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            final VineSnareProjectile projectile = iterator.next();
            if (projectile.getCaster() != null) {
                final Player caster = projectile.getCaster();
                if (!caster.isOnline()) {
                    projectile.setMarkForRemoval(true);
                }
            }

            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            projectile.tick();
        }
    }

    /**
     * Apply the projectile
     */
    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (event.isCancelled()) return;
        if (event.getBow() == null) return;
        if (!itemFactory.isItemOfType(event.getBow(), baseItem)) return;

        final ItemInstance instance = itemFactory.fromItemStack(event.getBow()).orElseThrow();
        final Optional<InteractionContainerComponent> containerOpt = instance.getComponent(InteractionContainerComponent.class);
        if (containerOpt.isEmpty()) return;
        final InteractionContainerComponent container = containerOpt.get();

        // Check if this ability is in the chain
        boolean hasAbility = container.getChain().hasRoot(this);

        if (hasAbility) {
            event.setCancelled(true);

            if (event.getEntity() instanceof Player player) {
                // Consume durability
                UtilItem.damageItem(player, instance, 1);
            }

            final VineSnareProjectile projectile = new VineSnareProjectile(
                    event.getEntity() instanceof Player player ? player : null,
                    hitboxSize,
                    event.getProjectile().getLocation(),
                    aliveTime,
                    getName(),
                    effectManager,
                    entangleAmplifier,
                    (long) (entangleSeconds * 1000L)
            );
            final Vector direction = event.getProjectile().getVelocity().normalize();
            projectile.redirect(direction.multiply(event.getForce() * speed));
            projectiles.add(projectile);
            new SoundEffect(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f).play(projectile.getLocation());
        }
    }
}
