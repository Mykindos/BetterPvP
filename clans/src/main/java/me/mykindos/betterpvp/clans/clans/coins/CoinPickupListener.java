package me.mykindos.betterpvp.clans.clans.coins;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalInt;

@BPvPListener
@Singleton
public class CoinPickupListener implements Listener {

    private final ClientManager clientManager;

    @Inject
    public CoinPickupListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPickup(InventoryPickupItemEvent event) {
        final PersistentDataContainer pdc = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer();
        if (pdc.has(ClansNamespacedKeys.COIN_AMOUNT, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            //Stop hoppers from picking up coins
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickup(EntityPickupItemEvent event) {

        if(event.isCancelled()) {
            return;
        }

        final OptionalInt coinsOpt = CoinItem.getCoinAmount(event.getItem().getItemStack());
        if (coinsOpt.isEmpty()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true); // Only players can pick up coins
            return;
        }

        final Gamer gamer = this.clientManager.search().online(player).getGamer();

        final int coins = coinsOpt.getAsInt();

        // Success
        event.getItem().remove();
        event.setCancelled(true);

        int newBalance = gamer.getBalance() + coins;
        gamer.saveProperty(GamerProperty.BALANCE, newBalance);
        new SoundEffect(Sound.ENTITY_ARROW_HIT_PLAYER, 2.0f, 2f).play(player);
        final TextComponent text = Component.text("+" + UtilFormat.formatNumber(coins) + " Coins!", TextColor.color(255, 215, 0));
        gamer.getActionBar().add(5, new TimedComponent(2, true, gmr -> text));
    }

}
