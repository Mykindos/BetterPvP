package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.EnergyCheckEvent;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.components.clans.events.ClansDropEnergyEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.OptionalInt;

@BPvPListener
@Singleton
public class ClanEnergyListener extends ClanListener {

    private final Clans clans;
    private final BlockTagManager blockTagManager;
    private final EffectManager effectManager;

    @Inject
    @Config(path = "clans.energy.enabled", defaultValue = "true")
    private boolean enabled;
    @Inject
    @Config(path = "clans.energy.energyWarnLevel", defaultValue = "30.0")
    private double energyWarnLevel;

    @Inject
    @Config(path = "fields.blocks.energy.minEnergy", defaultValue = "25")
    private int minEnergy;

    @Inject
    @Config(path = "fields.blocks.energy.maxEnergy", defaultValue = "50")
    private int maxEnergy;

    @Inject
    ClanEnergyListener(Clans clans, ClanManager clanManager, ClientManager clientManager, BlockTagManager blockTagManager, EffectManager effectManager) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
    }

    @UpdateEvent(delay = 300 * 1000, isAsync = true)
    public void checkEnergy() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            final Optional<Clan> clanOpt = this.clanManager.getClanByPlayer(player);
            if (clanOpt.isEmpty()) {
                continue;
            }

            final Clan clan = clanOpt.get();

            UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new EnergyCheckEvent(player, clan)), 5);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnergyCheck(EnergyCheckEvent event) {
        if (!enabled) return;
        Clan clan = event.getClan();
        if (clan.getEnergyDuration() >= energyWarnLevel * 1000 * 60) {
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
        if (!enabled) return;
        clanManager.getObjects().forEach((name, clan) -> {
            if (clan.isAdmin()) {
                return;
            }

            if (clan.isOnline()) {
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
                clan.messageClan("If your clan does not acquire more energy soon, your clan will disband!", null, true);
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

        if (event.getItem().getItemStack().getType() != Material.AMETHYST_SHARD) {
            return;
        }

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

        final Client client = this.clientManager.search().online(player);
        final int energy = energyOpt.getAsInt();

        // Success
        event.getItem().remove();
        event.setCancelled(true);
        clan.grantEnergy(energy);
        client.getStatContainer().incrementStat(ClientStat.CLANS_ENERGY_COLLECTED, energy);

        // Cues
        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 2f).play(player);
        final TextComponent text = Component.text("+" + energy + " Clan Energy", TextColor.color(173, 123, 212));
        client.getGamer().getActionBar().add(5, new TimedComponent(2, true, gmr -> text));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakEnergyOutsideOfFields(BlockBreakEvent event) {
        if (event.getBlock().getType().name().contains("AMETHYST_BUD") || event.getBlock().getType() == Material.AMETHYST_CLUSTER) {

            Optional<Clan> clanByLocation = clanManager.getClanByLocation(event.getPlayer().getLocation());
            if (clanByLocation.isEmpty() || !clanByLocation.get().isAdmin()) {
                event.setDropItems(false);

                final int range = maxEnergy - minEnergy;
                // depending on the type, distribute the energy amount between the range of min and max energy
                int amount = switch (event.getBlock().getType()) {
                    case Material.SMALL_AMETHYST_BUD -> minEnergy + UtilMath.RANDOM.nextInt(range / 4);
                    case Material.MEDIUM_AMETHYST_BUD -> minEnergy + UtilMath.RANDOM.nextInt(range / 3);
                    case Material.LARGE_AMETHYST_BUD -> minEnergy + range / 3 + UtilMath.RANDOM.nextInt(range / 3);
                    case Material.AMETHYST_CLUSTER -> minEnergy + 2 * range / 3 + UtilMath.RANDOM.nextInt(range / 3);
                    default -> 0;
                };

                final ItemStack item = ItemView.builder()
                        .material(Material.AMETHYST_SHARD)
                        .displayName(Component.text("Energy Shard", TextColor.color(227, 156, 255)))
                        .frameLore(true)
                        .lore(Component.text("Deposit this item into your clan core", NamedTextColor.GRAY))
                        .lore(Component.text("to gain energy.", NamedTextColor.GRAY))
                        .lore(Component.empty())
                        .lore(Component.text("This item yields ", NamedTextColor.GRAY)
                                .append(Component.text(amount + " energy", NamedTextColor.YELLOW)))
                        .build()
                        .get();

                item.editMeta(meta -> {
                    final PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    pdc.set(ClansNamespacedKeys.ENERGY_AMOUNT, PersistentDataType.INTEGER, amount);
                    pdc.set(ClansNamespacedKeys.AUTO_DEPOSIT, PersistentDataType.BOOLEAN, true);
                });

                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropEnergy(ClansDropEnergyEvent event) {
        ItemStack energyItem = EnergyItem.SHARD.generateItem(event.getAmount(), true);
        Item drop = event.getLocation().getWorld().dropItem(event.getLocation(), energyItem);
        if (!(event.getLivingEntity() instanceof Player player)) return;
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;
        UtilItem.reserveItem(drop, player, 10.0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakBlock(BlockBreakEvent event) {
        if(!blockTagManager.isPlayerPlaced(event.getBlock())){
            if (UtilMath.RANDOM.nextDouble() > 0.8) {
                UtilServer.callEvent(new ClansDropEnergyEvent(event.getPlayer(), event.getBlock().getLocation(), 2));
            }
        }
    }

}

