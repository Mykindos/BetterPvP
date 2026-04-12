package me.mykindos.betterpvp.clans.npc;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.transport.ClanTravelHubMenu;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class TravelerNPC extends ModeledNPC {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    public TravelerNPC(NPCFactory factory, Entity entity, String roleName, String name, String modelName,
                       ClanManager clanManager, ClientManager clientManager) {
        super(factory, entity);
        this.clanManager = clanManager;
        this.clientManager = clientManager;

        final ActiveModel model = ModelEngineAPI.createActiveModel(modelName);
        model.setHitboxScale(1.5);
        model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "idle", 0, 0, 1));
        getModeledEntity().addModel(model, true);

        BoneTagBehavior.addNameplate(this,
                model,
                "head",
                name,
                Component.text(roleName, NamedTextColor.YELLOW));
    }

    @Override
    public void act(Player runner) {
        // Menu
        new ClanTravelHubMenu(runner, clientManager.search().online(runner), clanManager).show(runner);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER).play(runner);
    }
}
