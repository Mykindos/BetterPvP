package me.mykindos.betterpvp.core.serialization.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.serialization.Serialization;

import java.lang.reflect.Type;
import java.util.Objects;

public class ClientDeserializer implements JsonDeserializer<Client> {

    @Override
    public Client deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject object = json.getAsJsonObject();
        final String uuid = object.get("uuid").getAsString();
        final String name = object.get("name").getAsString();
        final Rank rank = Objects.requireNonNull(Rank.getRank(object.get("rank").getAsInt()));
        final boolean administrating = object.get("administrating").getAsBoolean();
        final JsonObject properties = object.get("properties").getAsJsonObject();

        final Gamer gamer = new Gamer(uuid);
        final Client client = new Client(gamer, uuid, name, rank);
        client.setAdministrating(administrating);
        setProperties(client, properties);

        return client;
    }

    @SneakyThrows
    private void setProperties(Client client, JsonObject properties) {
        final JsonArray array = properties.getAsJsonArray();
        for (JsonElement element : array) {
            final JsonObject object = element.getAsJsonObject();
            final String type = object.get("type").getAsString();
            final String name = object.get("name").getAsString();
            final String valueRaw = object.get("value").getAsString();

            Class<?> clazz = Class.forName(type);
            final Object value = Serialization.GSON.fromJson(valueRaw, clazz);

            client.putProperty(name, value, true);
        }
    }
}
