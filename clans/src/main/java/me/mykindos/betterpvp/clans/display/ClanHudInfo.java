package me.mykindos.betterpvp.clans.display;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.display.FontCanvas;
import me.mykindos.betterpvp.core.utilities.model.display.PlayerHeadProvider;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Boss-bar HUD presentation of the clan info readout — the {@link me.mykindos.betterpvp.core.framework.sidebar.SidebarMode#HUD}
 * counterpart of {@link ClansSidebarListener}, painted with a {@link FontCanvas} like the combat
 * {@code StatusBar}.
 *
 * <p>Registered as the HUD provider in {@code Clans}. Returns {@code null} when there is nothing to
 * show (offline player), which the overlay silently skips.
 */
@Singleton
public class ClanHudInfo {

    private final ClanManager clanManager;
    private final ZoneManager zoneManager;
    private final PlayerHeadProvider playerHeadProvider;

    // Per-player memo: return the SAME instance while inputs are unchanged so the boss-bar overlay can
    // short-circuit its change check instead of deep-comparing the head's hundreds of glyphs each tick.
    private final Cache<UUID, CachedHudInfo> cache;

    @Inject
    public ClanHudInfo(ClanManager clanManager, ZoneManager zoneManager, PlayerHeadProvider playerHeadProvider) {
        this.clanManager = clanManager;
        this.zoneManager = zoneManager;
        this.playerHeadProvider = playerHeadProvider;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(30))
                .build();
    }

    public Component render(Gamer gamer) {
        final Player player = gamer.getPlayer();
        if (player == null) {
            return null;
        }

        // Cheap inputs that drive the whole readout. While they are unchanged we hand back the exact same
        // instance, which lets the overlay skip recompositing/resending the head every tick.
        final Component head = playerHeadProvider.head(player, 3, 5).orElse(null);
        final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
        final String clanName = clanManager.getClanByPlayer(player).map(Clan::getName).orElse(null);
        final Zone zone = zoneManager.getZone(player);

        final CachedHudInfo prev = cache.getIfPresent(player.getUniqueId());
        if (prev != null && prev.getCoins() == coins
                && Objects.equals(prev.getClan(), clanName)
                && Objects.equals(prev.getZone(), zone)
                && prev.getHead() == head) {
            return prev.getRendered();
        }

        final Component rendered = draw(gamer);
        cache.put(player.getUniqueId(), new CachedHudInfo(coins, clanName, head, zone, rendered));
        return rendered;
    }

    private Component draw(Gamer gamer) {
        final Player player = gamer.getPlayer();
        if (player == null) {
            return null;
        }

        final FontCanvas canvas = new FontCanvas();
        final int avatarHeight = 38;

        final Optional<Component> avatar = playerHeadProvider.head(player, 3, 16);
        final boolean hasAvatar = avatar.isPresent();
        canvas.space(20);
        int infoAdvance = 20;
        if (hasAvatar) {
            canvas.glyph('\uE000', TextColor.color(0xFDFAFD), "hud/down_40"); // 38px frame, head centered
            canvas.space(-32).append(avatar.get()).space(32); // -32/+32 cancel; head is net-zero
            infoAdvance += 38; // only the frame glyph's advance survives the avatar block
        }

        // Info
        final Optional<Clan> clanOpt = clanManager.getClanByPlayer(player);
        final int coins = (int) gamer.getProperty(GamerProperty.BALANCE).orElse(0);
        final String coinText = UtilFormat.formatNumber(coins);
        final int textBoxHeight = 13;
        final int top = 40 - avatarHeight;

        if (clanOpt.isPresent()) {
            // Two evenly-spaced rows \u2014 coins above, the clan readout below. The vertical slack left over
            // (avatar height minus the two boxes) is split into three equal gaps: top, middle, bottom.
            final int gap = (avatarHeight - textBoxHeight * 2) / 3;
            final int coinsYOffset = top + gap + textBoxHeight;
            final int clanYOffset = coinsYOffset + gap + textBoxHeight;
            drawRow(canvas, '\uE001', coinText, coinsYOffset);
            drawRow(canvas, '\uE002', clanOpt.get().getName(), clanYOffset);
        } else {
            // Single row, centered on the avatar's right-center.
            final int yOffset = top + avatarHeight / 2 + textBoxHeight / 2;
            drawRow(canvas, '\uE001', coinText, yOffset);
        }

        // Locator. Every drawRow is net-zero, so the only forward advance left is the lead space(20)
        // plus the avatar frame (both already in infoAdvance). Rewinding it lands the cursor back at the
        // HUD origin, so the locator sits in the same spot regardless of clan/avatar state or text length.
        canvas.space(-infoAdvance);

        // Locator: the current zone/territory name. Resolve any translatable node (e.g. the wilderness
        // key) to the player's locale first so it measures and renders identically, then pay its width
        // back so a longer/shorter name never changes the readout's net advance (which the client centers
        // on) and shifts the HUD.
        final Zone zone = zoneManager.getZone(player);
        final Component zoneName = Translations.render(zone == null
                ? Translations.component("clans.territory.wilderness").color(NamedTextColor.GRAY)
                : zone.getDisplayName(), player.locale())
                .font(FontCanvas.font("offset/down_10"))
                .shadowColor(ShadowColor.shadowColor(0xFF000000));

        // Centred locator pill with the empty slot icon hung off its left. Unmarked (WHITE), so it stays
        // centred on screen rather than re-anchoring to an edge like the info rows above.
        canvas.labeledBar(zoneName, NamedTextColor.WHITE, 13, '\uE007', '\uE006', '\uE003',
                '\uE004', 13, FontCanvas.IconSide.LEFT, FontCanvas.Alignment.CENTER, 0);

        return canvas.build().shadowColor(ShadowColor.none());
    }

    private void drawRow(FontCanvas canvas, char icon, String text, int yOffset) {
        canvas.space(5);
        canvas.glyph(icon, TextColor.color(0xFDFAFD), "hud/down_" + yOffset);
        canvas.space(3);
        final int textPixels = UtilFont.textWidth(text);
        final int textWidth = Math.max(40, textPixels + 10);

        canvas.glyph('\uE007', TextColor.color(0xFDFAFD), "hud/down_" + yOffset);
        for (int i = 0; i < textWidth / 2; i++) {
            canvas.space(-1).glyph('\uE006', TextColor.color(0xFDFAFD), "hud/down_" + yOffset);
        }
        canvas.space(-1).glyph('\uE003', TextColor.color(0xFDFAFD), "hud/down_" + yOffset);

        canvas.space(-textWidth + 3).text(text, TextColor.color(0xFDFAFD), "offset/down_" + (yOffset - 3));
        canvas.space(-21 - textWidth);

        canvas.space(textWidth - textPixels - 10);
    }

}
