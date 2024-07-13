@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.item.builder

import net.md_5.bungee.api.chat.BaseComponent
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.BungeeComponentWrapper

/**
 * Sets the lore of the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.setLore(lore: List<Array<BaseComponent>>): T = setLore(lore.map { BungeeComponentWrapper(it) })