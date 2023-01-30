package me.mykindos.betterpvp.clans.clans.map.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

@Singleton
public class MapCommand extends Command {

    private final ItemHandler itemHandler;

    @Inject
    public MapCommand(ItemHandler itemHandler){
        this.itemHandler = itemHandler;
    }

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public String getDescription() {
        return "Get a map of the world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!UtilInventory.contains(player, Material.FILLED_MAP, 1)) {
            ItemStack itemstack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) itemstack.getItemMeta();
            meta.setMapView(Bukkit.getMap(0));
            itemstack.setItemMeta(meta);
            player.getInventory().addItem(itemHandler.updateNames(itemstack));
        }else{
            UtilMessage.message(player, "Clans", "You already have a map in your inventory");
        }

    }

    @Singleton
    @SubCommand(MapCommand.class)
    private static class SaveMapSubCommand extends Command {

        @Inject
        private MapHandler mapHandler;

        @Override
        public String getName() {
            return "save";
        }

        @Override
        public String getDescription() {
            return "Save the current map state";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            mapHandler.saveMapData();
            UtilMessage.message(player, "Clans", "The map has been saved.");
        }


    }
}
