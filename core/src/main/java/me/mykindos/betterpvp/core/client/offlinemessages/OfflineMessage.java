package me.mykindos.betterpvp.core.client.offlinemessages;

import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

@Data
public class OfflineMessage implements Describable {
    private final UUID client;
    private final long time;
    private final Action action;
    private final String rawContent;


    public Component getContent() {
        return Component.empty().color(NamedTextColor.WHITE)
                .append(miniMessage.deserialize(rawContent, TagResolver.standard()));
    }

    public void send() {
        Player player = Bukkit.getPlayer(client);
        if (player != null) {
            Component message = Component.text(UtilTime.getDateTime(time)).appendSpace().append(getContent());
            UtilMessage.message(player, action.getTitle(), message);
        }

    }

    /**
     * @return the description of this entity
     */
    @Override
    public Description getDescription() {
        List<Component> lore = List.of(
                Component.text(UtilTime.getDateTime(time), NamedTextColor.WHITE),
                Component.text(UtilTime.getTime(System.currentTimeMillis() - time, 1), NamedTextColor.WHITE),
                UtilMessage.DIVIDER,
                getContent()
        );
        ItemProvider itemProvider = ItemView.builder()
                .displayName(Component.text(action.getTitle(), NamedTextColor.WHITE))
                .material(action.getMaterial())
                .customModelData(action.getCustomModelData())
                .lore(lore)
                .frameLore(true)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

    @Getter
    public enum Action {
        CLAN_DISBAND("Disband", Material.TNT, 0),
        CLAN_KICK("Kick", Material.PURPLE_BED, 0),
        CLAN_PILLAGE("Pillage", Material.GOAT_HORN, 0),
        PUNISHMENT("Punishment", Material.ANVIL, 0),
        OFFLINE_DEATH("Offline Death", Material.BONE, 0),
        OTHER("Other", Material.PAPER, 0);

        private final String title;
        private final Material material;
        private final int customModelData;

        Action(String title, Material material, int customModelData) {
            this.title = title;
            this.material = material;
            this.customModelData = customModelData;
        }

        public static Action fromString(String name) {
            return Arrays.stream(Action.values()).filter(action -> action.name().equalsIgnoreCase(name)).findFirst().orElse(Action.OTHER);
        }
    }
}
