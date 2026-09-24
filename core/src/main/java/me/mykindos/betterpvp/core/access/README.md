# Item Access

The `core.access` package provides a multi-scope item capability-gating system.

**Scopes** (`AccessScope`): `CRAFT`, `USE`, `DAMAGE`, `WEAR`.

**Providers** implement `ItemAccessProvider` and are registered with `ItemAccessService`:
```java
itemAccessService.register(myProvider);
```
Each `evaluate(Player, Key)` call returns an `Optional<AccessRequirement>` that carries the requirement lore, the set of gated scopes, and whether it is satisfied for this player.

**Items** declare which scopes they enforce via `RestrictedAccessComponent`:
```java
addBaseComponent(new RestrictedAccessComponent(Set.of(AccessScope.CRAFT, AccessScope.USE, AccessScope.DAMAGE)));
```
A scope is enforced for an item **if and only if** it appears in the item's `RestrictedAccessComponent.enforcedScopes`. CRAFT follows the same rule as USE/DAMAGE/WEAR — there are no exceptions. Lore lines are rendered by this component (without ✓/✗ since `LoreComponent.getLines` has no Player). Denial feedback happens at interaction time via `ItemAccessListener`.

**Recipes** call `isAllowed(player, resultBaseItem, recipeKey, CRAFT)` automatically through the `canCraft` override on each recipe type (`ShapedCraftingRecipe`, `ShapelessCraftingRecipe`, `AnvilRecipe`, `ImbuementRecipe`). The service checks the component first — if CRAFT is not in `enforcedScopes`, it returns `true` immediately without consulting any provider. No per-recipe override is needed.

**Listeners** in `core.access.listener.ItemAccessListener` gate `USE` (PlayerInteractEvent), `DAMAGE` (EntityDamageByEntityEvent), and `WEAR` (PlayerArmorChangeEvent). The listener now delegates the component check into the service via `firstBlocker(player, baseItem, itemKey, scope)`. Denial plays `BLOCK_NOTE_BLOCK_BASS` and sends an action-bar message, throttled to 1500 ms per (player, item) pair.
