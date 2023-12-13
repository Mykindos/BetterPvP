package me.mykindos.betterpvp.core.client.events;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ClientJoinEvent extends CustomEvent {

    private final Client client;
    private final Player player;
    @Setter
    private Component joinMessage;

    public ClientJoinEvent(Client client, Player player) {
        this.client = client;
        this.player = player;
        this.joinMessage = client.isNewClient()
                ? Component.text("New> ", NamedTextColor.GREEN).append(Component.text(player.getName(), NamedTextColor.GRAY))
                : Component.text("Join> ", NamedTextColor.GREEN).append(Component.text(player.getName(), NamedTextColor.GRAY));
    }
}
