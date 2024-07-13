@file:Suppress("PackageDirectoryMismatch")

package me.mykindos.betterpvp.core.inventory.window

import net.kyori.adventure.text.Component
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper

fun Window.changeTitle(title: Component) {
    changeTitle(AdventureComponentWrapper(title))
}