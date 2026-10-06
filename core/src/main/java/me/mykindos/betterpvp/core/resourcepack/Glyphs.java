package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Glyphs are art drawn inside text: menu backgrounds, sidebar and chat icons. Each id resolves to a character in the
 * font the current release draws it with, so text always matches the packs players have.
 */
@CustomLog
@Singleton
@BPvPListener
public class Glyphs implements Listener {

    private static volatile Map<String, ReleasedGlyph> glyphs = Map.of();
    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onReleaseChanged(PackReleaseChangedEvent event) {
        final Map<String, ReleasedGlyph> released = event.getCurrent().getGlyphs();
        glyphs = released == null ? Map.of() : Map.copyOf(released);
        reported.clear();
    }

    /**
     * The glyph drawn in white, so the art keeps its colors. Empty when the release has no such glyph.
     */
    public static Component glyph(String id) {
        final ReleasedGlyph glyph = glyphs.get(id);
        if (glyph == null) {
            if (!glyphs.isEmpty() && reported.add(id)) {
                log.warn("The resource pack release has no glyph {}", id).submit();
            }
            return Component.empty();
        }
        return Component.text(glyph.getCharacter(), NamedTextColor.WHITE).font(Key.key(glyph.getFont()));
    }

    /**
     * Moves the text cursor by a number of pixels, left when negative.
     */
    public static Component shift(int pixels) {
        return Component.translatable("space." + pixels).font(Resources.Font.SPACE);
    }

}
