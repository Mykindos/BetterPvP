package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.ImplementedBy;
import org.bukkit.entity.Player;

/**
 * Shows compiled dialogs to players.
 */
@ImplementedBy(PaperDialogSender.class)
public interface DialogSender {

    void show(Player player, CompiledDialog dialog);

    void close(Player player);
}
