@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.window

import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper
import net.kyori.adventure.text.Component

fun Window.changeTitle(title: Component) {
    changeTitle(AdventureComponentWrapper(title))
}