package me.mykindos.betterpvp.core.utilities.model.display.actionbar;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A priority stack of {@link ActionBar} overrides for a single gamer. The highest-priority
 * entry (ties broken by most recent push) is rendered instead of the gamer's base action bar.
 * The base bar keeps accepting components in the background, so nothing queued while an
 * override is active is lost — it resumes untouched once the override is removed.
 */
public class ActionBarOverrides {

    private final List<Entry> entries = new ArrayList<>();

    public void push(int priority, ActionBar bar) {
        synchronized (entries) {
            entries.add(new Entry(priority, bar));
        }
    }

    public void remove(ActionBar bar) {
        synchronized (entries) {
            entries.removeIf(entry -> entry.getBar() == bar);
        }
    }

    /** The override currently winning, or {@code null} if the base action bar should render. */
    public @Nullable ActionBar peek() {
        synchronized (entries) {
            Entry best = null;
            for (Entry entry : entries) {
                if (best == null || entry.getPriority() >= best.getPriority()) {
                    best = entry; // >= so the most recent push wins ties
                }
            }
            return best == null ? null : best.getBar();
        }
    }

    @Value
    private static class Entry {
        int priority;
        ActionBar bar;
    }
}
