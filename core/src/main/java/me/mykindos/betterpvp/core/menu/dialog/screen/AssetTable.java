package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The glyph codes of one namespace's screen art, assigned by {@link ScreenAssets#glyphs} order, the same way the pack
 * generator assigns them.
 */
public class AssetTable {

    @Getter
    private final String namespace;
    @Getter
    private final Key font;
    private final Set<String> assets;
    private final Map<String, Character> codes = new HashMap<>();

    public AssetTable(String namespace, Set<String> assets) {
        this.namespace = namespace;
        this.font = Key.key("betterpvp", "gui/" + namespace);
        this.assets = Set.copyOf(assets);
        final List<String> glyphs = ScreenAssets.glyphs(assets);
        if (ScreenAssets.FIRST_CODE + glyphs.size() - 1 > ScreenAssets.LAST_CODE) {
            throw new IllegalStateException("Namespace " + namespace + " needs " + glyphs.size() + " glyphs, more than the private use area holds");
        }
        for (int index = 0; index < glyphs.size(); index++) {
            codes.put(glyphs.get(index), (char) (ScreenAssets.FIRST_CODE + index));
        }
    }

    public boolean has(String asset) {
        return assets.contains(asset);
    }

    /** One glyph of an asset, in the namespace font. */
    public Component glyph(String asset, int part) {
        final String glyph = ScreenAssets.parts(asset) == 1 ? asset : asset + "#" + part;
        final Character code = codes.get(glyph);
        if (code == null) {
            throw new IllegalArgumentException("Asset " + asset + " is not declared in namespace " + namespace
                    + ". Add it to a screen, component or asset file so the pack generates it.");
        }
        return Component.text(code).font(font);
    }

    public char code(String asset) {
        return codes.get(asset);
    }
}
