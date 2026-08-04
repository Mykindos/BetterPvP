package me.mykindos.betterpvp.core.utilities;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Value;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a model is tinted: one colour for the whole thing, a colour per bone, or both.
 * <p>
 * Written as a comma-separated list of {@code <colour>} and {@code <bone>=<colour>} entries. A bare colour is the
 * default every bone takes; a named bone overrides it. So a lantern whose flame should stay warm while its frame goes
 * verdigris is {@code 66aa88,flame=ffdd66} — the common case stays one word, and the exception is written where the
 * exception is.
 * <p>
 * Each colour is hex ({@code #ff8800} or {@code ff8800}) or one of the sixteen vanilla colour names, resolved through
 * Adventure rather than a table here so what a builder types matches colours everywhere else.
 */
@Value
public class ModelTint {

    /** Applied to every renderer bone with no entry of its own; null leaves those bones alone. */
    @Nullable Color overall;

    /** Bone id (lower-cased) to colour. */
    @NotNull Map<String, Color> byBone;

    /**
     * @param spec the tag value, e.g. {@code "ff8800"}, {@code "head=black,body=red"}, {@code "808080,head=white"}
     * @return the tint, or empty if {@code spec} is blank or any entry is not a colour
     */
    public static @NotNull Optional<ModelTint> parse(@NotNull String spec) {
        if (spec.isBlank()) {
            return Optional.empty();
        }

        Color overall = null;
        final Map<String, Color> byBone = new LinkedHashMap<>();
        for (String entry : spec.split(",")) {
            final String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            final int equals = trimmed.indexOf('=');
            final Optional<Color> color = parseColor(equals < 0 ? trimmed : trimmed.substring(equals + 1).trim());
            if (color.isEmpty()) {
                return Optional.empty();
            }

            if (equals < 0) {
                overall = color.get();
            } else {
                byBone.put(trimmed.substring(0, equals).trim().toLowerCase(Locale.ROOT), color.get());
            }
        }

        if (overall == null && byBone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ModelTint(overall, byBone));
    }

    /**
     * Tints {@code model}'s renderer bones. Non-renderer bones — hitboxes, mount points, sub-model roots — are skipped:
     * they draw nothing, so tinting them is at best wasted and at worst confusing to debug.
     * <p>
     * Call this <em>after</em> any {@link ModelEngineHelper#remapModel skin remap}, so bones re-pointed at another
     * blueprint are tinted in their final form.
     *
     * @return the bone names this tint named that the model has no renderer bone for — worth reporting, since a
     * mistyped bone is otherwise a prop that simply keeps its old colour
     */
    public @NotNull Set<String> apply(@NotNull ActiveModel model) {
        final Set<String> unmatched = new HashSet<>(byBone.keySet());
        for (Map.Entry<String, ModelBone> entry : model.getBones().entrySet()) {
            final ModelBone bone = entry.getValue();
            if (!bone.isRenderer()) {
                continue;
            }

            final String id = entry.getKey().toLowerCase(Locale.ROOT);
            Color color = byBone.get(id);
            if (color != null) {
                unmatched.remove(id);
            } else {
                color = overall;
            }

            if (color != null) {
                bone.setDefaultTint(color);
            }
        }
        return unmatched;
    }

    private static @NotNull Optional<Color> parseColor(@NotNull String value) {
        final NamedTextColor named = NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
        if (named != null) {
            return Optional.of(Color.fromRGB(named.value()));
        }

        final String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            return Optional.empty();
        }
        try {
            return Optional.of(Color.fromRGB(Integer.parseInt(hex, 16)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
