package me.mykindos.betterpvp.clans.world.camp.settler.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static me.mykindos.betterpvp.clans.testing.Messages.mentions;
import static me.mykindos.betterpvp.clans.testing.Messages.text;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Reads the items a menu holds and clicks them, without opening a window. */
public final class MenuProbe {

    private MenuProbe() {
    }

    /**
     * Loads the menu framework's constants, which build an empty item stack once, against a stand-in server. Call
     * before building any menu.
     */
    public static void load() {
        final UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.createEmptyStack()).thenReturn(mock(ItemStack.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getUnsafe).thenReturn(unsafe);
            Class.forName(ItemProvider.class.getName(), true, ItemProvider.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static Optional<ItemView> view(AbstractGui gui, int slot) {
        final Item item = gui.getItem(slot);
        if (item instanceof SimpleItem simple && simple.getItemProvider() instanceof ItemView view) {
            return Optional.of(view);
        }
        return Optional.empty();
    }

    /** The slot whose item's name mentions translation key {@code key}. */
    public static Optional<Integer> slotNamed(AbstractGui gui, String key) {
        for (int slot = 0; slot < gui.getSize(); slot++) {
            final Optional<ItemView> view = view(gui, slot);
            if (view.isPresent() && view.get().getDisplayName() != null && mentions(view.get().getDisplayName(), key)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public static ItemView named(AbstractGui gui, String key) {
        return view(gui, slotNamed(gui, key).orElseThrow(() -> new AssertionError("no item named " + key))).orElseThrow();
    }

    /** Every item in the menu that is not background. */
    public static List<ItemView> views(AbstractGui gui) {
        final List<ItemView> views = new ArrayList<>();
        for (int slot = 0; slot < gui.getSize(); slot++) {
            view(gui, slot).ifPresent(views::add);
        }
        return views;
    }

    public static boolean loreMentions(ItemView view, String key) {
        return view.getLore().stream().anyMatch(line -> mentions(line, key));
    }

    public static String loreText(ItemView view) {
        final StringBuilder builder = new StringBuilder();
        view.getLore().forEach(line -> builder.append(text(line)).append('\n'));
        return builder.toString();
    }

    public static void click(AbstractGui gui, int slot, Player player, ClickType type) {
        final InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClick()).thenReturn(type);
        gui.getItem(slot).handleClick(type, player, event);
    }

    public static void click(AbstractGui gui, String key, Player player) {
        click(gui, slotNamed(gui, key).orElseThrow(() -> new AssertionError("no item named " + key)), player,
                ClickType.LEFT);
    }

    /**
     * Builds {@code type} through its injected constructor, passing the given dependencies and a mock for every other,
     * so the test keeps compiling when an unused dependency is dropped.
     */
    public static <T> T build(Class<T> type, Object... given) {
        final Constructor<?> constructor = Arrays.stream(type.getConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        final Object[] args = Arrays.stream(constructor.getParameterTypes())
                .map(parameter -> Arrays.stream(given).filter(parameter::isInstance).findFirst()
                        .orElseGet(() -> mock(parameter)))
                .toArray();
        try {
            return type.cast(constructor.newInstance(args));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
