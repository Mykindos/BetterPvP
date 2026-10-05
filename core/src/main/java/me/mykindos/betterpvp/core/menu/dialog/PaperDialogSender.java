package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;

/**
 * Builds a Paper dialog from a {@link CompiledDialog} and shows it.
 */
@Singleton
public class PaperDialogSender implements DialogSender {

    @Override
    public void show(Player player, CompiledDialog dialog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(Player player) {
        throw new UnsupportedOperationException();
    }
}
