package me.mykindos.betterpvp.core.item.purity.distribution;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Gson deserializer for PurityDistribution objects from Supabase JSON.
 * Expected JSON format:
 * <code>
 *  {
 *   "distribution_name": "default",
 *    "weights": {
 *      "PITIFUL": 5,
 *      "FRAGILE": 25,
 *      "MODERATE": 40,
 *      ...
 *    }
 *   }
 * </code>
 */
public class PurityDistributionDeserializer implements JsonDeserializer<PurityDistribution> {

    @Override
    public PurityDistribution deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        // Get distribution name
        String name = obj.get("distribution_name").getAsString();

        // Parse weights
        JsonObject weightsObj = obj.getAsJsonObject("weights");
        Map<ItemPurity, Integer> weights = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : weightsObj.entrySet()) {
            try {
                ItemPurity purity = ItemPurity.valueOf(entry.getKey());
                int weight = entry.getValue().getAsInt();
                weights.put(purity, weight);
            } catch (IllegalArgumentException e) {
                // Skip invalid purity names
                System.err.println("Warning: Invalid purity name in distribution '" + name + "': " + entry.getKey());
            }
        }

        return new PurityDistribution(name, weights);
    }
}
