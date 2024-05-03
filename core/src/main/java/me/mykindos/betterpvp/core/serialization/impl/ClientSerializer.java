package me.mykindos.betterpvp.core.serialization.impl;

import com.google.gson.*;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.customtypes.MyConcurrentHashMap;
import me.mykindos.betterpvp.core.serialization.Serialization;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSerializer implements JsonSerializer<Client> {
    @Override
    public JsonElement serialize(Client src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", src.getUuid());
        object.addProperty("name", src.getName());
        object.addProperty("rank", src.getRank().getId());
        object.addProperty("administrating", src.isAdministrating());
        object.add("properties", getProperties(src.getProperties()));
        return object;
    }

    private JsonElement getProperties(MyConcurrentHashMap<String, Object> properties) {
        final ConcurrentHashMap<String, Object> map = properties.getMap();
        JsonArray object = new JsonArray();
        map.keySet().forEach(key -> {
            JsonObject property = new JsonObject();
            final Object value = map.get(key);
            final Class<?> type = value.getClass();
            property.addProperty("type", type.getName());
            property.addProperty("name", key);
            property.addProperty("value", Serialization.GSON.toJson(value, type));
            object.add(property);
        });
        return object;
    }
}
