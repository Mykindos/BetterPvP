package me.mykindos.betterpvp.core.client.gamer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.Unique;
import me.mykindos.betterpvp.core.utilities.model.display.ActionBar;
import me.mykindos.betterpvp.core.utilities.model.display.PlayerList;
import me.mykindos.betterpvp.core.utilities.model.display.TitleQueue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A gamer represents a clients seasonal data.
 * Such as their blocks broken, their kills, deaths, etc.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = {"uuid"})
public class Gamer extends PropertyContainer implements Invitable, Unique, IMapListener {

    private final String uuid;
    private ActionBar actionBar = new ActionBar();
    private TitleQueue titleQueue = new TitleQueue();
    private PlayerList playerList = new PlayerList();
    private Sidebar sidebar = new Sidebar();

    private long lastDamaged;
    private long lastSafe;
    private long lastTip;
    private long lastBlock = -1;
    private String lastAdminMessenger;

    public Gamer(String uuid) {
        this.uuid = uuid;
        this.properties.registerListener(this);
    }

    public long timeSinceLastBlock() {
        if (lastBlock == -1) {
            return -1;
        }

        return System.currentTimeMillis() - lastBlock;
    }

    public boolean canBlock() {
        final Player player = getPlayer();
        if (player != null) {
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            final boolean sword = Compatibility.SWORD_BLOCKING && (UtilItem.isSword(main) || UtilItem.isSword(off));
            final boolean shield = main.getType().equals(Material.SHIELD) || off.getType().equals(Material.SHIELD);
            return sword || shield;
        }

        return false;
    }

    public boolean isHoldingRightClick() {
        if (canBlock()) {
            final Player player = getPlayer();
            if (player == null) {
                return false;
            }

            // If they're holding a cosmetic shield, give them a grace period for them to raise their hand
            // Otherwise, this would return false
            final ItemStack main = player.getInventory().getItemInMainHand();
            final ItemStack off = player.getInventory().getItemInOffHand();
            if (UtilItem.isCosmeticShield(main) || UtilItem.isCosmeticShield(off)) {
                return timeSinceLastBlock() <= 250;
            }

            return player.isBlocking() || player.isHandRaised();
        }

        return lastBlock != -1;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(uuid));
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public int getBalance() {
        return (int) getProperty(GamerProperty.BALANCE).orElse(0);
    }

    public int getIntProperty(Enum<?> key) {
        return (int) getProperty(key).orElse(0);
    }
    public long getLongProperty(Enum<?> key) {
        return (long) getProperty(key).orElse(0L);
    }

    public void setSidebar(@Nullable Sidebar sidebar) {
        if (this.sidebar != null && this.isOnline()) {
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> this.sidebar.removeViewer(Objects.requireNonNull(getPlayer())));
        }
        if (sidebar != null && this.isOnline()) {
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> sidebar.addViewer(Objects.requireNonNull(getPlayer())));
        }
        this.sidebar = sidebar;

    }

    @Override
    public void saveProperty(String key, Object object) {
        properties.put(key, object);
    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> UtilServer.callEvent(new GamerPropertyUpdateEvent( this, key, value)));
    }

    public void setLastTipNow() {
        setLastTip(System.currentTimeMillis());
    }
    public void setLastSafeNow() {
        setLastSafe(System.currentTimeMillis());
    }
    public void updateRemainingProtection() {
        long remainingProtection = getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        remainingProtection = remainingProtection - (System.currentTimeMillis() - getLastSafe());
        saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, remainingProtection);
    }

    @Override
    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    public boolean isInCombat() {
        return !UtilTime.elapsed(lastDamaged, 15000);
    }
}
