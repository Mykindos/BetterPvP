package me.mykindos.betterpvp.core.cooldowns;


import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilTime;

@Data
public class Cooldown {

    private final double seconds;
    private final long systemTime;
    private final boolean removeOnDeath;
    private final boolean inform;
    private boolean cancellable;

    public Cooldown(double d, long systime, boolean removeOnDeath, boolean inform) {
        this.seconds = d * 1000.0; // Convert to milliseconds
        this.systemTime = systime;
        this.removeOnDeath = removeOnDeath;
        this.inform = inform;
    }

    public Cooldown(double d, long systime, boolean removeOnDeath, boolean inform, boolean cancellable) {
        this.seconds = d * 1000.0; // Convert to milliseconds
        this.systemTime = systime;
        this.removeOnDeath = removeOnDeath;
        this.inform = inform;
        this.cancellable = cancellable;
    }


    public double getRemaining() {
        return UtilTime.convert((getSeconds() + getSystemTime()) - System.currentTimeMillis(), UtilTime.TimeUnit.SECONDS, 1);
    }


}
