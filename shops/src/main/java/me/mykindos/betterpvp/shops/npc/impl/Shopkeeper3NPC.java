package me.mykindos.betterpvp.shops.npc.impl;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.npc.NPCFactory;
import me.mykindos.betterpvp.core.npc.behavior.BoneTagBehavior;
import me.mykindos.betterpvp.core.npc.model.ModeledNPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.utilities.model.Actor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Shopkeeper3NPC extends ModeledNPC implements Actor {

    private final ShopManager shopManager;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ActiveModel model;
    private final String shopName;

    public Shopkeeper3NPC(NPCFactory factory, Entity entity, String shopName, String shopkeeperName, String skinBlueprint) {
        super(factory, entity);
        Shops plugin = JavaPlugin.getPlugin(Shops.class);
        this.shopManager = plugin.getInjector().getInstance(ShopManager.class);
        this.clientManager = plugin.getInjector().getInstance(ClientManager.class);
        this.itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        this.shopName = shopName;

        this.model = ModelEngineAPI.createActiveModel("scene_market_1");
        this.model.setHitboxScale(1.5);
        this.model.getAnimationHandler().setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, "vendor_stand_2", 0, 0, 1));
        this.getModeledEntity().addModel(model, true);
        ModelEngineHelper.remapModel(this.model, ModelEngineAPI.getBlueprint(skinBlueprint));

        BoneTagBehavior.addNameplate(this,
                this.model,
                "head",
                shopkeeperName,
                Component.text(shopName, NamedTextColor.YELLOW));
    }

    @Override
    public void act(Player runner) {
        // Open
        this.shopManager.showShopMenu(runner, shopName, itemFactory, clientManager);

        // VFX
        this.model.getAnimationHandler().playAnimation("vendor_stand_2_interact", 0.5, 3, 1.0, false);

        // SFX
        new SoundEffect(Sound.ENTITY_VILLAGER_YES).play(runner);
    }
}
