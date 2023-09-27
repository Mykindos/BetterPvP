package me.mykindos.betterpvp.core.utilities.model.actionbar;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents a component that is shown on the action bar and expires after [seconds] seconds.
 */
@Getter
public final class TimedComponent extends DisplayComponent {

    private final double seconds;
    private final boolean waitToExpire;
    private boolean hasStarted = false;

    /**
     * @param seconds      The amount of seconds to show the component for.
     * @param waitToExpire Whether to wait for the component to show first before allowing it to start expiring.
     * @param provider     The component to show. Return null to not display anything and skip this component.
     */
    public TimedComponent(double seconds, boolean waitToExpire, @Nullable Function<Gamer, Component> provider) {
        super(provider);
        this.seconds = seconds;
        this.waitToExpire = waitToExpire;
    }

    void startTime() {
        if (hasStarted) {
            return;
        }

        hasStarted = true;
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            setInvalid(true);
        }, (long) (seconds * 20L));
    }
}
