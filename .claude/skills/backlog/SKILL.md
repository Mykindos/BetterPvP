---
name: backlog
description: >
  Read and update the Network Expansion GitHub project board (Mykindos project #2). Use when the user asks
  what is on the backlog, what is in progress, to add a card, to move a card between Plans, TODO, Blocked,
  In progress, In review and Done, to link a PR to a card, or when a phase of tracked work starts, opens a PR
  or merges. Also use at the end of a camps pass to sync card statuses.
---

# Network Expansion backlog

All operations go through the `gh` CLI. Owner `Mykindos`, project number `2`, project id `PVT_kwHOAFI-eM4Ar-za`.

## Field ids

| Field | Id | Options |
|---|---|---|
| Status | `PVTSSF_lAHOAFI-eM4Ar-zazgi-tlk` | Plans `f75ad846`, TODO `61e4505c`, Blocked `e0ce6678`, In progress `47fc9ee4`, In review `df73e18b`, Done `98236657` |
| Priority | `PVTSSF_lAHOAFI-eM4Ar-zazgi-tmM` | Very High `90e5d62e`, High `79628723`, Medium `0a877460`, Low `da944a9c` |
| Size | `PVTSSF_lAHOAFI-eM4Ar-zazgi-tmQ` | Tiny `6c6483d2`, Small `f784b110`, Medium `7515a9f1`, Large `817d0097` |
| Area | `PVTSSF_lAHOAFI-eM4Ar-zazgmco34` | QOL `bfcdf0c7`, UX `f54f1fa5`, Content `4126ee03`, Internal `c84fb43a` |
| Kind | `PVTSSF_lAHOAFI-eM4Ar-zazhiTIUU` | Design `a122b301`, Code `07faa855`, Build `e3376081`, Balance `23a6110e` |
| Camp Phase | `PVTSSF_lAHOAFI-eM4Ar-zazhiTIUQ` | 0 Design `d0bdc469`, 1 Foundation `14f7018c`, 2 Resources `666fce56`, 3 Construction `060f0ff3`, 4 Workers `a3f56752`, 5 Visitors `78f8c773`, 6 Tier 1 `5c6e4b97`, 7 Tier 2 `2a6e787a`, 8 Tier 3 `1e09b392`, 9 Sieges `ad844d8a` |

If an edit fails with an unknown option, the ids changed. Refresh them with
`gh project field-list 2 --owner Mykindos --format json` and fix this table.

## Reading

```bash
gh project item-list 2 --owner Mykindos --limit 300 --format json \
  --jq '.items[] | select(.status == "🏗️ In progress") | {id, title}'
```

Status values carry their emoji: `📈 Plans`, `❗TODO`, `⛔ Blocked`, `🏗️ In progress`, `👀 In review`, `🛫 Done`.
Find a card by matching its title with `ascii_downcase | contains("...")`.

## Writing

Add a draft card, then set its fields from the returned item id:

```bash
gh project item-create 2 --owner Mykindos --title "..." --body "..." --format json --jq .id
gh project item-edit --project-id PVT_kwHOAFI-eM4Ar-za --id <item-id> \
  --field-id PVTSSF_lAHOAFI-eM4Ar-zazgi-tlk --single-select-option-id 47fc9ee4
```

A draft card's title and body are edited through its draft id (`.content.id`, starts with `DI_`), not the item id:
`gh project item-edit --id <DI_...> --body "..."`.

Add an existing issue or PR with `gh project item-add 2 --owner Mykindos --url <url>`.

## When to move cards

- A tracked phase starts: In progress.
- Its PR opens: In review. Put the PR link in the card body.
- The PR merges: Done.
- Groundwork with no card gets one, with the PR links.

Report which cards moved and to what, in one short list.
