package me.mykindos.betterpvp.core.effects;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class EffectManager extends Manager<ConcurrentHashMap<EffectType, List<Effect>>> {

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

            Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId()).or(() -> {
                ConcurrentHashMap<EffectType, List<Effect>> effects = new ConcurrentHashMap<>();
                addObject(target.getUniqueId().toString(), effects);
                return Optional.of(effects);
            });

            if (effectsOptional.isPresent()) {
                ConcurrentHashMap<EffectType, List<Effect>> effects = effectsOptional.get();
                List<Effect> effectList = effects.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()));
                effectList.add(effect);
                effect.getEffectType().onReceive(target, effect);
            }
        }
    }


    public Optional<Effect> getEffect(@Nullable LivingEntity target, EffectType type) {
        if (target == null) {
            return Optional.empty();
        }
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            ConcurrentHashMap<EffectType, List<Effect>> effects = effectsOptional.get();
            List<Effect> effectList = effects.get(type);
            if (effectList != null) {
                return effectList.stream().filter(effect -> effect.getUuid().equalsIgnoreCase(target.getUniqueId().toString())
                                && effect.getEffectType() == type)
                        .max(Comparator.comparingInt(Effect::getAmplifier));

            }
        }

        return Optional.empty();
    }

    public Optional<Effect> getEffect(@Nullable LivingEntity target, EffectType type, String name) {
        if (target == null) return Optional.empty();
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            ConcurrentHashMap<EffectType, List<Effect>> effects = effectsOptional.get();
            List<Effect> effectList = effects.get(type);
            if (effectList != null) {
                return effectList.stream().filter(effect -> effect.getEffectType() == type && effect.getName().equalsIgnoreCase(name))
                        .max(Comparator.comparingInt(Effect::getAmplifier));
            }

        }

        return Optional.empty();

    }

    public boolean hasEffect(@Nullable LivingEntity target, EffectType type) {
        return getEffect(target, type).isPresent();
    }

    public boolean hasEffect(@Nullable LivingEntity target, EffectType type, String name) {
        return getEffect(target, type, name).isPresent();
    }

    public List<Effect> getEffects(@Nullable LivingEntity target, Class<? extends EffectType> typeClass) {
        if (target == null) return List.of();
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId().toString());
        if (effectsOptional.isPresent()) {
            ConcurrentHashMap<EffectType, List<Effect>> effects = effectsOptional.get();
            synchronized (effects) {
                return effects.values().stream()
                        .flatMap(List::stream)
                        .filter(effect -> typeClass.isInstance(effect.getEffectType()))
                        .collect(Collectors.toList());
            }
        } else {
            return Collections.synchronizedList(new ArrayList<>());
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
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            List<Effect> effectList = effects.get(type);
            if (effectList == null) return;
            effectList.removeIf(effect -> {

                if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                    vanillaEffectType.onExpire(target, effect, notify);
                }

                UtilServer.callEvent(new EffectExpireEvent(target, effect, notify));
                return true;

            });
        });

    }

    public void removeEffect(LivingEntity target, EffectType type, String name) {
        removeEffect(target, type, name, true);
    }

    public void removeEffect(LivingEntity target, EffectType type, String name, boolean notify) {
        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId().toString());
        effectsOptional.ifPresent(effects -> {
            List<Effect> effectList = effects.get(type);
            if (effectList == null) return;
            effectList.removeIf(effect -> {
                if (effect.getName().equalsIgnoreCase(name)) {
                    UtilServer.callEvent(new EffectExpireEvent(target, effect, notify));
                    return true;
                }
                return false;
            });
        });

    }


    public void removeAllEffects(LivingEntity target) {
        ConcurrentHashMap<EffectType, List<Effect>> effects = objects.getOrDefault(target.getUniqueId().toString(), new ConcurrentHashMap<>());
        effects.values().removeIf(effectList -> {

            effectList.removeIf(effect -> {
                if (effect.getEffectType() == EffectTypes.PROTECTION) {
                    return false;
                }

                if (!effect.isPermanent()) {
                    UtilServer.callEvent(new EffectExpireEvent(target, effect, true));
                    return true;
                }

                return false;
            });

            return effectList.isEmpty();
        });

    }

    public void removeNegativeEffects(LivingEntity target) {
        for (PotionEffect pot : target.getActivePotionEffects()) {
            if (UtilEffect.isNegativePotionEffect(pot)) {
                target.removePotionEffect(pot.getType());
            }
        }

        ConcurrentHashMap<EffectType, List<Effect>> effects = objects.getOrDefault(target.getUniqueId().toString(), new ConcurrentHashMap<>());
        effects.values().removeIf(effectList -> {

            effectList.removeIf(effect -> {
                if (!effect.getEffectType().isNegative()) return false;
                if (effect.getEffectType().mustBeManuallyRemoved()) return false;
                if (effect.getApplier() != null && effect.getApplier().equals(target)) return false;

                if (effect.getEffectType() instanceof VanillaEffectType vanillaEffectType) {
                    vanillaEffectType.onExpire(target, effect, true);
                }

                UtilServer.callEvent(new EffectExpireEvent(target, effect, true));

                return true;
            });

            return effectList.isEmpty();
        });


        target.setFireTicks(0);
    }

    public long getDuration(LivingEntity target, EffectType type) {

        Optional<ConcurrentHashMap<EffectType, List<Effect>>> effectsOptional = getObject(target.getUniqueId());
        if(effectsOptional.isPresent()) {
            List<Effect> effects = effectsOptional.get().get(type);
            if (effects != null) {
                return effects.stream()
                        .map(Effect::getRemainingDuration)
                        .max(Long::compareTo)
                        .orElse(0L);
            }
        }

        return 0L;
    }

}
