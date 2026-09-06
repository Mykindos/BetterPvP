package me.mykindos.betterpvp.core.cutscene.hud;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * The bottom of the screen for the length of a cutscene: the lower letterbox bar, with any dialogue composited into it.
 * <p>
 * It composites rather than simply outranking the conversation's own override, which is the whole trick. Winning the
 * priority stack would suppress the dialogue entirely - the override stack draws only its top entry - so the cutscene
 * takes the surface and paints the conversation onto it instead.
 */
public class CutsceneActionBar extends ActionBar {

    private final Function<Gamer, Component> renderer;

    public CutsceneActionBar(@NotNull Function<Gamer, Component> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void show(Gamer gamer) {
        final Player player = gamer.getPlayer();
        if (player == null) {
            return;
        }
        final Component component = renderer.apply(gamer);
        player.sendActionBar(component == null ? EMPTY : component);
    }
}
