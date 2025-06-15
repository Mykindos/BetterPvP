package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.menu.resourceconverter.ResourceConverterMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class ResourceConverterListener implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    public ResourceConverterListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractStonecutter(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.GRINDSTONE) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material material = hand.getType();

        if (material != Material.DIAMOND
                && material != Material.EMERALD
                && material != Material.IRON_INGOT
                && material != Material.GOLD_INGOT
                && material != Material.NETHERITE_INGOT
                && material != Material.LEATHER) {
            UtilMessage.simpleMessage(player, "Resource Converter", "This item cannot be converted.");
            return;
        }

        new ResourceConverterMenu(player, hand, itemFactory).show(player);


    }

    @EventHandler (ignoreCancelled = true)
    public void onMoveItemToResourceConverter(InventoryMoveItemEvent event) {
        Location location = event.getDestination().getLocation();

        if (location != null) {
            if (location.getBlock().getType() == Material.GRINDSTONE) {
                event.setCancelled(true);
            }
        }
    }

}
