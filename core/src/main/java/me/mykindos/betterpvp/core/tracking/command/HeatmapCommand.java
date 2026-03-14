package me.mykindos.betterpvp.core.tracking.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.tracking.ActivitySnapshot;
import me.mykindos.betterpvp.core.tracking.PlayerActivityService;
import me.mykindos.betterpvp.core.tracking.model.GridKey;
import me.mykindos.betterpvp.core.tracking.model.HeatCell;
import me.mykindos.betterpvp.core.tracking.model.ZoneClassification;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
public class HeatmapCommand extends Command {

    @Override
    public String getName() {
        return "heatmap";
    }

    @Override
    public String getDescription() {
        return "Inspect the player activity heatmap";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Heatmap", UtilMessage.deserialize("<green>Usage: /heatmap <top|here>"));
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) return ArgumentType.SUBCOMMAND.name();
        return ArgumentType.NONE.name();
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(HeatmapCommand.class)
    public static class TopSubCommand extends Command {

        private final PlayerActivityService activityService;

        @Inject
        public TopSubCommand(PlayerActivityService activityService) {
            this.activityService = activityService;
        }

        @Override
        public String getName() {
            return "top";
        }

        @Override
        public String getDescription() {
            return "List the hottest grid cells";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            ActivitySnapshot snapshot = activityService.getSnapshot();
            if (snapshot == null) {
                UtilMessage.message(player, "Heatmap", "No snapshot available yet — wait up to 30 seconds.");
                return;
            }

            int limit = 10;
            if (args.length >= 1) {
                try {
                    limit = Math.max(1, Math.min(50, Integer.parseInt(args[0])));
                } catch (NumberFormatException ignored) {}
            }

            List<ActivitySnapshot.Entry> top = snapshot.entries().stream().limit(limit).toList();
            if (top.isEmpty()) {
                UtilMessage.message(player, "Heatmap", "No activity recorded yet.");
                return;
            }

            UtilMessage.message(player, "Heatmap", UtilMessage.deserialize(
                    "Top <yellow>%d</yellow> hotspots (snapshot age: <yellow>%ds</yellow>):",
                    top.size(),
                    (System.currentTimeMillis() - snapshot.timestamp()) / 1000
            ));

            for (int i = 0; i < top.size(); i++) {
                ActivitySnapshot.Entry entry = top.get(i);
                int blockX = entry.key().chunkX() * 16 + 8;
                int blockZ = entry.key().chunkZ() * 16 + 8;

                Component line = UtilMessage.deserialize(
                        "<gray>#%d</gray> <yellow>%s</yellow> (<aqua>%d, %d</aqua>) <gray>|</gray> %s <gray>|</gray> Heat: <yellow>%.1f</yellow> <gray>|</gray> Players: <green>%d</green> <gray>|</gray> Combat: <red>%d</red>",
                        i + 1,
                        entry.key().world(),
                        blockX, blockZ,
                        zoneColor(entry.classification()),
                        entry.heatValue(),
                        entry.currentPlayers(),
                        entry.combatEvents()
                ).clickEvent(ClickEvent.suggestCommand(String.format("/minecraft:tp %d ~ %d", blockX, blockZ)))
                 .hoverEvent(HoverEvent.showText(
                         Component.text("Teleport to ", NamedTextColor.GRAY)
                                 .append(Component.text(blockX + ", " + blockZ, NamedTextColor.AQUA))
                 ));

                UtilMessage.message(player, "Heatmap", line);
            }
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    @Singleton
    @SubCommand(HeatmapCommand.class)
    public static class HereSubCommand extends Command {

        private final PlayerActivityService activityService;

        @Inject
        public HereSubCommand(PlayerActivityService activityService) {
            this.activityService = activityService;
        }

        @Override
        public String getName() {
            return "here";
        }

        @Override
        public String getDescription() {
            return "Show heat data for the cell you are standing in";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            GridKey key = GridKey.of(player.getLocation());
            HeatCell cell = activityService.getCells().get(key);

            int blockX = key.chunkX() * 16 + 8;
            int blockZ = key.chunkZ() * 16 + 8;

            UtilMessage.message(player, "Heatmap", UtilMessage.deserialize(
                    "Cell <yellow>%s</yellow> (<aqua>%d, %d</aqua>) chunk (<gray>%d, %d</gray>)",
                    key.world(), blockX, blockZ, key.chunkX(), key.chunkZ()
            ));

            if (cell == null) {
                UtilMessage.message(player, "Heatmap", UtilMessage.deserialize("Zone: <gray>EMPTY</gray> — no activity recorded here."));
                return;
            }

            ZoneClassification zone = activityService.classify(cell.getHeatValue());
            UtilMessage.message(player, "Heatmap", UtilMessage.deserialize(
                    "Zone: %s <gray>|</gray> Heat: <yellow>%.2f</yellow> <gray>|</gray> Peak: <yellow>%.2f</yellow> <gray>|</gray> Visits: <green>%d</green> <gray>|</gray> Combat: <red>%d</red>",
                    zoneColor(zone),
                    cell.getHeatValue(),
                    cell.getPeakHeat(),
                    cell.getTotalVisits(),
                    cell.getCombatEvents()
            ));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    private static String zoneColor(ZoneClassification zone) {
        return switch (zone) {
            case HOTSPOT -> "<red>HOTSPOT</red>";
            case ACTIVE  -> "<gold>ACTIVE</gold>";
            case QUIET   -> "<yellow>QUIET</yellow>";
            case EMPTY   -> "<gray>EMPTY</gray>";
        };
    }

}
