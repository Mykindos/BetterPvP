package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.AsyncClientPreLoadEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PlayerListType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
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
    @Config(path = "tab.shop", defaultValue = "mineplex.com/shop")
    private String shop;

    @Inject
    @Config(path = "tab.server", defaultValue = "Clans-1")
    private String server;

    private final ClientManager manager;

    @Inject
    public GamerListener(ClientManager manager) {
        this.manager = manager;

        this.header = new PermanentComponent(gamer -> Component.text("Mineplex ", NamedTextColor.GOLD)
                .append(Component.text("Network ", NamedTextColor.WHITE))
                .append(Component.text(Objects.requireNonNull(server, ""), NamedTextColor.GREEN)));

        this.footer = new PermanentComponent(gamer -> Component.text("Type ", NamedTextColor.WHITE)
                .append(Component.text(Objects.requireNonNull(shop, ""), NamedTextColor.YELLOW))
                .append(Component.text(" for cool perks!", NamedTextColor.WHITE)));
    }

    @UpdateEvent (isAsync = true)
    public void onUpdate() {
        this.manager.getOnline().forEach(client -> {
            final Gamer gamer = client.getGamer();
            gamer.getActionBar().show(gamer);
            gamer.getTitleQueue().show(gamer);
            gamer.getPlayerList().show(gamer);
        });
    }

    @EventHandler
    public void onPreClientLoad(AsyncClientPreLoadEvent event) {
        this.manager.loadGamerProperties(event.getClient());
    }

    @EventHandler (priority =  EventPriority.MONITOR)
    public void onClientLoad(AsyncClientLoadEvent event) {
        final Gamer gamer = event.getClient().getGamer();
        checkUnsetProperties(gamer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Gamer gamer = this.manager.search().online(event.getPlayer()).getGamer();

        gamer.getPlayerList().clear();
        gamer.getPlayerList().add(PlayerListType.FOOTER, footer);
        gamer.getPlayerList().add(PlayerListType.HEADER, header);
    }

    private void checkUnsetProperties(Gamer gamer) {

        Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.BALANCE);
        if(coinsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.BALANCE, defaultCoins);
        }

        Optional<Integer> fragmentsOptional = gamer.getProperty(GamerProperty.FRAGMENTS);
        if(fragmentsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.FRAGMENTS, defaultFragments);
        }

        Optional<Long> remainingProtectionOptional = gamer.getProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtectionOptional.isEmpty()) {
            gamer.saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, (long) (defaultPvPProtection * 1000L));
        }

    }

}
