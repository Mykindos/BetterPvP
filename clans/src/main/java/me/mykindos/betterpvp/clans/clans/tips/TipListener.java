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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
public class TipListener extends ClanListener {

    @Inject
    @Config(path = "clans.tips.timeBetweenTips", defaultValue = "150")
    private int timeBetweenTips;

    private final Clans clans;

    private final TipManager tipManager;
    @Inject
    public TipListener(Clans clans, ClanManager clanManager, GamerManager gamerManager, TipManager tipManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
        this.tipManager = tipManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClientLogin(ClientLoginEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        Gamer gamer;
        if (gamerOptional.isPresent()) {
            gamer = gamerOptional.get();
            gamer.setLastTipNow();
        }
    }

    @UpdateEvent(delay = 10 * 1000, isAsync = true)
    public void tipSender() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            UtilServer.runTaskLaterAsync(clans, () -> UtilServer.callEvent(new TipEvent(player)), 5);
        });
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
                tipList.add(TipOld.ClAN_CREATE);
            }

            tipList.add(TipOld.CLAN_HELP);

            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                if (clan.getSquadCount() == 1) {
                    tipList.add(TipOld.CLAN_INVITE);
                }

                if (clan.getHome() == null) {
                    tipList.add(TipOld.CLAN_HOME);
                }

                tipList.add(TipOld.CLAN_ENERGY);
            }

            if (tipList.size() > 0) {
                TipOld tipOld = tipList.random();
                UtilMessage.message(player, "Tips", tipOld.getComponent());
                gamer.setLastTipNow();
            }
        }
    }
}
