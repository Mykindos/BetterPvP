package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.ModuleLoadedEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.core.tips.TipEvent;
import me.mykindos.betterpvp.core.tips.TipListener;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

@Singleton
@BPvPListener
public class ClanTipListener extends TipListener {

    public final ClanManager clanManager;

    @Inject
    ClanTipListener(Core core, ClanManager clanManager, GamerManager gamerManager, TipManager tipManager) {
        super(core, gamerManager, tipManager);
        this.clanManager = clanManager;
    }

    @Override
    public void tipSender() {
        //Override and do nothing
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        WeighedList<Tip> tipList = event.getTipList();
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
        if (gamerOptional.isEmpty()) {
            event.cancel("Gamer not found.");
            return;
        }
        Gamer gamer = gamerOptional.get();

        if ((boolean) gamer.getProperty(GamerProperty.TIPS_ENABLED).orElse(true) &&
                UtilTime.elapsed(gamer.getLastTip(), (long) 1 * 1000)
        ) {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            final Clan clan = clanOptional.orElse(null);

            tipManager.getTips().forEach(tip -> {
                if (tip instanceof ClanTip clanTip) {
                    if (clanTip.isValid(player, clan)) {
                        tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                    }
                }
            });

        }
    }



}
