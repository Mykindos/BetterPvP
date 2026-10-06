package me.mykindos.betterpvp.champions.champions.builds.screen;

import lombok.Value;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/**
 * An extra button on every build row of the class screen, for features outside champions, such as game mode's hotbar
 * layout editor. The class screen closes before {@link #action} runs.
 */
@Value
public class BuildExtra {

    /** An 18x18 sprite declared in champions' {@code gui/assets/build_extras.json}. */
    String icon;
    /** Translation key of the button's tooltip. */
    String tooltip;
    BiConsumer<Player, RoleBuild> action;
}
