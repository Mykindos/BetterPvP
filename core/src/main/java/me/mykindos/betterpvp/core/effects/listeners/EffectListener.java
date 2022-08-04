package me.mykindos.betterpvp.core.effects.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class EffectListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public EffectListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReceiveEffect(EffectReceiveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Effect effect = event.getEffect();
        Player player = event.getPlayer();

        Optional<List<Effect>> effectsOptional = effectManager.getObject(player.getUniqueId()).or(() -> {
            List<Effect> effects = new ArrayList<>();
            effectManager.addObject(player.getUniqueId().toString(), effects);
            return Optional.of(effects);
        });
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            if (effectManager.hasEffect(player, effect.getEffectType())) {
                effectManager.removeEffect(player, effect.getEffectType());
            }
            if (effect.getEffectType() == EffectType.STRENGTH) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) ((effect.getRawLength() / 1000) * 20), effect.getLevel() - 1));
            }
            if (effect.getEffectType() == EffectType.SILENCE) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1F, 1.5F);
            }

            if (effect.getEffectType() == EffectType.VULNERABILITY) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) ((effect.getRawLength() / 1000) * 20), 0));
            }

            if (effect.getEffectType() == EffectType.LEVITATION) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) ((effect.getRawLength() / 1000) * 20), effect.getLevel()));
            }
            effects.add(effect);
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (event.getEntity() instanceof Player) {
                if (effectManager.hasEffect((Player) event.getEntity(), EffectType.NOFALL)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        effectManager.removeAllEffects(e.getEntity());
    }


    @UpdateEvent
    public void shockUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (effectManager.hasEffect(player, EffectType.SHOCK)) {
                player.playEffect(EntityEffect.HURT);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        effectManager.getObjects().forEach((key, value) -> {
            value.removeIf(effect -> {
                if (effect.hasExpired()) {
                    if (effect.getEffectType() == EffectType.VULNERABILITY) {
                        Player player = Bukkit.getPlayer(UUID.fromString(key));
                        if (player != null) {
                            UtilMessage.message(player, "Condition", "Your vulnerability has worn off!");
                        }
                    }
                }

                return false;
            });
        });

        effectManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());

    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()
                || e.getFrom().getY() != e.getTo().getY()) {
            if (effectManager.hasEffect(e.getPlayer(), EffectType.STUN)) {
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void entityDamageEvent(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectType.PROTECTION)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player damagee && e.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectType.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and has protection!");
                e.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectType.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                UtilMessage.message(damager, "Protected", "Type '/protection' to disable this permanently.");
                e.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStrengthDamage(CustomDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (e.getDamager() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.STRENGTH);
            effectOptional.ifPresent(effect -> e.setDamage(e.getDamage() + (1.5 * effect.getLevel())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent e) {
        if (e.getDamagee() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.VULNERABILITY);
            effectOptional.ifPresent(effect -> {
                if (e.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) return;
                e.setDamage((e.getDamage() * (1.0 + (effect.getLevel() * 0.25))));
            });
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoadingTextureDamage(CustomDamageEvent e) {
        if (e.getDamagee() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectType.TEXTURELOADING)) {
                e.cancel("Player is loading the server texture pack");
            }
        }

        if (e.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damager, EffectType.TEXTURELOADING)) {
                effectManager.removeEffect(damager, EffectType.TEXTURELOADING);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void resistanceReduction(CustomDamageEvent e) {
        if (e.getDamagee() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.RESISTANCE);
            effectOptional.ifPresent(effect -> e.setDamage(e.getDamage() * (1.0 - (effect.getLevel() * 20) * 0.01)));
        }
    }

    @EventHandler
    public void onImmuneToNegativity(EffectReceiveEvent e) {
        if (effectManager.hasEffect(e.getPlayer(), EffectType.IMMUNETOEFFECTS)) {
            EffectType type = e.getEffect().getEffectType();

            if (type == EffectType.SILENCE || type == EffectType.SHOCK || type == EffectType.VULNERABILITY
                    || type == EffectType.STUN || type == EffectType.FRAILTY) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReceiveImmuneToEffect(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() == EffectType.IMMUNETOEFFECTS) {
            for (PotionEffect pot : event.getPlayer().getActivePotionEffects()) {

                if (pot.getType().getName().contains("SLOW")
                        || pot.getType().getName().contains("CONFUSION")
                        || pot.getType().getName().contains("POISON")
                        || pot.getType().getName().contains("BLINDNESS")
                        || pot.getType().getName().contains("WITHER")
                        || pot.getType().getName().contains("LEVITATION")) {
                    event.getPlayer().removePotionEffect(pot.getType());
                }
            }

            effectManager.removeEffect(event.getPlayer(), EffectType.SHOCK);
            effectManager.removeEffect(event.getPlayer(), EffectType.SILENCE);
            effectManager.removeEffect(event.getPlayer(), EffectType.STUN);
            effectManager.removeEffect(event.getPlayer(), EffectType.VULNERABILITY);
            effectManager.removeEffect(event.getPlayer(), EffectType.FRAILTY);
            effectManager.removeEffect(event.getPlayer(), EffectType.LEVITATION);
        }
    }

}
