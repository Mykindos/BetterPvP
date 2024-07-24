@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.item.builder

import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper
import net.kyori.adventure.text.Component

/**
 * Sets the display name of the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.setDisplayName(displayName: Component): T = setDisplayName(AdventureComponentWrapper(displayName))

/**
 * Sets the lore the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.setLore(lore: List<Component>): T = setLore(lore.map { AdventureComponentWrapper(it) })

/**
 * Adds lore lines to the item stack.
 */
fun <T : AbstractItemBuilder<T>> T.addLoreLines(vararg components: Component): T = addLoreLines(*components.map { AdventureComponentWrapper(it) }.toTypedArray())