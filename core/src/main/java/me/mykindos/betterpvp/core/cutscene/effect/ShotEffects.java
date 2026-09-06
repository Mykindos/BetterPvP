package me.mykindos.betterpvp.core.cutscene.effect;

import me.mykindos.betterpvp.core.cutscene.CutsceneSession;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The things a shot can make true while it is on screen.
 * <p>
 * Every one of these is per-viewer, because a session belongs to one player - a pointer hung over an NPC during one
 * player's tutorial is not something anyone else should see standing next to them.
 *
 * @see ShotEffect
 */
public final class ShotEffects {

    private static final Duration SUBTITLE_FADE = Duration.ofMillis(300);

    private ShotEffects() {
    }

    /** Runs something when the shot begins. */
    public static @NotNull ShotEffect run(@NotNull Consumer<Player> onEnter) {
        return run(onEnter, player -> {
        });
    }

    /**
     * Runs something when the shot begins and something else when it ends.
     * <p>
     * The exit half is guaranteed to run for any shot whose entry ran - on a normal beat change, on a skip, and on an
     * abort - so this is the safe way to hold state across a shot.
     */
    public static @NotNull ShotEffect run(@NotNull Consumer<Player> onEnter, @NotNull Consumer<Player> onExit) {
        return new ShotEffect() {
            @Override
            public void enter(@NotNull CutsceneSession session) {
                final Player player = session.getPlayer();
                if (player != null) {
                    onEnter.accept(player);
                }
            }

            @Override
            public void exit(@NotNull CutsceneSession session) {
                final Player player = session.getPlayer();
                if (player != null) {
                    onExit.accept(player);
                }
            }
        };
    }

    /**
     * Holds a subtitle for as long as the shot lasts.
     * <p>
     * The stay time is deliberately longer than any plausible shot and the title is cleared on exit, because a title
     * that expires on its own would blink out mid-shot and one that outlives its shot would bleed into the next.
     */
    public static @NotNull ShotEffect subtitle(@NotNull Component text) {
        return run(
                player -> player.showTitle(Title.title(Component.empty(), text,
                        Title.Times.times(SUBTITLE_FADE, Duration.ofMinutes(10), SUBTITLE_FADE))),
                Player::clearTitle);
    }

    /** Plays a sound as the shot begins. */
    public static @NotNull ShotEffect sound(@NotNull SoundEffect sound) {
        return run(sound::play);
    }

    /**
     * Hangs a model over something for the length of the shot, shown to this viewer alone.
     *
     * @param target where to hang it, re-resolved every tick so it can follow something that moves
     * @param model  what to show; any item, so a custom model works
     */
    public static @NotNull ShotEffect pointer(@NotNull Supplier<Location> target, @NotNull ItemStack model) {
        return new PointerEffect(target, model, 2.4);
    }

    /** @see #pointer(Supplier, ItemStack) */
    public static @NotNull ShotEffect pointer(@NotNull Location target, @NotNull ItemStack model) {
        return pointer(() -> target, model);
    }
}
