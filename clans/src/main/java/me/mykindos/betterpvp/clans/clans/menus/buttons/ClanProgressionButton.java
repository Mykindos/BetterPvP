package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.clans.menus.PerkMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ClanProgressionButton extends ControlItem<ClanMenu> {

    private final Clan clan;
    private final ItemProvider itemProvider;

    public ClanProgressionButton(Clan clan) {
        this.clan = clan;

        final long currentLevel = clan.getLevel();
        final double currentBaseExperience = Clan.getExperienceForLevel(currentLevel);
        final double experienceNeeded = Clan.getExperienceForLevel(currentLevel + 1) - currentBaseExperience;
        final double experienceHave = clan.getExperience() - currentBaseExperience;
        final double progress = experienceHave / experienceNeeded;
        final TextComponent progressBar = ProgressBar.withLength((float) progress, 20)
                .withCharacter(' ')
                .build()
                .decoration(TextDecoration.STRIKETHROUGH, true);

        final Component progressBarFinal = Component.text(currentLevel, NamedTextColor.YELLOW)
                .appendSpace()
                .append(progressBar)
                .appendSpace()
                .append(Component.text(currentLevel + 1, NamedTextColor.YELLOW))
                .appendSpace()
                .append(Component.text(String.format("(%,d%%)", (int) (progress * 100)), TextColor.color(222, 222, 222)));

        this.itemProvider = ItemView.builder().material(Material.BEACON)
                .displayName(Translations.component("clans.menu.clan.button.progression.name").color(NamedTextColor.BLUE))
                .itemModel(Key.key("betterpvp", "menu/icon/regular/level_up_icon"))
                .lore(progressBarFinal)
                .lore(Component.empty())
                .lore(Translations.component("clans.menu.clan.button.progression.lore.level").color(NamedTextColor.GRAY)
                        .appendSpace().append(Component.text(currentLevel, NamedTextColor.YELLOW)))
                .lore(Translations.component("clans.menu.clan.button.progression.lore.progress").color(NamedTextColor.GRAY)
                        .appendSpace().append(Component.text(String.format("%,.1f / %,.1f XP", experienceHave, experienceNeeded), NamedTextColor.YELLOW)))
                .frameLore(true)
                .action(ClickActions.ALL, Translations.component("clans.menu.clan.button.progression.action"))
                .build();
    }

    @Override
    public ItemProvider getItemProvider(ClanMenu gui) {
        return itemProvider;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new PerkMenu(clan, getGui()).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
