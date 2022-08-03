package me.mykindos.betterpvp.lunar.nethandler;

import com.google.gson.*;
import me.mykindos.betterpvp.lunar.nethandler.client.obj.ModSettings;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ModSettingsAdapter implements JsonDeserializer<ModSettings>, JsonSerializer<ModSettings> {
    @Override
    public ModSettings deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        ModSettings settings = new ModSettings();
        if (!jsonElement.isJsonObject()) {
            return settings;
        }

        JsonObject object = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject modSettingObject = entry.getValue().getAsJsonObject();

            settings.getModSettings().put(entry.getKey(), deserializeModSetting(modSettingObject));
        }
        return settings;
    }

    @Override
    public JsonElement serialize(ModSettings modSettings, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, ModSettings.ModSetting> entry : modSettings.getModSettings().entrySet()) {
            object.add(entry.getKey(), serializeModSetting(entry.getValue()));
        }
        return object;
    }

    private JsonObject serializeModSetting(ModSettings.ModSetting setting) {
        JsonObject object = new JsonObject();
        JsonObject properties = new JsonObject();
        object.addProperty("enabled", setting.isEnabled());
        for (Map.Entry<String, Object> entry : setting.getProperties().entrySet()) {

            JsonPrimitive primitive;
            if (entry.getValue() instanceof Boolean) {
                primitive = new JsonPrimitive((Boolean) entry.getValue());
            } else if (entry.getValue() instanceof String) {
                primitive = new JsonPrimitive((String) entry.getValue());
            } else if (entry.getValue() instanceof Number) {
                primitive = new JsonPrimitive((Number) entry.getValue());
            } else {
                continue;
            }
            properties.add(entry.getKey(), primitive);
        }
        object.add("properties", properties);
        return object;
    }

    private ModSettings.ModSetting deserializeModSetting(JsonObject object) {
        JsonObject propertiesObject = object.get("properties").getAsJsonObject();
        Map<String, Object> properties = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            JsonPrimitive primitive = entry.getValue().getAsJsonPrimitive();
            Object toSet;
            if (primitive.isString()) {
                toSet = primitive.getAsString();
            } else if (primitive.isNumber()) {
                toSet = primitive.getAsNumber();
            } else if (primitive.isBoolean()) {
                toSet = primitive.getAsBoolean();
            } else {
                continue;
            }
            properties.put(entry.getKey(), toSet);
        }
        return new ModSettings.ModSetting(object.get("enabled").getAsBoolean(), properties);
    }

}
