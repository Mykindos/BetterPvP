package me.mykindos.betterpvp.core.vote;

import me.mykindos.betterpvp.core.locale.Translations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.vote.menu.VotingCrateMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class VotingCrateAbility extends AbstractInteraction implements DisplayedInteraction {

    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;
    private final LootTableRegistry lootTableRegistry;

    @Inject
    public VotingCrateAbility(ItemFactory itemFactory, ItemRegistry itemRegistry, LootTableRegistry lootTableRegistry) {
        super("Open Voting Crate");
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
        this.lootTableRegistry = lootTableRegistry;
        setConsumesItem(true);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.ability.voting-crate.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("core.ability.voting-crate.description");
    }


    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context, @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (actor.getEntity() instanceof Player player) {
            new VotingCrateMenu(itemFactory, itemRegistry, lootTableRegistry).show(player);
            return InteractionResult.Success.ADVANCE;
        }

        return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
    }
}
