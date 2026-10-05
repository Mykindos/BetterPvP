package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the dialog screen each player has open and routes its clicks. Every screen sent gets a new id, so clicks
 * from a screen that was replaced or closed are ignored. Input values that clicks carry are kept and sent back as
 * initial values when the screen is re-rendered. After a click the screen re-renders, unless the callback already
 * re-rendered it, opened another screen or closed it.
 */
@BPvPListener
@Singleton
public class DialogSessions implements Listener {

    private static final String CLICK_PREFIX = "dialog/";

    private final DialogSender sender;
    private final DialogScheduler scheduler;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger ids = new AtomicInteger();

    @Inject
    private Core core;

    @Inject
    public DialogSessions(DialogSender sender, DialogScheduler scheduler) {
        this.sender = sender;
        this.scheduler = scheduler;
    }

    /** Shows {@code screen}, replacing any screen the player has open. */
    public void open(Player player, DialogScreen screen) {
        final Session session = new Session(screen, new HashMap<>());
        sessions.put(player.getUniqueId(), session);
        send(player, session);
    }

    /** Sends the open screen again, with the last input values as initial values. */
    public void rerender(Player player) {
        final Session session = sessions.get(player.getUniqueId());
        if (session != null) {
            send(player, session);
        }
    }

    public void close(Player player) {
        if (sessions.remove(player.getUniqueId()) != null) {
            sender.close(player);
        }
    }

    public boolean isOpen(UUID player) {
        return sessions.containsKey(player);
    }

    /** Routes a custom click. Returns false when the key is not a click of the player's open screen. */
    public boolean handle(Player player, Key action, DialogInputs inputs) {
        final Session session = sessions.get(player.getUniqueId());
        if (session == null || !action.namespace().equals("betterpvp") || !action.value().startsWith(CLICK_PREFIX)) {
            return false;
        }
        final String[] parts = action.value().substring(CLICK_PREFIX.length()).split("/");
        if (parts.length != 2 || !parts[0].equals(String.valueOf(session.id))) {
            return false;
        }
        final int slot;
        try {
            slot = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (slot < 0 || slot >= session.clicks.size()) {
            return false;
        }
        session.values.putAll(inputs.asMap());
        final int id = session.id;
        session.clicks.get(slot).onClick(player, inputs);
        // A click focuses the text block and the client outlines it. A fresh screen has no focus.
        if (sessions.get(player.getUniqueId()) == session && session.id == id) {
            send(player, session);
        }
        return true;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        final Player player = connection.getPlayer();
        final Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        final DialogInputs inputs = read(session.screen, event.getDialogResponseView());
        if (Bukkit.isPrimaryThread()) {
            handle(player, event.getIdentifier(), inputs);
        } else {
            Bukkit.getScheduler().runTask(core, () -> handle(player, event.getIdentifier(), inputs));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private void send(Player player, Session session) {
        session.id = ids.incrementAndGet();
        session.clicks.clear();

        final DialogScreen screen = session.screen;
        final List<CanvasElement> elements = screen.getCanvas().getElements();
        final Map<Integer, Key> regionKeys = new HashMap<>();
        for (int index = 0; index < elements.size(); index++) {
            final DialogClick click = elements.get(index).getClick();
            if (click != null) {
                regionKeys.put(index, register(session, click));
            }
        }

        final Key background = register(session, (who, inputs) -> { });

        final List<CompiledDialog.Button> buttons = new ArrayList<>();
        for (DialogButton button : screen.getButtons()) {
            buttons.add(compile(button, button.getClick() == null ? null : register(session, button.getClick())));
        }

        CompiledDialog.Button exit = null;
        if (screen.getExit() != null) {
            final DialogClick click = screen.getExit().getClick();
            final int id = session.id;
            exit = compile(screen.getExit(), register(session, (who, inputs) -> {
                if (click != null) {
                    click.onClick(who, inputs);
                }
                final Session current = sessions.get(who.getUniqueId());
                if (current != null && current.id == id) {
                    close(who);
                }
            }));
        }

        final List<DialogField> fields = screen.getFields().stream()
                .map(field -> session.values.containsKey(field.getKey()) ? field.withValue(session.values.get(field.getKey())) : field)
                .toList();

        // One extra pixel so a line ending in a glyph's trailing gap does not wrap.
        sender.show(player, new CompiledDialog(
                screen.getName(),
                DialogCompiler.backdrop(screen.getBackdrop(), screen.getCanvas().getWidth()),
                DialogCompiler.body(screen.getCanvas(), regionKeys::get, background),
                screen.getCanvas().getWidth() + DialogCompiler.TEXT_INSET * 2 + 1,
                fields,
                buttons,
                screen.getColumns(),
                exit,
                screen.isEscapable()));
    }

    private static Key register(Session session, DialogClick click) {
        session.clicks.add(click);
        return Key.key("betterpvp", CLICK_PREFIX + session.id + "/" + (session.clicks.size() - 1));
    }

    private static CompiledDialog.Button compile(DialogButton button, Key action) {
        return new CompiledDialog.Button(button.getLabel(), button.getTooltip(), button.getWidth(), action);
    }

    private static DialogInputs read(DialogScreen screen, DialogResponseView view) {
        if (view == null) {
            return DialogInputs.EMPTY;
        }
        final Map<String, Object> values = new HashMap<>();
        for (DialogField field : screen.getFields()) {
            final Object value = switch (field) {
                case DialogField.Text text -> view.getText(text.getKey());
                case DialogField.Choice choice -> view.getText(choice.getKey());
                case DialogField.Toggle toggle -> view.getBoolean(toggle.getKey());
                case DialogField.Slider slider -> view.getFloat(slider.getKey());
            };
            if (value != null) {
                values.put(field.getKey(), value);
            }
        }
        return new DialogInputs(values);
    }

    private static final class Session {
        private final DialogScreen screen;
        private final Map<String, Object> values;
        private final List<DialogClick> clicks = new ArrayList<>();
        private int id;

        private Session(DialogScreen screen, Map<String, Object> values) {
            this.screen = screen;
            this.values = values;
        }
    }
}
