package me.mykindos.betterpvp.core.effects.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
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

    private final Core core;
    private final EffectManager effectManager;

    @Inject
    public EffectListener(Core core, EffectManager effectManager) {
        this.core = core;
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
                UtilMessage.simpleMessage(player, "Silence", "You have been silenced for <alt>%s</alt> seconds.", effect.getRawLength() / 1000);
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
    public void onDeath(PlayerDeathEvent event) {
        effectManager.removeAllEffects(event.getEntity());
    }


    @UpdateEvent
    public void shockUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (effectManager.hasEffect(player, EffectType.SHOCK)) {
                player.playHurtAnimation(0);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        effectManager.getObjects().forEach((key, value) -> {
            value.removeIf(effect -> {
                if (effect.hasExpired()) {
                    Player player = Bukkit.getPlayer(UUID.fromString(key));

                    if (player != null) {
                        if (effect.getEffectType() == EffectType.VULNERABILITY) {
                            UtilMessage.message(player, "Condition", "Your vulnerability has worn off!");
                        }

                        UtilServer.callEvent(new EffectExpireEvent(player, effect));
                    }
                    return true;
                }

                return false;
            });
        });

        effectManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()
                || event.getFrom().getY() != event.getTo().getY()) {
            if (effectManager.hasEffect(event.getPlayer(), EffectType.STUN)) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void entityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectType.PROTECTION)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damagee && event.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectType.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and has protection!");
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectType.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                UtilMessage.message(damager, "Protected", "Type '/protection' to disable this permanently.");
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStrengthDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.getDamager() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.STRENGTH);
            effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() + (1.5 * effect.getLevel())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.VULNERABILITY);
            effectOptional.ifPresent(effect -> {
                if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) return;
                event.setDamage((event.getDamage() * (1.0 + (effect.getLevel() * 0.25))));
            });
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoadingTextureDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectType.TEXTURELOADING)) {
                event.cancel("Player is loading the server texture pack");
            }
        }

        if (event.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damager, EffectType.TEXTURELOADING)) {
                effectManager.removeEffect(damager, EffectType.TEXTURELOADING);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void resistanceReduction(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.RESISTANCE);
            effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() * (1.0 - (effect.getLevel() * 20) * 0.01)));
        }
    }

    @EventHandler
    public void onImmuneToNegativity(EffectReceiveEvent event) {
        if (effectManager.hasEffect(event.getPlayer(), EffectType.IMMUNETOEFFECTS)) {
            EffectType type = event.getEffect().getEffectType();

            if (type == EffectType.SILENCE || type == EffectType.SHOCK || type == EffectType.VULNERABILITY
                    || type == EffectType.STUN || type == EffectType.FRAILTY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInvisibilityGiven(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() == EffectType.INVISIBILITY) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.hidePlayer(core, event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInvisibilityRemoved(EffectExpireEvent event) {
        if (event.getEffect().getEffectType() == EffectType.INVISIBILITY) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(core, event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReceiveImmuneToEffect(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() == EffectType.IMMUNETOEFFECTS) {
            removeNegativeEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onEventClear(EffectClearEvent event) {
        removeNegativeEffects(event.getPlayer());
    }

    private void removeNegativeEffects(Player player) {
        for (PotionEffect pot : player.getActivePotionEffects()) {

            if (pot.getType().getName().contains("SLOW")
                    || pot.getType().getName().contains("CONFUSION")
                    || pot.getType().getName().contains("POISON")
                    || pot.getType().getName().contains("BLINDNESS")
                    || pot.getType().getName().contains("WITHER")
                    || pot.getType().getName().contains("LEVITATION")) {
                player.removePotionEffect(pot.getType());
            }
        }

        effectManager.removeEffect(player, EffectType.SHOCK);
        effectManager.removeEffect(player, EffectType.SILENCE);
        effectManager.removeEffect(player, EffectType.STUN);
        effectManager.removeEffect(player, EffectType.VULNERABILITY);
        effectManager.removeEffect(player, EffectType.FRAILTY);
        effectManager.removeEffect(player, EffectType.LEVITATION);
        player.setFireTicks(0);
    }
}
