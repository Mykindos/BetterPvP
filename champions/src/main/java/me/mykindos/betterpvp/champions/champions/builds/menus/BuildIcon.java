package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.utilities.model.item.skull.SkullBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

/**
 * The colour and head texture each of the four build slots is presented with, in slot order.
 */
@RequiredArgsConstructor
public enum BuildIcon {

    FIRST(NamedTextColor.RED, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNmNzliMjA3ZDYxZTEyMjUyM2I4M2Q2MTUwOGQ5OWNmYTA3OWQ0NWJmMjNkZjJhOWE1MTI3ZjkwNzFkNGIwMCJ9fX0="),
    SECOND(NamedTextColor.AQUA, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjEwZTM3NGNkYzJiYTk1YmI3MmYxYTAzNmM3N2RhMzUwOTkzNWExYWJkMjRiNjhjNmIzNTkxNjkwYjEwM2ZlZCJ9fX0="),
    THIRD(NamedTextColor.GREEN, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM5MTIwM2QzNzUyYWZjNzFkOGY4Y2IwZGE4ZGJjN2YxM2EzYmFhZmUwYmY1ZjkxMWNiMjFjMzgzNDFmODdkYiJ9fX0="),
    FOURTH(NamedTextColor.GOLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzhlNDQwMjNlMTFlZWI1YjI5M2QwODYzNTFlMjllNmZmYWVjMDFiNzY4ZGMxNDYwYjFiZTU0YjgwOWJkNmRiZiJ9fX0=");

    @Getter
    private final TextColor color;
    private final String texture;

    private ItemStack head;

    /**
     * @param build the one-indexed build id
     * @return the icon for that build, wrapping around if there are ever more builds than icons
     */
    public static BuildIcon of(int build) {
        return values()[Math.floorMod(build - 1, values().length)];
    }

    /**
     * @return a builder already carrying this icon's head, ready for a display name and lore
     */
    public ItemView.ItemViewBuilder itemBuilder() {
        if (head == null) {
            head = new SkullBuilder(texture).build();
        }
        return ItemView.builder().with(head);
    }
}
