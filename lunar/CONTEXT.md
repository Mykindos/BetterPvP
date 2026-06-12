# Lunar Module Context

## Purpose

`lunar` should document Lunar Client-specific integrations, features, and behavior in the BetterPvP project.

This file is a starter scaffold. It should be deepened by someone working in the module.

## Relationship To Shared Context

Read these first:

- [CONTEXT.md](../CONTEXT.md)
- [core/CONTEXT.md](../core/CONTEXT.md)

This module should explain how Lunar-specific capabilities layer on top of shared player state and gameplay systems.

## What Lunar Should Own

Likely domain areas:

- Lunar Client detection or registration
- Lunar-specific presentation or UX behavior
- feature toggles or metadata tied to Lunar usage
- integration contracts with Lunar-provided events or APIs

## Questions This File Should Eventually Answer

- What is the canonical meaning of Lunar support in this project?
- Which features are only available to Lunar users?
- Which behavior is cosmetic, and which affects gameplay or progression?
- How does Lunar state interact with client properties and runtime events?

## Suggested Sections To Add Next

1. Glossary
2. Detection/registration lifecycle
3. Feature behavior
4. Integration points with `core`
5. Fallback behavior for non-Lunar players
