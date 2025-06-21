package me.mykindos.betterpvp.core.utilities.model.display;

import java.util.function.Function;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public class GamerTimedDisplayObject<T> extends GamerDisplayObject<T> implements ITimedDisplay {

    private final double seconds;
    private final boolean waitToExpire;
    private long startTime = -1;

    /**
     * @param seconds      The amount of seconds to show the component for.
     * @param waitToExpire Whether to wait for the component to show first before allowing it to start expiring.
     * @param provider     The component to show. Return null to not display anything and skip this component.
     */
    public GamerTimedDisplayObject(double seconds, boolean waitToExpire, @NotNull Function<Gamer, T> provider) {
        super(provider);
        this.seconds = seconds;
        this.waitToExpire = waitToExpire;
    }

    @Override
    public long getRemaining() {
        long millisDuration = (long) (seconds * 1000);
        return (startTime + millisDuration) - System.currentTimeMillis();
    }

    @Override
    public boolean hasStarted() {
        return startTime != -1;
    }

    @Override
    public void startTime() {
        if (hasStarted()) {
            return;
        }

        startTime = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            setInvalid(true);
        }, (long) (seconds * 20L));
    }

}
