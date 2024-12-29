package me.mykindos.betterpvp.clans.logging.menu;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.button.PlayerButton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.logging.menu.button.RefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class PlayersOfClanMenu extends AbstractPagedGui<Item> implements Windowed {

    private final String name;
    private final UUID id;
    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final Windowed previous;

    public PlayersOfClanMenu(String name, UUID id, ClanManager clanManager, ClientManager clientManager, Windowed previous) {
        super(9, 4, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton())
                .addIngredient('R', new RefreshButton<>())
        );
        this.name = name;
        this.id = id;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.previous = previous;

        if (getItem(8, 3) instanceof IRefreshButton refreshButton) {
            refreshButton.setRefresh(this::refresh);
        }
        setContent(List.of(new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Loading..."))
                .build())
        ));
        refresh();
    }

    private CompletableFuture<Boolean> refresh() {
        CompletableFuture<List<Item>> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            List<UUID> players = clanManager.getRepository().getPlayersByClan(id);
            List<Client> clients = new ArrayList<>();
            players.forEach(playerID -> {
                clientManager.search().offline(playerID, (clientOptional) -> {
                    clientOptional.ifPresent(clients::add);
                }, false);
            });
            return clients.stream()
                    .map(client -> new PlayerButton(client, clanManager, clientManager, this))
                    .map(Item.class::cast).toList();
        });
        future = future.exceptionally((throwable -> {
            log.error("Error Players of Clan: {}", throwable).submit();
            return List.of(new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Component.text("Error! Check console!"))
                    .lore(Component.text("Please inform staff if you see this"))
                    .build()));
        }));
        return future.thenApply(logs -> {
            setContent(logs);
            return true;
        });
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(name + "'s Players");
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }
}
