package me.mykindos.betterpvp.champions.champions.skills.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.champions.properties.ChampionsProperty;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Optional;

@BPvPListener
public class SkillChatListener implements Listener {

    @Inject
    @Config(path = "chat.skill.preview", defaultValue = "true")
    private boolean chatSkillPreview;

    private final SkillManager skillManager;
    private final ClientManager clientManager;

    @Inject
    public SkillChatListener(SkillManager skillManager, ClientManager clientManager) {
        this.skillManager = skillManager;
        this.clientManager = clientManager;
    }

    /**
     * Show a preview of the skill when hovering over the skill name in chat
     *
     * @param event - ChatSentEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(ChatReceivedEvent event) {
        if (event.isCancelled()) return;
        if (!chatSkillPreview) return;

        Gamer gamer = clientManager.search().online(event.getTarget()).getGamer();
        Optional<Boolean> skillChatPreviewOptional = gamer.getProperty(ChampionsProperty.SKILL_CHAT_PREVIEW);
        if (skillChatPreviewOptional.isPresent()) {
            boolean skillChatPreview = skillChatPreviewOptional.get();
            if (!skillChatPreview) return;
        }

        String messageText = PlainTextComponentSerializer.plainText().serialize(event.getMessage());
        for (Map.Entry<String, Skill> entry : skillManager.getObjects().entrySet()) {
            if (!messageText.toLowerCase().contains(entry.getKey().toLowerCase())) continue;
            int index = messageText.toLowerCase().indexOf(entry.getKey().toLowerCase());
            Skill skill = entry.getValue();

            Component skillComponent = Component.text(messageText.substring(index, index + skill.getName().length()))
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                    .hoverEvent(HoverEvent.showText(getDescriptionComponent(skill)));

            Component newComponent = Component.text(messageText.substring(0, index))
                    .append(skillComponent)
                    .append(Component.empty())
                    .append(Component.text(messageText.substring(index + skill.getName().length())))
                    .color(NamedTextColor.WHITE);


            event.setMessage(newComponent);
            return;
        }

    }

    private Component getDescriptionComponent(Skill skill) {
        String[] lore = skill.getDescription(1);
        
        Component component = Component.empty();
        for (String str : lore) {
            component = component.append(MiniMessage.miniMessage().deserialize("<gray>" + str, SkillMenu.TAG_RESOLVER));
            component = component.append(Component.newline());
        }
        
        return component;
    }
}
