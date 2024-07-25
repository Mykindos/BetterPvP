package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;

import java.util.*;

@Singleton
@BPvPListener
public class RunedPickaxe extends ChannelWeapon implements InteractWeapon, LegendaryWeapon, Listener {

    private double miningSpeed;
    private double minEnergyToMine;

    private final EnergyHandler energyHandler;
    private final ClientManager clientManager;
    private final Set<UUID> cooldownPlayers;

    @Inject
    public RunedPickaxe(Champions champions, EnergyHandler energyHandler, ClientManager clientManager) {
        super(champions, "runed_pickaxe");
        this.energyHandler = energyHandler;
        this.clientManager = clientManager;
        this.cooldownPlayers = new HashSet<>();
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("A pickaxe of legendary power, capable ", NamedTextColor.WHITE));
        lore.add(Component.text("of mining any block instantly!", NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<yellow>Requires at least <green>" + (minEnergyToMine * 100) + "% <yellow>energy"));
        lore.add(UtilMessage.deserialize("<yellow>to use <green>Instant Mine"));
        return lore;
    }


    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }


    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent event) {

        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        if (!enabled) return;
        if (!isHoldingWeapon(player)) return;

        setMiningSpeed(player, (float) miningSpeed);

        if (!canUse(player)) {
            return;
        }

        Block block = event.getClickedBlock();

        if(block == null) return;

        if (!energyHandler.use(player, "Runed Pickaxe", energyPerTick, true)) {
            cooldownPlayers.add(player.getUniqueId());
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0F, 1.0F);
            return;
        }

        if (block.getType() == Material.BEDROCK) return;

        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        UtilServer.callEvent(blockBreakEvent);

        if (blockBreakEvent.isCancelled()) return;

        block.breakNaturally();
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
        energyHandler.degenerateEnergy(player, (energyPerTick / 100));
    }

    private void setMiningSpeed(Player player, float speed){
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        ToolComponent toolComponent = meta.getTool();
        toolComponent.setDefaultMiningSpeed(speed);
        meta.setTool(toolComponent);
        item.setItemMeta(meta);
    }


    @Override
    public boolean canUse(Player player) {
        if (energyHandler.getEnergy(player) >= minEnergyToMine){
            cooldownPlayers.remove(player.getUniqueId());
            return true;
        } else return !(energyHandler.getEnergy(player) < minEnergyToMine) || !cooldownPlayers.contains(player.getUniqueId());
    }


    @Override
    public double getEnergy() {
        return initialEnergyCost;
    }


    @Override
    public void loadWeaponConfig() {
        miningSpeed = getConfig("miningSpeed", 28.6, Double.class);
        minEnergyToMine = getConfig("minEnergyToMine", 0.75, Double.class);
    }
}
