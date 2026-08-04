package me.mykindos.betterpvp.clans.world.crew.menu;

import lombok.NonNull;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.crew.menu.button.CrewMemberButton;
import me.mykindos.betterpvp.clans.world.crew.menu.button.CrewRequestButton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.HorizontalScrollGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The quartermaster's book: who is sailing with you, and who is asking to.
 * <p>
 * Two bands, same shape as the broker's list. The crew is pinned above so an answer is never buried under a long queue
 * of hopefuls, and the queue scrolls below it.
 * <p>
 * Only the captain sees buttons that do anything — a member opening this is reading a roster, not editing one.
 */
public class CrewMenu extends AbstractGui implements Windowed {

    private final CrewService crewService;
    private final ClientManager clientManager;
    private final Crew crew;
    private final Player viewer;

    private final HorizontalScrollGui rosterBand = new HorizontalScrollGui(9, 1);
    private final HorizontalScrollGui requestBand = new HorizontalScrollGui(9, 3);

    public CrewMenu(@NotNull CrewService crewService, @NotNull ClientManager clientManager,
                    @NotNull Crew crew, @NotNull Player viewer) {
        super(9, 6);
        this.crewService = crewService;
        this.clientManager = clientManager;
        this.crew = crew;
        this.viewer = viewer;

        fillRectangle(0, 0, rosterBand, true);
        fillRectangle(0, 2, requestBand, true);

        fill(9, 18, Menu.BACKGROUND_GUI_ITEM, true);
        fill(45, 54, Menu.BACKGROUND_GUI_ITEM, true);

        refresh();
    }

    /**
     * Rebuilds both bands from the crew as it stands. Offline players are skipped rather than shown greyed out: they
     * cannot be taken aboard, and a head that does nothing when clicked reads as a broken button.
     */
    public void refresh() {
        final List<Item> roster = new ArrayList<>();
        for (UUID member : crew.roster()) {
            final Player online = Bukkit.getPlayer(member);
            if (online != null) {
                roster.add(new CrewMemberButton(crewService, crew, online, this::refresh));
            }
        }
        rosterBand.setItems(roster);

        final List<Item> requests = new ArrayList<>();
        if (isCaptain()) {
            for (UUID requester : crew.pendingRequests()) {
                final Player online = Bukkit.getPlayer(requester);
                if (online != null) {
                    requests.add(new CrewRequestButton(crewService, clientManager, crew, online, this::refresh));
                }
            }
        }
        requestBand.setItems(requests);
    }

    private boolean isCaptain() {
        return crew.getCaptain().equals(viewer.getUniqueId());
    }

    @Override
    public Window show(@NonNull Player player) {
        return Windowed.super.show(player);
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component(isCaptain() ? "clans.crew.menu.title.captain" : "clans.crew.menu.title.member");
    }
}
