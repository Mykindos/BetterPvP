package me.mykindos.betterpvp.progression.model.menu;

import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import xyz.xenondevs.invui.gui.AbstractGui;

import java.util.Set;


public abstract class PerksMenu extends AbstractGui implements Windowed {
    ProgressionTree tree;
    public static final TagResolver TAG_RESOLVER = TagResolver.resolver(
            TagResolver.resolver("val", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("effect", Tag.styling(NamedTextColor.WHITE)),
            TagResolver.resolver("stat", Tag.styling(NamedTextColor.YELLOW))
    );
    public PerksMenu(int width, int height, ProgressionTree tree) {
        super(width, height);

        Set<ProgressionPerk> perks;

    }
}
