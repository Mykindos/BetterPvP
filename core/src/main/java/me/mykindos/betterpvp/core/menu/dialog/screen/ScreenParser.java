package me.mykindos.betterpvp.core.menu.dialog.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads screen, component and asset files. The format is described by {@code gui/schema/screen.schema.json}. Errors
 * name the file and the path of the element that is wrong.
 */
public final class ScreenParser {

    private final String namespace;
    private final String file;

    private ScreenParser(String namespace, String file) {
        this.namespace = namespace;
        this.file = file;
    }

    /** A screen file. Its id is the {@code id} property, or the file name without {@code .json}. */
    public static ScreenDefinition screen(String namespace, String file, String json) {
        return new ScreenParser(namespace, file).screen(JsonParser.parseString(json).getAsJsonObject());
    }

    /** A component file: {@code {"components": {"name": {"params": [...], "elements": [...]}}}}. */
    public static Map<String, ScreenDefinition.Component> components(String namespace, String file, String json) {
        final ScreenParser parser = new ScreenParser(namespace, file);
        final Map<String, ScreenDefinition.Component> components = new LinkedHashMap<>();
        final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object(root, "components").entrySet()) {
            components.put(entry.getKey(), parser.component(entry.getValue().getAsJsonObject(), "components." + entry.getKey()));
        }
        return components;
    }

    /**
     * An asset file for screens built in code: {@code {"canvas": {"width": 300}, "assets": [nodes]}}. It reads as a
     * screen whose elements are the declared assets, so the same collection rules apply.
     */
    public static ScreenDefinition assets(String namespace, String file, String json) {
        final ScreenParser parser = new ScreenParser(namespace, file);
        final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return ScreenDefinition.builder()
                .namespace(namespace)
                .id("assets/" + stem(file))
                .canvasWidth(object(root, "canvas").get("width").getAsInt())
                .elements(parser.nodes(array(root, "assets"), "assets"))
                .build();
    }

    private ScreenDefinition screen(JsonObject root) {
        final JsonObject canvas = object(root, "canvas");
        final ScreenDefinition.ScreenDefinitionBuilder builder = ScreenDefinition.builder()
                .namespace(namespace)
                .id(root.has("id") ? root.get("id").getAsString() : stem(file))
                .canvasWidth(canvas.get("width").getAsInt())
                .canvasHeight(canvas.get("height").getAsInt())
                .name(root.has("name") ? text(root.get("name")) : null)
                .columns(root.has("columns") ? root.get("columns").getAsInt() : 2)
                .escapable(!root.has("escapable") || root.get("escapable").getAsBoolean())
                .backdrop(nodes(array(root, "backdrop"), "backdrop"))
                .elements(nodes(array(root, "elements"), "elements"));
        for (Map.Entry<String, JsonElement> entry : object(root, "state").entrySet()) {
            builder.stateValue(entry.getKey(), value(entry.getValue()));
        }
        array(root, "actions").forEach(action -> builder.action(action.getAsString()));
        object(root, "sounds").entrySet().forEach(entry -> builder.sound(entry.getKey(), entry.getValue().getAsString()));
        for (Map.Entry<String, JsonElement> entry : object(root, "components").entrySet()) {
            builder.component(entry.getKey(), component(entry.getValue().getAsJsonObject(), "components." + entry.getKey()));
        }
        final JsonArray fields = array(root, "fields");
        for (int index = 0; index < fields.size(); index++) {
            builder.field(field(fields.get(index).getAsJsonObject(), "fields[" + index + "]"));
        }
        final JsonArray buttons = array(root, "buttons");
        for (int index = 0; index < buttons.size(); index++) {
            builder.button(nativeButton(buttons.get(index).getAsJsonObject(), "buttons[" + index + "]"));
        }
        if (root.has("exit")) {
            builder.exit(nativeButton(root.getAsJsonObject("exit"), "exit"));
        }
        return builder.build();
    }

    private ScreenDefinition.Component component(JsonObject json, String path) {
        final List<String> params = new ArrayList<>();
        array(json, "params").forEach(param -> params.add(param.getAsString()));
        return new ScreenDefinition.Component(List.copyOf(params), nodes(array(json, "elements"), path + ".elements"));
    }

    List<Node> nodes(JsonArray array, String path) {
        final List<Node> nodes = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            nodes.add(node(array.get(index).getAsJsonObject(), path + "[" + index + "]"));
        }
        return List.copyOf(nodes);
    }

    private Node node(JsonObject json, String path) {
        try {
            if (json.has("text")) {
                return Node.Text.builder()
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .text(text(json.get("text")))
                        .style(string(json, "style", "body"))
                        .align(Node.Align.valueOf(string(json, "align", "left").toUpperCase()))
                        .width(integer(json, "width"))
                        .tooltip(json.has("tooltip") ? text(json.get("tooltip")) : null)
                        .onClick(action(json.get("on_click")))
                        .build();
            }
            if (json.has("box")) {
                return Node.Box.builder()
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .width(integer(json, "width")).height(integer(json, "height"))
                        .style(json.get("box").getAsString())
                        .build();
            }
            if (json.has("button")) {
                return Node.Button.builder()
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .width(integer(json, "width")).height(integer(json, "height"))
                        .style(json.get("button").getAsString())
                        .selectedStyle(string(json, "selected_style", null))
                        .selected(string(json, "selected", null))
                        .label(json.has("label") ? text(json.get("label")) : null)
                        .labelStyle(string(json, "label_style", "body"))
                        .hover(string(json, "hover", null))
                        .pressed(json.has("pressed") && json.get("pressed").getAsBoolean())
                        .tooltip(json.has("tooltip") ? text(json.get("tooltip")) : null)
                        .onClick(action(json.get("on_click")))
                        .build();
            }
            if (json.has("icon")) {
                return Node.Icon.builder()
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .width(integer(json, "width")).height(integer(json, "height"))
                        .sprite(json.get("icon").getAsString())
                        .frames(integer(json, "frames"))
                        .fps(integer(json, "fps"))
                        .tooltip(json.has("tooltip") ? text(json.get("tooltip")) : null)
                        .onClick(action(json.get("on_click")))
                        .build();
            }
            for (String layout : List.of("row", "column", "grid")) {
                if (json.has(layout)) {
                    return group(layout, json.getAsJsonObject(layout))
                            .children(nodes(array(json, "children"), path + ".children"))
                            .build();
                }
            }
            if (json.has("repeat")) {
                final JsonObject layout = object(json, "layout");
                final String kind = layout.keySet().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("repeat needs a layout: row, column or grid"));
                return Node.Repeat.builder()
                        .list(json.get("repeat").getAsString())
                        .as(string(json, "as", "item"))
                        .max(integer(json, "max"))
                        .layout(group(kind, layout.getAsJsonObject(kind)).build())
                        .children(nodes(array(json, "children"), path + ".children"))
                        .build();
            }
            if (json.has("switch")) {
                final Map<String, List<Node>> cases = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : object(json, "cases").entrySet()) {
                    cases.put(entry.getKey(), nodes(entry.getValue().getAsJsonArray(), path + ".cases." + entry.getKey()));
                }
                return Node.Switch.builder()
                        .value(json.get("switch").getAsString())
                        .cases(cases)
                        .fallback(nodes(array(json, "default"), path + ".default"))
                        .build();
            }
            if (json.has("when")) {
                return Node.When.builder()
                        .condition(json.get("when").getAsString())
                        .then(nodes(array(json, "then"), path + ".then"))
                        .otherwise(nodes(array(json, "else"), path + ".else"))
                        .build();
            }
            if (json.has("use")) {
                final Map<String, String> with = new HashMap<>();
                object(json, "with").entrySet().forEach(entry -> with.put(entry.getKey(), entry.getValue().getAsString()));
                return Node.Use.builder()
                        .component(json.get("use").getAsString())
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .with(with)
                        .build();
            }
            if (json.has("custom")) {
                final Node.Custom.CustomBuilder builder = Node.Custom.builder()
                        .type(json.get("custom").getAsString())
                        .x(integer(json, "x")).y(integer(json, "y"))
                        .width(integer(json, "width")).height(integer(json, "height"))
                        .assets(nodes(array(json, "assets"), path + ".assets"));
                object(json, "properties").entrySet().forEach(entry -> builder.property(entry.getKey(), value(entry.getValue())));
                return builder.build();
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(file + " at " + path + ": " + e.getMessage(), e);
        }
        throw new IllegalArgumentException(file + " at " + path + ": unknown element " + json.keySet());
    }

    private static Node.Group.GroupBuilder group(String kind, JsonObject json) {
        final JsonArray cell = array(json, "cell");
        return Node.Group.builder()
                .layout(Node.Layout.valueOf(kind.toUpperCase()))
                .x(integer(json, "x")).y(integer(json, "y"))
                .cellWidth(cell.isEmpty() ? 0 : cell.get(0).getAsInt())
                .cellHeight(cell.size() < 2 ? 0 : cell.get(1).getAsInt())
                .gap(integer(json, "gap"))
                .columns(json.has("columns") ? json.get("columns").getAsInt() : 1);
    }

    private ScreenDefinition.Field field(JsonObject json, String path) {
        try {
            final ScreenDefinition.Field.FieldBuilder builder = ScreenDefinition.Field.builder()
                    .label(text(json.get("label")))
                    .width(json.has("width") ? json.get("width").getAsInt() : 200);
            if (json.has("text_field")) {
                return builder.kind(ScreenDefinition.Field.Kind.TEXT).bind(json.get("text_field").getAsString())
                        .maxLength(json.has("max_length") ? json.get("max_length").getAsInt() : 32).build();
            }
            if (json.has("toggle")) {
                return builder.kind(ScreenDefinition.Field.Kind.TOGGLE).bind(json.get("toggle").getAsString()).build();
            }
            if (json.has("slider")) {
                return builder.kind(ScreenDefinition.Field.Kind.SLIDER).bind(json.get("slider").getAsString())
                        .start(json.get("start").getAsFloat()).end(json.get("end").getAsFloat())
                        .step(json.has("step") ? json.get("step").getAsFloat() : 1).build();
            }
            if (json.has("choice")) {
                builder.kind(ScreenDefinition.Field.Kind.CHOICE).bind(json.get("choice").getAsString());
                array(json, "options").forEach(option -> builder.option(new ScreenDefinition.Option(
                        option.getAsJsonObject().get("id").getAsString(), text(option.getAsJsonObject().get("label")))));
                return builder.build();
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(file + " at " + path + ": " + e.getMessage(), e);
        }
        throw new IllegalArgumentException(file + " at " + path + ": unknown field " + json.keySet());
    }

    private ScreenDefinition.NativeButton nativeButton(JsonObject json, String path) {
        try {
            return ScreenDefinition.NativeButton.builder()
                    .label(text(json.get("label")))
                    .tooltip(json.has("tooltip") ? text(json.get("tooltip")) : null)
                    .width(json.has("width") ? json.get("width").getAsInt() : 150)
                    .onClick(action(json.get("on_click")))
                    .build();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(file + " at " + path + ": " + e.getMessage(), e);
        }
    }

    static TextSpec text(JsonElement json) {
        if (json.isJsonPrimitive()) {
            return TextSpec.of(json.getAsString());
        }
        final JsonObject object = json.getAsJsonObject();
        final List<String> args = new ArrayList<>();
        array(object, "args").forEach(arg -> args.add(arg.getAsString()));
        return new TextSpec(null, object.get("key").getAsString(), List.copyOf(args));
    }

    static ActionSpec action(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        }
        if (json.isJsonPrimitive()) {
            return ActionSpec.call(json.getAsString());
        }
        if (json.isJsonArray()) {
            final List<ActionSpec> actions = new ArrayList<>();
            json.getAsJsonArray().forEach(element -> actions.add(action(element)));
            return ActionSpec.sequence(actions);
        }
        final JsonObject object = json.getAsJsonObject();
        if (object.has("call")) {
            return new ActionSpec.Call(object.get("call").getAsString(), strings(object(object, "args")));
        }
        if (object.has("set")) {
            return new ActionSpec.Set(strings(object.getAsJsonObject("set")));
        }
        if (object.has("open")) {
            return new ActionSpec.Open(object.get("open").getAsString(), strings(object(object, "state")));
        }
        if (object.has("back")) {
            return new ActionSpec.Back();
        }
        if (object.has("close")) {
            return new ActionSpec.Close();
        }
        if (object.has("sound")) {
            return new ActionSpec.Sound(object.get("sound").getAsString());
        }
        throw new IllegalArgumentException("unknown action " + object.keySet());
    }

    private static Map<String, String> strings(JsonObject object) {
        final Map<String, String> values = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
        return values;
    }

    private static Object value(JsonElement json) {
        if (json.isJsonNull()) {
            return null;
        }
        if (json.isJsonPrimitive()) {
            final JsonPrimitive primitive = json.getAsJsonPrimitive();
            return primitive.isBoolean() ? primitive.getAsBoolean()
                    : primitive.isNumber() ? primitive.getAsDouble() : primitive.getAsString();
        }
        if (json.isJsonArray()) {
            final List<Object> list = new ArrayList<>();
            json.getAsJsonArray().forEach(element -> list.add(value(element)));
            return list;
        }
        final Map<String, Object> map = new LinkedHashMap<>();
        json.getAsJsonObject().entrySet().forEach(entry -> map.put(entry.getKey(), value(entry.getValue())));
        return map;
    }

    private static JsonObject object(JsonObject json, String key) {
        return json.has(key) ? json.getAsJsonObject(key) : new JsonObject();
    }

    private static JsonArray array(JsonObject json, String key) {
        return json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
    }

    private static int integer(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static String stem(String file) {
        final String name = file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }
}
