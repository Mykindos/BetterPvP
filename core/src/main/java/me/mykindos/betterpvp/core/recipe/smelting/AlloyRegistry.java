package me.mykindos.betterpvp.core.recipe.smelting;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for managing alloy types in the smelting system.
 * Handles registration, validation, and provides utilities for alloy lookup.
 */
@CustomLog
@Singleton
public class AlloyRegistry {
    
    private final Map<String, Alloy> alloysByName = new HashMap<>();
    
    /**
     * Registers a new alloy type.
     * Validates that no duplicate alloy exists with the same name.
     * 
     * @param alloy The alloy to register
     * @throws IllegalArgumentException if an alloy with the same name already exists
     */
    public void registerAlloy(@NotNull Alloy alloy) {
        String alloyName = alloy.getName();
        
        if (alloysByName.containsKey(alloyName)) {
            throw new IllegalArgumentException(
                "Duplicate alloy detected! Alloy with name '" + alloyName + "' already exists."
            );
        }
        
        alloysByName.put(alloyName, alloy);
        
        log.info("Registered alloy: {} (min temp: {}°C)", 
                alloyName, alloy.getMinimumTemperature()).submit();
    }
    
    /**
     * Finds an alloy by its name.
     * 
     * @param name The name of the alloy to find
     * @return The alloy if found, empty otherwise
     */
    public @NotNull Optional<Alloy> findByName(@NotNull String name) {
        return Optional.ofNullable(alloysByName.get(name));
    }
    
    /**
     * Gets an alloy by its name, throwing an exception if not found.
     * 
     * @param name The name of the alloy to get
     * @return The alloy
     * @throws IllegalArgumentException if the alloy is not found
     */
    public @NotNull Alloy getByName(@NotNull String name) {
        return findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown alloy: " + name));
    }
    
    /**
     * Checks if an alloy with the given name is registered.
     * 
     * @param name The name to check
     * @return true if the alloy exists, false otherwise
     */
    public boolean isRegistered(@NotNull String name) {
        return alloysByName.containsKey(name);
    }
    
    /**
     * Gets all registered alloy names.
     * 
     * @return An unmodifiable set of all alloy names
     */
    public @NotNull Set<String> getAlloyNames() {
        return Collections.unmodifiableSet(alloysByName.keySet());
    }
    
    /**
     * Gets all registered alloys.
     * 
     * @return An unmodifiable collection of all alloys
     */
    public @NotNull Set<Alloy> getAlloys() {
        return Collections.unmodifiableSet(Set.copyOf(alloysByName.values()));
    }
    
    /**
     * Gets the count of registered alloys.
     * 
     * @return The number of registered alloys
     */
    public int getRegisteredCount() {
        return alloysByName.size();
    }
    
    /**
     * Clears all registered alloys.
     * This method is primarily for testing purposes.
     */
    public void clear() {
        alloysByName.clear();
        log.info("Cleared all registered alloys").submit();
    }
} 