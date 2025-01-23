package me.mykindos.betterpvp.hub.feature.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.Ticked;
import me.mykindos.betterpvp.hub.Hub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class StoreChestNPC extends ModeledNPC implements HubNPC, Ticked {

    private final CooldownManager cooldownManager;
    private final Hub hub;

    StoreChestNPC(HubNPCFactory factory, Entity entity, CooldownManager cooldownManager, Hub hub) {
        super(factory, entity);
        this.cooldownManager = cooldownManager;
        this.hub = hub;

        final Location tagLoc = entity.getLocation().add(0, 2, 0);
        attachToLifecycle(entity.getWorld().spawn(tagLoc, TextDisplay.class, display -> {
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
            display.text(Component.text("Store", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        }));

        final ActiveModel model = ModelEngineAPI.createActiveModel("chest_shadow");
        model.setHitboxScale(2);
        model.setScale(2);
        this.modeledEntity.addModel(model, true);
    }

    @Override
    public void act(Player runner) {
        if (!cooldownManager.use(runner, "StoreNPC", 4, false)) {
            return;
        }

        // todo: open store
        modeledEntity.getModel("chest_shadow").orElseThrow().getAnimationHandler().playAnimation("hit", 0, 0.2, 0.7, false);
        new SoundEffect(Sound.BLOCK_CHEST_LOCKED, 0.7f, 1f).play(runner);
        new SoundEffect("littleroom_piratepack", "littleroom.piratepack.captain_chest_item", 0f, 1f).play(runner);
        UtilServer.runTaskLater(hub, () -> {
            new SoundEffect(Sound.BLOCK_ENDER_CHEST_OPEN, 0f, 1f).play(runner);
        }, 15L);
        UtilServer.runTaskLater(hub, () -> {
            new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1f).play(runner);
            Particle.ENCHANT.builder()
                    .count(30)
                    .location(entity.getLocation().add(0, 1.5, 0))
                    .offset(0.5, 0.5, 0.5)
                    .extra(0)
                    .receivers(runner)
                    .spawn();
        }, 25L);
        UtilMessage.message(runner, "NPC", "<red>Coming soon...");
    }

    @Override
    public void tick() {
        final Location location = entity.getLocation();

        final long time = System.currentTimeMillis();
        final double angleValue = Math.toRadians(time / 10d % 360);
        final double x = Math.cos(angleValue) * 1.5;
        final double z = Math.sin(angleValue) * 1.5;
        final double yAngle = Math.toRadians(time / 10d % 360);
        final double y = Math.sin(yAngle) * 0.75 + 0.75;

        if (Bukkit.getCurrentTick() % 120 == 0) {
            new SoundEffect(Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.08f).play(location);
        }

        final List<Player> viewers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Particle.WITCH.builder()
                .count(1)
                .location(location.clone().add(x, y, z))
                .extra(0)
                .receivers(viewers)
                .spawn();
        Particle.WITCH.builder()
                .count(1)
                .location(location.clone().add(-x, y, -z))
                .extra(0)
                .receivers(viewers)
                .spawn();
    }
}
