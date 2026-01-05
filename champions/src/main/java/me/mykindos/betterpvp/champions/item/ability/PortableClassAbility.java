package me.mykindos.betterpvp.champions.item.ability;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public class PortableClassAbility extends ItemAbility {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "portable_class");

    private final Role role;

    public PortableClassAbility(Role role) {
        super(KEY,
                "Equip",
                "Instantly swaps to the " + role.getName() + " class. Does not work in combat. Armor stored on this item will be equipped.",
                TriggerTypes.RIGHT_CLICK);
        this.role = role;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Gamer gamer = client.getGamer();
        final Player player = Objects.requireNonNull(gamer.getPlayer());
        final RoleManager roleManager = JavaPlugin.getPlugin(Champions.class).getInjector().getInstance(RoleManager.class);
        if (gamer.isInCombat() || roleManager.getRole(player) == role) {
            new SoundEffect(Sound.ENTITY_BEE_STING, 0f, 1f).play(player);
            return false; // They're already the role or they're in combat
        }

        if (!roleManager.equipRole(player, role)) {
            new SoundEffect(Sound.ENTITY_BEE_STING, 0f, 1f).play(player);
            return false; // They weren't allowed to equip from the event
        }

        // Equip them with gear, if ANY
        final Optional<ArmorStorageComponent> storageComponent = itemInstance.getComponent(ArmorStorageComponent.class);
        if (storageComponent.isPresent()) {
            final ArmorStorageComponent armorComponent = storageComponent.get();

            // Refund current gear
            for (ItemStack armorContent : player.getInventory().getArmorContents()) {
                UtilItem.insert(player, armorContent);
            }

            // Populate with new
            player.getInventory().setItem(EquipmentSlot.HEAD, armorComponent.getItem(EquipmentSlot.HEAD));
            player.getInventory().setItem(EquipmentSlot.CHEST, armorComponent.getItem(EquipmentSlot.CHEST));
            player.getInventory().setItem(EquipmentSlot.LEGS, armorComponent.getItem(EquipmentSlot.LEGS));
            player.getInventory().setItem(EquipmentSlot.FEET, armorComponent.getItem(EquipmentSlot.FEET));
        }

        return true;
    }
}
