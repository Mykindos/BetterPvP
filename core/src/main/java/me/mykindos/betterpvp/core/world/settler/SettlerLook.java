package me.mykindos.betterpvp.core.world.settler;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** How a settler is shown: the ModelEngine model, a skin blueprint to remap it with, and the animations it plays. */
@Value
public class SettlerLook {

    @NotNull String model;
    /** A blueprint whose bones replace the model's, or null to show the model as it is. */
    @Nullable String skin;
    @NotNull String idleAnimation;
    @NotNull String walkAnimation;
    /** Played while standing at its workplace. */
    @NotNull String workAnimation;
    double size;
}
