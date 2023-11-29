package me.mykindos.betterpvp.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.serialization.impl.ClientDeserializer;
import me.mykindos.betterpvp.core.serialization.impl.ClientSerializer;

@UtilityClass
public class Serialization {

    // Add custom de/serializers here
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Client.class, new ClientSerializer())
            .registerTypeAdapter(Client.class, new ClientDeserializer())
            .create();

}
