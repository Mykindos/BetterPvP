package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A place on the network: the server, the site or world there, the zone covering it if any, and the block. */
@Value
public class Whereabouts {

    @NotNull String server;
    /** The site's name, or the world's for a world no site owns. */
    @NotNull Component place;
    @Nullable Component zone;
    int x;
    int y;
    int z;
}
