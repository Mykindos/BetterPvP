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
import me.mykindos.betterpvp.core.locale.Translations;
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
        return "core.command.heatmap.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.heatmap", Translations.component("core.heatmap.usage").color(NamedTextColor.GREEN));
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
        return "core.command.top.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            ActivitySnapshot snapshot = activityService.getSnapshot();
            if (snapshot == null) {
                UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.no_snapshot");
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
                UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.no_activity");
                return;
            }

            UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.top.header",
                    Component.text(top.size(), NamedTextColor.YELLOW),
                    Component.text((System.currentTimeMillis() - snapshot.timestamp()) / 1000, NamedTextColor.YELLOW));

            for (int i = 0; i < top.size(); i++) {
                ActivitySnapshot.Entry entry = top.get(i);
                int blockX = entry.key().chunkX() * 16 + 8;
                int blockZ = entry.key().chunkZ() * 16 + 8;

                Component line = Translations.component("core.heatmap.top.entry",
                        Component.text("#" + (i + 1), NamedTextColor.GRAY),
                        Component.text(entry.key().world(), NamedTextColor.YELLOW),
                        Component.text(blockX, NamedTextColor.AQUA),
                        Component.text(blockZ, NamedTextColor.AQUA),
                        zoneColor(entry.classification()),
                        Component.text(String.format("%.1f", entry.heatValue()), NamedTextColor.YELLOW),
                        Component.text(entry.currentPlayers(), NamedTextColor.GREEN),
                        Component.text(entry.combatEvents(), NamedTextColor.RED)
                ).clickEvent(ClickEvent.suggestCommand(String.format("/minecraft:tp %d ~ %d", blockX, blockZ)))
                 .hoverEvent(HoverEvent.showText(
                         Translations.component("core.heatmap.teleport_to").color(NamedTextColor.GRAY)
                                 .append(Component.text(blockX + ", " + blockZ, NamedTextColor.AQUA))
                 ));

                UtilMessage.message(player, "core.prefix.heatmap", line);
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
        return "core.command.here.description";
    }

        @Override
        public void execute(Player player, Client client, String... args) {
            GridKey key = GridKey.of(player.getLocation());
            HeatCell cell = activityService.getCells().get(key);

            int blockX = key.chunkX() * 16 + 8;
            int blockZ = key.chunkZ() * 16 + 8;

            UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.here.cell",
                    Component.text(key.world(), NamedTextColor.YELLOW),
                    Component.text(blockX, NamedTextColor.AQUA),
                    Component.text(blockZ, NamedTextColor.AQUA),
                    Component.text(key.chunkX(), NamedTextColor.GRAY),
                    Component.text(key.chunkZ(), NamedTextColor.GRAY));

            if (cell == null) {
                UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.here.empty",
                        Translations.component("core.heatmap.zone.empty").color(NamedTextColor.GRAY));
                return;
            }

            ZoneClassification zone = activityService.classify(cell.getHeatValue());
            UtilMessage.message(player, "core.prefix.heatmap", "core.heatmap.here.stats",
                    zoneColor(zone),
                    Component.text(String.format("%.2f", cell.getHeatValue()), NamedTextColor.YELLOW),
                    Component.text(String.format("%.2f", cell.getPeakHeat()), NamedTextColor.YELLOW),
                    Component.text(cell.getTotalVisits(), NamedTextColor.GREEN),
                    Component.text(cell.getCombatEvents(), NamedTextColor.RED));
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }
    }

    private static Component zoneColor(ZoneClassification zone) {
        return switch (zone) {
            case HOTSPOT -> Translations.component("core.heatmap.zone.hotspot").color(NamedTextColor.RED);
            case ACTIVE  -> Translations.component("core.heatmap.zone.active").color(NamedTextColor.GOLD);
            case QUIET   -> Translations.component("core.heatmap.zone.quiet").color(NamedTextColor.YELLOW);
            case EMPTY   -> Translations.component("core.heatmap.zone.empty").color(NamedTextColor.GRAY);
        };
    }

}
