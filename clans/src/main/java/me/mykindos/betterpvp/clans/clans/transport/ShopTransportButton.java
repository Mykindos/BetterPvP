package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.TutorialSubCommand;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ShopTransportButton extends ControlItem<ClanTravelHubMenu> {

    private final Clan clan;
    private final Client client;
    private final Material material;
    private final NamedTextColor namedTextColor;

    public ShopTransportButton(Clan clan, Client client, Material material, NamedTextColor namedTextColor) {
        this.clan = clan;
        this.client = client;
        this.material = material;
        this.namedTextColor = namedTextColor;
    }

    @Override
    public ItemProvider getItemProvider(ClanTravelHubMenu clanTravelHubMenu) {
        ItemView.ItemViewBuilder provider = ItemView.builder().material(material)
                .displayName(Component.text(clan.getName(), namedTextColor, TextDecoration.BOLD))
                .action(ClickActions.LEFT, Component.text("Teleport"))
                .action(ClickActions.RIGHT, Component.text("Set Spawn Location"));
        return provider.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (clickType.isLeftClick() && clan.getCore().isSet()) {
            clan.getCore().teleport(player, client, false);
            Component component = Component.empty().append(Component.text("Teleported to "))
                    .append(Component.text(clan.getName(), namedTextColor));
            UtilMessage.message(player, "Clans", component);

            Gamer gamer = client.getGamer();
            Optional<String> property = gamer.getProperty(GamerProperty.PREFERRED_SPAWN);
            if (property.isEmpty() || property.get().isEmpty()) {
                gamer.saveProperty(GamerProperty.PREFERRED_SPAWN, clan.getName());
                UtilMessage.simpleMessage(player, "Clans", "<gray>Set your preferred spawn location to <yellow>%s", clan.getName());
                UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                    UtilMessage.message(player, "Clans", UtilMessage.deserialize("Welcome to <gold>Clans</gold>! It looks like this is your first time playing this season. ")
                            .append(Component.text("Click to join our discord!", NamedTextColor.DARK_PURPLE)
                                    .decoration(TextDecoration.UNDERLINED, true)
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to join our discord!")))
                                    .clickEvent(ClickEvent.openUrl("https://discord.gg/PE32pYfZn9"))
                            )
                            .appendNewline()
                            .append(UtilMessage.deserialize("You can always open the tutorial again by running "))
                            .append(Component.text("/c tutorial", NamedTextColor.YELLOW)
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to open the tutorial!")))
                                    .clickEvent(ClickEvent.runCommand("/c tutorial")))
                    );

                    player.openBook(TutorialSubCommand.book);
                }, 20L);
            }

        } else if (clickType.isRightClick()) {
            Gamer gamer = client.getGamer();
            gamer.saveProperty(GamerProperty.PREFERRED_SPAWN, clan.getName());
            UtilMessage.simpleMessage(player, "Clans", "<gray>Set your preferred spawn location to <yellow>%s", clan.getName());
        }
    }
}
