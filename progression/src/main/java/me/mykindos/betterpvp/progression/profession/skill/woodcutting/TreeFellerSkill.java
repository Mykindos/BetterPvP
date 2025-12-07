package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.CooldownProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.PerkActivator;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class TreeFellerSkill extends WoodcuttingProgressionSkill implements CooldownProgressionSkill {

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    @Getter
    private final CooldownManager cooldownManager;
    private double cooldown;
    private double cooldownDecreasePerLevel;

    @Getter
    private int maxBlocksThatCanBeFelled;

    /**
     * Global map containing a player's UUID as the key and the number of blocks they have felled as the value
     */
    public Map<UUID, Integer> blocksFelledByPlayer = new HashMap<>();

    @Inject
    public TreeFellerSkill(Progression progression, ItemRegistry itemRegistry, ItemFactory itemFactory, CooldownManager cooldownManager) {
        super(progression);
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "Tree Feller";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Cut down an entire tree by chopping a single log",
                "",
                "Cooldown: <green>" + getCooldown(level)
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_AXE;
    }

    @Override
    public ItemFlag getFlag() {
        return ItemFlag.HIDE_ATTRIBUTES;
    }

    @Override
    public double getCooldown(int level) {
        return (cooldown - (cooldownDecreasePerLevel * level));
    }

    @Override
    public void whenPlayerUsesSkill(Player player, int level) {
        if (cooldownManager.use(player, getName(), getCooldown(level), false, true,
                false, this::shouldDisplayActionBar, getPriority(),
                cd -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1.5f))) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 2.0f, 1.0f);
            UtilMessage.simpleMessage(player, getProgressionTree(), "You used <alt>" + getName() + "</alt>");
        }
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        Player player = gamer.getPlayer();
        if (player == null) return false;

        if (getActivator().equals(PerkActivator.AXE)) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (UtilItem.isAxe(hand)) {
                return true;
            }

            final NamespacedKey key = new NamespacedKey("champions", "hyper_axe");
            final BaseItem hyperAxe = itemRegistry.getItem(key);
            if (hyperAxe != null) {
                return itemFactory.isItemOfType(hand, hyperAxe);
            }
        }

        return false;
    }

    @Override
    public PerkActivator getActivator() {
        return PerkActivator.AXE;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        cooldown = getConfig("cooldown", 20.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        maxBlocksThatCanBeFelled = getConfig("maxBlocksThatCanBeFelled", 15, Integer.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Tactician", "Forest Flourisher", "Bark Bounty"};
        return new ProgressionSkillDependency(dependencies, 250);
    }
}
