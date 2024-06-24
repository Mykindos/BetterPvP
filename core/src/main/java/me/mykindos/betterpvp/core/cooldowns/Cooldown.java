package me.mykindos.betterpvp.core.cooldowns;


import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilTime;

import java.util.function.Consumer;

@Data
public class Cooldown {

    private final String name;
    private double seconds;
    private final long systemTime;
    private final boolean removeOnDeath;
    private final boolean inform;
    private boolean cancellable;
    private Consumer<Cooldown> onExpire;

    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform) {
        this(name, d, systime, removeOnDeath, inform, false, null);
    }

    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform, boolean cancellable) {
        this(name, d, systime, removeOnDeath, inform, cancellable, null);
    }

    public Cooldown(String name, double d, long systime, boolean removeOnDeath, boolean inform, boolean cancellable, Consumer<Cooldown> onExpire) {
        this.name = name;
        this.seconds = d * 1000.0; // Convert to milliseconds
        this.systemTime = systime;
        this.removeOnDeath = removeOnDeath;
        this.inform = inform;
        this.cancellable = cancellable;
        this.onExpire = onExpire;
    }


    public double getRemaining() {
        return UtilTime.convert((getSeconds() + getSystemTime()) - System.currentTimeMillis(), UtilTime.TimeUnit.SECONDS, 1);
    }


}
