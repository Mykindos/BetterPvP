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
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class EffectListener implements Listener {

    private final Core core;
    private final EffectManager effectManager;
    private final Map<UUID, Long> lastBleedTimes = new HashMap<>();
    private final Map<UUID, Long> bleedEntities = new HashMap<>();


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
        LivingEntity target = event.getTarget();

        Optional<List<Effect>> effectsOptional = effectManager.getObject(target.getUniqueId()).or(() -> {
            List<Effect> effects = new ArrayList<>();
            effectManager.addObject(target.getUniqueId().toString(), effects);
            return Optional.of(effects);
        });
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            if (effectManager.hasEffect(target, effect.getEffectType())) {
                effectManager.removeEffect(target, effect.getEffectType());
            }
            if (effect.getEffectType() == EffectType.STRENGTH) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) ((effect.getRawLength() / 1000d) * 20), effect.getLevel() - 1));
            } else if (effect.getEffectType() == EffectType.SILENCE) {
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BAT_AMBIENT, 2.0F, 1.0F);
                UtilMessage.simpleMessage(target, "Silence", "You have been silenced for <alt>%s</alt> seconds.", effect.getRawLength() / 1000d);
            } else if (effect.getEffectType() == EffectType.VULNERABILITY) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) ((effect.getRawLength() / 1000d) * 20), 0));
            } else if (effect.getEffectType() == EffectType.LEVITATION) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) ((effect.getRawLength() / 1000d) * 20), effect.getLevel()));
            } else if (effect.getEffectType() == EffectType.POISON) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) ((effect.getRawLength() / 1000d) * 20), effect.getLevel()));
            } else if (effect.getEffectType() == EffectType.NO_JUMP) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) ((effect.getRawLength() / 1000d) * 20), 128, false, false, false));
            } else if (effect.getEffectType() == EffectType.BLEED) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, (int) ((effect.getRawLength() / 1000d) * 20), 0, false, false));
                bleedEntities.put(target.getUniqueId(), System.currentTimeMillis());
            }
            effects.add(effect);
        }
    }

    @UpdateEvent
    public void onBleed() {
        bleedEntities.keySet().removeIf(uuid -> {
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if (entity == null) return true;
            if (!entity.hasPotionEffect(PotionEffectType.BAD_OMEN)) return true;

            long currentTime = System.currentTimeMillis();
            long lastBleedTime = lastBleedTimes.getOrDefault(uuid, 0L);
            int marginOfError = 20;

            if (currentTime - lastBleedTime >= 1000 - marginOfError) {
                // Apply damage to any LivingEntity (including players)
                var cde = new CustomDamageEvent(entity, null, null, EntityDamageEvent.DamageCause.CUSTOM, 2.0, false, "Bleed");
                cde.setIgnoreArmour(true);
                UtilDamage.doCustomDamage(cde);

                entity.getWorld().playSound(entity.getLocation().add(0, 1, 0), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 2f);
                entity.getWorld().playEffect(entity.getLocation().add(0, 1, 0), org.bukkit.Effect.STEP_SOUND, Material.REDSTONE_BLOCK);

                lastBleedTimes.put(uuid, currentTime);
            }
            return false;
        });
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
                player.playSound(net.kyori.adventure.sound.Sound.sound(Sound.ENTITY_PLAYER_HURT.key(),
                        net.kyori.adventure.sound.Sound.Source.PLAYER,
                        1f,
                        1f), player);
                player.playHurtAnimation(270);
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


    @EventHandler(priority = EventPriority.LOW)
    public void onStrengthDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (event.getDamager() instanceof Player player) {
            Optional<Effect> effectOptional = effectManager.getEffect(player, EffectType.STRENGTH);
            effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() + (1.5 * effect.getLevel())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if(event.isCancelled()) return;
        Optional<Effect> effectOptional = effectManager.getEffect(event.getDamagee(), EffectType.VULNERABILITY);
        effectOptional.ifPresent(effect -> {
            if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) return;
            event.setDamage((event.getDamage() * (1.0 + (effect.getLevel() * 0.25))));
        });
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
    public void poisonDamageMultiplier(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.POISON) return;
        Optional<Effect> effectOptional = effectManager.getEffect(event.getDamagee(), EffectType.POISON);
        effectOptional.ifPresent(effect -> event.setDamage(event.getDamage() * effect.getLevel()));
    }

    @EventHandler
    public void onImmuneToNegativity(EffectReceiveEvent event) {
        if (effectManager.hasEffect(event.getTarget(), EffectType.IMMUNETOEFFECTS)) {
            EffectType type = event.getEffect().getEffectType();

            if (type.isNegative()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInvisibilityGiven(EffectReceiveEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (event.getEffect().getEffectType() == EffectType.INVISIBILITY) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.hidePlayer(core, player);
                }
            }
        }
    }

    @EventHandler
    public void onEffectRemove(EffectExpireEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (event.getEffect().getEffectType() == EffectType.INVISIBILITY) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.showPlayer(core, player);
                }
            } else if (event.getEffect().getEffectType() == EffectType.NO_JUMP) {
                player.removePotionEffect(PotionEffectType.JUMP);
            }
        }
    }

    @EventHandler
    public void onRespawnInvisibility(PlayerRespawnEvent event) {
        UtilServer.runTaskLater(core, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.canSee(event.getPlayer())) {
                    player.showPlayer(core, event.getPlayer());
                }
            }
        }, 2);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReceiveImmuneToEffect(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() == EffectType.IMMUNETOEFFECTS) {
            removeNegativeEffects(event.getTarget());
        }
    }

    @EventHandler
    public void onEventClear(EffectClearEvent event) {
        removeNegativeEffects(event.getPlayer());
    }

    private void removeNegativeEffects(LivingEntity target) {
        for (PotionEffect pot : target.getActivePotionEffects()) {

            if (pot.getType().getName().contains("SLOW")
                    || pot.getType().getName().contains("CONFUSION")
                    || pot.getType().getName().contains("POISON")
                    || pot.getType().getName().contains("BLINDNESS")
                    || pot.getType().getName().contains("WITHER")
                    || pot.getType().getName().contains("LEVITATION")) {
                target.removePotionEffect(pot.getType());
            }
        }

        for (EffectType value : EffectType.values()) {
            if (!value.isNegative()) continue;
            effectManager.removeEffect(target, value);
        }

        target.setFireTicks(0);
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        if (effectManager.hasEffect(event.getPlayer(), EffectType.NO_SPRINT)) {
            event.getPlayer().setSprinting(false);
        }
    }
}
