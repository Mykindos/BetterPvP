package me.mykindos.betterpvp.clans.world.resource;

import lombok.Value;

/**
 * One point of a per-player mine as a single viewer sees it: where it is, and the degrade stage they have worked it
 * down to. The world block at those coordinates is something else entirely - see {@code PersonalOreArchetype}.
 */
@Value
public class PersonalBlock {

    int x;
    int y;
    int z;
    String stage;
}
