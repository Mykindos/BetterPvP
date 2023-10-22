package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanBuyEnergyEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.EnergyCheckEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.text.NumberFormat;
import java.util.Optional;

@BPvPListener
public class ClanEnergyListener extends ClanListener {

    private final Clans clans;

    @Inject
    @Config(path = "clans.energy.energyWarnLevel", defaultValue = "20.0")
    private double energyWarnLevel;

    @Inject
    ClanEnergyListener(Clans clans, ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
    }

    @UpdateEvent(delay = 300 * 1000, isAsync = true)
    public void checkEnergy() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new EnergyCheckEvent(player, clan)), 5);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnergyCheck(EnergyCheckEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Clan clan = event.getClan();
        if (clan.getEnergy() < energyWarnLevel) {
            Component title = Component.text("CLAN ENERGY LOW", NamedTextColor.RED);
            Component subTitle = Component.text("Time Remaining: ", NamedTextColor.YELLOW).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN));
            player.showTitle(Title.title(title, subTitle));
            UtilMessage.message(player, "Clans", title);
            UtilMessage.message(player, "Clans", subTitle);
        }
    }

    @EventHandler
    public void onBuyEnergy(ClanBuyEnergyEvent event) {
        if (event.isCancelled()) return;

        gamerManager.getObject(event.getPlayer().getUniqueId()).ifPresent(gamer -> {
            if (gamer.getBalance() < event.getCost()) {
                UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You do not have enough money to buy this amount of energy.");
                event.setCancelled(true);
            }
        });

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBuyEnergyFinal(ClanBuyEnergyEvent event) {
        if (event.isCancelled()) return;

        gamerManager.getObject(event.getPlayer().getUniqueId()).ifPresent(gamer -> {
            gamer.saveProperty(GamerProperty.BALANCE.name(), gamer.getBalance() - event.getCost(), true);
            event.getClan().setEnergy(event.getClan().getEnergy() + event.getAmount());

            UtilSound.playSound(event.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F, false);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You bought <yellow>%s<gray> energy for <yellow>$%s<gray>.",
                    NumberFormat.getInstance().format(event.getAmount()), NumberFormat.getInstance().format(event.getCost()));
        });
    }

    @UpdateEvent(delay = 60_000 * 5)
    public void processClanEnergy() {
        clanManager.getObjects().forEach((name, clan) -> {
            if (clan.getTerritory().isEmpty()) return;
            if (clan.isAdmin()) return;

            clan.setEnergy(clan.getEnergy() - (((int) clan.getEnergyRatio() / 12)));
            if (clan.getEnergy() - (((int) clan.getEnergyRatio() / 12)) <= 0) {
                clan.messageClan("If you do not buy more energy, your clan will disband in 5 minutes.", null, true);
            }

            if (clan.getEnergy() <= 0) {
                UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new ClanDisbandEvent(null, clan)), 1);
            }
        });
    }
}

