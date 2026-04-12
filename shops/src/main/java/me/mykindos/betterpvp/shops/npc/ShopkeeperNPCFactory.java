package me.mykindos.betterpvp.shops.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.NPC;
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
    private ShopkeeperNPCFactory(NPCRegistry registry) {
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
            case "fisherman" -> new ShopkeeperFishermanNPC(this, backingEntity(location), "Fisherman", "Gillbert", "skin_fisherman");
            case "resource_merchant" ->  {
                List<ItemStack> items = List.of(
                        ItemStack.of(Material.DIAMOND),
                        ItemStack.of(Material.GOLD_INGOT),
                        ItemStack.of(Material.IRON_INGOT),
                        ItemStack.of(Material.NETHERITE_INGOT),
                        ItemStack.of(Material.EMERALD)
                );
                yield new Shopkeeper1NPC(this, backingEntity(location), "Resource Merchant", "Orelando", "skin_resource_merchant", items);
            }
            case "blacksmith" -> new Shopkeeper2NPC(this, backingEntity(location), "Blacksmith", "Garrick", "skin_blacksmith");
            case "block_merchant" -> new Shopkeeper2NPC(this, backingEntity(location), "Block Merchant", "Brock", "skin_block_merchant");
            case "farmer" -> new Shopkeeper3NPC(this, backingEntity(location), "Farmer", "Wesley", "skin_farmer");
            case "lumberjack" -> new ShopkeeperSitNPC(this, backingEntity(location), "Lumberjack", "Tim Burr", "skin_lumberjack");
            case "auctioneer" -> new AuctionHouseNPC(this, backingEntity(location), "Auctioneer", "Baxter", "skin_auctioneer");
            case "attuner" -> new AttunerNPC(this, backingEntity(location), "Attuner", "Josh", "skin_attuner");
            case "reforger" -> new ReforgerNPC(this, backingEntity(location), "Reforger", "Ragnar", "skin_reforger");
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
