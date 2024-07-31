package me.mykindos.betterpvp.progression.profession.woodcutting

import org.bukkit.Material

/**
 * Represents loot that will be dropped from doing something related to Woodcutting
 */
data class WoodcuttingLoot(
    val type: WoodcuttingLootType,
    val material: Material,
    val customModelData: Int,
    val frequency: Int,
    val minAmount: Int,
    val maxAmount: Int,
)
