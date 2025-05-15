package me.mykindos.betterpvp.game.framework.model.attribute;

import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Represents an attribute of a game that can be set or retrieved via commands.
 * @param <T> The type of the attribute value
 */
public abstract class GameAttribute<T> {

    private final List<BiConsumer<T, T>> changeListeners = new ArrayList<>();
    @Setter
    private T defaultValue;
    private final String key;
    private T value;

    protected GameAttribute(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /**
     * Register a listener to be notified when the value changes
     * @param listener Consumer that will be called with the new value
     */
    public void addChangeListener(BiConsumer<T, T> listener) {
        changeListeners.add(listener);
    }

    /**
     * Resets the attribute to its default value.
     * @return True if the attribute was reset successfully, false otherwise
     */
    public boolean resetToDefault() {
        return setValueSilently(defaultValue);
    }

    /**
     * Gets the name of the attribute.
     * @return The attribute name
     */
    @NotNull
    public String getKey() {
        return this.key;
    }

    /**
     * Gets the current value of the attribute.
     *
     * @return The current value
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the value of the attribute. This does not trigger any listeners.
     *
     * @param value The value to set
     * @return True if the value was set successfully, false otherwise
     */
    public boolean setValueSilently(T value) {
        this.value = value;
        return true;
    }

    /**
     * Sets the value of the attribute and triggers any listeners.
     *
     * @param value The value to set
     * @return True if the value was set successfully, false otherwise
     */
    public final boolean setValue(T value) {
        final T oldValue = this.value;
        boolean result = setValueSilently(value);
        // Notify listeners if value changed
        if (result && !Objects.equals(oldValue, value)) {
            for (BiConsumer<T, T> listener : changeListeners) {
                listener.accept(oldValue, value);
            }
        }
        return result;
    }

    /**
     * Checks if the attribute can be set in the current state.
     *
     * @param sender The command sender
     * @return True if the attribute can be set, false otherwise
     */
    public boolean canSet(CommandSender sender) {
        return true;
    }

    /**
     * Checks if the attribute can be retrieved in the current state.
     *
     * @param sender The command sender
     * @return True if the attribute can be retrieved, false otherwise
     */
    public boolean canGet(CommandSender sender) {
        return true;
    }

    /**
     * Checks if the given value is valid for this attribute.
     * @param value The value to check
     * @return True if the value is valid, false otherwise
     */
    public boolean isValidValue(T value) {
        return true;
    }

    /**
     * Parses a string value to the attribute type.
     * @param value The string value to parse
     * @return The parsed value, or null if parsing failed
     */
    @Nullable
    public abstract T parseValue(String value);

    /**
     * Formats the attribute value as a string.
     *
     * @param value The value to format
     * @return The formatted string
     */
    @NotNull
    public String formatValue(@NotNull T value) {
        return String.valueOf(value);
    }

    /**
     * Gets the error message to display when the attribute cannot be set.
     * @return The error message
     */
    @NotNull
    public String getCannotSetMessage() {
        return "You cannot set this attribute in the current state.";
    }

    /**
     * Gets the error message to display when the attribute cannot be retrieved.
     * @return The error message
     */
    @NotNull
    public String getCannotGetMessage() {
        return "You cannot get this attribute in the current state.";
    }

    /**
     * Gets the error message to display when the value is invalid.
     * @param value The invalid value
     * @return The error message
     */
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Invalid value: " + value;
    }

    /**
     * Gets all possible values, or an empty list if not applicable.
     * @return A collection of possible values
     */
    @NotNull
    public Collection<T> getPossibleValues() {
        return Set.of();
    }
}
