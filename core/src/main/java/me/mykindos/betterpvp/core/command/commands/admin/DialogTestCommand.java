package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.dialog.DialogButton;
import me.mykindos.betterpvp.core.menu.dialog.DialogCanvas;
import me.mykindos.betterpvp.core.menu.dialog.DialogClick;
import me.mykindos.betterpvp.core.menu.dialog.DialogScreen;
import me.mykindos.betterpvp.core.menu.dialog.DialogSessions;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/**
 * Opens a throwaway screen that exercises the dialog canvas: a backdrop in the title, clickable tabs placed at free
 * positions, a canvas button and a footer button with an art label. Staff use it to check placement at each GUI
 * scale.
 */
@Singleton
public class DialogTestCommand extends Command {

    private final DialogSessions sessions;

    @Inject
    public DialogTestCommand(DialogSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public String getName() {
        return "dialogtest";
    }

    @Override
    public String getDescription() {
        return "core.command.dialogtest.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        show(player, 0, 0);
    }

    private void show(Player player, int selected, int clicks) {
        final DialogCanvas backdrop = new DialogCanvas(300);
        backdrop.art(0, 0, '\uE000', 150, 120);
        backdrop.art(150, 0, '\uE001', 150, 120);

        final DialogCanvas canvas = new DialogCanvas(300);
        canvas.text(8, 3, text(player, "core.dialog.test.name", TextColor.color(0xFFD36B)));
        for (int tab = 0; tab < 3; tab++) {
            final int index = tab;
            final int y = 22 + tab * 23;
            final DialogClick open = (who, inputs) -> show(who, index, clicks);
            final Component name = text(player, "core.dialog.test.tab." + (tab + 1), TextColor.color(0xEEF0F5));
            final Component tooltip = text(player, "core.dialog.test.tab.tooltip", TextColor.color(0xEEF0F5));
            canvas.art(8, y, tab == selected ? '\uE003' : '\uE002', 64, 18).tooltip(tooltip).onClick(open);
            canvas.text(14, y + 5, name).tooltip(tooltip).onClick(open);
        }

        canvas.text(84, 24, text(player, "core.dialog.test.tab." + (selected + 1), TextColor.color(0xFFD36B)));
        canvas.text(84, 40, text(player, "core.dialog.test.description." + (selected + 1), TextColor.color(0xA3AABD)));
        canvas.text(84, 60, text(player, "core.dialog.test.clicks", TextColor.color(0xEEF0F5), Component.text(clicks)));

        final DialogClick confirm = (who, inputs) -> show(who, selected, clicks + 1);
        final Component confirmTooltip = text(player, "core.dialog.test.confirm.tooltip", TextColor.color(0xEEF0F5));
        canvas.art(208, 92, '\uE004', 80, 20).tooltip(confirmTooltip).onClick(confirm);
        canvas.text(224, 98, text(player, "core.dialog.test.confirm", TextColor.color(0x2A1606))).tooltip(confirmTooltip).onClick(confirm);

        sessions.open(player, DialogScreen.builder()
                .name(text(player, "core.dialog.test.name", TextColor.color(0xEEF0F5)))
                .backdrop(backdrop)
                .canvas(canvas)
                .exit(DialogButton.builder()
                        .label(Component.text('\uE006').font(Key.key("betterpvp", "ui")))
                        .tooltip(text(player, "core.dialog.test.close.tooltip", TextColor.color(0xEEF0F5)))
                        .width(40)
                        .build())
                .build());
    }

    private static Component text(Player player, String key, TextColor color, Component... args) {
        return Translations.render(Translations.component(key, args).color(color), player.locale());
    }
}
