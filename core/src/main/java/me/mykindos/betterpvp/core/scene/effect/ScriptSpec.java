package me.mykindos.betterpvp.core.scene.effect;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * One parsed effect instruction: a kind followed by named parameters.
 * <p>
 * The grammar is {@code <kind>;key=value;key=value}, for example
 * {@code particle;t=flame;o=0.5,0.5,0.5;r=60} or {@code sound;s=block.gravel.step;v=0.4;p=1.1}.
 * Parameters are order-free, keys are case-insensitive, and a bare token with no {@code =} is a flag
 * worth {@code true} - so {@code particle;t=flame;force} reads the way it looks.
 * <p>
 * Aliases are not declared anywhere. A reader asks for every name it accepts and the first one
 * present wins ({@code spec.getInt(1, "c", "count")}), which keeps short and long forms working
 * without a table to maintain.
 * <p>
 * Written for ModelEngine script keyframes, where the string is authored in Blockbench, but nothing
 * here is tied to animations.
 */
public final class ScriptSpec {

    private final String kind;
    private final Map<String, String> arguments;

    private ScriptSpec(@NotNull String kind, @NotNull Map<String, String> arguments) {
        this.kind = kind;
        this.arguments = arguments;
    }

    /**
     * @param raw the instruction, with any {@code reader:} prefix already stripped
     * @return the parsed instruction; its {@link #kind()} is empty when {@code raw} is blank
     */
    public static @NotNull ScriptSpec parse(@NotNull String raw) {
        final String[] parts = raw.trim().split(";");
        final Map<String, String> arguments = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            final String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            final int separator = part.indexOf('=');
            if (separator < 0) {
                arguments.put(part.toLowerCase(Locale.ROOT), "true");
            } else {
                arguments.put(part.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                        part.substring(separator + 1).trim());
            }
        }
        return new ScriptSpec(parts.length == 0 ? "" : parts[0].trim().toLowerCase(Locale.ROOT), arguments);
    }

    /** @return which kind of effect this is, lower-cased - {@code "particle"}, {@code "sound"}, ... */
    public @NotNull String kind() {
        return kind;
    }

    /** @return whether this instruction carried any parameters at all. */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * @param keys the accepted names for one parameter, most preferred first
     * @return the value of the first name present
     */
    public @NotNull Optional<String> get(@NotNull String... keys) {
        for (String key : keys) {
            final String value = arguments.get(key.toLowerCase(Locale.ROOT));
            if (value != null && !value.isEmpty()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public @NotNull String getString(@NotNull String defaultValue, @NotNull String... keys) {
        return get(keys).orElse(defaultValue);
    }

    public int getInt(int defaultValue, @NotNull String... keys) {
        final Optional<String> value = get(keys);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.get());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("'" + value.get() + "' is not a whole number", exception);
        }
    }

    public double getDouble(double defaultValue, @NotNull String... keys) {
        final Optional<String> value = get(keys);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.get());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("'" + value.get() + "' is not a number", exception);
        }
    }

    public boolean getBoolean(boolean defaultValue, @NotNull String... keys) {
        return get(keys).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /**
     * Reads three comma-separated numbers, or one number applied to all three axes so an even spread
     * can be written {@code o=0.3}.
     *
     * @return the parsed vector, or {@code defaultValue} if the parameter is absent
     */
    @Nullable
    public Vector getVector(@Nullable Vector defaultValue, @NotNull String... keys) {
        final Optional<String> value = get(keys);
        if (value.isEmpty()) {
            return defaultValue;
        }

        final String[] components = value.get().split(",");
        try {
            if (components.length == 1) {
                final double uniform = Double.parseDouble(components[0].trim());
                return new Vector(uniform, uniform, uniform);
            }
            if (components.length == 3) {
                return new Vector(Double.parseDouble(components[0].trim()),
                        Double.parseDouble(components[1].trim()),
                        Double.parseDouble(components[2].trim()));
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("'" + value.get() + "' is not a vector", exception);
        }
        throw new IllegalArgumentException("'" + value.get() + "' needs 1 or 3 comma-separated numbers");
    }
}
