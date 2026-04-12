package me.mykindos.betterpvp.shops.npc.impl.reforger;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.npc.behavior.ModelEngineScriptBehavior;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

public class ReforgerScriptBehavior extends ModelEngineScriptBehavior {

    private final ReforgerNPC reforgerNPC;
    private final Shops plugin;

    public ReforgerScriptBehavior(ReforgerNPC reforgerNPC, Shops plugin, ActiveModel model) {
        super(model);
        this.plugin = plugin;
        this.reforgerNPC = reforgerNPC;
    }

    @Override
    public boolean acceptsScript(IAnimationProperty property, String script) {
        return super.acceptsScript(property, script) && ModelEngineHelper.isExclusivelyPlayingAnimation(getModel(), "idle");
    }

    @Override
    public void onScript(IAnimationProperty property, String script) {
        UtilServer.runTask(plugin, () -> {
            switch (script) {
                case "head_hit" -> playHeadHit();
                case "sweat_throw" -> playSweatThrow();
                case "anvil_hit" -> playAnvilHit();
            }
        });
    }

    private void playHeadHit() {
        final Location location = reforgerNPC.getEntity().getLocation();
        new SoundEffect(Sound.ENTITY_PLAYER_SMALL_FALL, 2f, 0.05f).play(location);
        final Location headLocation = this.getModel().getBone("head").orElseThrow().getLocation().clone();
        headLocation.add(location.getDirection().multiply(0.1));

        Particle.SPLASH.builder()
                .location(headLocation)
                .receivers(60)
                .count(10)
                .spawn();
    }

    private void playSweatThrow() {
        final Location location = reforgerNPC.getEntity().getLocation();
        new SoundEffect(Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.8f, 0.05f).play(location);

        UtilServer.runTaskLater(plugin, () -> {
            new SoundEffect(Sound.ENTITY_PLAYER_SPLASH, 2f, 0.05f).play(location);
            final Vector offset = location.getDirection().clone().multiply(-1);
            offset.rotateAroundY(Math.toRadians(-25));

            Location splashLoc = location.clone().add(offset);
            Particle.SPLASH.builder()
                    .location(splashLoc.toLocation(location.getWorld()))
                    .receivers(60)
                    .count(15)
                    .spawn();
        }, 5L);
    }

    private void playAnvilHit() {
        new SoundEffect(Sound.BLOCK_ANVIL_PLACE, 1.5f, 0.025f).play(reforgerNPC.getEntity().getLocation());
    }
}
