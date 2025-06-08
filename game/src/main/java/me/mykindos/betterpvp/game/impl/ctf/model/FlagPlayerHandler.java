package me.mykindos.betterpvp.game.impl.ctf.model;

import java.util.HashMap;
import java.util.Map;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.hat.HatProvider;
import me.mykindos.betterpvp.core.framework.hat.PacketHatController;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.builder.ItemBuilder;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.impl.ctf.controller.FlagInventoryCache;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlagPlayerHandler implements HatProvider, ItemProvider, Lifecycled {
    private final Flag flag;
    private final FlagInventoryCache cache;
    private final PacketHatController hatController;
    private final EffectManager effectManager;

    public FlagPlayerHandler(Flag flag, FlagInventoryCache cache, PacketHatController hatController, EffectManager effectManager) {
        this.flag = flag;
        this.cache = cache;
        this.hatController = hatController;
        this.effectManager = effectManager;
    }

    public void pickUp(Player holder) {
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

    }

    public void tick(Player holder) {
        effectManager.addEffect(holder, holder, EffectTypes.GLOWING, "FlagGlowing", 1, 100, true);
        effectManager.addEffect(holder, holder,EffectTypes.SLOWNESS, "FlagSlowness", 2, 100, true);
        effectManager.addEffect(holder, holder,EffectTypes.SILENCE, "FlagSilence", 1, 100, true);
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