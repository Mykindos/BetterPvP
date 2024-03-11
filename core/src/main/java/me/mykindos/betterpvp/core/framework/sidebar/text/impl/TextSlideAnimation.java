package me.mykindos.betterpvp.core.framework.sidebar.text.impl;

import lombok.NonNull;
import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.framework.sidebar.text.FrameIterator;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextFrame;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextIterator;

import java.util.List;

/**
 * Text sliding animation. WIP
 *
 * @author CatCoder
 */
public class TextSlideAnimation extends TextIterator {

    private final String text;

    @Delegate(types = {FrameIterator.class})
    private final FrameIterator frameIterator;

    public TextSlideAnimation(@NonNull String text) {
        this.text = text;
        this.frameIterator = new FrameIterator(createAnimationFrames());
    }

    private List<TextFrame> createAnimationFrames() {
        // TODO: 17.11.2022 contributions are welcome :)
        throw new UnsupportedOperationException("Unsupported yet");
    }

}
