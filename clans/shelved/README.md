# Shelved

Code that is finished enough to keep but is not part of the running plugin. Nothing under this directory is on any
source set, so it is neither compiled nor shipped — it is here to be moved back, not to be maintained.

## discovery

Steerable ship exploration: the rudder and its physics (`ShipDynamics`), the two deck controls, hazards (whirlpools,
icebergs), island sightings on the horizon, wind bands, and the `ExpeditionService` that drove a crew's open-sea run.
The helm offered it as one more course — "the open sea" — through `ExpeditionDestination`.

Shelved because the open sea is not the way players reach a resource island for now: islands are ordinary destinations
in the helm's navigator, sailed to on the same voyage timing as anywhere else (`world/voyage/`). Steering was the whole
point of the expedition, so it comes back whole rather than half-wired into the voyage.

Its translation keys (`clans.discovery.*`) were left in the live tree, so a restore does not have to re-translate
twelve files.

### Putting it back

1. **Move the source back.** The package names already line up, so nothing has to be rewritten:

   ```sh
   mv clans/shelved/discovery/src/main/java/me/mykindos/betterpvp/clans/world/discovery \
      clans/src/main/java/me/mykindos/betterpvp/clans/world/
   mv clans/shelved/discovery/src/test/java/me/mykindos/betterpvp/clans/world/discovery \
      clans/src/test/java/me/mykindos/betterpvp/clans/world/
   ```

   Everything in it is `@BPvPListener`-annotated or injected, so it registers itself from there.

2. **Offer it at the helm again.** In `ShipInteractions`, inject `ExpeditionDestination` and `ExpeditionService`, and
   restore the two things `takeTheWheel` did:

   ```java
   // Under way the wheel is inert. It stands between the two controls the crew is steering with, and ending the
   // voyage for everyone aboard is not something to trigger by misclicking the thing in the middle.
   if (expeditionService.expeditionOf(player.getUniqueId()).isPresent()) {
       return;
   }
   ...
   if (expeditionDestination.isReady()) {
       destinations.add(expeditionDestination);
   }
   ```

   Whether the island offers stay in that menu alongside it is the open design question — two ways to reach the same
   islands is a choice, not an accident.

3. **Give `IslandOfferProvider` its single draw back.** Sightings need one fresh offer outside the menu's cache: two
   sightings must be two places, and handing out the cached list would put the same island on the horizon twice.

   ```java
   public @NotNull Optional<IslandOffer> draw() {
       for (IslandTemplate template : servableTemplates()) {
           final IslandOffer offer = offerFor(template);
           if (offer.isReady()) {
               return Optional.of(offer);
           }
       }
       return Optional.empty();
   }
   ```

4. **Land the crew through `setAshore`, not `receive`.** `IslandOffer.receive` no longer teleports anybody — it sets a
   course and puts the crew to sea. `ExpeditionService.putAshore` calls it per sailor and must instead call
   `offer.setAshore(sailors)` once for the whole landing party, which is what allocates one island and lands them
   together.

5. **Decide about the camera work.** `SteeringService`, `SteeringControls` and `SteeringCues` roll and shake the view
   through `core.framework.shader`, which is no longer in the build — see [screen-effects](screen-effects/README.md),
   which holds both halves. Restoring it is its own job, so discovery can come back without it first: drop the
   `ScreenEffectService` parameter from those three constructors and the two calls that use it. Nothing else depends
   on it.

## screen-effects

The server-driven core-shader system (camera roll, screen shake, weather grades) that discovery steered with. Taken
out of both the plugin and the resource pack. The pack's GLSL is here in full, along with the plugin half as compiled
classes. See its own README.
