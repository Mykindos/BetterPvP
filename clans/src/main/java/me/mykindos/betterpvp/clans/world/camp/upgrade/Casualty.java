package me.mykindos.betterpvp.clans.world.camp.upgrade;

import lombok.Value;
import me.mykindos.betterpvp.core.world.site.Whereabouts;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A player's last death: where, to whom or what, and when. */
@Value
public class Casualty {

    @NotNull UUID member;
    @NotNull Whereabouts where;
    /** The killer's name or the cause, or null when neither is known. */
    @Nullable Component killer;
    long diedAt;
}
