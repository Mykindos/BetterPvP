package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

@DisplayName("Naming a site's world")
class SiteWorldsTest {

    private static final UUID INSTANCE = UUID.fromString("0123abcd-0000-0000-0000-000000000000");

    private final SiteWorlds worlds = new SiteWorlds(mock(Core.class));

    private static Site site(String id, WorldSource source) {
        return new Site(id, Component.text(id), Material.GRASS_BLOCK, source, SitePolicy.builder().build(),
                TransitTiming.DEFAULT, ArrivalPoints.DEFAULT_MARKER, ArrivalDistribution.random());
    }

    @Test
    @DisplayName("an adopted site always resolves to the world it was given")
    void adoptedSitesKeepTheirWorld() {
        final Site aldenmark = site("aldenmark", WorldSource.adopt("Aldenmark"));

        assertEquals("Aldenmark", worlds.worldNameFor(aldenmark, aldenmark.key(), INSTANCE));
        assertEquals("Aldenmark", worlds.worldNameFor(aldenmark, aldenmark.key(), UUID.randomUUID()));
    }

    @Test
    @DisplayName("each clone gets its own world under the site's prefix")
    void clonesAreNamedPerInstance() {
        final Site isle = site("lost-isle", WorldSource.clone("templates/isle_a"));

        assertEquals("sites/lost-isle/0123abcd", worlds.worldNameFor(isle, isle.key(), INSTANCE));
        assertNotEquals(worlds.worldNameFor(isle, isle.key(), UUID.randomUUID()),
                worlds.worldNameFor(isle, isle.key(), UUID.randomUUID()));
        assertEquals("sites/lost-isle/", SiteWorlds.worldNamePrefix("lost-isle"));
    }

    @Test
    @DisplayName("an owned site gets one world per owner, whatever the instance")
    void ownedWorldsAreKeyedByOwner() {
        final Site camp = site("camp", WorldSource.own("camps/"));

        assertEquals("camps/42", worlds.worldNameFor(camp, camp.keyFor(42L), INSTANCE));
        assertEquals("camps/42", worlds.worldNameFor(camp, camp.keyFor(42L), UUID.randomUUID()));
        assertNotEquals(worlds.worldNameFor(camp, camp.keyFor(42L), INSTANCE),
                worlds.worldNameFor(camp, camp.keyFor(43L), INSTANCE));
    }
}
