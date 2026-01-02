package me.mykindos.betterpvp.core.item.runeslot;

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
 * Gson deserializer for RuneSlotDistribution objects from Supabase JSON.
 * <p>
 * Expected JSON format from Supabase:
 * <pre>
 * {
 *   "purity": "PERFECT",
 *   "socket_weights": {
 *     "0": 1,
 *     "1": 2,
 *     "2": 10,
 *     "3": 35,
 *     "4": 52
 *   },
 *   "max_socket_weights": {
 *     "0": 1,
 *     "1": 2,
 *     "2": 7,
 *     "3": 25,
 *     "4": 65
 *   },
 *   "notes": "Perfect purity - heavily favors 3-4 sockets"
 * }
 * </pre>
 * <p>
 * The "notes" field is optional and used only for database documentation.
 */
public class RuneSlotDistributionDeserializer implements JsonDeserializer<RuneSlotDistribution> {

    @Override
    public RuneSlotDistribution deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        // Parse purity enum
        String purityName = obj.get("purity").getAsString();
        ItemPurity purity;
        try {
            purity = ItemPurity.valueOf(purityName);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid purity name: " + purityName, e);
        }

        // Parse socket_weights
        Map<Integer, Integer> socketWeights = parseWeightMap(
            obj.getAsJsonObject("socket_weights"),
            "socket_weights"
        );

        // Parse max_socket_weights
        Map<Integer, Integer> maxSocketWeights = parseWeightMap(
            obj.getAsJsonObject("max_socket_weights"),
            "max_socket_weights"
        );

        return new RuneSlotDistribution(purity, socketWeights, maxSocketWeights);
    }

    /**
     * Parses a weight map from a JSON object.
     *
     * @param weightsObj The JSON object containing weights
     * @param fieldName Name for error messages
     * @return Map of slot counts to weights
     * @throws JsonParseException if parsing fails
     */
    private Map<Integer, Integer> parseWeightMap(JsonObject weightsObj, String fieldName)
            throws JsonParseException {
        if (weightsObj == null) {
            throw new JsonParseException("Missing required field: " + fieldName);
        }

        Map<Integer, Integer> weights = new HashMap<>();

        for (String key : weightsObj.keySet()) {
            try {
                int slotCount = Integer.parseInt(key);
                int weight = weightsObj.get(key).getAsInt();

                if (slotCount < 0 || slotCount > 4) {
                    throw new JsonParseException(
                        fieldName + " key must be 0-4, got: " + slotCount
                    );
                }

                if (weight < 0) {
                    throw new JsonParseException(
                        fieldName + " weight cannot be negative: " + weight
                    );
                }

                weights.put(slotCount, weight);
            } catch (NumberFormatException e) {
                throw new JsonParseException(
                    "Invalid " + fieldName + " key (must be integer): " + key, e
                );
            }
        }

        if (weights.isEmpty()) {
            throw new JsonParseException(fieldName + " cannot be empty");
        }

        return weights;
    }
}
