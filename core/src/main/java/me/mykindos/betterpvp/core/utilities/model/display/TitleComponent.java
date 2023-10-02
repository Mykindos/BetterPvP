package me.mykindos.betterpvp.core.utilities.model.display;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

@Getter
public class TitleComponent extends TimedComponent {

    protected boolean subtitle = false;
    private final Function<Gamer, Component> subtitleProvider;
    private final double fadeIn;
    private final double fadeOut;

    /**
     * @param fadeIn       The amount of seconds to fade in the component.
     * @param seconds      The amount of seconds to show the component for.
     * @param fadeOut      The amount of seconds to fade out the component.
     * @param waitToExpire Whether to wait for the component to show first before allowing it to start expiring.
     * @param provider     The component to show. Return null to not display anything and skip this component.
     * @param subtitleProvider The subtitle to show. Return null to not display anything and skip this component.
     */
    public TitleComponent(double fadeIn, double seconds, double fadeOut, boolean waitToExpire, Function<Gamer, Component> provider, Function<Gamer, Component> subtitleProvider) {
        super(fadeIn + seconds + fadeOut, waitToExpire, provider);
        Preconditions.checkNotNull(subtitleProvider, "subtitleProvider");
        this.subtitleProvider = subtitleProvider;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
    }

    public static TitleComponent title(double fadeIn, double seconds, double fadeOut, boolean waitToExpire, Function<Gamer, Component> provider) {
        return new TitleComponent(fadeIn, seconds, fadeOut, waitToExpire, provider, gamer -> null);
    }

    public static TitleComponent subtitle(double fadeIn, double seconds, double fadeOut, boolean waitToExpire, Function<Gamer, Component> subtitleProvider) {
        return new TitleComponent(fadeIn, seconds, fadeOut, waitToExpire, gamer -> null, subtitleProvider);
    }

    public void sendPlayer(@NotNull Player player, @NotNull Gamer gamer) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(!hasStarted() ? (long) (fadeIn * 1000L) : 0),
                Duration.ofMillis(!hasStarted() ? (long) ((getSeconds() - fadeIn - fadeOut) * 1000L) : getRemaining()),
                Duration.ofMillis(!hasStarted() ? (long) (fadeOut * 1000L) : Math.min(getRemaining(), (long) (fadeOut * 1000L)))
        );

        Component title = Optional.ofNullable(getProvider().apply(gamer)).orElse(TitleQueue.EMPTY);
        Component subtitle = Optional.ofNullable(getSubtitleProvider().apply(gamer)).orElse(TitleQueue.EMPTY);

        player.sendTitlePart(TitlePart.TIMES, times);
        player.sendTitlePart(TitlePart.TITLE, title);
        player.sendTitlePart(TitlePart.SUBTITLE, subtitle);
    }

}
