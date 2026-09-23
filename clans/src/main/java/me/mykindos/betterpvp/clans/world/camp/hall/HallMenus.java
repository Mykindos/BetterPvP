package me.mykindos.betterpvp.clans.world.camp.hall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWageFund;
import me.mykindos.betterpvp.clans.world.camp.settler.FarmWorkplace;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.CrewMenus;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerCards;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.RecruitConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.blueprint.BlueprintSessions;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.wage.Payroll;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the Great Hall's menus, all reached from the Steward: the hub, and under it the settlers, the hiring board,
 * the wage fund, the crews, the farm, the construction menu and the camp's permissions. Carries out what the wage fund asks for.
 */
@Singleton
@Getter(AccessLevel.PACKAGE)
public class HallMenus {

    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final CampPermissions permissions;
    private final CampStructures structures;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;
    private final BlueprintSessions blueprints;
    private final CrewMenus crews;
    private final SettlerService settlers;
    private final Payroll payroll;
    private final CampWageFund wageFund;
    private final SettlerCards cards;
    private final ProfessionRegistry professions;
    private final CampRecruitment recruitment;
    private final RecruitConfig recruitConfig;
    private final FarmWorkplace farm;

    @Inject
    public HallMenus(@NotNull ClanManager clanManager, @NotNull ClientManager clientManager,
                     @NotNull CampPermissions permissions, @NotNull CampStructures structures,
                     @NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue,
                     @NotNull BlueprintSessions blueprints, @NotNull CrewMenus crews, @NotNull SettlerService settlers,
                     @NotNull Payroll payroll, @NotNull CampWageFund wageFund, @NotNull SettlerCards cards,
                     @NotNull ProfessionRegistry professions, @NotNull CampRecruitment recruitment,
                     @NotNull RecruitConfig recruitConfig, @NotNull FarmWorkplace farm) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.permissions = permissions;
        this.structures = structures;
        this.construction = construction;
        this.catalogue = catalogue;
        this.blueprints = blueprints;
        this.crews = crews;
        this.settlers = settlers;
        this.payroll = payroll;
        this.wageFund = wageFund;
        this.cards = cards;
        this.professions = professions;
        this.recruitment = recruitment;
        this.recruitConfig = recruitConfig;
        this.farm = farm;
    }

    /** The hub for camp {@code key}, for members of its clan only. */
    public void openHub(@NotNull Player player, @NotNull SiteKey key) {
        final boolean member = clanManager.getClanById(key.getOwnerId())
                .flatMap(clan -> clan.getMemberByUUID(player.getUniqueId()))
                .isPresent();
        if (!member) {
            tell(player, "clans.camp.hall.members_only");
            return;
        }
        new GreatHallMenu(this, player, key).show(player);
    }

    boolean isLeader(@NotNull Player player, @NotNull SiteKey key) {
        return clanManager.getClanById(key.getOwnerId())
                .flatMap(clan -> clan.getMemberByUUID(player.getUniqueId()))
                .map(member -> member.getRank() == ClanMember.MemberRank.LEADER)
                .orElse(false);
    }

    boolean mayPay(@NotNull Player player, @NotNull SiteKey key) {
        return permissions.allows(player, key.getOwnerId(), SettlerAction.PAY);
    }

    /** Moves {@code amount} of the player's coins into the wage fund, then settles wages so strikers can return. */
    void pay(@NotNull Player player, @NotNull SiteKey key, long amount) {
        if (!mayPay(player, key)) {
            tell(player, "clans.settler.card.not_allowed");
            return;
        }
        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (gamer.getBalance() < amount) {
            tell(player, "clans.camp.hall.wages.cannot_afford", Component.text(UtilFormat.formatNumber((int) amount),
                    NamedTextColor.GOLD));
            return;
        }
        gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - (int) amount);
        wageFund.deposit(key, amount);
        payroll.settle(key);
        tell(player, "clans.camp.hall.wages.paid", Component.text(UtilFormat.formatNumber((int) amount),
                NamedTextColor.GOLD));
    }

    void tell(@NotNull Player player, @NotNull String key, @NotNull ComponentLike... args) {
        UtilMessage.message(player, Translations.component("clans.prefix.camp"),
                Translations.component(key, args).color(NamedTextColor.GRAY));
    }
}
