package me.mykindos.betterpvp.core.vote;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@ItemKey("core:voting_crate")
@Singleton
public class VotingCrateItem extends BaseItem {

    private final VotingCrateAbility votingCrateAbility;

    @Inject
    public VotingCrateItem(VotingCrateAbility votingCrateAbility) {
        super(translatableName("core.item.voting-crate.name"), ItemStack.of(Material.CHEST_MINECART),
                ItemGroup.MISC,
                ItemRarity.UNCOMMON
        );
        this.votingCrateAbility = votingCrateAbility;


        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, votingCrateAbility)
                .build());
    }

}
