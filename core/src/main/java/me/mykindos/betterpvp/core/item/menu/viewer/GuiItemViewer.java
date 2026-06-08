package me.mykindos.betterpvp.core.item.menu.viewer;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.item.impl.AutoUpdateItem;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.CooldownButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.menu.button.filter.NameSearchButton;
import me.mykindos.betterpvp.core.menu.button.filter.RaritySearchButton;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiItemViewer extends AbstractPagedGui<GuiItemViewer.CachedEntry> implements Windowed {

    private static final Object CACHE_LOCK = new Object();
    private static volatile List<CachedEntry> cache;

    /** Drops the shared item cache so the next refresh rebuilds every ItemInstance. */
    public static void invalidateCache() {
        synchronized (CACHE_LOCK) {
            cache = null;
        }
    }

    public record CachedEntry(ItemInstance instance, String lowerKey, int[] charBag,
                              double charBagNorm, ItemRarity rarity, boolean minecraft, ItemButton button) {}

    private static final URL url;

    static {
        try {
            url = URI.create("https://wiki.betterpvp.net/").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("gui-item-viewer-search");
        return thread;
    });

    private final ItemFactory itemFactory;
    private final RecipeRegistries recipeRegistries;
    private boolean customOnly = true;
    private ItemRarity raritySearch = null;
    private String nameSearch = null;
    private CompletableFuture<Void> searchFuture = CompletableFuture.completedFuture(null);
    private final AtomicLong searchGeneration = new AtomicLong();

    public GuiItemViewer(ItemFactory itemFactory, RecipeRegistries recipeRegistries) {
        super(9, 6, false, new Structure(
                "0XXX0XXX0",
                "0XXX0XXX0",
                "<XXX0XXX>",
                "0XXX0XXX0",
                "0XXX0XXX0",
                "00000000I"
        ).addIngredient('X', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', PageBackwardButton.defaultTexture())
                .addIngredient('>', PageForwardButton.defaultTexture())
                .addIngredient('I', InfoTabButton.builder()
                        // todo: wiki entry
                        .wikiEntry("Test", url)
                        .descriptionLines(List.of(Translations.rawComponentLines("core.menu.items.info.description")))
                        .build()));

        this.recipeRegistries = recipeRegistries;

        setItem(45, new NameSearchButton(() -> nameSearch, newName -> {
            nameSearch = newName;
            refresh();
        }));
        setItem(46, new CustomOnlyButton());
        setItem(47, new RaritySearchButton(() -> raritySearch, newRarity -> raritySearch = newRarity, this::refresh));
        setItem(52, new AutoUpdateItem(1, () -> {
            if (!searchFuture.isDone()) {
                return ItemView.builder()
                        .material(Material.PAPER)
                        .itemModel(Key.key("betterpvp", "menu/icon/regular/exclamation_mark_icon"))
                        .displayName(Translations.component("core.menu.button.loading.name").color(NamedTextColor.RED))
                        .build();
            }
            return Menu.INVISIBLE_BACKGROUND_ITEM;
        }));

        addPageChangeHandler((previousPage, nextPage) -> {
            for (Player player : findAllCurrentViewers()) {
                new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN).play(player);
            }
        });

        this.itemFactory = itemFactory;
        refresh();
    }

    public void refresh() {
        final long myGen = searchGeneration.incrementAndGet();
        final boolean customOnlyLocal = this.customOnly;
        final ItemRarity raritySearchLocal = this.raritySearch;
        final String queryLower = nameSearch == null ? null : nameSearch.toLowerCase();

        this.searchFuture = CompletableFuture.supplyAsync(() -> {
            if (searchGeneration.get() != myGen) return null;

            final List<CachedEntry> entries = getOrBuildCache();

            // pre-compute query character bag once; only used when query length >= 3
            final boolean useFuzzy = queryLower != null && queryLower.length() >= 3;
            int[] queryBag = null;
            double queryNorm = 0.0;
            if (useFuzzy) {
                queryBag = new int[128];
                for (int i = 0; i < queryLower.length(); i++) {
                    char c = queryLower.charAt(i);
                    if (c < 128) queryBag[c]++;
                }
                long sumSq = 0;
                for (int v : queryBag) sumSq += (long) v * v;
                queryNorm = Math.sqrt(sumSq);
            }

            List<CachedEntry> result = new ArrayList<>(entries.size());
            for (CachedEntry e : entries) {
                if (searchGeneration.get() != myGen) return null;

                // cheap filters first
                if (customOnlyLocal && e.minecraft()) continue;
                if (raritySearchLocal != null && e.rarity() != raritySearchLocal) continue;

                // name filter: literal contains, then fuzzy fallback
                if (queryLower != null && !e.lowerKey().contains(queryLower)) {
                    if (!useFuzzy) continue;
                    long dot = 0;
                    for (int i = 0; i < 128; i++) dot += (long) e.charBag()[i] * queryBag[i];
                    double sim = dot / (e.charBagNorm() * queryNorm);
                    if (sim <= 0.75) continue;
                }

                result.add(e);
            }

            return result;
        }, executor).exceptionally(ex -> {
            log.error("Failed to refresh item viewer", ex).submit();
            return null;
        }).thenAccept(result -> {
            if (result == null || searchGeneration.get() != myGen) return;
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                if (searchGeneration.get() != myGen) return;
                setContent(result);
                for (Player player : findAllCurrentViewers()) {
                    new SoundEffect(Sound.ITEM_SPYGLASS_USE).play(player);
                }
            });
        });
    }

    private List<CachedEntry> getOrBuildCache() {
        List<CachedEntry> local = cache;
        if (local != null) return local;
        synchronized (CACHE_LOCK) {
            if (cache != null) return cache;
            cache = buildCache();
            return cache;
        }
    }

    private List<CachedEntry> buildCache() {
        final Map<NamespacedKey, BaseItem> pool = itemFactory.getItemRegistry().getItemsSorted();
        List<CachedEntry> built = new ArrayList<>(pool.size());
        for (Map.Entry<NamespacedKey, BaseItem> entry : pool.entrySet()) {
            ItemInstance instance = itemFactory.createPreview(entry.getValue());
            String lowerKey = entry.getKey().getKey().toLowerCase();
            int[] bag = new int[128];
            for (int i = 0; i < lowerKey.length(); i++) {
                char c = lowerKey.charAt(i);
                if (c < 128) bag[c]++;
            }
            long sumSq = 0;
            for (int v : bag) sumSq += (long) v * v;
            double norm = Math.sqrt(sumSq);
            boolean minecraft = entry.getKey().getNamespace().equals("minecraft");
            built.add(new CachedEntry(instance, lowerKey, bag, norm, instance.getRarity(), minecraft, new ItemButton(instance)));
        }
        built.sort(Comparator.comparing(CachedEntry::lowerKey));
        return List.copyOf(built);
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (CachedEntry e : content) {
            page.add(new SlotElement.ItemSlotElement(e.button()));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_recipe_viewer>").font(NEXO);
    }

    private class CustomOnlyButton extends AbstractItem implements CooldownButton {

        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/star_icon"))
                    .displayName(Translations.component("core.menu.items.button.custom-only.name")
                            .color(customOnly ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            customOnly = !customOnly;
            refresh();
            notifyWindows();
        }

        @Override
        public double getCooldown() {
            return 0.4;
        }
    }

}
