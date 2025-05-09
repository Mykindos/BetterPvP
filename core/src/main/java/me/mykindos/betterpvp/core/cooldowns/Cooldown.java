package me.mykindos.betterpvp.core.cooldowns;


import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilTime;

import java.util.function.Consumer;

@Data
public class Cooldown {

    /**
     * The unique identifier or name for the cooldown.
     * This value is immutable once set and is typically used
     * to distinguish between different cooldown instances.
     */
    private final String name;
    /**
     * Represents the duration of the cooldown in seconds.
     * This value is stored as a double to allow for fractional seconds
     * and is internally converted to milliseconds for processing.
     */
    private double seconds;
    /**
     * Represents the system time in milliseconds when the cooldown was initialized.
     * This value is typically used to calculate the remaining time for the cooldown.
     * It is a fixed value set during the creation of the {@code Cooldown} instance and does not change.
     */
    private final long systemTime;
    /**
     * Indicates whether this cooldown should be removed automatically when the associated
     * entity experiences a death-related event. If set to true, the cooldown will be cleared
     * upon death, otherwise it will persist.
     */
    private final boolean removeOnDeath;
    /**
     * Indicates whether a notification or alert should be sent when the cooldown is triggered.
     * This property is immutable and determined at the time of object creation.
     */
    private final boolean inform;
    /**
     * Indicates whether the cooldown can be cancelled manually before it expires.
     * If set to true, the cooldown is cancellable, otherwise it cannot be manually interrupted.
     */
    private boolean cancellable;
    /**
     * A callback function that is triggered when the cooldown expires.
     * The callback accepts the specific {@link Cooldown} instance as a parameter.
     */
    private Consumer<Cooldown> onExpire;

    /**
     * Constructs a new Cooldown instance with the specified parameters.
     *
     * @param name the name of the cooldown, used to identify it
     * @param d the duration of the cooldown in seconds
     * @param systime the system time (in milliseconds) at which the cooldown starts
     * @param removeOnDeath whether the cooldown should be removed upon death
     * @param inform whether to inform when the cooldown is initialized or modified
     */
    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform) {
        this(name, d, systime, removeOnDeath, inform, false, null);
    }

    /**
     * Constructs a new cooldown instance with the specified parameters.
     *
     * @param name the name of the cooldown
     * @param d the duration of the cooldown in seconds
     * @param systime the system time at which the cooldown starts
     * @param removeOnDeath whether the cooldown should be removed upon death
     * @param inform whether to inform about the cooldown's state
     * @param cancellable whether the cooldown is cancellable
     */
    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform, boolean cancellable) {
        this(name, d, systime, removeOnDeath, inform, cancellable, null);
    }

    /**
     * Creates a new Cooldown object with the specified parameters.
     *
     * @param name the name of the cooldown
     * @param d the duration of the cooldown, in seconds
     * @param systime the system time when the cooldown was started, in milliseconds
     * @param removeOnDeath a flag indicating whether the cooldown should be removed upon death
     * @param inform a flag indicating whether notifications should be sent when the cooldown expires
     * @param cancellable a flag indicating whether the cooldown can be canceled before it expires
     * @param onExpire a consumer that will be executed when the cooldown expires
     */
    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform, boolean cancellable, Consumer<Cooldown> onExpire) {
        this.name = name;
        this.seconds = d * 1000.0; // Convert to milliseconds
        this.systemTime = systime;
        this.removeOnDeath = removeOnDeath;
        this.inform = inform;
        this.cancellable = cancellable;
        this.onExpire = onExpire;
    }


    /**
     * Calculates the remaining time in seconds until the cooldown expires.
     *
     * The remaining time is computed by taking the sum of the cooldown's initial duration
     * (in milliseconds) and the system time when the cooldown was created,
     * subtracting the current system time, and converting the result to seconds
     * with one decimal place.
     *
     * @return The remaining time in seconds as a double, rounded to one decimal place.
     *         If the cooldown has expired, the value will be zero or negative.
     */
    public double getRemaining() {
        return UtilTime.convert((getSeconds() + getSystemTime()) - System.currentTimeMillis(), UtilTime.TimeUnit.SECONDS, 1);
    }


}
