package me.mykindos.betterpvp.core.item.purity.bias;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;

import java.lang.reflect.Type;

/**
 * Gson deserializer for PurityReforgeBias objects from Supabase JSON.
 * <p>
 * Expected JSON format from Supabase:
 * <pre>
 * {
 *   "purity": "PITIFUL",
 *   "alpha": 0.3,
 *   "beta": 3.0,
 *   "notes": "Very harsh minimum bias - heavily favors worst stats"
 * }
 * </pre>
 * <p>
 * The "notes" field is optional and used only for database documentation.
 */
public class PurityReforgeBiasDeserializer implements JsonDeserializer<PurityReforgeBias> {

    @Override
    public PurityReforgeBias deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
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

        // Parse alpha and beta parameters
        double alpha = obj.get("alpha").getAsDouble();
        double beta = obj.get("beta").getAsDouble();

        // Validate parameters (beta distribution requires α > 0 and β > 0)
        if (alpha <= 0 || beta <= 0) {
            throw new JsonParseException(
                    "Invalid beta distribution parameters for " + purityName +
                            ": alpha=" + alpha + ", beta=" + beta + " (both must be > 0)"
            );
        }

        return new PurityReforgeBias(purity, alpha, beta);
    }
}
