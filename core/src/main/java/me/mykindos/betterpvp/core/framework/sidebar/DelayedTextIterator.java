package me.mykindos.betterpvp.core.framework.sidebar;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import me.catcoder.sidebar.text.FrameIterator;
import me.catcoder.sidebar.text.TextFrame;
import me.catcoder.sidebar.text.TextIterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.function.Consumer;

public class DelayedTextIterator extends TextIterator {

    private final FrameIterator frameIterator;

    public DelayedTextIterator(@NonNull String text, @NonNull TextColor primaryColor, @NonNull TextColor fadeColor, @NonNull TextColor secondaryColor) {
        Preconditions.checkNotNull(text, "text is marked non-null but is null");
        Preconditions.checkNotNull(primaryColor, "primaryColor is marked non-null but is null");
        Preconditions.checkNotNull(fadeColor, "fadeColor is marked non-null but is null");
        Preconditions.checkNotNull(secondaryColor, "secondaryColor is marked non-null but is null");
        Preconditions.checkArgument(text.length() >= 3, "Text length must be at least 3 characters");

        text = text.trim();
        final ArrayList<TextFrame> frames = new ArrayList<>();
        frames.add(TextFrame.of(toString(Component.text(text, primaryColor, TextDecoration.BOLD)), 60L));

        for (int i = 0; i <= text.length(); i++) {
            final TextComponent.Builder builder = Component.text();
            if (i < text.length() && text.charAt(i) == ' ') {
                continue;
            }

            if (i - 1 >= 0) {
                builder.append(Component.text(text.substring(0, i - 1), primaryColor, TextDecoration.BOLD));
                builder.append(Component.text(text.charAt(i - 1), secondaryColor, TextDecoration.BOLD));
            }

            if (i < text.length()) {
                builder.append(Component.text(text.charAt(i), fadeColor, TextDecoration.BOLD));
            }

            if (i + 1 < text.length()) {
                builder.append(Component.text(text.substring(i + 1), primaryColor, TextDecoration.BOLD));
            }

            frames.add(TextFrame.of(toString(builder.build()), 4L));
        }

        this.frameIterator = new FrameIterator(frames);
    }

    private String toString(final TextComponent component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public String next() {
        if (!this.frameIterator.hasNext()) {
            return "";
        }

        return this.frameIterator.next();
    }

    @Override
    public boolean hasNext() {
        return this.frameIterator.hasNext();
    }

    @Override
    public void remove() {
        this.frameIterator.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super String> action) {
        this.frameIterator.forEachRemaining(action);
    }

}
