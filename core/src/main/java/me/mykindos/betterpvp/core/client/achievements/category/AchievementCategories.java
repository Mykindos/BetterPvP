package me.mykindos.betterpvp.core.client.achievements.category;

import org.bukkit.NamespacedKey;

public class AchievementCategories {
    //todo list of specific namespace keys for categories
    public static final NamespacedKey COMBAT_CATEGORY = new NamespacedKey("core", "combat_category");
    public static final NamespacedKey DEATH_TYPE = new NamespacedKey("core", "death_category");

    public static final NamespacedKey CLANS = new NamespacedKey("clans", "clans_category");

    //game categories
    public static final NamespacedKey GAME = new NamespacedKey("game", "game_category");
    public static final NamespacedKey GAME_FLAG_CAPTURES = new NamespacedKey("game", "flag_captures_category");
    public static final NamespacedKey GAME_POINTS_CAPTURED = new NamespacedKey("game", "points_captured_category");
    public static final NamespacedKey GAME_CHAMPIONS_WINS = new NamespacedKey("game", "champions_wins_category");
}
