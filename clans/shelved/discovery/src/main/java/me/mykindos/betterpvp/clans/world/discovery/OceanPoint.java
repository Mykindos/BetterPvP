package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;

/** A fixed spot on the virtual sea plane, in the same {@code (x, z)} units the ship travels in. */
@Value
public class OceanPoint {

    double x;

    double z;
}
