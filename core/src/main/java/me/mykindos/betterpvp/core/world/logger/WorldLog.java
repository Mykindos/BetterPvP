package me.mykindos.betterpvp.core.world.logger;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.HashMap;


@Data
@Builder
public class WorldLog {

    private static final Gson gson = new Gson();

    private String world;
    private int blockX;
    private int blockY;
    private int blockZ;
    private BlockData blockData;
    private String action;
    private String material;
    private ItemStack itemStack;
    private HashMap<String, String> metadata;
    private Instant time;

    public static class WorldLogBuilder {
        public WorldLogBuilder block(Block block) {
            this.world = block.getWorld().getName();
            this.blockX = block.getX();
            this.blockY = block.getY();
            this.blockZ = block.getZ();
            this.material = block.getType().name();
            this.blockData = block.getBlockData();
            return this;
        }

        public WorldLogBuilder location(Location loc) {
            this.world = loc.getWorld().getName();
            this.blockX = loc.getBlockX();
            this.blockY = loc.getBlockY();
            this.blockZ = loc.getBlockZ();
            return this;
        }

        public WorldLogBuilder action(WorldLogAction worldLogAction) {
            this.action = worldLogAction.name();
            return this;
        }

        public WorldLogBuilder material(Material material) {
            this.material = material.name();
            return this;
        }

        public WorldLogBuilder material(String material) {
            this.material = material;
            return this;
        }

        public WorldLogBuilder metadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public WorldLogBuilder metadata(HashMap<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public WorldLogBuilder playerMetadata(Player player) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put("PlayerName", player.getName());
            this.metadata.put("PlayerUUID", player.getUniqueId().toString());
            return this;
        }

        public WorldLogBuilder entityMetadata(Entity entity) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }

            if(entity instanceof Player player) {
                this.metadata.put("PlayerName", player.getName());
                this.metadata.put("PlayerUUID", player.getUniqueId().toString());
            } else {
                this.metadata.put("EntityName", entity.getName());
                this.metadata.put("EntityType", entity.getType().name());
            }

            return this;
        }

        public WorldLogBuilder itemMetadata(ItemStack itemStack) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }

            this.itemStack = itemStack.clone();

            this.metadata.put("ItemMaterial", itemStack.getType().name());
            this.metadata.put("ItemAmount", String.valueOf(itemStack.getAmount()));

            ItemMeta itemMeta = itemStack.getItemMeta();
            if(itemMeta != null) {
                Component displayName = itemMeta.displayName();
                if(displayName != null) {
                    this.metadata.put("ItemName", PlainTextComponentSerializer.plainText().serialize(displayName));
                }
                if(itemMeta.hasCustomModelData()) {
                    this.metadata.put("CustomModelData", String.valueOf(itemMeta.getCustomModelData()));
                }

                CraftPersistentDataContainer persistentData = (CraftPersistentDataContainer) itemMeta.getPersistentDataContainer();
                if (persistentData.getKeys().isEmpty()) return this;

                persistentData.getRaw().forEach((key, value) -> {
                    this.metadata.put(key.replace("\"", ""),value.toString().replace("\"", ""));
                });
            }

            return this;
        }
    }

}
