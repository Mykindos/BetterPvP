package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.dialog.screen.GuiScreens;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Opens {@code gui/dialog_test.json}, a screen that exercises the definition format: a backdrop, tabs that swap the
 * canvas, hover and pressed art, a looping sprite, and a repeat filled from Java. Staff use it to check placement at
 * each GUI scale.
 */
@Singleton
public class DialogTestCommand extends Command {

    private final GuiScreens screens;

    @Inject
    public DialogTestCommand(GuiScreens screens) {
        this.screens = screens;
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
        final List<String> rewards = IntStream.rangeClosed(1, 3)
                .mapToObj(index -> PlainTextComponentSerializer.plainText().serialize(
                        Translations.render(Translations.component("core.dialog.test.reward." + index), player.locale())))
                .toList();
        screens.open(player, "core:dialog_test", Map.of("rewards", rewards), Map.of());
    }
}
