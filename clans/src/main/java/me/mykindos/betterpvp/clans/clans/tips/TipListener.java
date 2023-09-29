package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
public class TipListener extends ClanListener {

    @Inject
    @Config(path = "clans.tips.timeBetweenTips", defaultValue = "300")
    private int timeBetweenTips;

    private final Clans clans;
    @Inject
    public TipListener(Clans clans, ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClientLogin(ClientLoginEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        Gamer gamer;
        if (gamerOptional.isPresent()) {
            gamer = gamerOptional.get();
            gamer.setLastTip(System.currentTimeMillis());
        }
    }

    @UpdateEvent(delay = 10 * 1000, isAsync = true)
    public void tipSender() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            UtilServer.runTaskLaterAsync(clans, () -> UtilServer.callEvent(new TipEvent(player)), 5);
        });
    }

    public void sendTip(Player player, Tip tip){
        UtilMessage.message(player, "Tips", tip.getComponent());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTip(TipEvent event) {
        Player player = event.getPlayer();
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
        if (gamerOptional.isEmpty()) {
            event.cancel("Gamer not found.");
            return;
        }
        Gamer gamer = gamerOptional.get();

        TipList tipList = new TipList();

        if ((boolean) gamer.getProperty(GamerProperty.TIPS_ENABLED).orElse(true) &&
                UtilTime.elapsed(gamer.getLastTip(), (long) 1 * 1000)
                ) {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isEmpty()) {
                tipList.add(Tip.ClAN_CREATE);
                /*sendTip(player, Tip.ClAN_CREATE);
                gamer.setLastTipNow();
                return;*/
            }
            //do stuff that non-clan members get tips for

            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                if (clan.getSquadCount() == 1) {
                    tipList.add(Tip.CLAN_INVITE);
                /*sendTip(player, Tip.CLAN_INVITE);
                gamer.setLastTipNow();
                return;*/
                }
            }

            if (tipList.size() > 0) {
                Tip tip = tipList.random();
                sendTip(player, tip);
                gamer.setLastTipNow();
            }
        }
    }
}
