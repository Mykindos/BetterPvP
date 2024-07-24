@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.window.type.context

import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper
import me.mykindos.betterpvp.core.inventory.window.Window
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Contract

/**
 * Sets the title of the window.
 *
 * @param title the new title
 * @return This Window Builder
 */
@Contract("_ -> this")
fun <B : Window.Builder<*, B>> B.setTitle(title: Component): B = setTitle(AdventureComponentWrapper(title))