package me.mykindos.betterpvp.hub.feature.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class TrainerNPC extends ModeledNPC implements HubNPC {

    private final CooldownManager cooldownManager;
    private final Location ffaSpawnpoint;

    TrainerNPC(HubNPCFactory factory, CooldownManager cooldownManager, @NotNull Location ffaSpawnpoint) {
        super(factory);
        this.cooldownManager = cooldownManager;
        this.ffaSpawnpoint = ffaSpawnpoint.clone();
    }

    @Override
    protected void onInit() {
        super.onInit();

        final Location tagLoc = getEntity().getLocation().add(0, 2.8, 0);
        attachToLifecycle(getEntity().getWorld().spawn(tagLoc, TextDisplay.class, display -> {
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(1.3f),
                    new AxisAngle4f()
            ));
            display.text(Component.text("FFA Arena", NamedTextColor.RED, TextDecoration.BOLD));
        }));

        final ActiveModel model = ModelEngineAPI.createActiveModel("dummy");
        getModeledEntity().addModel(model, true);
    }

    @Override
    public void act(Player runner) {
        if (!cooldownManager.use(runner, "TrainerNPC", 2L, false)) {
            return;
        }

        getModeledEntity().getModel("dummy").orElseThrow().getAnimationHandler().playAnimation("hit", 0, 0, 1, false);

        new SoundEffect("emaginationfallenreaper", "custom.spell.rslash", 1f, 10f).play(runner);
        new SoundEffect(Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 10f).play(runner);
        runner.teleport(ffaSpawnpoint);
        Particle.SWEEP_ATTACK.builder()
                .count(5)
                .location(getEntity().getLocation().add(0, 2, 0))
                .offset(0.7, 0.7, 0.7)
                .extra(0)
                .receivers(runner)
                .spawn();
    }
}
