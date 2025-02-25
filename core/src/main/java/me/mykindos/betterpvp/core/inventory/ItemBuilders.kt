@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.item.builder

import net.minecraft.network.chat.Component

/**
 * Sets the lore of the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.setLore(lore: List<Component>): T = setLore(lore.map { it })