package me.mykindos.betterpvp.core.stats.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class LeaderboardCategoryMenu extends AbstractGui implements Windowed {

    private final LeaderboardManager manager;

    public LeaderboardCategoryMenu(LeaderboardManager manager) {
        super(9, 3);
        this.manager = manager;
        populate();
    }

    private void populate() {
        final ItemView clansItem = ItemView.builder().material(Material.IRON_DOOR).displayName(Component.text("Clans", NamedTextColor.DARK_RED, TextDecoration.BOLD)).build();
        setItem(10, new SimpleItem(clansItem, click -> new LeaderboardListMenu(manager, LeaderboardCategory.CLANS, this).show(click.getPlayer())));

        final ItemView championsItem = ItemView.builder().material(Material.GOLDEN_CHESTPLATE).flag(ItemFlag.HIDE_ATTRIBUTES).displayName(Component.text("Champions", NamedTextColor.RED, TextDecoration.BOLD)).build();
        setItem(12, new SimpleItem(championsItem, click -> new LeaderboardListMenu(manager, LeaderboardCategory.CHAMPIONS, this).show(click.getPlayer())));

        final ItemView professionItem = ItemView.builder().material(Material.COD_BUCKET).displayName(Component.text("Professions", NamedTextColor.GREEN, TextDecoration.BOLD)).build();
        setItem(14, new SimpleItem(professionItem, click -> new LeaderboardListMenu(manager, LeaderboardCategory.PROFESSION, this).show(click.getPlayer())));

        final ItemView dungeonsItem = ItemView.builder().material(Material.SPAWNER).flag(ItemFlag.HIDE_ITEM_SPECIFICS).displayName(Component.text("Dungeons", NamedTextColor.YELLOW, TextDecoration.BOLD)).build();
        setItem(16, new SimpleItem(dungeonsItem, click -> new LeaderboardListMenu(manager, LeaderboardCategory.DUNGEONS, this).show(click.getPlayer())));

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Leaderboard");
    }
}
