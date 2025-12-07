package me.mykindos.betterpvp.core.combat.cause;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing all damage causes in the system
 */
@Singleton
@CustomLog
public class DamageCauseRegistry {
    
    private final Map<String, DamageCause> causes = new ConcurrentHashMap<>();
    private final Map<EntityDamageEvent.DamageCause, VanillaDamageCause> vanillaCauses = new ConcurrentHashMap<>();
    
    public DamageCauseRegistry() {
        initializeVanillaCauses();
        initializeEnvironmentalCauses();
        
        log.info("Damage cause registry initialized with {} causes", causes.size()).submit();
    }
    
    /**
     * Registers a custom damage cause
     * @param cause the damage cause to register
     */
    public void register(DamageCause cause) {
        if (causes.containsKey(cause.getName())) {
            log.warn("Overriding existing damage cause: {}", cause.getName()).submit();
        }
        
        causes.put(cause.getName(), cause);
        log.info("Registered damage cause: {} ({})", cause.getName(), cause.getDisplayName()).submit();
    }
    
    /**
     * Unregisters a damage cause
     * @param causeName the name of the cause to unregister
     * @return true if the cause was removed
     */
    public boolean unregister(String causeName) {
        DamageCause removed = causes.remove(causeName);
        if (removed != null) {
            log.info("Unregistered damage cause: {}", causeName).submit();
            return true;
        }
        return false;
    }
    
    /**
     * Gets a damage cause by name
     * @param name the name of the damage cause
     * @return the damage cause or null if not found
     */
    @Nullable
    public DamageCause get(String name) {
        return causes.get(name);
    }
    
    /**
     * Gets or creates a wrapper for a vanilla damage cause
     * @param vanillaCause the vanilla damage cause
     * @return the wrapped damage cause
     */
    public VanillaDamageCause fromVanilla(EntityDamageEvent.DamageCause vanillaCause) {
        return vanillaCauses.computeIfAbsent(vanillaCause, VanillaDamageCause::new);
    }
    
    /**
     * Checks if a damage cause is registered
     * @param name the name of the damage cause
     * @return true if the cause is registered
     */
    public boolean isRegistered(String name) {
        return causes.containsKey(name);
    }
    
    /**
     * Gets all registered damage causes
     * @return collection of all damage causes
     */
    public Collection<DamageCause> getAllCauses() {
        return causes.values();
    }
    
    /**
     * Gets all true damage causes
     * @return collection of true damage causes
     */
    public Collection<DamageCause> getTrueDamageCauses() {
        return causes.values().stream()
                .filter(DamageCause::isTrueDamage)
                .toList();
    }
    
    /**
     * Gets the total number of registered causes
     * @return the number of registered causes
     */
    public int getRegisteredCount() {
        return causes.size();
    }
    
    /**
     * Clears all registered causes (except vanilla causes)
     */
    public void clear() {
        causes.clear();
        initializeVanillaCauses();
        initializeEnvironmentalCauses();
        
        log.info("Cleared all custom damage causes, keeping vanilla causes").submit();
    }
    
    /**
     * Initializes all vanilla damage cause wrappers
     */
    private void initializeVanillaCauses() {
        for (EntityDamageEvent.DamageCause vanillaCause : EntityDamageEvent.DamageCause.values()) {
            VanillaDamageCause wrappedCause = new VanillaDamageCause(vanillaCause);
            causes.put(wrappedCause.getName(), wrappedCause);
            vanillaCauses.put(vanillaCause, wrappedCause);
        }
        
        log.debug("Initialized {} vanilla damage causes", EntityDamageEvent.DamageCause.values().length).submit();
    }
    
    /**
     * Initializes common environmental damage causes
     */
    private void initializeEnvironmentalCauses() {
//        register(EnvironmentalDamageCause.TEST);
        
        log.debug("Initialized {} environmental damage causes", 5).submit();
    }
}
