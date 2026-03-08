package me.mykindos.betterpvp.core.item.component.impl.runes.detonation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@BPvPListener
@Singleton
public class DetonationRuneHandler implements Listener {

    private final DetonationRune detonationRune;
    private final DamageLogManager damageLogManager;
    private final ComponentLookupService componentLookupService;

    @Inject
    public DetonationRuneHandler(DetonationRune detonationRune, DamageLogManager damageLogManager, ComponentLookupService lookupService) {
        this.detonationRune = detonationRune;
        this.damageLogManager = damageLogManager;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        final DamageLog lastDamage = damageLogManager.getLastDamage(event.getEntity());
        if (lastDamage == null) {
            return; // Wasn't killed by a player
        }

        if (!(lastDamage.getDamager() instanceof LivingEntity damager)) {
            return; // Wasn't killed by a player
        }

        if (!lastDamage.getDamageCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            return; // Wasn't a melee hit
        }

        final EntityEquipment equipment = damager.getEquipment();
        if (equipment == null) {
            return;
        }

        final ItemStack item = equipment.getItemInMainHand();
        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(item, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        // Check if the rune is present in the container
        if (!container.get().hasRune(detonationRune)) {
            return; // Rune not present
        }

        // Explode
        explode(event.getEntity().getLocation(), damager);
    }

    private void explode(@NotNull Location location, LivingEntity damager) {
        final double radius = detonationRune.getRadius();
        final double damage = detonationRune.getDamage();

        // Spawn particle effect at the victim's location
        Particle.DUST_PILLAR.builder()
                .location(location.clone().add(0, 1, 0))
                .data(Material.RED_CONCRETE.createBlockData())
                .count((int) (100 * radius))
                .offset(radius / 2, radius / 2, radius / 2)
                .receivers(60)
                .spawn();
        new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f).play(location);

        // Get nearby enemies and damage them
        for (LivingEntity enemy : UtilEntity.getNearbyEnemies(damager, location, radius)) {
            // Create and apply damage event
            UtilDamage.doDamage(new DamageEvent(
                    enemy,
                    damager,
                    null,
                    new VanillaDamageCause(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, TriState.FALSE),
                    damage,
                    detonationRune.getName()
            ));
        }
    }

}
