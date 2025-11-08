package me.mykindos.betterpvp.core.item.component.impl.runes.brutality;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
import me.mykindos.betterpvp.core.combat.modifiers.impl.GenericModifier;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class BrutalityRuneHandler implements Listener {

    private final BrutalityRune brutalityRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public BrutalityRuneHandler(BrutalityRune brutalityRune, ComponentLookupService lookupService) {
        this.brutalityRune = brutalityRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager) || !event.isDamageeLiving()) {
            return; // Only handle player damage events
        }

        final boolean melee = event.getCause().getCategories().contains(DamageCauseCategory.MELEE);
        final boolean ranged = event.getCause().getCategories().contains(DamageCauseCategory.RANGED);
        if (!melee && !ranged) {
            return; // Only handle melee attacks
        }

        // get the item used to deal damage
        // for ranged, we get the projectile "bow"
        // for melee, its equipment
        final ItemStack item;
        if (melee) {
            final EntityEquipment equipment = damager.getEquipment();
            if (equipment == null) {
                return;
            }

            item = equipment.getItemInMainHand();
        } else if (ranged && event.getProjectile() instanceof Arrow arrow) {
            item = arrow.getWeapon();
        } else {
            return;
        }

        if (item == null) {
            return; // Not shot by a bow
        }

        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(item, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        // Check if the slayer rune is present in the container
        if (!container.get().hasRune(brutalityRune)) {
            return; // Slayer rune not present
        }

        if (Math.random() < brutalityRune.getChance()) {
            event.addModifier(new GenericModifier(
                    brutalityRune.getName(),
                    ModifierType.RUNE,
                    1 + brutalityRune.getScalar(),
                    0)
            );

            final double width = event.getDamagee().getWidth();
            new SoundEffect(Sound.ITEM_TRIDENT_HIT, 0.5f, 1f).play(event.getDamagee().getLocation());
            new SoundEffect(Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1f, 1f).play(event.getDamagee().getLocation());
            Particle.DUST_PILLAR.builder()
                    .data(Material.REDSTONE_BLOCK.createBlockData())
                    .location(event.getDamagee().getLocation())
                    .offset(width / 2, width / 2, width / 2)
                    .receivers(60)
                    .count(100)
                    .spawn();
        }
    }
}
