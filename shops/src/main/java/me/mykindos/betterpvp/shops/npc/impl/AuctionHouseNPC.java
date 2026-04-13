package me.mykindos.betterpvp.shops.npc.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.scene.npc.NPCFactory;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AuctionHouseNPC extends ModeledNPC implements Actor {

    private final AuctionManager auctionManager;
    private final String roleName;
    private final String shopkeeperName;
    private final String skinBlueprint;
    private ActiveModel model;

    public AuctionHouseNPC(NPCFactory factory, String roleName, String shopkeeperName, String skinBlueprint) {
        super(factory);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.auctionManager = plugin.getInjector().getInstance(AuctionManager.class);
        this.roleName = roleName;
        this.shopkeeperName = shopkeeperName;
        this.skinBlueprint = skinBlueprint;
    }

    @Override
    protected void onInit() {
        super.onInit();
        this.model = ModelEngineAPI.createActiveModel("scene_market_4");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_books", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(roleName, NamedTextColor.YELLOW));
    }

    @Override
    public void act(Player runner) {
        // Open
        new AuctionHouseMenu(auctionManager).show(runner);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
