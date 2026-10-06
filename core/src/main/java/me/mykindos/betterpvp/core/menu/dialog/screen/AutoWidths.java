package me.mykindos.betterpvp.core.menu.dialog.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.key.Key;

import java.util.List;
import java.util.Map;

/**
 * Turns {@code "width": "auto"} on buttons into a number before a screen file is parsed, for one language: the label's
 * width in the menu font, plus {@code padding} on each side (6 by default) and the 1 px outline on each side. A row
 * whose cell width is {@code "auto"} places its children one after another, {@code gap} apart. The pack generator
 * resolves the same files the same way ({@code build_gui.py}), so both draw the same art.
 */
final class AutoWidths {

    static final int DEFAULT_PADDING = 6;
    private static final Key MENU_FONT = Key.key("betterpvp", "rpg");

    /** Translations a label comes from, first match wins: the language, then English, each namespace before core. */
    private final List<Map<String, String>> translations;

    AutoWidths(List<Map<String, String>> translations) {
        this.translations = translations;
    }

    void resolve(JsonElement element) {
        if (element instanceof JsonArray array) {
            array.forEach(this::resolve);
        } else if (element instanceof JsonObject object) {
            object.entrySet().forEach(entry -> resolve(entry.getValue()));
            if (object.has("button") && isAuto(object.get("width"))) {
                object.addProperty("width", buttonWidth(object));
            }
            if (object.has("row") && object.get("row").getAsJsonObject().has("cell")
                    && isAuto(object.getAsJsonObject("row").getAsJsonArray("cell").get(0))) {
                flow(object);
            }
        }
    }

    private int buttonWidth(JsonObject button) {
        final JsonElement label = button.get("label");
        if (label == null) {
            throw new IllegalArgumentException("A button with an auto width needs a label");
        }
        final int padding = button.has("padding") ? button.get("padding").getAsInt() : DEFAULT_PADDING;
        return labelWidth(label) + 2 * padding + 2;
    }

    private int labelWidth(JsonElement label) {
        if (label.isJsonPrimitive()) {
            final String text = label.getAsString();
            if (text.contains("{")) {
                throw new IllegalArgumentException("A button with an auto width needs a fixed label, not " + text);
            }
            return UtilFont.textWidth(text, MENU_FONT);
        }
        final JsonObject spec = label.getAsJsonObject();
        if (spec.has("args")) {
            throw new IllegalArgumentException("A button with an auto width needs a label without arguments");
        }
        final String key = spec.get("key").getAsString();
        for (Map<String, String> table : translations) {
            final String text = table.get(key);
            if (text != null) {
                return UtilFont.textWidth(text.replace("''", "'"), MENU_FONT);
            }
        }
        throw new IllegalArgumentException("No translation of " + key + " to size an auto width button by");
    }

    private static void flow(JsonObject group) {
        final JsonObject row = group.getAsJsonObject("row");
        final JsonArray cell = row.getAsJsonArray("cell");
        final int gap = row.has("gap") ? row.get("gap").getAsInt() : 0;
        int x = 0;
        for (JsonElement child : group.getAsJsonArray("children")) {
            final JsonObject node = child.getAsJsonObject();
            node.addProperty("x", x + (node.has("x") ? node.get("x").getAsInt() : 0));
            x += node.get("width").getAsInt() + gap;
        }
        cell.set(0, new JsonPrimitive(0));
        row.addProperty("gap", 0);
    }

    private static boolean isAuto(JsonElement element) {
        return element != null && element.isJsonPrimitive() && "auto".equals(element.getAsString());
    }
}
