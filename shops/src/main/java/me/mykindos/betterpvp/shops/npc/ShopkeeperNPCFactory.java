package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.scene.npc.NPCFactory;
import me.mykindos.betterpvp.shops.npc.impl.AuctionHouseNPC;
import me.mykindos.betterpvp.shops.npc.impl.Shopkeeper1NPC;
import me.mykindos.betterpvp.shops.npc.impl.Shopkeeper2NPC;
import me.mykindos.betterpvp.shops.npc.impl.Shopkeeper3NPC;
import me.mykindos.betterpvp.shops.npc.impl.ShopkeeperFishermanNPC;
import me.mykindos.betterpvp.shops.npc.impl.ShopkeeperSitNPC;
import me.mykindos.betterpvp.shops.npc.impl.attuner.AttunerNPC;
import me.mykindos.betterpvp.shops.npc.impl.reforger.ReforgerNPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class ShopkeeperNPCFactory extends NPCFactory {

    @Inject
    private ShopkeeperNPCFactory(SceneObjectRegistry registry) {
        super("shops", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[] {
                "fisherman",
                "resource_merchant",
                "blacksmith",
                "block_merchant",
                "farmer",
                "lumberjack",
                "auctioneer",
                "attuner",
                "reforger"
        };
    }

    @Override
    public NPC spawnDefault(@NotNull Location location, @NotNull String type) {
        return switch (type) {
            case "fisherman" -> spawnNPC(new ShopkeeperFishermanNPC(this, "Fisherman", "Gillbert", "skin_fisherman"), backingEntity(location));
            case "resource_merchant" -> {
                List<ItemStack> items = List.of(
                        ItemStack.of(Material.DIAMOND),
                        ItemStack.of(Material.GOLD_INGOT),
                        ItemStack.of(Material.IRON_INGOT),
                        ItemStack.of(Material.NETHERITE_INGOT),
                        ItemStack.of(Material.EMERALD)
                );
                yield spawnNPC(new Shopkeeper1NPC(this, "Resource Merchant", "Orelando", "skin_resource_merchant", items), backingEntity(location));
            }
            case "blacksmith" -> spawnNPC(new Shopkeeper2NPC(this, "Blacksmith", "Garrick", "skin_blacksmith"), backingEntity(location));
            case "block_merchant" -> spawnNPC(new Shopkeeper2NPC(this, "Block Merchant", "Brock", "skin_block_merchant"), backingEntity(location));
            case "farmer" -> spawnNPC(new Shopkeeper3NPC(this, "Farmer", "Wesley", "skin_farmer"), backingEntity(location));
            case "lumberjack" -> spawnNPC(new ShopkeeperSitNPC(this, "Lumberjack", "Tim Burr", "skin_lumberjack"), backingEntity(location));
            case "auctioneer" -> spawnNPC(new AuctionHouseNPC(this, "Auctioneer", "Baxter", "skin_auctioneer"), backingEntity(location));
            case "attuner" -> spawnNPC(new AttunerNPC(this, "Attuner", "Josh", "skin_attuner"), backingEntity(location));
            case "reforger" -> spawnNPC(new ReforgerNPC(this, "Reforger", "Ragnar", "skin_reforger"), backingEntity(location));
            default -> throw new IllegalArgumentException("Invalid shopkeeper type: " + type);
        };
    }

    private Entity backingEntity(@NotNull Location location) {
        return location.getWorld().spawn(location, Pig.class, spawned -> {
            spawned.setAI(false);
            spawned.setInvulnerable(true);
            spawned.setCollidable(false);
            spawned.setPersistent(false);
            spawned.setInvisible(true);
            spawned.setSilent(true);
        });
    }
}
