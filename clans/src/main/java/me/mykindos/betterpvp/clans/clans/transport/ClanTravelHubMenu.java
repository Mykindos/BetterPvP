package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class ClanTravelHubMenu extends AbstractGui implements Windowed {

    private final Player player;
    private final Client client;
    private final ClanManager clanManager;
    private final ZoneManager zoneManager;

    /** True while a teleport press is "activating"; locks out further clicks and drives the waystone animation. */
    private boolean teleporting = false;
    /** The single button currently showing its pressed state, so a click only lights up the button that was clicked. */
    private Item activeButton = null;
    /** The slot-16 idle animation, hidden for the duration of the buffer. */
    private Item animationItem;

    public ClanTravelHubMenu(Player player, Client client, ClanManager clanManager) {
        super(9, 6);
        this.player = player;
        this.client = client;
        this.clanManager = clanManager;
        this.zoneManager = JavaPlugin.getPlugin(Clans.class).getInjector().getInstance(ZoneManager.class);

        loadMenu();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-8><glyph:menu_waystone>").font(NEXO);
    }

    private void loadMenu() {
        // Spawn waypoint
        zoneManager.getZone(ClanZones.regionKey("Spawn")).ifPresent(spawn ->
                setItem(22, new SpawnTransportButton(spawn, client, Material.BLUE_WOOL, NamedTextColor.BLUE)));

        // Clan home waypoint
        clanManager.getClanByPlayer(player).ifPresent(clan ->
                setItem(4, new CoreTransportButton(clan)));

        // Static frame detail
        setItem(0, new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .hideTooltip(true)
                .itemModel(Key.key("betterpvp", "menu/gui/waystone/detail_1"))
                .build()));

        // Idle animation — hidden while a teleport is buffering
        animationItem = new ControlItem<>() {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                // ignore
            }

            @Override
            public ItemProvider getItemProvider(Gui gui) {
                if (teleporting) {
                    return ItemView.builder()
                            .material(Material.PAPER)
                            .hideTooltip(true)
                            .itemModel(Key.key("betterpvp", "menu/gui/waystone/detail_2"))
                            .build();
                }
                return ItemView.EMPTY;
            }
        };
        setItem(1, animationItem);
    }

    /**
     * Starts the shared 0.5s teleport buffer: locks the menu, lights up the clicked {@code button}, hides the idle
     * animation, then after the buffer runs {@code teleportAction}. Re-entrant clicks while buffering are ignored.
     *
     * @param button         the button that was clicked, lit up for the duration of the buffer
     * @param teleportAction the teleport to perform once the buffer elapses
     */
    public void beginTeleport(@NotNull Item button, @NotNull Runnable teleportAction) {
        if (teleporting) {
            return;
        }

        teleporting = true;
        activeButton = button;
        refreshBufferItems(button);
        new SoundEffect(Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1F).play(player, player);

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), () -> {
            teleporting = false;
            activeButton = null;
            refreshBufferItems(button);
            teleportAction.run();
        }, 5L); // 0.25s
    }

    /**
     * @param button a transport button
     * @return whether that button is the one currently lit up by an active teleport buffer
     */
    public boolean isPressed(@Nullable Item button) {
        return teleporting && button == activeButton;
    }

    private void refreshBufferItems(@NotNull Item button) {
        button.notifyWindows();
        if (animationItem != null) {
            animationItem.notifyWindows();
        }
    }
}
