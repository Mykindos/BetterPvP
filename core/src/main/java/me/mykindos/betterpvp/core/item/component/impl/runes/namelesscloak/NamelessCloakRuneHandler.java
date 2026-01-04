package me.mykindos.betterpvp.core.item.component.impl.runes.namelesscloak;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class NamelessCloakRuneHandler implements Listener {

    private final @NotNull String RUNE_IDENTIFIER = NamelessCloakRune.KEY.getKey();
    private final Set<LivingEntity> tracked = Collections.newSetFromMap(new WeakHashMap<>());

    private final DamageLogManager damageLogManager;
    private final ClientManager clientManager;
    private final NamelessCloakRune namelessCloakRune;
    private final ComponentLookupService componentLookupService;
    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;

    @Inject
    public NamelessCloakRuneHandler(DamageLogManager damageLogManager, ClientManager clientManager, NamelessCloakRune namelessCloakRune,
                                    ComponentLookupService lookupService, EffectManager effectManager, CooldownManager cooldownManager) {
        this.damageLogManager = damageLogManager;
        this.clientManager = clientManager;
        this.namelessCloakRune = namelessCloakRune;
        this.componentLookupService = lookupService;
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
    }

    /**
     * Called when a player changes their equipment.
     * Todo: some duplicated logic with WandererRuneHandler, consider refactoring.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquip(EntityEquipmentChangedEvent event) {
        final @NotNull LivingEntity entity = event.getEntity();
        final @Nullable EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        tracked.remove(entity);

        for (ItemStack armorContent : equipment.getArmorContents()) {
            final Optional<RuneContainerComponent> container = componentLookupService.getComponent(armorContent, RuneContainerComponent.class);
            if (container.isEmpty()) continue;  // -> no runee

            final @NotNull RuneContainerComponent runeContainer = container.get();

            if (runeContainer.hasRune(namelessCloakRune)) {
                tracked.add(entity);
                return;
            }
        }
    }

    @UpdateEvent
    public void onTick() {
        Iterator<LivingEntity> iterator = tracked.iterator();
        while (iterator.hasNext()) {
            final @NotNull LivingEntity entity = iterator.next();

            if (!(entity instanceof Player player)) continue;
            if (!player.isOnline() || !player.isValid()) {
                iterator.remove();
                continue;
            }

            // Check if they're in combat
            final @Nullable DamageLog lastDamager = damageLogManager.getLastDamager(entity);
            final boolean hasLastDamager = lastDamager != null && lastDamager.getDamager() != null;

            if (hasLastDamager || UtilPlayer.isPlayerInCombat(clientManager, player)) {
                effectManager.removeEffect(player, EffectTypes.INVISIBILITY, RUNE_IDENTIFIER);
                continue;
            }

            // prevents vfx/sfx spam
            if (!effectManager.hasEffect(player, EffectTypes.INVISIBILITY, RUNE_IDENTIFIER)
                    && cooldownManager.use(player, RUNE_IDENTIFIER, namelessCloakRune.getVfxDelay(), false)) {

                playVfxAndSfx(player);
            }

            this.effectManager.addEffect(entity, null, EffectTypes.INVISIBILITY, RUNE_IDENTIFIER, 1, 100L);
        }
    }

    private void playVfxAndSfx(@NotNull Player player) {
        final Location location = player.getLocation().add(0, 2.5, 0);
        final World world = player.getWorld();
        world.spawnParticle(Particle.FALLING_NECTAR, location, 10, 0.5, 0.5, 0.5, 0.0);

        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.0f);
    }
}
