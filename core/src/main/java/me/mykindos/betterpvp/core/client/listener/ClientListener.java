package me.mykindos.betterpvp.core.client.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@BPvPListener
public class ClientListener implements Listener {

    @Inject
    @Config(path = "pvp.enableOldPvP", defaultValue = "true")
    private boolean enableOldPvP;

    private final ClientManager clientManager;

    @Inject
    public ClientListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        Optional<Client> clientOptional = clientManager.getObject(uuid);
        Client client;
        if (clientOptional.isEmpty()) {
            client = Client.builder().uuid(uuid).name(event.getPlayer().getName()).rank(Rank.PLAYER).build();
            clientManager.addObject(uuid, client);
            clientManager.getRepository().save(client);

            event.joinMessage(Component.text(ChatColor.GREEN + "New> " + ChatColor.GRAY + event.getPlayer().getName()));
        } else {
            event.joinMessage(Component.text(ChatColor.GREEN + "Login> " + ChatColor.GRAY + event.getPlayer().getName()));
            client = clientOptional.get();
        }

        Bukkit.getPluginManager().callEvent(new ClientLoginEvent(client, event.getPlayer()));
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {
        if (enableOldPvP) {
            AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attribute != null) {
                double baseValue = attribute.getBaseValue();

                if (baseValue != 100000000) {
                    attribute.setBaseValue(100000000);
                    event.getPlayer().saveData();

                }
            }
        }

        updateTab(event.getPlayer());
    }

    @UpdateEvent(delay = 5000)
    public void updateTabAllPlayers(){
        Bukkit.getOnlinePlayers().forEach(this::updateTab);
    }

    public void updateTab(Player player) {

        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);

        pc.getChatComponents().write(0, WrappedChatComponent.fromText(ChatColor.RED + "" + ChatColor.BOLD + "Welcome to BetterPvP Clans!\n"
                + ChatColor.RED + "" + ChatColor.BOLD + "Visit our website at: " + ChatColor.YELLOW + ChatColor.BOLD + "http://betterpvp.net"))
                .write(1, WrappedChatComponent.fromText(ChatColor.GOLD.toString() + ChatColor.BOLD + "Ping: " + ChatColor.YELLOW + UtilPlayer.getPing(player)
                        + ChatColor.GOLD.toString() + ChatColor.BOLD + " Online: " + ChatColor.YELLOW + Bukkit.getOnlinePlayers().size()));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }

}
