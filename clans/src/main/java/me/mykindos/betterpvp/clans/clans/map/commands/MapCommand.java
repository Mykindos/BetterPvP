package me.mykindos.betterpvp.clans.clans.map.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

@Singleton
public class MapCommand extends Command {

    private final ItemFactory itemFactory;

    @Inject
    public MapCommand(ItemFactory itemFactory){
        this.itemFactory = itemFactory;
    }

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public String getDescription() {
        return "clans.command.map.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0f, 1f).play(player);
        if (!UtilInventory.contains(player, Material.FILLED_MAP, 1)) {
            ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) itemStack.getItemMeta();
            meta.setMapView(Bukkit.getMap(0));
            itemStack.setItemMeta(meta);
            player.getInventory().addItem(itemFactory.convertItemStack(itemStack).orElse(itemStack));
        } else {
            UtilMessage.message(player, "clans.prefix", Translations.component("clans.command.map.already-have").color(NamedTextColor.RED));
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
        return "clans.command.save-map.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            mapHandler.saveMapData();
            UtilMessage.message(player, "clans.prefix", "clans.command.map.save.success");
        }


    }

    @Singleton
    @SubCommand(MapCommand.class)
    private static class ResetMapSubCommand extends Command {

        @Inject
        private MapHandler mapHandler;

        @Override
        public String getName() {
            return "reset";
        }

        @Override
        public String getDescription() {
        return "clans.command.reset-map.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            mapHandler.resetMapData();
            UtilMessage.message(player, "clans.prefix", "clans.command.map.reset.success");
        }


    }
}
