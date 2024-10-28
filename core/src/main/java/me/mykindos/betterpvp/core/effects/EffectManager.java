package me.mykindos.betterpvp.core.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class EffectManager extends Manager<List<Effect>> {

    public void addEffect(LivingEntity target, EffectType type, long length) {
        addEffect(target, type, type.defaultAmplifier(), length);
    }

    public void addEffect(LivingEntity target, EffectType type, int level, long length) {
        addEffect(target, null, type, level, length);
    }

    public void addEffect(LivingEntity target, EffectType type, String name, int level, long length) {
        addEffect(target, null, type, name, level, length);
    }

    public void addEffect(LivingEntity target, EffectType type, String name, int level, long length, boolean overwrite) {
        addEffect(target, null, type, name, level, length, overwrite);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, long length) {
        addEffect(target, applier, type, type.defaultAmplifier(), length);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, long length, Predicate<LivingEntity> removalPredicate) {
        addEffect(target, applier, type, "", type.defaultAmplifier(), length, removalPredicate);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, int level, long length) {
        addEffect(target, applier, type, "", level, length);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length) {
        addEffect(target, applier, type, name, level, length, false);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, Predicate<LivingEntity> removalPredicate) {
        addEffect(target, applier, type, name, level, length, false, false, removalPredicate);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, boolean overwrite) {
        addEffect(target, applier, type, name, level, length, overwrite, false);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, boolean overwrite, boolean permanent) {
        addEffect(target, applier, type, name, level, length, overwrite, permanent, null);
    }

    public void addEffect(LivingEntity target, LivingEntity applier, EffectType type, String name, int level, long length, boolean overwrite, boolean permanent, Predicate<LivingEntity> removalPredicate) {
        Effect effect = new Effect(target.getUniqueId().toString(), applier, type, name, level, length, permanent, removalPredicate);
        addEffect(target, effect, overwrite);
    }

    public void addEffect(LivingEntity target, Effect effect) {
        addEffect(target, effect, false);
    }

    public void addEffect(LivingEntity target, Effect effect, boolean overwrite) {
        EffectReceiveEvent event = UtilServer.callEvent(new EffectReceiveEvent(target, effect));
        EffectType type = effect.getEffectType();
        if (!event.isCancelled()) {
            if (target.isDead()) {
                return;
            }

            if (!type.canStack()) {
                if (hasEffect(target, effect.getEffectType())) {
                    removeEffect(target, effect.getEffectType());
                }
            }

            if (overwrite) {
                Effect overwriteEffect = getEffect(target, type, effect.getName()).orElse(null);
                if (overwriteEffect != null) {
                    overwriteEffect.setAmplifier(effect.getAmplifier());
                    overwriteEffect.setLength(effect.getLength() - System.currentTimeMillis());

                    if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                        vanillaEffectType.checkActive(target, effect);
                    }

                    return;
                }
            }

            Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId()).or(() -> {
                List<Effect> effects = new ArrayList<>();
                addObject(target.getUniqueId().toString(), effects);
                return Optional.of(effects);
            });

            if (effectsOptional.isPresent()) {
                List<Effect> effects = effectsOptional.get();
                effects.add(effect);
                effect.getEffectType().onReceive(target, effect);
            }
        }
    }


    public Optional<Effect> getEffect(LivingEntity target, EffectType type) {
        if (target == null) {
            return Optional.empty();
        }
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                            && effect.getEffectType() == type)
                    .max(Comparator.comparingInt(Effect::getAmplifier));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Effect> getEffect(LivingEntity target, EffectType type, String name) {

        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                            && effect.getEffectType() == type && effect.getName().equalsIgnoreCase(name))
                    .max(Comparator.comparingInt(Effect::getAmplifier));
        } else {
            return Optional.empty();
        }


    }

    public boolean hasEffect(LivingEntity target, EffectType type) {
        return getEffect(target, type).isPresent();
    }

    public boolean hasEffect(LivingEntity target, EffectType type, String name) {
        return getEffect(target, type, name).isPresent();
    }

    public List<Effect> getEffects(LivingEntity target, Class<? extends EffectType> typeClass) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get();
            return effects.stream().filter(effect -> typeClass.isInstance(effect.getEffectType())).toList();
        } else {
            return new ArrayList<>();
        }
    }

    public Set<LivingEntity> getAllEntitiesWithEffects() {
        return objects.keySet().stream()
                .map(uuidString -> {
                    UUID uuid = UUID.fromString(uuidString);
                    return Bukkit.getEntity(uuid);
                })
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toSet());
    }

    public void removeEffect(LivingEntity target, EffectType type) {
        removeEffect(target, type, true);
    }

    public void removeEffect(LivingEntity target, EffectType type, boolean notify) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> {
                if (effect.getEffectType() == type) {

                    if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                        vanillaEffectType.onExpire(target, effect, notify);
                    }

                    UtilServer.callEvent(new EffectExpireEvent(target, effect, notify));
                    return true;
                }
                return false;
            });
        });

    }

    public void removeEffect(LivingEntity target, EffectType type, String name) {
        removeEffect(target, type, name, true);
    }

    public void removeEffect(LivingEntity target, EffectType type, String name, boolean notify) {
        Optional<List<Effect>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            effects.removeIf(effect -> {
                if (effect.getEffectType() == type && effect.getName().equalsIgnoreCase(name)) {

                    UtilServer.callEvent(new EffectExpireEvent(target, effect, notify));
                    return true;
                }
                return false;
            });
        });

    }


    public void removeAllEffects(LivingEntity target) {
        objects.getOrDefault(target.getUniqueId().toString(), new ArrayList<>()).removeIf(effect -> {

            if(effect.getEffectType() == EffectTypes.PROTECTION) {
                return false;
            }

            if (!effect.isPermanent()) {
                UtilServer.callEvent(new EffectExpireEvent(target, effect, true));
                return true;
            }


            return false;
        });

    }

    public void removeNegativeEffects(LivingEntity target) {
        for (PotionEffect pot : target.getActivePotionEffects()) {
            if (UtilEffect.isNegativePotionEffect(pot)) {
                target.removePotionEffect(pot.getType());
            }
        }

        Optional<List<Effect>> effectOptional = getObject(target.getUniqueId().toString());
        if (effectOptional.isPresent()) {
            List<Effect> effects = effectOptional.get();
            effects.removeIf(effect -> {
                if (!effect.getEffectType().isNegative()) return false;
                if (effect.getEffectType().mustBeManuallyRemoved()) return false;
                if (effect.getApplier() != null && effect.getApplier().equals(target)) return false;

                if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                    vanillaEffectType.onExpire(target, effect, true);
                }

                UtilServer.callEvent(new EffectExpireEvent(target, effect, true));

                return true;
            });
        }


        target.setFireTicks(0);
    }

    public long getDuration(LivingEntity target, EffectType type) {
        return getObject(target.getUniqueId())
                .map(effects -> effects.stream()
                        .filter(effect -> effect.getEffectType() == type)
                .sorted(Comparator.comparingLong(Effect::getRemainingDuration).reversed())
                        .map(Effect::getRemainingDuration)
                        .findFirst()
                        .orElse(0L))
                .orElse(0L);
    }

}
