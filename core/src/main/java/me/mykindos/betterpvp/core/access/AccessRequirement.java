package me.mykindos.betterpvp.core.access;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.Set;

/**
 * Describes a single access requirement returned by an {@link ItemAccessProvider}.
 *
 * @param source        adventure Key that uniquely identifies the gating source (e.g. "progression:skill_tree")
 * @param lore          human-readable description, e.g. "Requires Woodcutting Lvl. 30"
 * @param gatedScopes   which {@link AccessScope}s this requirement controls
 * @param satisfied     whether the requirement is currently met for the queried player
 */
public record AccessRequirement(
        Key source,
        Component lore,
        Set<AccessScope> gatedScopes,
        boolean satisfied
) {
}
