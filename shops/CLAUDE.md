# Shops Module

NPC shops with fixed and dynamic pricing, and the auction house backend. The auction house UI lives in `:clans`.
Source root: `shops/src/main/java/me/mykindos/betterpvp/shops/`.

## Where things live

| Package | What it holds |
|---|---|
| `shops/ShopManager` | Shop catalog. Items are `NormalShopItem` or `DynamicShopItem` (stock-driven price) |
| `shops/menus/`, `shops/events/` | Buy and sell menus, buy and sell events |
| `npc/` | Shopkeeper NPCs (`ShopkeeperNPCFactory`, `ShopsSceneContent`) |
| `auctionhouse/` | `Auction`, `AuctionManager`, `AuctionTransaction`, `IAuctionDeliveryService` |

See `CONTEXT.md` for shop and auction lifecycles and states.

## Migrations

`shops/src/main/resources/shops-migrations/postgres/`
