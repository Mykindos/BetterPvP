package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.playerlist.PlayerListType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class GamerListener implements Listener {

    private final PermanentComponent header;
    private final PermanentComponent footer;

    @Inject
    @Config(path="gamer.default.coins", defaultValue = "5000")
    private int defaultCoins;

    @Inject
    @Config(path="gamer.default.fragments", defaultValue = "0")
    private int defaultFragments;

    @Inject
    @Config(path="gamer.default.pvpprotection", defaultValue = "3600.0")
    private double defaultPvPProtection;

    @Inject
    @Config(path = "tab.shop", defaultValue = "shop.betterpvp.net")
    private String shop;

    private final ClientManager manager;

    @Inject
    public GamerListener(Core core, ClientManager manager) {
        this.manager = manager;

        this.header = new PermanentComponent(gamer -> Component.text("BetterPvP ", NamedTextColor.GOLD)
                .append(Translations.component("core.tab.header.network",
                                Component.text(core.getConfig().getOrSaveString("core.info.server", "unknown"), NamedTextColor.GREEN))
                        .color(NamedTextColor.WHITE)));

        this.footer = new PermanentComponent(gamer -> Translations.component("core.tab.footer",
                        Component.text(Objects.requireNonNull(shop, ""), NamedTextColor.YELLOW))
                .color(NamedTextColor.WHITE));
    }

    @UpdateEvent (isAsync = true)
    public void onUpdateAsync() {
        try {
            this.manager.getOnline().forEach(client -> {
                final Gamer gamer = client.getGamer();
                gamer.getActionBar().show(gamer);
                gamer.getTitleQueue().show(gamer);
                gamer.getPlayerList().show(gamer);
                gamer.getExperienceBar().show(gamer);
                gamer.getExperienceLevel().show(gamer);
            });
        }catch(Exception ex) {
            log.error("Error with gamer async onUpdateAsync", ex).submit();
        }
    }

    @UpdateEvent
    public void onUpdate() {
        try {
            this.manager.getOnline().forEach(client -> {
                final Gamer gamer = client.getGamer();
                gamer.getBossBarOverlay().show(gamer);
                gamer.getBossBarQueue().show(gamer);
            });
        }catch(Exception ex) {
            log.error("Error with gamer onUpdate", ex).submit();
        }
    }

    @EventHandler (priority =  EventPriority.MONITOR)
    public void onClientLoad(AsyncClientLoadEvent event) {
        checkUnsetProperties(event.getClient());


    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Gamer gamer = this.manager.search().online(event.getPlayer()).getGamer();

        gamer.getPlayerList().clear();
        gamer.getPlayerList().add(PlayerListType.FOOTER, footer);
        gamer.getPlayerList().add(PlayerListType.HEADER, header);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        final Gamer gamer = manager.search().online(event.getPlayer()).getGamer();
        gamer.setLastMovementNow();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBalanceUpdate(GamerPropertyUpdateEvent event) {
        if (event.getProperty().equals(GamerProperty.BALANCE.name())) {
            log.info("Balance updated for {}: {} -> {}", event.getContainer().getUniqueId(), event.getOldValue(), event.getNewValue()).submit();
        }
    }

    private void checkUnsetProperties(Client client) {
        Gamer gamer = client.getGamer();
        validateExistingClientDefaults(client, gamer);

        Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.BALANCE);
        if(coinsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.BALANCE, defaultCoins);
        }

        Optional<Integer> fragmentsOptional = gamer.getProperty(GamerProperty.FRAGMENTS);
        if(fragmentsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.FRAGMENTS, defaultFragments);
        }

        Optional<Long> remainingProtectionOptional = gamer.getProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtectionOptional.isEmpty() && defaultPvPProtection > 0) {
            gamer.saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, (long) (defaultPvPProtection * 1000L));
        }

        Optional<String> preferredSpawnOptional = gamer.getProperty(GamerProperty.PREFERRED_SPAWN);
        if (preferredSpawnOptional.isEmpty()) {
            gamer.saveProperty(GamerProperty.PREFERRED_SPAWN, "");
        }

    }

    private void validateExistingClientDefaults(Client client, Gamer gamer) {
        if (client.isNewClient()) {
            return;
        }

        if (gamer.getProperty(GamerProperty.BALANCE).isPresent()) {
            return;
        }

        if (!manager.getSqlLayer().hasCurrentRealmGamerProperties(client.getId())) {
            return;
        }

        throw new IllegalStateException("Existing client " + client.getName() + " (" + client.getUuid()
                + ") has gamer properties in realm " + Core.getCurrentRealm().getId()
                + " but BALANCE was not loaded into memory");
    }

}
