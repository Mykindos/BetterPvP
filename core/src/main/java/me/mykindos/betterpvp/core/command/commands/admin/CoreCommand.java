package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.scorching.ScorchingRuneItem;
import me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking.UnbreakingRuneItem;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.resourcepack.ResourcePackHandler;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class CoreCommand extends Command implements IConsoleCommand {

    private final ItemFactory itemFactory;
    private final BlueprintItem blueprintItem;
    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private final ScorchingRuneItem scorchingRune;
    private final UnbreakingRuneItem unbreakingRune;

    @Inject
    public CoreCommand(ItemFactory itemFactory, BlueprintItem blueprintItem, CraftingRecipeRegistry craftingRecipeRegistry, ScorchingRuneItem scorchingRune, UnbreakingRuneItem unbreakingRune) {
        this.itemFactory = itemFactory;
        this.blueprintItem = blueprintItem;
        this.craftingRecipeRegistry = craftingRecipeRegistry;
        this.scorchingRune = scorchingRune;
        this.unbreakingRune = unbreakingRune;
    }

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public String getDescription() {
        return "Base core command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        List<CraftingRecipe> craftingRecipes = new ArrayList<>(craftingRecipeRegistry.getRecipesForResult(scorchingRune));
        craftingRecipes.addAll(craftingRecipeRegistry.getRecipesForResult(unbreakingRune));
        final ItemInstance item = itemFactory.create(blueprintItem);
        final BlueprintComponent blueprintComponent = item.getComponent(BlueprintComponent.class).orElseThrow();
        blueprintComponent.withCraftingRecipes(craftingRecipes);
        player.getInventory().addItem(item.createItemStack());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Singleton
    @SubCommand(CoreCommand.class)
    private static class ReloadCommand extends Command implements IConsoleCommand {

        @Inject
        private Core core;

        @Inject
        private CoreCommandLoader commandLoader;

        @Inject
        private CoreListenerLoader listenerLoader;

        @Inject
        private TipManager tipManager;

        @Inject
        private ResourcePackHandler resourcePackHandler;

        @Inject
        private RuleManager ruleManager;

        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getDescription() {
            return "Reload the core plugin";
        }

        @Override
        public void execute(Player player, Client client, String... args) {
            execute(player, args);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            core.reload();
            core.getReloadHooks().forEach(ReloadHook::reload);

            commandLoader.reload(core.getClass().getPackageName());
            tipManager.reloadTips(core);
            resourcePackHandler.reload();
            ruleManager.reload(core);

            UtilMessage.message(sender, "Core", "Successfully reloaded core");
        }

        @Override
        public Rank getRequiredRank() {
            return Rank.ADMIN;
        }


    }
}
