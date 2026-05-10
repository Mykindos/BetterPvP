package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@Singleton
@ItemKey("core:bow")
@BPvPListener
@FallbackItem(value = Material.BOW, keepRecipes = true)
public class Bow extends WeaponItem implements Listener, Reloadable {

    @EqualsAndHashCode.Exclude
    private final ItemFactory itemFactory;

    @EqualsAndHashCode.Exclude
    private final RoleManager roleManager;

    @Inject
    private Bow(Champions champions, ItemFactory itemFactory, RoleManager roleManager) {
        super(champions, "Bow", ItemStack.of(Material.BOW), ItemRarity.COMMON);
        this.itemFactory = itemFactory;
        this.roleManager = roleManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getBow() == null) return;
        if (!(event.getEntity() instanceof Player)) return;
        Optional<ItemInstance> itemInstanceOptional = itemFactory.fromItemStack(event.getBow());
        if (itemInstanceOptional.isPresent() && itemInstanceOptional.get().getBaseItem() != this) return;

        final LivingEntity livingEntity = event.getEntity();
        if (UtilBlock.isInLiquid(livingEntity)) {
            UtilMessage.message(livingEntity, "Bow", "You cannot shoot this bow in liquid.");
            event.setCancelled(true);
            return;
        }

        final Role role = roleManager.getRole(livingEntity).orElse(null);
        if (role != Role.ASSASSIN && role != Role.RANGER) {
            UtilMessage.message(livingEntity, "Bow", "You can't shoot this bow without Assassin or Ranger equipped.");
            event.setCancelled(true);
        }
    }
}
