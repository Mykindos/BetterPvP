package me.mykindos.betterpvp.champions.item.bloomrot;

import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Getter
@Setter
public class NectarOfDecay extends ItemAbility implements Listener {

    private double cooldown;
    private double poisonSeconds;
    private int poisonAmplifier;
    private double speed;
    private double hitbox;
    private double travelSeconds;
    private double cloudRadius;
    private double cloudSeconds;
    private double healPercent;

    @EqualsAndHashCode.Exclude
    private final WeakHashMap<Player, List<NectarOfDecayProjectile>> projectiles = new WeakHashMap<>();
    @EqualsAndHashCode.Exclude
    private final CooldownManager cooldownManager;
    @EqualsAndHashCode.Exclude
    private final EffectManager effectManager;
    @EqualsAndHashCode.Exclude
    private final Champions plugin;

    @Inject
    private NectarOfDecay(Champions champions, EffectManager effectManager, CooldownManager cooldownManager, EffectManager effectManager1) {
        super(new NamespacedKey(champions, "nectar_of_decay"),
                "Nectar of Decay",
                "Unleash a toxic blossom that bursts into a spreading poison cloud, siphoning health from enemies to restore your own.",
                TriggerTypes.RIGHT_CLICK);
        this.plugin = champions;
        this.cooldownManager = cooldownManager;
        this.effectManager = effectManager1;
        UtilServer.runTaskTimer(champions, this::processProjectiles, 0, 1);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());

        if (!cooldownManager.use(player, getName(), cooldown, true, true, false, true)) {
            return false;
        }

        final Location location = player.getEyeLocation();
        final NectarOfDecayProjectile projectile = new NectarOfDecayProjectile(
                player,
                location,
                hitbox,
                (long) (travelSeconds * 1000L),
                poisonAmplifier,
                (long) (poisonSeconds * 1000L),
                cloudRadius,
                (long) (cloudSeconds * 1000L),
                this,
                effectManager
        );

        new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 2.0f).play(player.getLocation());
        projectile.redirect(player.getLocation().getDirection().multiply(speed));
        projectiles.computeIfAbsent(player, p -> new ArrayList<>()).add(projectile);
        UtilItem.damageItem(player, itemStack, 1);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (event.getBukkitCause() != EntityDamageEvent.DamageCause.POISON) return;

        final Optional<Effect> poisonOpt = effectManager.getEffect(event.getLivingDamagee(), EffectTypes.POISON, getName());
        if (poisonOpt.isEmpty()) {
            return;
        }

        final Effect effect = poisonOpt.get();
        if (!(effect.getApplier().get() instanceof Player player) || !player.isValid()) {
            return;
        }

        final double damage = event.getModifiedDamage();
        UtilPlayer.slowHealth(plugin, player, damage * healPercent, (int) (((damage * healPercent) / 2) * 5), false);

        Particle.DAMAGE_INDICATOR.builder()
                .location(event.getLivingDamagee().getEyeLocation().add(0, 0.25, 0))
                .count(3)
                .offset(0.2f, 0.22f, 0.2f)
                .extra(0.1)
                .receivers(60)
                .spawn();
    }

    public void processProjectiles() {
        final Iterator<Map.Entry<Player, List<NectarOfDecayProjectile>>> iterator = projectiles.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<NectarOfDecayProjectile>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<NectarOfDecayProjectile> activeProjectiels = cur.getValue();

            if (player == null || !player.isValid()) {
                iterator.remove();
                continue;
            }

            final Iterator<NectarOfDecayProjectile> projectileIterator = activeProjectiels.iterator();
            while (projectileIterator.hasNext()) {
                final NectarOfDecayProjectile projectile = projectileIterator.next();
                if (projectile.isMarkForRemoval() || projectile.isExpired()) {
                    projectileIterator.remove();
                    continue;
                }

                projectile.tick();
            }

            if (activeProjectiels.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
