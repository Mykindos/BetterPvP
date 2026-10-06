package me.mykindos.betterpvp.core.menu.dialog.screen;

import me.mykindos.betterpvp.core.menu.dialog.CanvasElement;
import me.mykindos.betterpvp.core.menu.dialog.DialogClick;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code core:text_icons}: a line of text with clickable icons right after it, wherever the text ends, such as a
 * title followed by its rename and reset buttons. Properties:
 * <ul>
 *   <li>{@code text}: a template, {@code style}: its text style (default {@code body}), {@code text_y}: its top</li>
 *   <li>{@code gap}: pixels between the text and the first icon, {@code spacing}: between icons</li>
 *   <li>{@code icons}: a list of {@code {sprite, width, height, y, tooltip, action}}, where tooltip is a translation
 *   key and action names a bound or registered action</li>
 * </ul>
 * Every sprite must also be listed in the node's {@code assets}.
 */
public class TextIconsElement implements ElementType {

    @Override
    public List<String> validate(Node.Custom node) {
        final List<String> problems = new ArrayList<>();
        if (!(node.getProperties().get("text") instanceof String)) {
            problems.add("needs a text template");
        }
        for (Map<?, ?> icon : icons(node)) {
            if (!(icon.get("sprite") instanceof String) || !(icon.get("width") instanceof Number) || !(icon.get("height") instanceof Number)) {
                problems.add("every icon needs a sprite, width and height");
            }
        }
        return problems;
    }

    @Override
    public void render(Node.Custom node, RenderContext context) {
        final Map<String, Object> properties = node.getProperties();
        final String style = (String) properties.getOrDefault("style", "body");
        final Component text = context.styled(TextSpec.of((String) properties.get("text")), style);
        final int textWidth = UtilFont.componentWidth(text) - 1;
        context.text(0, number(properties, "text_y"), text, Node.Align.LEFT, textWidth);

        int x = textWidth + number(properties, "gap");
        for (Map<?, ?> icon : icons(node)) {
            final int width = ((Number) icon.get("width")).intValue();
            final String asset = ScreenAssets.sprite((String) icon.get("sprite"), width, ((Number) icon.get("height")).intValue(), 0, 0);
            final int y = icon.get("y") instanceof Number number ? number.intValue() : 0;
            for (CanvasElement placed : context.art(asset, x, y)) {
                if (icon.get("tooltip") instanceof String key) {
                    placed.tooltip(context.text(TextSpec.key(key)));
                }
                if (icon.get("action") instanceof String action) {
                    final DialogClick click = context.click(new ActionSpec.Call(action, Map.of()));
                    if (click != null) {
                        placed.onClick(click);
                    }
                }
            }
            x += width + number(properties, "spacing");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<?, ?>> icons(Node.Custom node) {
        return node.getProperties().get("icons") instanceof List<?> list ? (List<Map<?, ?>>) list : List.of();
    }

    private static int number(Map<String, Object> properties, String key) {
        return properties.get(key) instanceof Number number ? number.intValue() : 0;
    }
}
