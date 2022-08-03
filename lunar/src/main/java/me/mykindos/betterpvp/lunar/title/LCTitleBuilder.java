package me.mykindos.betterpvp.lunar.title;

import me.mykindos.betterpvp.lunar.LunarClientAPI;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketTitle;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class LCTitleBuilder {

    /**
     * Create a LCTitle by type and message.
     * The message is final, but the type can be changed
     * until the packet is built.
     *
     * @param message The message that will be displayed.
     * @param type The {@link TitleType} to send to the client
     * @return A new title builder.
     */
    public static LCTitleBuilder of(String message, TitleType type) {
        return new LCTitleBuilder(message, type);
    }

    /**
     * Create a LCTitle with a message.
     * The type defaults to a title.
     *
     * @param message The message to display to the user.
     * @return The new title builder.
     */
    public static LCTitleBuilder of(String message) {
        return of(message, TitleType.TITLE);
    }

    private final String message;
    private TitleType type;
    private float scale;
    private Duration displayDuration;
    private Duration fadeInDuration;
    private Duration fadeOutDuration;

    /**
     * Starts a new builder with default params.
     * If nothing else is specified then it will
     * default to a title with your message that lasts
     * 1.5 seconds and fades in and out for .5 seconds.
     *
     * @param message The final message specified to start the builder.
     * @param type The type to start the builder.
     */
    private LCTitleBuilder(String message, TitleType type) {
        this.message = message;
        this.type = type;
        this.scale = 1;
        this.displayDuration = Duration.ofMillis(1500);
        this.fadeInDuration = Duration.ofMillis(500);
        this.fadeOutDuration = Duration.ofMillis(500);
    }

    /**
     * Specify the {@link TitleType} for the title to be displayed.
     * @param type The type of type to display.
     * @return This builder with the updated value.
     */
    public LCTitleBuilder type(TitleType type) {
        this.type = type;
        return this;
    }

    /**
     * Scale up or down the title sent to the client.
     *
     * @param scale The new scale (greater than 0) that the message will be displayed on.
     * @return This builder with the updated value.
     */
    public LCTitleBuilder scale(float scale) {
        this.scale = scale;
        return this;
    }

    /**
     * Specify the display time for which this
     * title will be displayed to the user.
     *
     * The title will display for fade in + DISPLAY TIME + fade out = total display time.
     *
     * @param duration The {@link Duration} of how long to display the title for.
     * @return This builder with the updated value.
     */
    public LCTitleBuilder displayFor(Duration duration) {
        this.displayDuration = duration;
        return this;
    }

    /**
     * Specify the amount of time it takes
     * the title to fade in for the user.
     *
     * The title will display for FADE IN + duration + fade out = total display time.
     *
     * @param duration The {@link Duration} of how long to fade in the title for.
     * @return This builder with the updated value.
     */
    public LCTitleBuilder fadeInFor(Duration duration) {
        this.fadeInDuration = duration;
        return this;
    }

    /**
     * Specify the amount of time it takes
     * the title to fade out for the user.
     *
     * The title will display for fade in + duration + FADE OUT = total display time.
     *
     * @param duration The {@link Duration} of how long to fade in the title for.
     * @return This builder with the updated value.
     */
    public LCTitleBuilder fadeOutFor(Duration duration) {
        this.fadeOutDuration = duration;
        return this;
    }


    /**
     * Take all the inputs from the builder and convert
     * it into a single LCPacketTitle you can use to
     * save and send to players as needed.
     *
     * Ideally if possible you would build the title packet once
     * and then send it to all the players that can see it.
     * A good example of this would be a welcome title, where
     * its entirely static but needs to be sent to all users.
     *
     * @return The {@link LCPacketTitle} to send to Lunar Client users as needed.
     */
    public LCPacketTitle build() {
        return new LCPacketTitle(type.name().toLowerCase(), message, scale, displayDuration.toMillis(), fadeInDuration.toMillis(), fadeOutDuration.toMillis());
    }

    /**
     * Builds the current packet and sends it to all players required.
     *
     * A good use case for this would be if all players online need
     * to see the same message, at the same time and it won't be sent again.
     * Like an announcement for an objective in a mini-game.
     *
     * @param players All the {@link Player} that need to see the title.
     * @return The {@link LCPacketTitle} generated that can be used later if needed.
     */
    public LCPacketTitle sendAndBuild(Player... players) {
        LCPacketTitle title = build();
        for (Player player : players) {
            LunarClientAPI.getInstance().sendPacket(player, title);
        }
        return title;
    }
}
