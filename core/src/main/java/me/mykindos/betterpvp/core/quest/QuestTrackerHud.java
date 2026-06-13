package me.mykindos.betterpvp.core.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.model.QuestDefinition;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;

/**
 * A lightweight objective tracker on the boss-bar overlay. Renders the player's
 * first active quest and its objective counts; renders nothing (skipped) when
 * they have no active quests.
 */
@Singleton
@BPvPListener
public class QuestTrackerHud implements Listener {

    private static final Component SEPARATOR = Component.translatable("newlayer").font(Resources.Font.SPACE);
    private static final int MAX_OBJECTIVES = 4;

    private final QuestManager questManager;
    private final QuestRegistry questRegistry;

    @Inject
    public QuestTrackerHud(QuestManager questManager, QuestRegistry questRegistry) {
        this.questManager = questManager;
        this.questRegistry = questRegistry;
    }

    @EventHandler
    public void onLoad(AsyncClientLoadEvent event) {
        Gamer gamer = event.getClient().getGamer();
        gamer.getBossBarOverlay().add(new DisplayObject<>(this::render));
    }

    private Component render(Gamer gamer) {
        Player player = gamer.getPlayer();
        if (player == null) return null;
        List<QuestInstance> active = questManager.activeFor(player);
        if (active.isEmpty()) return null;

        QuestInstance instance = active.getFirst();
        if (!instance.getStatus().equals(QuestInstance.STATUS_ACTIVE)) return null;

        String name = questRegistry.get(instance.getQuestId())
                .map(QuestDefinition::getName)
                .orElse(instance.getQuestId());

        Component combined = Component.text("⚔ " + name).color(NamedTextColor.GOLD);
        int shown = 0;
        for (Map.Entry<String, Integer> entry : instance.getProgress().entrySet()) {
            if (shown++ >= MAX_OBJECTIVES) break;
            int target = instance.getTargets().getOrDefault(entry.getKey(), 1);
            boolean done = entry.getValue() >= target;
            combined = combined.append(SEPARATOR).append(Component.text(
                    (done ? "✔ " : "• ") + entry.getValue() + "/" + target)
                    .color(done ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
        return combined;
    }
}
