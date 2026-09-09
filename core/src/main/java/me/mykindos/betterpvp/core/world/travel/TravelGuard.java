package me.mykindos.betterpvp.core.world.travel;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A single reason travel may be refused. {@link TravelService} asks every registered guard before it starts the
 * departure ceremony and reports the first denial to the player.
 * <p>
 * Guards are a list rather than a condition chain so that a new restriction, such as a cooldown or a level
 * requirement, is a new class and one registration, with no edit to the travel path itself.
 */
@FunctionalInterface
public interface TravelGuard {

    /**
     * @param traveller   the player attempting to travel
     * @param destination where they are trying to go
     * @return the reason to refuse, or {@link Optional#empty()} to allow it
     */
    @NotNull Optional<Component> veto(@NotNull Player traveller, @NotNull Destination destination);
}
