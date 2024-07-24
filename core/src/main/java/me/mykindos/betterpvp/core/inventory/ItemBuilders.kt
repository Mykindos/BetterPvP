@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.item.builder

import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.BungeeComponentWrapper
import net.md_5.bungee.api.chat.BaseComponent

/**
 * Sets the lore of the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.setLore(lore: List<Array<BaseComponent>>): T = setLore(lore.map { BungeeComponentWrapper(it) })