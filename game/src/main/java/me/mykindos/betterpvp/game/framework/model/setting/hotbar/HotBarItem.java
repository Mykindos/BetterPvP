package me.mykindos.betterpvp.game.framework.model.setting.hotbar;

import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.Role;

/**
 * Represents an item that can be put into a hotbar
 */
@Getter
public enum HotBarItem {

    //<editor-fold desc="Swords">
    STANDARD_SWORD("champions:standard_sword",  3),
    BOOSTER_SWORD("champions:booster_sword", 4),
    POWER_SWORD("champions:power_sword", 4),
    ANCIENT_SWORD("champions:ancient_sword", 5),
    //</editor-fold>
    //<editor-fold desc="Axes">
    STANDARD_AXE("champions:standard_axe", 3),
    BOOSTER_AXE("champions:booster_axe", 4),
    POWER_AXE("champions:power_axe", 4),
    ANCIENT_AXE("champions:ancient_axe", 5),
    //</editor-fold>
    //<editor-fold desc="Bows (Assassin, Ranger)">
    BOW("champions:bow", 1, 1, Role.ASSASSIN, Role.RANGER),
    ARROWS("champions:arrow", 1, 32, Role.ASSASSIN, Role.RANGER),
    //</editor-fold>
    //<editor-fold desc="Consumables">
    MUSHROOM_STEW("champions:mushroom_stew", 2, 1);
    //</editor-fold>

    private final String namespacedKey;
    private final int tokenCost;
    private final Role[] allowedRoles;
    private final int amount;

    HotBarItem(String namespacedKey, int tokenCost, int amount, Role... allowedRoles) {
        this.namespacedKey = namespacedKey;
        this.tokenCost = tokenCost;
        this.allowedRoles = allowedRoles;
        this.amount = amount;
    }

    HotBarItem(String namespacedKey, int tokenCost, int amount) {
        this.namespacedKey = namespacedKey;
        this.tokenCost = tokenCost;
        this.allowedRoles = Role.values();
        this.amount = amount;
    }

    HotBarItem(String namespacedKey, int tokenCost) {
        this.namespacedKey = namespacedKey;
        this.tokenCost = tokenCost;
        this.allowedRoles = Role.values();
        this.amount = 1;
    }
}
