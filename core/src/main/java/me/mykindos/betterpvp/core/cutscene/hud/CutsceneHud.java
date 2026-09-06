package me.mykindos.betterpvp.core.cutscene.hud;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import me.mykindos.betterpvp.core.quest.conversation.ConversationManager;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Owns the screen for the length of a cutscene: a letterbox bar top and bottom, the dialogue composited into the
 * lower one, and every other HUD held back until it is over.
 * <p>
 * Holding the rest back is not only about a clean frame. The boss-bar slot the top bar hangs from moves down the
 * screen when other boss bars stack above it, so anything else drawing there would drag the top bar with it.
 */
@Singleton
public class CutsceneHud {

    /** Above the conversation override at 100, because a cutscene composites the dialogue rather than yielding to it. */
    private static final int OVERRIDE_PRIORITY = 200;

    private final Letterbox letterbox;
    private final ClientManager clientManager;
    private final ConversationManager conversations;

    @Inject
    public CutsceneHud(Letterbox letterbox, ClientManager clientManager, ConversationManager conversations) {
        this.letterbox = letterbox;
        this.clientManager = clientManager;
        this.conversations = conversations;
    }

    /** Takes over both HUD surfaces for this session. */
    public void attach(@NotNull Player player, @NotNull CutsceneSession session) {
        if (!letterbox.hasBars()) {
            return;
        }

        final Gamer gamer = clientManager.search().online(player).getGamer();
        final CutsceneActionBar bar = new CutsceneActionBar(ignored -> bottom(session));
        final DisplayObject<Component> overlay = new DisplayObject<>(ignored -> letterbox.top(session.getTick()));

        session.setActionBar(bar);
        session.setOverlay(overlay);
        gamer.getActionBarOverrides().push(OVERRIDE_PRIORITY, bar);
        gamer.getBossBarOverlay().add(overlay);
        gamer.getBossBarOverlay().setExclusive(overlay);
    }

    /** Gives both surfaces back, whatever ended the cutscene. */
    public void detach(@NotNull Player player, @NotNull CutsceneSession session) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (session.getActionBar() != null) {
            gamer.getActionBarOverrides().remove(session.getActionBar());
            session.setActionBar(null);
        }
        if (session.getOverlay() != null) {
            gamer.getBossBarOverlay().setExclusive(null);
            gamer.getBossBarOverlay().remove(session.getOverlay());
            session.setOverlay(null);
        }
        player.clearTitle();
    }

    /**
     * The lower bar with the dialogue drawn into it. Both blocks have zero net advance, so each stays centred on its
     * own terms and neither displaces the other.
     */
    private @NotNull Component bottom(@NotNull CutsceneSession session) {
        final Component band = letterbox.bottom(session.getTick());
        final Player player = session.getPlayer();
        final Component dialogue = player == null ? null : conversations.renderFor(player);
        return dialogue == null ? band : Component.text().append(band).append(dialogue).build();
    }
}
