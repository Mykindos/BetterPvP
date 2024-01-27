package me.mykindos.betterpvp.progression.model.menu;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.util.Set;

@Slf4j
public class PerksMenu extends AbstractGui implements Windowed {
    ProgressionTree tree;
    public static final TagResolver TAG_RESOLVER = TagResolver.resolver(
            TagResolver.resolver("val", Tag.styling(NamedTextColor.GREEN)),
            TagResolver.resolver("effect", Tag.styling(NamedTextColor.WHITE)),
            TagResolver.resolver("stat", Tag.styling(NamedTextColor.YELLOW))
    );
    public PerksMenu(int width, int height, Player player, ProgressionTree tree, ProgressionData<?> data) {
        super(width, height);
        this.tree = tree;
        Set<ProgressionPerk> perks = tree.getPerks();

        int i = 0;
        for (ProgressionPerk perk : perks) {
            setItem(i++, getPerkItem(player, perk, data));
        }
    }

    private static SimpleItem getPerkItem(Player player, ProgressionPerk perk, ProgressionData<?> data) {
        final ItemView.ItemViewBuilder builder = ItemView.builder();
        for (String str : perk.getDescription(player, data)) {
            builder.lore(MiniMessage.miniMessage().deserialize("<gray>" + str, PerksMenu.TAG_RESOLVER));
        }

        builder.material(Material.PAPER)
                .displayName(Component.text(perk.getName(), perk.canUse(player, data) ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES);
        return builder.build().toSimpleItem();
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text(tree.getName());
    }
}
