package me.mykindos.betterpvp.core.world.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.logging.formatters.ILogFormatter;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.logger.formatters.WorldLogFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.ocpsoft.prettytime.PrettyTime;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@Getter
@CustomLog
public class WorldLogHandler extends Manager<WorldLogSession> {

    private static final PrettyTime PRETTY_TIME = new PrettyTime();

    private final WorldLogRepository worldLogRepository;

    private final Set<UUID> inspectingPlayers = new java.util.HashSet<>();
    private final List<WorldLog> pendingLogs = new ArrayList<>();
    private final HashMap<String, WorldLogFormatter> formatters = new HashMap<>();

    @Inject
    public WorldLogHandler(WorldLogRepository worldLogRepository) {
        this.worldLogRepository = worldLogRepository;

        Reflections reflections = new Reflections(getClass().getPackageName());
        Set<Class<? extends WorldLogFormatter>> classes = reflections.getSubTypesOf(WorldLogFormatter.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum() || clazz.isAnnotationPresent(Deprecated.class))
                continue;
            try {
                WorldLogFormatter worldLogFormatter = clazz.getConstructor().newInstance();
                formatters.put(worldLogFormatter.requiredAction(), worldLogFormatter);
            }catch (Exception e){
               log.error("Error loading WorldLogFormatter: " + clazz.getName()).submit();
            }
        }
    }

    public void addLog(WorldLog log) {
        log.setTime(Instant.now());
        pendingLogs.add(log);
    }

    public void saveLogs(List<WorldLog> logs) {
        worldLogRepository.saveLogs(logs);
    }

    public WorldLogSession getSession(UUID uuid) {
        return objects.computeIfAbsent(uuid.toString(), k -> new WorldLogSession());
    }

    public void displayResults(Player player, WorldLogSession session, int page) {
        CompletableFuture.runAsync(() -> {
            worldLogRepository.processSession(session, page);

            // Sort time ascending
            session.getData().sort(Comparator.comparing(WorldLog::getTime));

            if(session.getData().isEmpty()) {
                UtilMessage.simpleMessage(player, "World Logger", "No results found.");
                return;
            }

            for (WorldLog worldLog : session.getData()) {

                WorldLogFormatter formatter = formatters.get(worldLog.getAction());
                if(formatter == null) {
                    log.warn("No formatter found for action: " + worldLog.getAction()).submit();
                    continue;
                }

                Component component = Component.text(WorldLogTimeFormat.toDuration(worldLog.getTime()), NamedTextColor.GRAY)
                        .append(formatter.getPrefix())
                        .append(formatter.format(worldLog));


                UtilMessage.message(player, component.clickEvent(ClickEvent.runCommand("/tppos " + worldLog.getBlockX() + " " + worldLog.getBlockY() + " " + worldLog.getBlockZ()))
                        .hoverEvent(HoverEvent.showText(Component.text("Teleport to " + worldLog.getBlockX() + " " + worldLog.getBlockY() + " " + worldLog.getBlockZ()))));
            }

            if (session.getPages() > 1) {
                List<Component> pages = generatePagination(session.getCurrentPage(), session.getPages());
                Component pageComponent = Component.text("Page ", NamedTextColor.DARK_AQUA)
                        .append(Component.text(session.getCurrentPage(), NamedTextColor.WHITE))
                        .append(Component.text("/", NamedTextColor.WHITE)).append(Component.text(session.getPages(), NamedTextColor.WHITE))
                        .append(Component.text(" (", NamedTextColor.GRAY));

                for(Component cmp : pages) {
                    pageComponent = pageComponent.append(cmp);
                    if(cmp != pages.getLast()) {
                        String text = PlainTextComponentSerializer.plainText().serialize(cmp);
                        pageComponent = pageComponent.append(Component.text(" | ", NamedTextColor.GRAY));
                    }
                }
                pageComponent = pageComponent.append(Component.text(")", NamedTextColor.GRAY));
                UtilMessage.message(player, pageComponent);
            }
        });
    }


    private List<Component> generatePagination(int currentPage, int totalPages) {
        List<Component> pages = new ArrayList<>();

        // Always show the first page
        pages.add(Component.text("1").hoverEvent(HoverEvent.showText(Component.text("/wl page 1")))
                .clickEvent(ClickEvent.runCommand("/wl page 1")));

        // Add "..." if the gap between first and visible range is more than 1
        if (currentPage > 4) {
            pages.add(Component.text("..."));
        }

        // Add pages around the current page
        for (int i = Math.max(2, currentPage - 2); i <= Math.min(totalPages - 1, currentPage + 2); i++) {
            pages.add(Component.text(String.valueOf(i)).hoverEvent(HoverEvent.showText(Component.text("/wl page " + i)))
                    .clickEvent(ClickEvent.runCommand("/wl page " + i)));
        }

        // Add "..." if there's a gap before the last page
        if (currentPage < totalPages - 3) {
            pages.add(Component.text("..."));
        }

        // Always show the last page if there's more than one page
        if (totalPages > 1) {
            pages.add(Component.text(String.valueOf(totalPages))
                    .hoverEvent(HoverEvent.showText(Component.text("/wl page " + totalPages)))
                    .clickEvent(ClickEvent.runCommand("/wl page " + totalPages)));
        }

        return pages;
    }
}
