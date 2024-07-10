package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.EnergyCheckEvent;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.OptionalInt;

@BPvPListener
public class ClanEnergyListener extends ClanListener {

    private final Clans clans;

    @Inject
    @Config(path = "clans.energy.energyWarnLevel", defaultValue = "20.0")
    private double energyWarnLevel;

    @Inject
    ClanEnergyListener(Clans clans, ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        this.clans = clans;
    }

    @UpdateEvent(delay = 300 * 1000, isAsync = true)
    public void checkEnergy() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final Optional<Clan> clanOpt = this.clanManager.getClanByPlayer(player);
            if (clanOpt.isEmpty()) {
                continue;
            }

            final Clan clan = clanOpt.get();
            if (clan.getTerritory().isEmpty()) {
                continue;
            }

            UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new EnergyCheckEvent(player, clan)), 5);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnergyCheck(EnergyCheckEvent event) {
        Clan clan = event.getClan();
        if (clan.getEnergy() >= energyWarnLevel) {
            return;
        }

        Player player = event.getPlayer();
        Component title = Component.text("CLAN ENERGY LOW", NamedTextColor.RED);
        Component subTitle = Component.text("Time Remaining: ", NamedTextColor.YELLOW)
                .append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN));
        player.showTitle(Title.title(title, subTitle));
        UtilMessage.message(player, "Clans", title);
        UtilMessage.message(player, "Clans", subTitle);
    }

    @UpdateEvent(delay = 60_000 * 5)
    public void processClanEnergy() {
        clanManager.getObjects().forEach((name, clan) -> {
            if (clan.getTerritory().isEmpty() || clan.isAdmin()) {
                return;
            }

            // Deplete their energy
            final int depletion = (int) clan.getEnergyDepletionRatio() / 12;
            clan.setEnergy(clan.getEnergy() - depletion);

            if (clan.getEnergy() <= 0) {
                // Disband the clan if energy is 0
                UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new ClanDisbandEvent(null, clan)), 1);
            } else if (clan.getEnergy() - depletion <= 0) {
                // Otherwise, check if they will disband after next depletion
                clan.messageClan("If you do not buy more energy, your clan will disband in 5 minutes.", null, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPickup(InventoryPickupItemEvent event) {
        final PersistentDataContainer pdc = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.ENERGY_AMOUNT, PersistentDataType.INTEGER)
                || !pdc.has(ClansNamespacedKeys.AUTO_DEPOSIT, PersistentDataType.BOOLEAN)) {
            return; // Not an auto-deposit energy-shard
        }

        // Remove auto-deposit energy shards from being picked up by blocks
        event.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        final OptionalInt energyOpt = EnergyItem.getEnergyAmount(event.getItem().getItemStack(), true);
        if (energyOpt.isEmpty()) {
            return;
        }

        final LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            event.setCancelled(true); // Only players can pick up energy items
            return;
        }

        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            return; // Let them pick up the item, but don't give them energy, they can cash it in later
        }

        final Gamer gamer = this.clientManager.search().online(player).getGamer();
        final int energy = energyOpt.getAsInt();

        // Success
        event.getItem().remove();
        event.setCancelled(true);
        clan.grantEnergy(energy);

        // Cues
        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 2f).play(player);
        final TextComponent text = Component.text("+" + energy + " Clan Energy", TextColor.color(173, 123, 212));
        gamer.getActionBar().add(5, new TimedComponent(1, true, gmr -> text));
    }
}

