package me.mykindos.betterpvp.clans.clans.fatigue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.fatigue.events.PlayerFatigueGainEvent;
import me.mykindos.betterpvp.clans.clans.fatigue.events.PlayerFatigueTierChangeEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The voice of battle fatigue. The only layer that builds text. It listens to
 * the scoring events and renders medieval flavor via <b>title</b> (the visceral
 * moment) and <b>chat</b> (the durable record) — never the action bar.
 * <p>
 * Scoring code fires events and forgets; this class decides how it <i>feels</i>.
 */
@Singleton
@BPvPListener
public class BattleFatigueMessages implements Listener {

    // Minor accumulation — a low growl, chat only.
    private static final String[] MINOR = {
            "clans.fatigue.flavor.minor.0",
            "clans.fatigue.flavor.minor.1",
            "clans.fatigue.flavor.minor.2",
            "clans.fatigue.flavor.minor.3"
    };

    // Crossed into a worse tier — felt, with a title.
    private static final String[] WORN = {
            "clans.fatigue.flavor.worn.0",
            "clans.fatigue.flavor.worn.1"
    };
    private static final String[] WEARY = {
            "clans.fatigue.flavor.weary.0",
            "clans.fatigue.flavor.weary.1"
    };
    private static final String[] EXHAUSTED = {
            "clans.fatigue.flavor.exhausted.0",
            "clans.fatigue.flavor.exhausted.1"
    };

    // Recovered back down a tier — relief.
    private static final String[] RECOVERED = {
            "clans.fatigue.flavor.recovered.0",
            "clans.fatigue.flavor.recovered.1",
            "clans.fatigue.flavor.recovered.2"
    };

    private final ClientManager clientManager;

    @Inject
    public BattleFatigueMessages(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onGain(PlayerFatigueGainEvent event) {
        // Only nag when the gain was meaningful, so it doesn't spam every death.
        if (event.getDelta() >= 3.0) {
            flavor(event.getPlayer(), pick(MINOR));
        }
    }

    @EventHandler
    public void onTierChange(PlayerFatigueTierChangeEvent event) {
        final Player player = event.getPlayer();
        final FatigueTier tier = event.getNewTier();

        if (!event.isEscalation()) {
            if (event.getOldTier().requiresHold() && !tier.requiresHold()) {
                flavor(player, pick(RECOVERED));
            }
            return;
        }

        final String line = switch (tier) {
            case WORN -> pick(WORN);
            case WEARY -> pick(WEARY);
            case EXHAUSTED -> pick(EXHAUSTED);
            default -> null;
        };
        if (line == null) {
            return;
        }

        title(player,
                Translations.component(tier.getDisplayName()).color(tier.getColor()).decorate(TextDecoration.BOLD),
                Translations.component(line).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC),
                1.5);
        flavor(player, line);
    }

    /** Shown by {@link RespawnHoldService} the instant the hold begins. */
    public void onRespawnHoldStart(Player player, FatigueTier tier, double seconds) {
        title(player,
                Translations.component("clans.fatigue.hold.broken").color(tier.getColor()).decorate(TextDecoration.BOLD),
                Translations.component("clans.fatigue.hold.refuse").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC),
                2.0);

        final Component text = Translations.component("clans.fatigue.hold.price", Component.text(Math.ceil(seconds))).color(NamedTextColor.RED).decorate(TextDecoration.ITALIC);
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, text);
        UtilMessage.message(player, Component.empty());
    }

    /** Shown by {@link RespawnHoldService} when the player is returned to the world. */
    public void onRespawnHoldRelease(Player player, FatigueTier tier) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(1, TitleComponent.subtitle(0.3, 1.5, 0.5, false,
                gmr -> Translations.component("clans.fatigue.hold.stagger").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)));
        final Component text = Translations.component("clans.fatigue.hold.remember").color(NamedTextColor.RED).decorate(TextDecoration.ITALIC);
        UtilMessage.message(player, Component.empty());
        UtilMessage.message(player, text);
        UtilMessage.message(player, Component.empty());
    }

    private void title(Player player, Component title, Component subtitle, double seconds) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(1, new TitleComponent(0.3, seconds, 0.5, false,
                gmr -> title, gmr -> subtitle));
    }

    private void flavor(Player player, String line) {
        UtilMessage.message(player, "clans.fatigue.prefix", Translations.component(line).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
    }

    private static String pick(String[] pool) {
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }
}
