package me.mykindos.betterpvp.game.impl.domination.model;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.impl.domination.Domination;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

@Data
public class CapturePointFX {

    private final CapturePoint point;
    private final ClientManager clientManager;
    private final Domination game;

    public void tick() {

    }

    public void capture(Team team) {
        // Firework
        final Location midpoint = UtilLocation.getMidpoint(point.getRegion().getMin(), point.getRegion().getMax());
        Firework firework = midpoint.getWorld().spawn(midpoint, Firework.class);
        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.setPower(1);
        final TextColor color = team.getProperties().color();
        final FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.fromRGB(color.value()))
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .build();
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        firework.getPersistentDataContainer().set(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN, true);
        firework.detonate();

        // Title
        final Component component = Component.text(String.format("%s captured %s", team.getProperties().name(), point.getName()), color);
        final TitleComponent title = TitleComponent.subtitle(0, 2, 0.7, true, gmr -> component);
        for (Client client : clientManager.getOnline()) {
            client.getGamer().getTitleQueue().add(0, title);
        }

        // Sounds
        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, 10F).play(midpoint);
        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.4f, 10F).play(midpoint);
        final SoundEffect captured = new SoundEffect("betterpvp", "game.domination.point_captured");
        final SoundEffect lost = new SoundEffect("betterpvp", "game.domination.point_lost");
        for (Team participant : game.getParticipants()) {
            if (participant == team) {
                captured.play(participant);
            } else {
                lost.play(participant);
            }
        }
    }
}
