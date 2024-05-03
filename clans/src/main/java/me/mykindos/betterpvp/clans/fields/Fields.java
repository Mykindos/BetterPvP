package me.mykindos.betterpvp.clans.fields;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.clans.fields.repository.FieldsBlockEntry;
import me.mykindos.betterpvp.clans.fields.repository.FieldsRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Stores all data for the Fields zone.
 */
@Singleton
@CustomLog
public class Fields {

    private final SetMultimap<FieldsInteractable, FieldsBlock> blocks = HashMultimap.create();

    private final @NotNull FieldsRepository repository;
    private final Map<Integer, Double> playerCountSpeeds = new HashMap<>();
    private final Clans clans;

    @Inject
    public Fields(@NotNull final FieldsRepository repository, @NotNull Clans clans) {
        this.repository = repository;
        this.clans = clans;
        populate();
    }

    public Set<FieldsInteractable> getBlockTypes() {
        return repository.getTypes();
    }

    private void populate() {
        repository.getAll().forEach(ore -> blocks.put(ore.getType(), new FieldsBlock(ore.getWorld(),
                ore.getX(),
                ore.getY(),
                ore.getZ())));
        log.info("Loaded {} fields blocks", blocks.size()).submit();
        reload(clans);
    }

    public void reload(@NotNull Clans clans) {
        for (FieldsInteractable type : getBlockTypes()) {
            final String name = type.getName().toLowerCase().replace(" ", "");
            final Double delay = clans.getConfig().getOrSaveObject("fields.blocks." + name + ".respawn", 60.0, Double.class);
            Objects.requireNonNull(delay, "Delay must be present");
            type.setRespawnDelay(delay);

            type.loadConfig(clans.getConfig());
        }

        playerCountSpeeds.clear();
        final Set<String> keys = clans.getConfig().getConfigurationSection("fields.playerCountSpeeds").getKeys(false);
        for (String key : keys) {
            final int playerCount = Integer.parseInt(key);
            Preconditions.checkArgument(playerCount >= 0, "Player count speed buff must be greater than or equal to 0");
            final double speed = clans.getConfig().getDouble("fields.playerCountSpeeds." + key);
            playerCountSpeeds.put(playerCount, speed);
        }

    }

    /**
     * Adds a block to the Fields zone.
     * @param type the type of block
     * @param block the block
     */
    public void addBlock(@NotNull FieldsInteractable type, @NotNull Block block) {
        blocks.put(type, new FieldsBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        repository.save(new FieldsBlockEntry(type, block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    /**
     * Adds multiple blocks to the Fields zone.
     * @param blocksIn the blocks. The key is the block, the value is the type of block. The type can be null if it
     *             was deleted.
     */
    protected void addBlocks(@NotNull Map<@NotNull Block, @Nullable FieldsInteractable> blocksIn) {
        final List<FieldsBlockEntry> entries = blocksIn.entrySet().stream()
                .map(o -> new FieldsBlockEntry(o.getValue(), o.getKey().getWorld().getName(), o.getKey().getX(), o.getKey().getY(), o.getKey().getZ()))
                .toList();

        repository.saveBatch(entries);
        blocksIn.forEach((block, type) -> {
            final FieldsBlock ore = new FieldsBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (type == null) {
                blocks.values().remove(ore);
            } else {
                blocks.put(type, ore);
            }
        });
    }

    /**
     * Deletes a block from the Fields zone.
     * @param block the block.
     */
    public void deleteBlock(Block block) {
        blocks.values().remove(new FieldsBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        repository.delete(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * Gets the fields block from a specific block in the Fields zone.
     * @param block the block
     * @return The block, if present
     */
    public Optional<Pair<FieldsInteractable, FieldsBlock>> getBlock(Block block) {
        return blocks.entries().stream()
                .filter(e -> e.getValue().equals(new FieldsBlock(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .findFirst();
    }

    /**
     * Gets all blocks from a specific type in the Fields zone.
     * @param type the type of block
     * @return An unmodifiable collection of blocks
     */
    public Collection<FieldsBlock> getBlocks(FieldsInteractable type) {
        return Collections.unmodifiableSet(blocks.get(type));
    }

    /**
     * Gets all blocks in the Fields zone.
     *
     * @return An unmodifiable collection of blocks
     */
    public SetMultimap<FieldsInteractable, FieldsBlock> getBlocks() {
        return blocks;
    }

    public Optional<FieldsInteractable> getTypeFromBlock(Block block) {
        return getBlockTypes().stream()
                .filter(type -> type.matches(block))
                .findFirst();
    }

    /**
     * Gets the respawn delay speed buff for ores. This depends on player count and other factors. Base is 1.
     * @return The respawn delay speed
     */
    public double getSpeedBuff() {
        final int players = Bukkit.getOnlinePlayers().size();
        return playerCountSpeeds.keySet().stream()
                .filter(required -> players >= required) // Skip if the player count is less than the required
                .max(Comparator.comparingInt(a -> a)) // Get the highest required player count we can reach
                .map(playerCountSpeeds::get) // Get the modifier for the one we reached
                .orElse(1.0); // If there's no modifier, default to 1.0
    }

}
