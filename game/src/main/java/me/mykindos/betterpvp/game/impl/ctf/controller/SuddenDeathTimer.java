package me.mykindos.betterpvp.game.impl.ctf.controller;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.attribute.bound.GameDurationAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import me.mykindos.betterpvp.game.impl.ctf.CaptureTheFlag;
import me.mykindos.betterpvp.game.impl.ctf.model.CTFConfiguration;
import me.mykindos.betterpvp.game.impl.ctf.model.attribute.SuddenDeathDurationAttribute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@GameScoped
public class SuddenDeathTimer implements Runnable {

    private final GamePlugin plugin;
    private final ClientManager clientManager;
    private final GameController gameController;
    private final GameDurationAttribute gameDurationAttribute;
    private final SuddenDeathDurationAttribute suddenDeathDurationAttribute;
    private BukkitTask task;

    @Inject
    public SuddenDeathTimer(GamePlugin plugin, ClientManager clientManager, GameController gameController, CaptureTheFlag game) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.gameController = gameController;
        this.gameDurationAttribute = game.getConfiguration().getGameDurationAttribute();
        this.suddenDeathDurationAttribute = game.getConfiguration().getSuddenDeathDurationAttribute();
    }

    @Override
    public void run() {
        Preconditions.checkState(task == null, "Task is already running");
        task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks += 1;
                final long target = gameDurationAttribute.getValue().minus(suddenDeathDurationAttribute.getValue()).toSeconds() * 20;
                if (ticks >= target && gameController.triggerSuddenDeath()) {
                    playEffects();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playEffects() {
        new SoundEffect(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 10f).broadcast();

        for (Client client : clientManager.getOnline()) {
            final Gamer gamer = client.getGamer();
            gamer.getTitleQueue().add(0, new TitleComponent(
                    0, 3, 1, false,
                    gmr -> Component.text("Sudden Death", NamedTextColor.YELLOW),
                    gmr -> Component.text("Next Capture Wins! No Respawns!", NamedTextColor.WHITE)
            ));
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }
}
