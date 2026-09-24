# Sailing

Places are core sites (`core/world/site/`) and the helm's menu is core's `menu/navigation/`. This package holds the
voyage between them.

- `ShipDestination` is one site offered as a travel destination. `ShipDestinations` is the list a helm offers:
  permanent sites under their own names, on-demand sites under generated ones (`PlaceNames`).
- `VoyageService` owns the voyage lifecycle and nothing else.
- `Sailors` resolves a crew to online players and performs the teleports, including the guard that tells them apart
  from player-triggered teleports.
- `VoyageCues` shows titles, sounds, effects and messages during a voyage. It takes players and text, never a `Voyage`.
- `Ocean` is the staging world, cloned per voyage and deleted on arrival. It is not a site. It also returns players to
  the ship if they leave it.
- `Landfall` is a voyage's destination and places the crew on arrival.

`world/ship/` holds hulls, berths and helms. `ship/crew/CrewListener` turns walking on and off the hull into joining and
leaving a crew, and clicking a captain into asking to join. The crew rules themselves are core's
(`core/world/site/crew/`). Clanmates and allies skip the asking.
