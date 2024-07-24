package me.mykindos.betterpvp.champions.settings;

import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.settings.menus.SettingCategory;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class ChampionsSettingsMenu extends AbstractGui implements SettingCategory {

    public ChampionsSettingsMenu(Client client) {
        super(9, 1);

        final Description skillTooltipDescription = Description.builder().icon(lang -> {
            final boolean setting = (boolean) client.getProperty(ChampionsProperty.SKILL_WEAPON_TOOLTIP).orElse(false);
            final NamedTextColor color = setting ? NamedTextColor.GREEN : NamedTextColor.RED;
            return ItemView.builder()
                    .material(Material.WOODEN_SWORD)
                    .displayName(Component.text("Skill Tooltips", color))
                    .lore(Component.text("Whether or not to display your current skills description on your weapon", NamedTextColor.GRAY))
                    .frameLore(true)
                    .build()
                    .get();
        }).build();


        addItems(new SettingsButton(client, ChampionsProperty.SKILL_WEAPON_TOOLTIP, skillTooltipDescription));


    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Champions Settings");
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_CHESTPLATE)
                        .displayName(Component.text("Champions Settings", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(Component.text("View generic settings related to the champions classes", NamedTextColor.GRAY))
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .frameLore(true)
                        .build())
                .build();
    }
}
