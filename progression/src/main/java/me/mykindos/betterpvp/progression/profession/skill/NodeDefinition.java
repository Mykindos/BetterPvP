package me.mykindos.betterpvp.progression.profession.skill;

/**
 * Record to hold node definition information
 * @param profession The profession the node belongs to
 * @param classPath The full class path of the node (optional)
 */
record NodeDefinition(String profession, String classPath) {}