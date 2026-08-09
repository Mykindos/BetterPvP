package me.mykindos.betterpvp.clans.clans.map.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.map.ClanMapService;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.terrain.MapTerrainService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Singleton
public class MapCommand extends Command {

    private final ItemFactory itemFactory;
    private final ClanMapService clanMapService;

    @Inject
    public MapCommand(ItemFactory itemFactory, ClanMapService clanMapService) {
        this.itemFactory = itemFactory;
        this.clanMapService = clanMapService;
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
            final ItemStack itemStack = clanMapService.createMapItem();
            player.getInventory().addItem(itemFactory.convertItemStack(itemStack).orElse(itemStack));
        } else {
            UtilMessage.message(player, "clans.prefix",
                    Translations.component("clans.command.map.already-have").color(NamedTextColor.RED));
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
        @Inject
        private MapTerrainService terrainService;

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
            mapHandler.resetMapData(player.getWorld());
            UtilMessage.message(player, "clans.prefix", "clans.command.map.reset.success");
            // Rebuild the whole displayable area from the world's terrain, asynchronously (no walking, no freeze).
            terrainService.regenerate(player.getWorld(), player);
        }
    }

    @Singleton
    @SubCommand(MapCommand.class)
    private static class RedrawMapSubCommand extends Command {

        @Inject
        private ClanMapService clanMapService;

        @Override
        public String getName() {
            return "redraw";
        }

        @Override
        public String getDescription() {
            return "clans.command.map.redraw.description";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            clanMapService.getOrCreateMapSettings(player).setForceRedraw(true);
            UtilMessage.message(player, "clans.prefix", "Redrawing map...");
        }
    }
}
