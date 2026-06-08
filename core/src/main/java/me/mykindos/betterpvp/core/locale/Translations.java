package me.mykindos.betterpvp.core.locale;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("UnstableApiUsage")
public final class Translations {

    /**
     * Component-level cache of rendered components keyed by (component, locale). Adventure components are
     * immutable with stable {@code equals}/{@code hashCode}, so static names/lore (and any component that
     * compares equal, including its translated args) hit the cache. Dynamic components simply miss; the
     * bounded size evicts them so memory stays capped. This is intentionally component-level rather than
     * item-level so it never depends on mutable item state (durability, purity, amount, prices, etc.).
     */
    private static final Cache<RenderKey, Component> RENDER_CACHE = Caffeine.newBuilder()
            .maximumSize(20_000)
            .build();

    private Translations() {
    }

    public static Component component(String key, ComponentLike... args) {
        return Component.translatable(key, Arrays.stream(args).map(ComponentLike::asComponent).toList());
    }

    /**
     * Whether the given string is a known translation key in any registered bundle. Useful for treating a
     * string as a translation key when present, and as literal text otherwise.
     */
    public static boolean hasTranslation(String key) {
        return key != null && !key.isEmpty() && TranslationService.translator().hasTranslation(key);
    }

    /**
     * Resolves a (possibly translatable) component server-side into a fully rendered component for the
     * given language tag. See {@link #render(Component, Locale)}.
     *
     * @param component the component to render (may contain translatable nodes)
     * @param lang      the viewer language tag (e.g. {@code en}, {@code es}, {@code es-ES}, {@code es_es});
     *                  when null, blank, invalid, or missing a translation, English is used
     * @return the rendered component
     */
    public static Component render(Component component, @Nullable String lang) {
        return render(component, toLocale(lang));
    }

    /**
     * Resolves a (possibly translatable) component server-side into a fully rendered component.
     *
     * <p>This must be used before writing item names/lore into the packets sent to a client: item data is
     * shipped to the client, which cannot resolve our server-side translation bundles, so unresolved
     * {@link Component#translatable(String) translatable} components would display as raw keys. Rendering
     * through the {@link GlobalTranslator} replaces every translatable node (including nested children,
     * hover/click event contents, and translated arguments) with the localized text while preserving all
     * styles, colors, decorations and fonts. Plain (non-translatable) components are returned unchanged.</p>
     *
     * <p>Missing translations for the requested locale fall back to English (never raw keys).</p>
     *
     * @param component the component to render (may contain translatable nodes)
     * @param locale    the viewer locale; when null, English is used
     * @return the rendered component
     */
    public static Component render(Component component, @Nullable Locale locale) {
        final Locale resolved = locale == null ? Locale.ENGLISH : locale;
        return RENDER_CACHE.get(new RenderKey(component, resolved), key -> doRender(key.component(), key.locale()));
    }

    private static Component doRender(Component component, Locale locale) {
        final Component rendered = GlobalTranslator.render(component, locale);
        if (Locale.ENGLISH.equals(locale)) {
            return rendered;
        }
        // Any keys not present for the requested locale are left unresolved by the GlobalTranslator;
        // re-render through English so missing translations fall back to English instead of showing keys.
        // Nodes already resolved above are plain text now, so this pass is a no-op for them.
        return GlobalTranslator.render(rendered, Locale.ENGLISH);
    }

    /**
     * Produces a copy of {@code itemStack} with its display name and lore resolved server-side into the
     * given locale, for use at the outgoing packet/render boundary.
     *
     * <p>The original stack is never mutated: a clone is made only if something actually changes, so plain
     * (non-translatable) items are returned as-is with no allocation. Only the display-name components
     * ({@link DataComponentTypes#ITEM_NAME} / {@link DataComponentTypes#CUSTOM_NAME}) and
     * {@link DataComponentTypes#LORE} lines are touched; item identity, amount, enchantments, durability,
     * persistent data and all other components are left untouched.</p>
     *
     * @param itemStack the stack to render (typically a packet-only copy)
     * @param locale    the recipient's locale (null falls back to English)
     * @return a localized copy, or the original instance if nothing required rendering
     */
    public static ItemStack renderItemStack(@Nullable ItemStack itemStack, @Nullable Locale locale) {
        if (itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }

        ItemStack result = itemStack; // cloned lazily on first actual change

        result = renderNameComponent(itemStack, result, DataComponentTypes.ITEM_NAME, locale);
        result = renderNameComponent(itemStack, result, DataComponentTypes.CUSTOM_NAME, locale);

        if (itemStack.hasData(DataComponentTypes.LORE)) {
            final ItemLore lore = itemStack.getData(DataComponentTypes.LORE);
            if (lore != null) {
                final List<Component> lines = lore.lines();
                final List<Component> rendered = new ArrayList<>(lines.size());
                boolean changed = false;
                for (Component line : lines) {
                    final Component renderedLine = render(line, locale);
                    // Lore lines flagged via ComponentWrapper.markForWrap are word-wrapped now, once the
                    // translatable text has been resolved into the recipient's locale and is measurable.
                    final List<Component> wrappedLines = ComponentWrapper.wrapIfMarked(renderedLine);
                    if (wrappedLines.size() == 1) {
                        final Component only = wrappedLines.get(0);
                        changed |= !only.equals(line);
                        rendered.add(only);
                    } else {
                        changed = true;
                        rendered.addAll(wrappedLines);
                    }
                }
                if (changed) {
                    result = result == itemStack ? itemStack.clone() : result;
                    result.setData(DataComponentTypes.LORE, ItemLore.lore(rendered));
                }
            }
        }

        return result;
    }

    private static ItemStack renderNameComponent(ItemStack original, ItemStack working,
                                                 DataComponentType.Valued<Component> type,
                                                 @Nullable Locale locale) {
        if (!original.hasData(type)) {
            return working;
        }
        final Component name = original.getData(type);
        if (name == null) {
            return working;
        }
        final Component rendered = render(name, locale);
        if (rendered.equals(name)) {
            return working;
        }
        final ItemStack result = working == original ? original.clone() : working;
        result.setData(type, rendered);
        return result;
    }

    /**
     * Converts a nullable language tag into a {@link Locale}, falling back to {@link Locale#ENGLISH}
     * when the tag is null, blank, or does not resolve to a language. Accepts BCP-47 tags ({@code es},
     * {@code es-ES}) as well as Java-style underscore tags ({@code es_es}).
     *
     * @param lang the language tag
     * @return the resolved locale, or English as a fallback
     */
    public static Locale toLocale(@Nullable String lang) {
        if (lang == null || lang.isBlank()) {
            return Locale.ENGLISH;
        }
        final Locale locale = Locale.forLanguageTag(lang.replace('_', '-'));
        if (locale.getLanguage().isBlank()) {
            return Locale.ENGLISH;
        }
        return locale;
    }

    public static Component[] componentLines(String baseKey, ComponentLike... args) {
        BetterPvPTranslator translator = TranslationService.translator();
        String lineCountStr = translator.getString(baseKey + ".lines", Locale.ENGLISH);
        int lineCount = 0;
        if (lineCountStr != null) {
            try {
                lineCount = Integer.parseInt(lineCountStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        Component[] components = new Component[lineCount];
        for (int i = 0; i < lineCount; i++) {
            String key = baseKey + "." + (i + 1);
            components[i] = component(key, args)
                    .colorIfAbsent(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return components;
    }

    /**
     * Like {@link #componentLines(String, ComponentLike...)} but applies no default colour or italic
     * styling, so the caller can style the (unresolved) lines themselves (e.g. info-tab descriptions
     * rendered white). Reads the line count from {@code <baseKey>.lines} and the lines from
     * {@code <baseKey>.1}, {@code <baseKey>.2}, ...
     *
     * @param baseKey the base translation key
     * @param args    MessageFormat arguments applied to every line
     * @return the unstyled translatable line components
     */
    public static Component[] rawComponentLines(String baseKey, ComponentLike... args) {
        BetterPvPTranslator translator = TranslationService.translator();
        String lineCountStr = translator.getString(baseKey + ".lines", Locale.ENGLISH);
        int lineCount = 0;
        if (lineCountStr != null) {
            try {
                lineCount = Integer.parseInt(lineCountStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        Component[] components = new Component[lineCount];
        for (int i = 0; i < lineCount; i++) {
            components[i] = component(baseKey + "." + (i + 1), args);
        }
        return components;
    }

    private record RenderKey(Component component, Locale locale) {
    }
}
