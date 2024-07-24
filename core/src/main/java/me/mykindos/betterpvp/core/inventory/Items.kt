@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.item

/**
 * Calls [Item.notifyWindows] for all items in this [Iterable].
 */
fun Iterable<Item>.notifyWindows() = forEach { it.notifyWindows() }