package me.mykindos.betterpvp.game.impl.ctf.model;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.DEFAULT;
import static me.mykindos.betterpvp.core.utilities.Resources.Font.SMALL_CAPS;

@RequiredArgsConstructor
public class FlagFX {
    private final Flag flag;
    private final ClientManager clientManager;
    private final CaptureTheFlag game;
    
    public void tick() {
        final Player holder = flag.getHolder();
        if (holder != null) {
            final TeamProperties properties = flag.getTeam().getProperties();
            final Location location = UtilPlayer.getMidpoint(holder);
            Particle.DUST_COLOR_TRANSITION.builder()
                    .location(location)
                    .colorTransition(properties.color().value(), properties.secondary().value())
                    .count(5)
                    .offset(0.2, 0.2, 0.2)
                    .receivers(60)
                    .spawn();
        }
    }

    public void playReturnEffects() {
        Location location = flag.getCurrentLocation();

        // Spawn firework
        Firework firework = location.getWorld().spawn(location, Firework.class);
        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.setPower(1);
        final TeamProperties properties = flag.getTeam().getProperties();
        final FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.fromRGB(properties.color().value()))
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .build();
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        firework.getPersistentDataContainer().set(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN, true);
        firework.detonate();

        // Title
        final Component selfFlag = Component.text(properties.name() + " Flag", properties.color()).font(DEFAULT);
        showSubTitle(selfFlag.append(Component.text(" has returned to base", NamedTextColor.WHITE).font(SMALL_CAPS)));

        // Sound
        final SoundEffect returnedSelf = new SoundEffect("betterpvp", "game.capture_the_flag.flag.returned");
        final SoundEffect returnedOther = new SoundEffect("betterpvp", "game.capture_the_flag.flag.returned", 0.9f);
        for (Team team : game.getParticipants()) {
            if (team == flag.getTeam()) {
                returnedSelf.play(team);
            } else {
                returnedOther.play(team);
            }
        }
    }

    public void playPickupEffects(Player holder, Team holderTeam) {
        final Component holderText = Component.text(holder.getName(), holderTeam.getProperties().color()).font(DEFAULT);
        final TeamProperties properties = flag.getTeam().getProperties();
        final Component selfFlag = Component.text(properties.name() + " Flag!", properties.color()).font(DEFAULT);

        // Title
        final SoundEffect soundSelf;
        final SoundEffect soundOther;
        if (flag.getState() == Flag.State.AT_BASE) {
            showSubTitle(holderText
                    .appendSpace()
                    .append(Component.text("stole", NamedTextColor.WHITE).font(SMALL_CAPS))
                    .appendSpace()
                    .append(selfFlag));
            soundSelf = new SoundEffect("betterpvp", "game.capture_the_flag.flag.stolen", 0.9f);
            soundOther = new SoundEffect("betterpvp", "game.capture_the_flag.flag.stolen", 1);
        } else {
            showSubTitle(holderText
                    .appendSpace()
                    .append(Component.text(" picked up ", NamedTextColor.WHITE).font(SMALL_CAPS))
                    .appendSpace()
                    .append(selfFlag));
            soundSelf = new SoundEffect("betterpvp", "game.capture_the_flag.flag.picked_up", 0.9f);
            soundOther = new SoundEffect("betterpvp", "game.capture_the_flag.flag.picked_up", 1);
        }

        // Sound
        for (Team team : game.getParticipants()) {
            if (team == flag.getTeam()) {
                soundSelf.play(team);
            } else {
                soundOther.play(team);
            }
        }
    }

    public void playDropEffects(Player holder, Team holderTeam) {
        // Title
        final TeamProperties properties = flag.getTeam().getProperties();
        final Component holderText = Component.text(holder.getName(), holderTeam.getProperties().color()).font(DEFAULT);
        final Component selfFlag = Component.text(properties.name() + " Flag!", properties.color()).font(DEFAULT);
        showSubTitle(holderText
                .appendSpace()
                .append(Component.text("dropped", NamedTextColor.WHITE).font(SMALL_CAPS))
                .appendSpace()
                .append(selfFlag));

        // Sound
        final SoundEffect soundSelf = new SoundEffect("betterpvp", "game.capture_the_flag.flag.dropped", 1f);
        final SoundEffect soundOther = new SoundEffect("betterpvp", "game.capture_the_flag.flag.dropped", 0.9f);
        for (Team team : game.getParticipants()) {
            if (team == flag.getTeam()) {
                soundSelf.play(team);
            } else {
                soundOther.play(team);
            }
        }
    }

    public void playCaptureEffects(Player holder, Team holderTeam) {
        final Component holderText = Component.text(holder.getName(), holderTeam.getProperties().color()).font(DEFAULT);
        final Component holderTeamText = Component.text(holderTeam.getProperties().name(), holderTeam.getProperties().color()).font(DEFAULT);
        final Component component = holderText
                .appendSpace()
                .append(Component.text("scored for", NamedTextColor.WHITE).font(SMALL_CAPS))
                .appendSpace()
                .append(holderTeamText);

        // Title
        final TitleComponent title = TitleComponent.title(0, 3, 0, true, gmr -> component);
        for (Client client : clientManager.getOnline()) {
            client.getGamer().getTitleQueue().add(0, title);
        }

        // Sound
        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, 10F).play(flag.getCurrentLocation());
        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.4f, 10F).play(flag.getCurrentLocation());
        final SoundEffect soundSelf = new SoundEffect("betterpvp", "game.capture_the_flag.flag.captured", 0.9f);
        final SoundEffect soundOther = new SoundEffect("betterpvp", "game.capture_the_flag.flag.captured", 1f);
        for (Team team : game.getParticipants()) {
            if (team == holderTeam) {
                soundSelf.play(team);
            } else {
                soundOther.play(team);
            }
        }
    }
    
    private void showSubTitle(Component component) {
        final TitleComponent title = TitleComponent.subtitle(0, 3, 0, true, gmr -> component);
        for (Client client : clientManager.getOnline()) {
            client.getGamer().getTitleQueue().add(5, title);
        }
    }
}