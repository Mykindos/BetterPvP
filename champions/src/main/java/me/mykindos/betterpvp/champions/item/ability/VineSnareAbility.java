package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.projectile.VineSnareProjectile;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class VineSnareAbility extends ItemAbility implements Listener {

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
        super(new NamespacedKey(champions, "vine_snare"),
                "Vine Snare",
                "Bolts release vines that ensnare enemies and anchor them to the ground.",
                TriggerTypes.PASSIVE);
        this.itemFactory = itemFactory;
        this.baseItem = baseItem;
        this.effectManager = effectManager;
        Bukkit.getPluginManager().registerEvents(this, champions);
        UtilServer.runTaskTimer(champions, this::processProjectiles, 0L, 1L);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        return true;
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
        final Optional<AbilityContainerComponent> containerOpt = instance.getComponent(AbilityContainerComponent.class);
        if (containerOpt.isEmpty()) return;
        final AbilityContainerComponent container = containerOpt.get();

        if (container.getAbilities().contains(this)) {
            event.setCancelled(true);

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