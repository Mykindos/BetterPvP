package me.mykindos.betterpvp.core.utilities.model.display.component;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.display.TimedDisplayObject;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents a component that is shown on the action bar and expires after [seconds] seconds.
 */
@Getter
public class TimedComponent extends TimedDisplayObject<Component> {
    /**
     * @param seconds      The amount of seconds to show the component for.
     * @param waitToExpire Whether to wait for the component to show first before allowing it to start expiring.
     * @param provider     The component to show. Return null to not display anything and skip this component.
     */
    public TimedComponent(double seconds, boolean waitToExpire, @NotNull Function<Gamer, Component> provider) {
        super(seconds, waitToExpire, provider);
    }
}
