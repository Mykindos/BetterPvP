package me.mykindos.betterpvp.core.block;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for smart blocks in the game.
 */
@Singleton
public class SmartBlockRegistry {

    private final Map<String, SmartBlock> blocks = new HashMap<>();

    @Inject
    private SmartBlockRegistry() {
    }

    /**
     * Registers a smart block with the given key.
     *
     * @param block the smart block to register
     */
    public void registerBlock(SmartBlock block) {
        String key = block.getKey();
        if (blocks.containsKey(key)) {
            throw new IllegalArgumentException("Smart block with key '" + key + "' is already registered.");
        }
        blocks.put(key, block);
    }

    /**
     * Retrieves a smart block by its key.
     *
     * @param key the key of the smart block
     * @return the smart block, or null if not found
     */
    public SmartBlock getBlock(String key) {
        return blocks.get(key);
    }

    /**
     * Checks if a smart block is registered with the given key.
     *
     * @param key the key of the smart block
     * @return true if the smart block is registered, false otherwise
     */
    public boolean isBlockRegistered(String key) {
        return blocks.containsKey(key);
    }

    /**
     * Retrieves all registered smart blocks.
     *
     * @return a map of all registered smart blocks
     */
    public Map<String, SmartBlock> getAllBlocks() {
        return Collections.unmodifiableMap(blocks);
    }

}
