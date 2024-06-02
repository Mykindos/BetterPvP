package me.mykindos.betterpvp.clans.clans.transport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

@Singleton
@BPvPListener
public class TransportListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public TransportListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getRightClicked() instanceof LivingEntity target)) return;

        Component customName = target.customName();
        if(customName == null) return;

        String name = PlainTextComponentSerializer.plainText().serialize(customName);
        if(name.contains("Travel Hub")) {
            new ClanTravelHubMenu(event.getPlayer(), clanManager).show(event.getPlayer());
        }

    }
}
