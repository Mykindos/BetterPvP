package me.mykindos.betterpvp.core.stats.menu;

import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LeaderboardEntryButton<E, T> extends ControlItem<LeaderboardMenu<E, T>> implements CooldownButton {

    private final Supplier<CompletableFuture<LeaderboardEntry<E, T>>> entrySupplier;
    private final Component title;
    private final ItemProvider loading;
    private final ItemProvider failed;
    private CompletableFuture<LeaderboardEntry<E, T>> future;
    private CompletableFuture<Description> descriptionFuture;

    public LeaderboardEntryButton(Supplier<CompletableFuture<LeaderboardEntry<E, T>>> entrySupplier, ItemProvider loading, ItemProvider failed, final Component title) {
        this.entrySupplier = entrySupplier;
        this.title = title;
        this.loading = loading;
        this.failed = failed;
        this.future = CompletableFuture.completedFuture(null);
        this.descriptionFuture = CompletableFuture.completedFuture(null);
    }

    protected void fetch() {
        future = entrySupplier.get();
        future.thenAccept(entry -> {
            descriptionFuture = getGui().getLeaderboard().getDescription(getGui().getSearchOptions(), entry);
            descriptionFuture.thenRun(this::notifyWindows); // Notify again after the description is loaded
        }).thenRun(this::notifyWindows); // Notify after the entry is loaded
    }

    @SneakyThrows
    @Override
    public ItemProvider getItemProvider(LeaderboardMenu<E, T> gui) {
        if (!future.isDone() || !descriptionFuture.isDone()) {
            // Update the GUI when the entry is completely loaded (including description)
            return loading;
        }

        final LeaderboardEntry<E, T> currentEntry = future.get();
        if (currentEntry == null) {
            // If the entry is null, then the position is empty
            return failed;
        }

        final Description description = descriptionFuture.get();
        if(description == null) {
            return failed;
        }

        final ItemStack itemStack = description.getIcon().get();
        final ItemMeta meta = itemStack.getItemMeta();

        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        final List<Component> lore = new ArrayList<>(List.of(
                UtilMessage.DIVIDER,
                Component.empty(),
                Component.empty(),
                UtilMessage.DIVIDER));

        // After "holder"
        int index = 2;
        final Map<String, Component> properties = Objects.requireNonNull(description.getProperties(), "Properties cannot be null");
        for (Map.Entry<String, Component> entry : properties.entrySet()) {
            final TextComponent keyText = Component.text(entry.getKey() + ": ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
            final Component valueText = entry.getValue().decoration(TextDecoration.ITALIC, false).applyFallbackStyle(Style.style(NamedTextColor.GRAY));
            lore.add(index, keyText.append(valueText));
            index++;
        }

        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return ItemView.of(itemStack);
    }

    @SneakyThrows
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Attempt to execute the click function for the current entry
        if (future == null || !future.isDone() || future.get() == null) {
            return; // Do nothing if we have no data, or it hasn't loaded.
        }

        final LeaderboardEntry<E, T> currentEntry = future.get();
        if(currentEntry == null) {
            return;
        }

        final CompletableFuture<Description> descriptionFuture = getGui().getLeaderboard().getDescription(getGui().getSearchOptions(), currentEntry);
        if (!descriptionFuture.isDone()) {
            return; // Do nothing if the description hasn't loaded
        }

        final Consumer<Click> clickFunction = descriptionFuture.get().getClickFunction();
        if (clickFunction == null) {
            return; // Otherwise, do nothing
        }

        clickFunction.accept(new Click(event));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    @Override
    public double getCooldown() {
        return 0.4;
    }
}
