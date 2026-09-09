package me.mykindos.betterpvp.clans.world.sailing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Titles, sounds, effects and messages shown during a voyage.
 * <p>
 * Every method takes a player and already-resolved text rather than a {@link Voyage}, so this holds no voyage state and
 * callers decide when each cue fires. Changing how a voyage presents itself is a change to this class only.
 */
@Singleton
public class VoyageCues {

    /** Blindness duration on departure and arrival, long enough to cover the world change. */
    private static final long BLINDNESS_MILLIS = 3000L;

    /** Above ordinary messages, below anything urgent. */
    private static final int TITLE_PRIORITY = 5;

    /** The lowest priority available, so the voyage clock never hides a cooldown or a warning. */
    private static final int ACTION_BAR_PRIORITY = 1000;

    private final EffectManager effects;
    private final ClientManager clientManager;

    @Inject
    public VoyageCues(@NotNull EffectManager effects, @NotNull ClientManager clientManager) {
        this.effects = effects;
        this.clientManager = clientManager;
    }

    /** Shown when a voyage starts and the player is teleported out to the staging world. */
    public void castOff(@NotNull Player player, @NotNull Component destination) {
        effects.addEffect(player, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(player, Translations.component("clans.voyage.title.departing"),
                destination.color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_WATER, 0.7f, 1.0f).play(player);
        new SoundEffect(Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 0.6f, 0.8f).play(player);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.5f).play(player);
    }

    /** Shown when a voyage ends and the player is teleported to the destination site. */
    public void landfall(@NotNull Player player, @NotNull Component destination) {
        effects.addEffect(player, EffectTypes.BLINDNESS, 1, BLINDNESS_MILLIS);
        title(player, destination.color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Translations.component("clans.voyage.title.landfall").color(NamedTextColor.GRAY));

        new SoundEffect(Sound.ENTITY_BOAT_PADDLE_LAND, 0.8f, 1.0f).play(player);
        new SoundEffect(Sound.AMBIENT_UNDERWATER_EXIT, 1.0f, 0.6f).play(player);
    }

    /**
     * Elapsed voyage time on the action bar, refreshed once per second by the caller.
     * <p>
     * Arrival is a repeated random roll rather than a fixed duration, so this counts up from the start instead of down
     * to a time it cannot predict.
     */
    public void clock(@NotNull Player player, @NotNull Component destination, long elapsedSeconds) {
        final Component name = destination.color(NamedTextColor.AQUA);
        final Component elapsed = Component.text(
                String.format("%d:%02d", elapsedSeconds / 60, elapsedSeconds % 60), NamedTextColor.WHITE);

        gamer(player).ifPresent(gamer -> gamer.getActionBar().add(ACTION_BAR_PRIORITY,
                new TimedComponent(1.5, false,
                        gmr -> Translations.component("clans.voyage.actionbar", name, elapsed))));
    }

    /** Shown when a player left the ship in the staging world and was teleported back onto it. */
    public void hauledAboard(@NotNull Player player) {
        UtilMessage.message(player, "clans.prefix.ship", "clans.voyage.hauled-aboard");
        new SoundEffect(Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f).play(player);
    }

    /** Shown when a voyage could not start, leaving the player where they were. */
    public void castOffFailed(@NotNull Player player) {
        UtilMessage.message(player, "clans.prefix.ship", "clans.voyage.failed");
    }

    /** Shown when the destination refused the arrival and the player is being returned to their origin. */
    public void noHarbour(@NotNull Player player) {
        UtilMessage.message(player, "clans.prefix.ship", "clans.voyage.no-harbour");
    }

    /** Shown when a player was moved out of the staging world part way through, ending their voyage. */
    public void leftMidCrossing(@NotNull Player player) {
        UtilMessage.message(player, "clans.prefix.ship", "clans.voyage.left");
    }

    private void title(@NotNull Player player, @NotNull Component heading, @NotNull Component sub) {
        gamer(player).ifPresent(gamer -> gamer.getTitleQueue().add(TITLE_PRIORITY,
                new TitleComponent(0.2, 1.6, 0.4, false, gmr -> heading, gmr -> sub)));
    }

    private @NotNull Optional<Gamer> gamer(@NotNull Player player) {
        return Optional.ofNullable(clientManager.search().online(player)).map(client -> client.getGamer());
    }
}
