package me.mykindos.betterpvp.game.impl.ctf.model;

import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.hat.HatProvider;
import me.mykindos.betterpvp.core.framework.hat.PacketHatController;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.impl.ctf.controller.FlagInventoryCache;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FlagPlayerHandler implements HatProvider, ItemProvider, Lifecycled {
    private final Flag flag;
    private final FlagInventoryCache cache;
    private final PacketHatController hatController;
    private final EffectManager effectManager;
    private final StatManager statManager;

    public FlagPlayerHandler(Flag flag, FlagInventoryCache cache, PacketHatController hatController, EffectManager effectManager, StatManager statManager) {
        this.flag = flag;
        this.cache = cache;
        this.hatController = hatController;
        this.effectManager = effectManager;
        this.statManager = statManager;
    }

    public void pickUp(Player holder) {
        final InventoryView openInventory = holder.getOpenInventory();
        if (openInventory.getTopInventory() instanceof CraftingInventory craftingInventory) {
            craftingInventory.clear();
        }
        // Hotbar
        final ItemStack item = get();
        Map<Integer, ItemStack> hotBar = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            hotBar.put(i, item);
        }
        cache.setInventory(holder, hotBar);

        // Hat
        hatController.broadcast(holder, true);

        // Effect
        effectManager.removeEffect(holder, EffectTypes.SPEED);
        effectManager.removeEffect(holder, EffectTypes.VANISH, "Smoke Bomb");
        GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_PICKUP);
        statManager.incrementGameMapStat(holder.getUniqueId(), builder, 1);
    }

    public void tick(Player holder) {
        effectManager.addEffect(holder, holder, EffectTypes.GLOWING, "FlagGlowing", 1, 100, true);
        effectManager.addEffect(holder, holder,EffectTypes.SLOWNESS, "FlagSlowness", 2, 100, true);
        effectManager.addEffect(holder, holder,EffectTypes.SILENCE, "FlagSilence", 1, 100, true);
        GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_CARRIER_TIME);
        statManager.incrementGameMapStat(holder.getUniqueId(), builder, 50);
    }
    
    public void drop(Player holder) {
        // Hotbar
        cache.refundInventory(holder);

        // Hat
        hatController.broadcast(holder, true);

        // Effect
        effectManager.removeEffect(holder, EffectTypes.SLOWNESS, "FlagSlowness");
        effectManager.removeEffect(holder, EffectTypes.SILENCE, "FlagSilence");
        effectManager.removeEffect(holder, EffectTypes.GLOWING, "FlagGlowing");

        final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder =  GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_DROP);
        statManager.incrementGameMapStat(holder.getUniqueId(), builder, 1);
    }

    @Override
    public ItemStack apply(Player player) {
        return player == flag.getHolder() ? get() : null;
    }

    @Override
    public @NotNull ItemStack get(@Nullable String lang) {
        return new ItemBuilder(flag.getMaterial())
                .setDisplayName(Component.text("Flag", flag.getTeam().getProperties().color()))
                .get();
    }

    @Override
    public void setup() {
        hatController.addProvider(-100, this);
    }

    @Override
    public void tearDown() {
        hatController.removeProvider(this);

        if (flag.getHolder() != null) {
            effectManager.removeEffect(flag.getHolder(), EffectTypes.SLOWNESS, "Flag");
        }
    }
}