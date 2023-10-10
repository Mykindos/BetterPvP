package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

@Slf4j
@BPvPListener
public class WorldListener implements Listener {

    private final GamerManager gamerManager;
    private final ItemHandler itemHandler;

    @Inject
    public WorldListener(GamerManager gamerManager, ItemHandler itemHandler) {
        this.gamerManager = gamerManager;
        this.itemHandler = itemHandler;
    }

    /*
     * Stops players from lighting fires on stuff like grass, wood, etc.
     * Helps keep the map clean
     */
    @EventHandler
    public void blockFlint(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId());
            if(gamerOptional.isPresent()) {
                if(gamerOptional.get().getClient().isAdministrating()){
                    return;
                }
            }

            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.FLINT_AND_STEEL) {
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null && clickedBlock.getType() != Material.TNT && clickedBlock.getType() != Material.NETHERRACK) {
                    UtilMessage.message(event.getPlayer(), "Flint and Steel", "You may not use Flint and Steel on this block type!");
                    event.setCancelled(true);
                }
            }
        }
    }

    /*
     * Stops players from filling buckets with water or lava, and also breaks the bucket.
     */
    @EventHandler
    public void handleBucket(PlayerBucketFillEvent event) {
        event.setCancelled(true);
        UtilMessage.simpleMessage(event.getPlayer(), "Game", "Your <alt2>Bucket</alt2> broke!");
        ItemStack replacement = new ItemStack(Material.IRON_INGOT, event.getPlayer().getInventory().getItemInMainHand().getAmount() * 3);
        event.getPlayer().getInventory().setItemInMainHand(replacement);
    }



    /*
     * Prevent fall damage when landing on wool or sponge
     */
    @EventHandler
    public void onSafeFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (UtilBlock.getBlockUnder(player.getLocation()).getType().name().contains("SPONGE")
                        || UtilBlock.getBlockUnder(player.getLocation()).getType().name().contains("WOOL")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /*
     * Players were creating chest rooms at sky limit, making them a lot harder to raid.
     * This requires all forms of item storage to be placed below 200y.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        BlockState state = block.getState();
        if (state instanceof Container) {
            if (block.getLocation().getY() > 200) {
                UtilMessage.message(player, "Restriction", "You can only place chests lower than 200Y!");
                event.setCancelled(true);
            }
        }

    }



    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getBlock().getType() != Material.DIRT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            event.setCancelled(true);
        }
    }

    /*
     * Stops players from opening certain inventories related to blocks,
     * Such as brewing stands, and enchanting tables.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {

            if (event.getInventory().getType() == InventoryType.BREWING
                    || event.getInventory().getType() == InventoryType.DISPENSER
                    || event.getInventory().getType() == InventoryType.CARTOGRAPHY
                    || event.getInventory().getType() == InventoryType.GRINDSTONE
                    || event.getInventory().getType() == InventoryType.LECTERN
                    || event.getInventory().getType() == InventoryType.SHULKER_BOX
                    || event.getInventory().getType() == InventoryType.LOOM
                    || event.getInventory().getType() == InventoryType.STONECUTTER
                    || event.getInventory().getType() == InventoryType.SMITHING
                    || event.getInventory().getType() == InventoryType.BEACON) {
                UtilMessage.simpleMessage(player, "Game", "<alt2>" + UtilFormat.cleanString(event.getInventory().getType().toString()) + "</alt2> is disabled.");
                event.setCancelled(true);
            }

            if (event.getInventory().getType() == InventoryType.ENCHANTING) {
                event.setCancelled(true);
            }
        }
    }

    /*
     * Stops crops from being trampled by mobs and players
     */
    @EventHandler
    public void soilChangePlayer(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            if (Objects.requireNonNull(event.getClickedBlock()).getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }

    /*
     * Stops non players from passing through portals
     * When we had the nether, people would pull the boss in.
     */
    @EventHandler
    public void onPortal(EntityPortalEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void clearArmourStands(ModuleLoadedEvent event) {
        if (!event.getModuleName().equals("Clans")) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand stand) {
                    if (!stand.isVisible()) {
                        entity.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlaceScaffold(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.SCAFFOLDING) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(event.getPlayer().getUniqueId().toString());
            gamerOptional.ifPresent(gamer -> {
                if (!gamer.getClient().isAdministrating()) {
                    event.setCancelled(true);
                }
            });
        }
    }

    /*
     * Stops players from placing certain types of blocks
     * as well as turns wooden doors into iron doors (to stop door hitting)
     */
    @EventHandler
    public void onBlockCancelPlace(BlockPlaceEvent event) {

        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {
            if (!gamer.getClient().isAdministrating()) {
                // maybe load from config later
                if (block.getType().name().contains("OBSIDIAN") || block.getType() == Material.BEDROCK || block.getType() == Material.WATER_BUCKET
                        || block.getType() == Material.SPAWNER || block.getType() == Material.COBWEB
                        || block.getType() == Material.BREWING_STAND || block.getType().name().contains("_BED")) {
                    UtilMessage.simpleMessage(player, "Server", "You cannot place <alt2>" + WordUtils.capitalizeFully(block.getType().toString()) + "</alt2>.");
                    event.setCancelled(true);
                }
            }
        });

    }

    /**
     * Prevent players setting a new spawn
     *
     * @param event event
     */
    @EventHandler
    public void disableBeds(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType().name().contains("_BED")) {
                event.setCancelled(true);
            }
        }
    }

    /*
     * Stops potions from being brewed (via auto brew methods)
     */
    @EventHandler
    public void onBrew(BrewEvent event) {
        event.setCancelled(true);
    }

    /*
     * Modifies the drops for just about all mobs in minecraft
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void handleDeath(EntityDeathEvent event) {

        event.setDroppedExp(0);


        if (event.getEntity().customName() == null) {
            if (event.getEntityType() != EntityType.PLAYER) {
                event.getDrops().clear();
                List<ItemStack> drops = new ArrayList<>();

                if (event.getEntityType() == EntityType.CHICKEN) {
                    drops.add(new ItemStack(Material.CHICKEN, 1));
                    drops.add(new ItemStack(Material.FEATHER, 2 + UtilMath.randomInt(1)));
                } else if (event.getEntityType() == EntityType.COW) {
                    drops.add(new ItemStack(Material.BEEF, 1 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.LEATHER, 1 + UtilMath.randomInt(2)));
                }
                if (event.getEntityType() == EntityType.MUSHROOM_COW) {
                    drops.add(new ItemStack(Material.BEEF, 1 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.RED_MUSHROOM, 2 + UtilMath.randomInt(2)));
                } else if (event.getEntityType() == EntityType.OCELOT) {
                    int rand = UtilMath.randomInt(10);
                    if (rand == 0 || rand == 1 || rand == 2) {
                        drops.add(new ItemStack(Material.LEATHER, 1 + UtilMath.randomInt(2)));
                    } else if (rand == 3 || rand == 4 || rand == 5) {
                        drops.add(new ItemStack(Material.COD, 2 + UtilMath.randomInt(2)));
                    } else if (rand == 6 || rand == 7) {
                        drops.add(new ItemStack(Material.COAL, 1 + UtilMath.randomInt(2)));
                    } else {
                        drops.add(new ItemStack(Material.COD, 10 + UtilMath.randomInt(10)));
                    }
                    drops.add(new ItemStack(Material.BONE, 4 + UtilMath.randomInt(4)));

                } else if (event.getEntityType() == EntityType.PIG) {
                    drops.add(new ItemStack(Material.PORKCHOP, 1 + UtilMath.randomInt(2)));
                } else if (event.getEntityType() == EntityType.SHEEP) {
                    drops.add(new ItemStack(Material.WHITE_WOOL, 1 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.WHITE_WOOL, 1 + UtilMath.randomInt(4)));
                } else if (event.getEntityType() == EntityType.VILLAGER) {
                    drops.add(new ItemStack(Material.BONE, 2 + UtilMath.randomInt(3)));
                } else if (event.getEntityType() == EntityType.BLAZE) {
                    drops.add(new ItemStack(Material.BLAZE_ROD, 1));
                    drops.add(new ItemStack(Material.BONE, 6 + UtilMath.randomInt(7)));
                } else if (event.getEntityType() == EntityType.CAVE_SPIDER) {

                    drops.add(new ItemStack(Material.COBWEB, 1));
                    drops.add(new ItemStack(Material.STRING, 2 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.SPIDER_EYE, 1));
                    drops.add(new ItemStack(Material.BONE, 4 + UtilMath.randomInt(4)));

                } else if (event.getEntityType() == EntityType.CREEPER) {
                    drops.add(new ItemStack(Material.COAL, 2 + UtilMath.randomInt(4)));
                    drops.add(new ItemStack(Material.BONE, 4 + UtilMath.randomInt(7)));
                } else if (event.getEntityType() == EntityType.ENDERMAN) {
                    drops.add(new ItemStack(Material.BONE, 12 + UtilMath.randomInt(8)));
                } else if (event.getEntityType() == EntityType.GHAST) {
                    drops.add(new ItemStack(Material.GHAST_TEAR, 1));
                    drops.add(new ItemStack(Material.BONE, 16 + UtilMath.randomInt(8)));
                } else if (event.getEntityType() == EntityType.IRON_GOLEM) {
                    drops.add(new ItemStack(Material.IRON_INGOT, 2 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.BONE, 12 + UtilMath.randomInt(6)));
                } else if (event.getEntityType() == EntityType.MAGMA_CUBE) {
                    drops.add(new ItemStack(Material.MAGMA_CREAM, UtilMath.randomInt(1, 3)));
                    drops.add(new ItemStack(Material.BONE, 1 + UtilMath.randomInt(2)));
                } else if (event.getEntityType() == EntityType.ZOMBIFIED_PIGLIN) {
                    PigZombie z = (PigZombie) event.getEntity();
                    if (z.getEquipment().getItemInMainHand().getType() == Material.GOLDEN_AXE) {
                        drops.add(new ItemStack(Material.GOLDEN_AXE));
                    }
                    drops.add(new ItemStack(Material.BONE, 2 + UtilMath.randomInt(2)));
                    if (UtilMath.randomInt(50) > 48) {
                        ItemStack[] temp = {new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.CHAINMAIL_BOOTS),
                                new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.CHAINMAIL_LEGGINGS)};
                        drops.add(temp[UtilMath.randomInt(temp.length - 1)]);
                    }
                    if (UtilMath.randomInt(100) > 90) {
                        drops.add(new ItemStack(Material.GOLDEN_PICKAXE));
                    } else if (UtilMath.randomInt(1000) > 990) {
                        drops.add(new ItemStack(Material.GOLDEN_SWORD));
                    }
                } else if (event.getEntityType() == EntityType.SILVERFISH) {
                    drops.add(new ItemStack(Material.BONE, 1 + UtilMath.randomInt(2)));
                } else if (event.getEntityType() == EntityType.SKELETON) {
                    drops.add(new ItemStack(Material.ARROW, 4 + UtilMath.randomInt(5)));
                    drops.add(new ItemStack(Material.BONE, 3 + UtilMath.randomInt(4)));
                } else if (event.getEntityType() == EntityType.SLIME) {
                    drops.add(new ItemStack(Material.SLIME_BALL, 1));
                    drops.add(new ItemStack(Material.BONE, 1 + UtilMath.randomInt(2)));
                } else if (event.getEntityType() == EntityType.SPIDER) {
                    drops.add(new ItemStack(Material.STRING, 2 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.COBWEB, 1));
                    drops.add(new ItemStack(Material.SPIDER_EYE, 1));
                    drops.add(new ItemStack(Material.BONE, 4 + UtilMath.randomInt(4)));
                } else if (event.getEntityType() == EntityType.ZOMBIE) {
                    event.getDrops().add(new ItemStack(Material.ROTTEN_FLESH, 1));
                    drops.add(new ItemStack(Material.BONE, 3 + UtilMath.randomInt(4)));
                } else if (event.getEntityType() == EntityType.RABBIT) {
                    drops.add(new ItemStack(Material.RABBIT_HIDE, 1 + UtilMath.randomInt(3)));
                    drops.add(new ItemStack(Material.BONE, 2 + UtilMath.randomInt(3)));
                }

                for (ItemStack t : drops) {
                    event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), t);
                }
            }
        }

    }

    /*
     * Updates the names of items that are picked up from the ground (sets there name to be yellow from wh ite)
     * Other than enchanted armour
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            itemHandler.updateNames(event.getItem().getItemStack());
        }
    }

    /*
     * Stops magma cubes from splitting
     */
    @EventHandler
    public void onMagmaSplit(SlimeSplitEvent event) {
        if (event.getEntity() instanceof MagmaCube) {
            event.setCancelled(true);
        }
    }

    /*
     * Throws out red dye everywhere when players die
     * Creates a blood splatter effect
     */
    public static HashMap<Item, Long> blood = new HashMap<>();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        for (int i = 0; i < 10; i++) {
            final Item item = event.getEntity().getWorld().dropItem(event.getEntity().getEyeLocation(), new ItemStack(Material.RED_DYE, 1));
            item.setVelocity(new Vector((Math.random() - 0.5D) * 0.5, Math.random() * 0.5, (Math.random() - 0.5D) * 0.5));
            item.setPickupDelay(Integer.MAX_VALUE);
            blood.put(item, System.currentTimeMillis());
        }

    }

    /*
     * Makes sure the blood items get removed after 500 milliseconds
     */
    @UpdateEvent(delay = 250)
    public void blood() {
        if (blood.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Item, Long>> it = blood.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Item, Long> next = it.next();
            if (UtilTime.elapsed(next.getValue(), 500)) {
                next.getKey().remove();
                it.remove();
            }
        }
    }

    /**
     * No hand swapping!
     *
     * @param event the event
     */
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    /*
     * Stops ground items from being destroyed from things like lava, fire, lightning, etc.
     */
    @EventHandler
    public void itemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            event.setCancelled(true);
        }
    }

    /*
     * Removes arrows when they hit the ground, or a player
     */
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            arrow.remove();
        }
    }

    @EventHandler
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if (event.getEntityType() == EntityType.PHANTOM) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageWarden(CustomDamageEvent event) {
        if(event.getDamagee() instanceof Warden) {
            event.setKnockback(false);
        }
    }

}
