package me.mykindos.betterpvp.core.quest.conversation;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * An action bar override that renders the active conversation exclusively, bypassing the
 * gamer's regular component queue while leaving it untouched in the background.
 */
public class ConversationActionBar extends ActionBar {

    private final Function<Gamer, Component> renderer;

    public ConversationActionBar(Function<Gamer, Component> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void show(Gamer gamer) {
        final Component component = renderer.apply(gamer);
        final Player player = gamer.getPlayer();
        if (player != null) {
            player.sendActionBar(component == null ? EMPTY : component);
        }
    }
}
