package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanXpFormula;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.clans.menus.PerkMenu;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
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
    private final ClanXpFormula formula;
    private final ItemProvider itemProvider;

    public ClanProgressionButton(Clan clan, ClanXpFormula formula) {
        this.clan = clan;
        this.formula = formula;

        final long currentLevel = formula.levelFromXp(clan.getExperience());
        final double xpIn = formula.xpInCurrentLevel(currentLevel, clan.getExperience());
        final double xpNeeded = formula.xpRequiredForNextLevel(currentLevel);
        final double progress = (xpNeeded > 0) ? xpIn / xpNeeded : 1.0;

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
                .append(Component.text(String.format("(%,d%%)", (int) (progress * 100)),
                        TextColor.color(222, 222, 222)));

        this.itemProvider = ItemView.builder().material(Material.BEACON)
                .displayName(Component.text("Clan Level", NamedTextColor.BLUE))
                .lore(progressBarFinal)
                .lore(Component.empty())
                .lore(Component.text("Level: ", NamedTextColor.GRAY)
                        .append(Component.text(currentLevel, NamedTextColor.YELLOW)))
                .lore(Component.text("Progress: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                String.format("%,.1f / %,.1f XP", xpIn, xpNeeded),
                                NamedTextColor.YELLOW)))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("View Perks"))
                .build();
    }

    @Override
    public ItemProvider getItemProvider(ClanMenu gui) {
        return itemProvider;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                            @NotNull InventoryClickEvent event) {
        new PerkMenu(clan, getGui(), formula).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

}
