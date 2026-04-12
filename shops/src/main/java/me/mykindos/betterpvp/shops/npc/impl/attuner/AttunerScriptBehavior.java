package me.mykindos.betterpvp.shops.npc.impl.attuner;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.npc.behavior.ModelEngineScriptBehavior;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import org.bukkit.Location;
import org.bukkit.Sound;

public class AttunerScriptBehavior extends ModelEngineScriptBehavior {

    private final AttunerNPC attunerNPC;
    private final Shops plugin;

    public AttunerScriptBehavior(AttunerNPC attunerNPC, Shops plugin, ActiveModel model) {
        super(model);
        this.attunerNPC = attunerNPC;
        this.plugin = plugin;
    }

    @Override
    public boolean acceptsScript(IAnimationProperty property, String script) {
        return super.acceptsScript(property, script) && property.getName().equals("idle") && property.getModel().getAnimationHandler().getAnimations().size() == 1;
    }

    @Override
    public void onScript(IAnimationProperty property, String script) {
        UtilServer.runTask(plugin, () -> {
            switch (script) {
                case "sharpen" -> playSharpen();
            }
        });
    }

    private void playSharpen() {
        Location location = attunerNPC.getEntity().getLocation();
        UtilServer.repeatTask(plugin, ticks -> {
            if (ticks < 8) {
                new SoundEffect(Sound.ENTITY_VILLAGER_WORK_WEAPONSMITH, 2f, 0.03f).play(location);
            }

            if (ticks == 6) { // last
                new SoundEffect("embandits1", "custom.embandit1.slashhita", 2f, 0.2f).play(location);
            }
            return true;
        }, 10, 1L);
    }
}
