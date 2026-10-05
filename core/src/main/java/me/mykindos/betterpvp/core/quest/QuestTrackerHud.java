package me.mykindos.betterpvp.core.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
    private final DisplayObject<Component> overlay = new DisplayObject<>(this::render);

    @Inject
    public QuestTrackerHud(QuestManager questManager, QuestRegistry questRegistry) {
        this.questManager = questManager;
        this.questRegistry = questRegistry;
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final Gamer gamer = event.getClient().getGamer();
        // Quit clears the overlays. Removing first keeps a single one if join fires twice without a quit.
        gamer.getBossBarOverlay().remove(overlay);
        gamer.getBossBarOverlay().add(overlay);
    }

    private Component render(Gamer gamer) {
        // todo: quest hud
        return null;
    }
}
