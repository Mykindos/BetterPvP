package me.mykindos.betterpvp.game.guice;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Guice scope for game-related components.
 * Objects in this scope exist only during the IN_GAME state.
 */
@Singleton
public class GameScope implements Scope {

    private final Map<Key<?>, Object> instances = new HashMap<>();
    private boolean active = false;
    private final ThreadLocal<Boolean> creating = ThreadLocal.withInitial(() -> false);

    @Override
    public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
        return () -> {
            if (!active) {
                throw new IllegalStateException("Cannot access " + key + " outside of game scope");
            }

            @SuppressWarnings("unchecked")
            T instance = (T) instances.get(key);

            if (instance == null) {
                // Prevent recursive creation of the same key
                if (creating.get()) {
                    // Return from unscoped provider during recursive calls
                    return unscoped.get();
                }

                try {
                    creating.set(true);
                    instance = unscoped.get();
                    instances.put(key, instance);
                } finally {
                    creating.set(false);
                }
            }

            return instance;
        };
    }

    /**
     * Activates the game scope, allowing creation of scoped objects
     */
    public void enter() {
        active = true;
    }

    /**
     * Deactivates the game scope, preventing creation of new objects
     * and clearing existing instances
     */
    public void exit() {
        active = false;
        instances.clear();
    }

    /**
     * @return Whether the game scope is currently active
     */
    public boolean isActive() {
        return active;
    }
}
