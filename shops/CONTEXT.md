# Shops Module Context

## Purpose

`shops` is the trading-economy module.

It owns the server's concrete trading surfaces:

- NPC shopkeeper catalogs for buying and selling items
- player-to-player auction house listings
- persisted shop catalog pricing, ordering, flags, and dynamic stock state
- persisted auction listing state and auction-processing rules

It is not the owner of shared player identity, shared balance storage, or the canonical definition of custom items. Instead, it uses shared `core` player/item abstractions to turn trading rules into live gameplay.

## Relationship To Shared Context

Read these first:

- [../CONTEXT.md](../CONTEXT.md)
- [../core/CONTEXT.md](../core/CONTEXT.md)

`core` owns the shared concepts that Shops builds on:

- shared player identity like `Client` and `Gamer`
- the canonical balance property on a player
- shared item registration and item construction
- generic menu, window, and inventory frameworks
- shared shop interfaces and buy/sell event types
- shared persistence infrastructure

`shops` uses those shared concepts to define concrete catalogs, currencies, dynamic stock, and auction rules. Contributors should describe how Shops uses `Client`, `Gamer`, `Realm`, and shared items rather than redefining them here.

## Ubiquitous Language

### `ShopManager`

The main in-memory coordinator for NPC shop trading. It loads persisted shop items, groups them by `shopkeeper`, opens shop menus, and refreshes open windows when dynamic prices change.

### `ShopItem`

The persisted catalog entry type owned by this module. A shop item points at an `item_key`, belongs to a `shopkeeper`, has an `order`, buy/sell pricing, and optional flags.

### `NormalShopItem`

A fixed-price shop item. Buy and sell prices come directly from persisted fields and do not change from stock movement.

### `DynamicShopItem`

A stock-sensitive shop item. Buy and sell prices are derived from persisted stock and pricing curve inputs rather than read directly as fixed values.

### `shopkeeper`

The catalog grouping key used by the module. A `shopkeeper` is the string bucket that shop items are loaded under and that shop menus open against, such as `Farmer`, `Blacksmith`, or `Block Merchant`.

### `item_key`

The canonical item reference stored in a shop item, such as a vanilla key or a custom item key from another module. Shops trades by item key, not by raw material/display-name matching.

### `order`

The persisted display ordering key for shop catalogs. Menu layout is built from this stored ordering rather than from ad hoc runtime sorting.

### Shop item flags

Additional persisted key/value metadata attached to a shop item. Flags let the module extend behavior for a catalog entry without changing the shared shop interface.

### `ShopCurrency`

The currency type a shop transaction uses. Current visible currencies are `COINS` and `BARK`.

### Dynamic pricing / stock

The part of the shop system where current buy/sell prices move with stock. Dynamic items track stock and derive prices from it instead of staying fixed.

### Base stock / max stock / current stock

The core stock values for a `DynamicShopItem`.

- `base stock` is the equilibrium point the system trends back toward
- `max stock` is the upper bound for stock calculations
- `current stock` is the live value that drives price changes right now

### `Auction`

The module-owned record for an auction house listing. It contains seller identity, listed item, price, duration/expiry, and state such as sold, cancelled, delivered, and transaction info.

### Active auction

An auction currently held in the `AuctionManager` in-memory set of undelivered listings for the current realm.

### Delivered / sold / cancelled / expired auction states

The main auction lifecycle states:

- `sold` means a buyer has purchased the listing
- `cancelled` means the listing was withdrawn or invalidated
- `expired` means the listing reached its expiry time
- `delivered` means the item or its return path has been completed and the listing can leave the active set

These states matter because auction processing is split across in-memory tracking plus repository updates.

### `IAuctionDeliveryService`

The delivery handoff interface for auction outcomes. Shops owns the auction flow, but actual delivery of sold items, returned items, or sale proceeds can be supplied by integration code.

### Shop buy/sell events

The shared event surface for NPC shop transactions, primarily `PlayerBuyItemEvent`, `PlayerSellItemEvent`, and their final post-transaction variants. Shops uses these to validate, mutate, and expose trade behavior to other modules.

### Auction prepare / create / buy / cancel events

The auction lifecycle event surface exposed by this module:

- `PlayerPrepareListingEvent`
- `AuctionCreateEvent`
- `AuctionBuyEvent`
- `AuctionCancelEvent`

These are the main integration points for auction restrictions and cross-module policies.

## Primary Trading Lifecycle And Main Flows

Shops has two connected halves: NPC shop trading and the auction house.

### 1. Shop catalog boot

At startup, Shops runs its migrations and loads catalog state from persistence.

The important flow is:

1. copy templated dynamic-pricing rows from realm `0` into the current realm if they do not already exist
2. load persisted `shopitems`
3. load any dynamic-pricing rows for the current realm
4. load attached item flags
5. materialize each entry as either `NormalShopItem` or `DynamicShopItem`
6. group items by `shopkeeper`
7. keep the grouped catalog in memory for menu access

This means catalog definitions are persisted globally as `shopitems`, while dynamic stock/pricing is concretely realm-scoped once copied into the live realm.

### 2. NPC shop flow

The main shopkeeper trade loop is:

1. a player interacts with a shopkeeper NPC
2. the NPC asks `ShopManager` to open the menu for that `shopkeeper`
3. the menu resolves the displayed item from `item_key`
4. the menu resolves transaction currency, usually `COINS` but sometimes `BARK`
5. the player chooses a buy, sell, or sell-all action
6. Shops fires shared buy/sell events for validation
7. on success, the module mutates balance or removes inventory items as needed
8. on success, the module inserts bought items or credits sale proceeds
9. if the shop item is dynamic, stock changes and live prices may change too
10. affected windows refresh if the price changed
11. stock is flushed back to persistence on a schedule

The current module shape makes a few rules especially important:

- trading is driven by canonical item keys, not by raw item material or display name
- item sellability is filtered through shared item metadata such as `SHOP_NOT_SELLABLE`
- sell-all staging is part of the main trade experience, not a side utility
- some shop items trade in `BARK`, which couples the shop flow to a Progression-owned item without transferring ownership of shop truth to Progression

### 3. Dynamic stock behavior

Dynamic pricing is not a one-time calculation.

Live transactions move `current stock`:

- buying from the shop lowers stock
- selling to the shop raises stock

The module then periodically:

- flushes current dynamic stock values to persistence
- nudges stock back toward `base stock`

That second step changes live buy/sell prices and causes open menus viewing those items to refresh.

### 4. Auction flow

The auction house is the other primary trading surface.

The main listing loop is:

1. a player starts listing the item in their hand
2. Shops fires `PlayerPrepareListingEvent`
3. listeners validate whether the item is allowed to be listed
4. the player sets a sale price in the listing UI
5. Shops fires `AuctionCreateEvent`
6. on success, the listing is persisted and added to the active in-memory auction set
7. players browse/search listings from the auction UI
8. a buyer attempts purchase, which fires `AuctionBuyEvent`
9. on success, buyer balance is debited, the auction is marked sold, and transaction history is recorded
10. a seller can also cancel a listing, which fires `AuctionCancelEvent`
11. asynchronous auction processing continues after listing through the periodic listener that handles sold, expired, cancelled, and delivered auctions
12. delivery is attempted through `IAuctionDeliveryService`
13. once delivery succeeds, the listing is marked delivered and removed from the active set

This matters because listing creation is immediate, but auction completion is often deferred into later processing passes.

## State Model And Ownership

### What `core` owns

`core` owns:

- shared player identity like `Client` and `Gamer`
- the canonical balance property and generic player property persistence
- shared item registry, item keys, item factories, and item metadata patterns
- generic menu, GUI, inventory, and window frameworks
- shared shop interfaces like `IShopItem`
- shared buy/sell event types
- generic database access and repository patterns

### What `shops` owns

`shops` owns:

- concrete shop catalogs
- persisted `shopitems`
- shopkeeper-to-catalog mapping
- shop item pricing, ordering, and flags
- dynamic stock/pricing rules and stock mutation
- shop transaction validation and catalog-specific trade behavior
- auction listing state
- auction lifecycle state transitions
- auction persistence and processing rules

### Boundaries that contributors should keep clear

- Shops does not own the canonical definition of custom items. It references `item_key`s from `core`, `champions`, `progression`, `clans`, and vanilla items.
- Player balance is player-owned shared state. Shops spends or credits that balance, but does not own the property itself.
- Auction listings are module-owned state that can outlive a player session.
- Dynamic stock belongs to Shops, but its persisted live values are realm-scoped after templating from realm `0`.

## Integration Points

### `core`

Shops depends heavily on `core` for item lookup, inventory helpers, menus, player properties, shared events, and persistence wiring.

### `clans`

Clans is the most important current auction integration:

- it supplies the real auction delivery service implementation
- it restricts who can create and buy auctions
- it routes sale proceeds and item delivery through clan-owned systems like balance and mailbox flows

Shops should mention this because it materially affects live auction behavior, but Shops still owns the auction lifecycle itself.

### `progression`

Progression shows up mainly through referenced item keys and `BARK` purchases. This is an integration point, not a transfer of shop ownership.

### Shared event surface

Both shop trading and auctions expose event-based integration points. Other modules can add restrictions or side effects by listening to those events without taking ownership of Shops state.

## Attached Subsystems

These are important, but they are attached to the main trading economy rather than equal-weight core concepts.

### Shopkeeper NPC presentation and scene loading

Shopkeeper NPCs, modeled scenes, and scene-loader wiring are presentation and access surfaces for shop catalogs. They are important for how players reach Shops, but they are not the core state model.

### Attuner / reforger NPC-backed integrations

`attuner` and `reforger` live in this module as notable NPC-backed systems, but they should be documented as attached integrations rather than as the primary identity of Shops.

### Auction search and filter UI

The auction house includes listing views, search/filter controls, and menu flows. These are key contributor touchpoints, but they sit on top of the underlying listing state machine.

### Auction stat listeners and logging

The module logs trade and auction actions and exposes listeners that react to those flows. These matter for observability and stats, not for defining the core domain model.

### Shop admin / reload commands

Admin commands exist to reload listeners, commands, and shop items. These are operational tooling rather than the heart of the module.

### ModelEngine / Mapper adapters

NPC scene and model integrations are adapter-driven and should remain documented as infrastructure around the trading surfaces.

### Shop item flags beyond currency

Flags are a useful extension surface for catalog entries. They matter, but the main narrative should still stay centered on catalog entries, pricing, and transaction behavior.

### Specialized auction restrictions

Auction listeners can block certain listings, such as rune-related restrictions on listable items. These are policy layers on top of the main auction lifecycle.

## Invariants And Easy-To-Break Rules

- Shops is a trading-economy module first. Do not recenter it around NPC rendering or auxiliary NPC integrations.
- Shop catalogs trade by canonical `item_key`. Regressing toward raw material/display-name matching would break cross-module item handling.
- `ShopItemRepository.copyTemplatedDynamicPrices()` is part of the boot contract for dynamic items in the current realm.
- Global catalog rows and realm-scoped dynamic stock are intentionally different layers of truth.
- Dynamic stock is not flushed on every transaction. Contributors changing persistence timing should treat that as a performance and consistency decision, not a cosmetic refactor.
- Dynamic stock also trends back toward `base stock`, so prices are expected to move even without immediate player interaction.
- `SHOP_NOT_SELLABLE` must continue to short-circuit sellability even if an item otherwise matches a shop entry.
- Auction persistence loads only undelivered listings for the current realm at startup.
- Auction state transitions are split between in-memory state and repository updates for sold/cancelled/delivered flags.
- The default auction delivery service is a no-op. Real auction success depends on integration code wiring in a real `IAuctionDeliveryService`, currently via Clans.
- Contributors changing auction flows must keep in mind that listing creation, purchase, cancellation, expiry, and delivery do not all happen in one synchronous step.
- `BARK` purchases currently depend on a Progression-owned item key, but Shops still owns the trading rule itself.

## Open Questions

There are no major contributor-facing open questions required to work in the current module shape.

If contributors start changing the module more often, the most likely future deep-dive docs would be:

- dynamic pricing and stock behavior
- auction delivery contracts and integration assumptions
- shopkeeper catalog administration and migration patterns
