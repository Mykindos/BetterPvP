package me.mykindos.betterpvp.core.resourcepack;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * The font and character that draw one glyph in the released packs.
 */
@Value
public class ReleasedGlyph {

    String font;
    @SerializedName("char")
    String character;

}
