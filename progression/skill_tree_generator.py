#!/usr/bin/env python3
"""
Skill Tree Generator for Minecraft Professions (v3)

Key insight: Each "step" in the tree generation consists of:
1. A CONNECTION row (vertical lines from previous skills)
2. A SKILL row (skills + any outgoing diagonal connections for divergence)

The divergence happens ON the skill row - the skill is placed with its
UP_RIGHT/UP_LEFT connections, not on a separate row.
"""

import random
import yaml  # kept (even if unused) to match your original file
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class ConnectionType(Enum):
    STRAIGHT_HORIZONTAL = "STRAIGHT_HORIZONTAL"
    STRAIGHT_VERTICAL = "STRAIGHT_VERTICAL"
    DOWN_LEFT = "DOWN_LEFT"
    DOWN_RIGHT = "DOWN_RIGHT"
    UP_LEFT = "UP_LEFT"
    UP_RIGHT = "UP_RIGHT"


# Position constants for sub-cells within a column
LEFT = 0
CENTER = 1
RIGHT = 2


@dataclass
class Attribute:
    name: str
    base: float
    per_level: float


@dataclass
class SkillNode:
    id: str
    display_name: str
    enabled: bool = True
    max_level: int = 1
    java_class: str = "me.mykindos.betterpvp.progression.profession.skill.ProfessionAttributeNode"
    dependencies: list = field(default_factory=list)
    levels_required: int = 1
    required_level: int = 0
    attributes: list = field(default_factory=list)


@dataclass
class Branch:
    """An active branch in the tree."""
    col: int
    sub: int
    node_id: str
    attr_idx: int

    @property
    def abs_pos(self) -> int:
        return self.col * 3 + self.sub


class SkillTreeGenerator:
    def __init__(self, profession_name: str, attribute_types: list, seed: int = None,
                 side_merge_chance: float = 0.35,
                 block_end_chance: float = 0.35,
                 block_max_height: int = 6,
                 block_edge_chance: float = 0.75,
                 shift_chance: float = 0.10,
                 double_corner_chance: float = 0.15):
        self.profession_name = profession_name
        self.attribute_types = attribute_types

        self.side_merge_chance = max(0.0, min(1.0, side_merge_chance))
        self.block_end_chance = max(0.0, min(1.0, block_end_chance))
        self.block_max_height = max(1, int(block_max_height))
        self.block_edge_chance = max(0.0, min(1.0, block_edge_chance))
        self.shift_chance = max(0.0, min(1.0, shift_chance))
        self.double_corner_chance = max(0.0, min(1.0, double_corner_chance))

        self.nodes: dict = {}
        self.rows: list = []
        self.attribute_counters: dict = {attr['name']: 0 for attr in attribute_types}
        self.branches: list = []

        if seed is not None:
            random.seed(seed)

    # ---------- row helpers ----------

    def _create_row(self) -> dict:
        return {
            "col_1": ["AIR", "AIR", "AIR"],
            "col_2": ["AIR", "AIR", "AIR"],
            "col_3": ["AIR", "AIR", "AIR"]
        }

    def _get_col_key(self, col: int) -> str:
        return f"col_{col + 1}"

    def _set_cell(self, row: dict, col: int, sub: int, value: str):
        row[self._get_col_key(col)][sub] = value

    def _abs_to_col_sub(self, abs_pos: int) -> tuple[int, int]:
        return abs_pos // 3, abs_pos % 3

    # ---------- node helpers ----------

    def _create_attribute_node(self, attr_idx: int) -> SkillNode:
        attr_type = self.attribute_types[attr_idx]
        attr_name = attr_type['name']
        self.attribute_counters[attr_name] += 1
        count = self.attribute_counters[attr_name]

        node_id = f"{attr_name}_attribute_{count}"

        return SkillNode(
            id=node_id,
            display_name=node_id,
            attributes=[Attribute(
                name=attr_type['attribute'],
                base=attr_type['base'],
                per_level=attr_type['per_level']
            )]
        )

    def _add_node(self, node: SkillNode):
        self.nodes[node.id] = node

    def _make_connection(self, conn_type: ConnectionType, dest: str, source: str = None) -> str:
        if source:
            return f"CONNECTION:{conn_type.value}:{dest}:{source}"
        return f"CONNECTION:{conn_type.value}:{dest}"

    # ---------- adjacency helpers ----------

    def _has_cross_column_adjacency(self, positions: list[tuple[int, int]]) -> bool:
        """True if any positions are adjacent (abs diff == 1) AND in different columns."""
        abs_positions = [(c * 3 + s, c) for (c, s) in positions]
        abs_positions.sort(key=lambda x: x[0])

        for i in range(len(abs_positions) - 1):
            abs1, col1 = abs_positions[i]
            abs2, col2 = abs_positions[i + 1]
            if abs2 - abs1 == 1 and col1 != col2:
                return True
        return False

    def _has_any_abs_adjacency(self, positions: list[tuple[int, int]]) -> bool:
        """True if any positions are adjacent in absolute space (abs diff == 1)."""
        abs_positions = sorted([c * 3 + s for (c, s) in positions])
        for i in range(len(abs_positions) - 1):
            if abs_positions[i + 1] - abs_positions[i] == 1:
                return True
        return False

    # ---------- “double corner” extra parent ----------

    def _try_add_extra_parent(
        self,
        skill_row: dict,
        child_node: SkillNode,
        child_pos: tuple[int, int],
        candidate_nodes_by_pos: dict[tuple[int, int], SkillNode]
    ) -> None:
        """
        With probability double_corner_chance, add an extra dependency to child_node from
        a nearby node whose position is abs-diff 2 away (so connector has a single midpoint).
        Also place a STRAIGHT_HORIZONTAL connector in the midpoint cell on this skill_row.
        """
        if random.random() >= self.double_corner_chance:
            return

        child_abs = child_pos[0] * 3 + child_pos[1]

        candidates: list[tuple[int, SkillNode, int, int]] = []
        for (pos, node) in candidate_nodes_by_pos.items():
            if pos == child_pos:
                continue
            other_abs = pos[0] * 3 + pos[1]
            if abs(other_abs - child_abs) != 2:
                continue

            left_abs = min(other_abs, child_abs)
            mid_abs = left_abs + 1
            mid_col, mid_sub = self._abs_to_col_sub(mid_abs)

            if skill_row[self._get_col_key(mid_col)][mid_sub] != "AIR":
                continue

            candidates.append((other_abs, node, mid_col, mid_sub))

        if not candidates:
            return

        other_abs, other_node, mid_col, mid_sub = random.choice(candidates)

        # Only link from smaller abs -> larger abs to avoid cycles
        if other_abs < child_abs:
            child_node.dependencies.append(other_node.id)
            self._set_cell(
                skill_row,
                mid_col,
                mid_sub,
                self._make_connection(ConnectionType.STRAIGHT_HORIZONTAL, child_node.id, other_node.id)
            )

    # ---------- divergence / convergence feasibility ----------

    def _can_diverge_at(self, col: int) -> bool:
        """Check if diverging at this column would cause adjacency issues."""
        other_positions = [(b.col, b.sub) for b in self.branches if b.col != col]
        test_both = other_positions + [(col, LEFT), (col, RIGHT)]
        return not self._has_cross_column_adjacency(test_both)

    def _allowed_merge_targets_for_column(self, col: int) -> set[int]:
        """
        Side-merge constraints:
        - leftmost column (0) cannot merge into LEFT
        - rightmost column (2) cannot merge into RIGHT
        - middle column (1) can merge into either side
        """
        targets = {LEFT, RIGHT}
        if col == 0:
            targets.discard(LEFT)
        if col == 2:
            targets.discard(RIGHT)
        return targets

    def _can_side_merge(self, converge_col: int, target_sub: int,
                        left_branch: Branch, right_branch: Branch) -> bool:
        """
        Allow merging into LEFT/RIGHT only if it won't create clunky visuals:
        - no overlap
        - no abs adjacency (abs diff == 1) with any other branch
        """
        if target_sub not in (LEFT, RIGHT):
            return False

        positions_after: list[tuple[int, int]] = []
        for b in self.branches:
            if b is left_branch or b is right_branch:
                continue
            positions_after.append((b.col, b.sub))

        if (converge_col, target_sub) in positions_after:
            return False

        positions_after.append((converge_col, target_sub))

        if self._has_any_abs_adjacency(positions_after):
            return False

        return True

    def _can_converge(self) -> bool:
        """Check if any column has both LEFT and RIGHT branches."""
        for col in range(3):
            col_branches = [b for b in self.branches if b.col == col]
            subs = [b.sub for b in col_branches]
            if LEFT in subs and RIGHT in subs:
                return True
        return False

    # ---------- block eligibility ----------

    def _eligible_horizontal_edges(self) -> list[tuple[Branch, Branch, int]]:
        """
        Returns list of edges (left_branch, right_branch, mid_abs) where:
        - right.abs_pos - left.abs_pos == 2 (exactly one slot between)
        - midpoint abs is not occupied by a branch
        """
        occupied = {b.abs_pos for b in self.branches}
        cands = sorted(self.branches, key=lambda b: b.abs_pos)
        edges: list[tuple[Branch, Branch, int]] = []

        for i in range(len(cands) - 1):
            a = cands[i]
            b = cands[i + 1]
            if b.abs_pos - a.abs_pos == 2:
                mid = a.abs_pos + 1
                if mid not in occupied:
                    edges.append((a, b, mid))
        return edges

    # ---------- shifting (non-divergence movement) ----------

    def _can_shift_branch(self, b: Branch, target_sub: int) -> bool:
        """
        Allows shifting within the same col:
          LEFT <-> CENTER <-> RIGHT (but NOT LEFT <-> RIGHT directly)
        Must not overlap, and must not create abs adjacency (diff == 1).
        """
        if target_sub not in (LEFT, CENTER, RIGHT):
            return False
        if target_sub == b.sub:
            return False
        if (b.sub == LEFT and target_sub == RIGHT) or (b.sub == RIGHT and target_sub == LEFT):
            return False  # require passing through CENTER

        if any(x.col == b.col and x.sub == target_sub for x in self.branches):
            return False

        positions_after = [(x.col, x.sub) for x in self.branches if x is not b]
        positions_after.append((b.col, target_sub))

        if self._has_any_abs_adjacency(positions_after):
            return False

        return True

    def _pick_shift(self) -> Optional[tuple[Branch, int]]:
        candidates: list[tuple[Branch, int]] = []
        for b in self.branches:
            for t in (LEFT, CENTER, RIGHT):
                if self._can_shift_branch(b, t):
                    candidates.append((b, t))
        return random.choice(candidates) if candidates else None

    # ---------- main generation ----------

    def generate_tree(self, total_rows: int = 50,
                      diverge_chance: float = 0.15,
                      converge_chance: float = 0.15,
                      block_chance: float = 0.0):
        """Generate the skill tree."""
        self._create_initial_row()

        steps = 0
        max_steps = total_rows // 2

        while steps < max_steps - 2:
            action = self._decide_action(diverge_chance, converge_chance, block_chance)

            if action == 'shift':
                self._step_with_shift()
            elif action == 'diverge':
                self._step_with_divergence()
            elif action == 'converge':
                self._step_with_convergence()
            elif action == 'block':
                used_layers = self._step_with_block_dynamic()
                steps += (used_layers - 1)
            else:
                self._step_normal()

            steps += 1

        self._add_mega_node()

    def _create_initial_row(self):
        """Create the first row with 3 skills at center positions."""
        row = self._create_row()

        for col in range(3):
            attr_idx = col % len(self.attribute_types)
            node = self._create_attribute_node(attr_idx)
            self._add_node(node)
            self._set_cell(row, col, CENTER, f"SKILL:{node.id}")
            self.branches.append(Branch(col=col, sub=CENTER, node_id=node.id, attr_idx=attr_idx))

        self.rows.append(row)

    def _decide_action(self, diverge_chance: float, converge_chance: float, block_chance: float) -> str:
        """Decide what action to take this step."""
        r = random.random()

        can_shift = self._pick_shift() is not None
        can_diverge = any(b.sub == CENTER and self._can_diverge_at(b.col) for b in self.branches)
        can_converge = self._can_converge()
        can_block = bool(self._eligible_horizontal_edges())

        cumulative = 0.0

        if can_shift:
            cumulative += self.shift_chance
            if r < cumulative:
                return 'shift'

        if can_diverge and len(self.branches) < 7:
            cumulative += diverge_chance
            if r < cumulative:
                return 'diverge'

        if can_converge:
            cumulative += converge_chance
            if r < cumulative:
                return 'converge'

        if can_block:
            cumulative += block_chance
            if r < cumulative:
                return 'block'

        return 'normal'

    # ---------- step types ----------

    def _step_normal(self):
        """Normal step: connection row + skill row for all branches."""
        new_data: list[tuple[Branch, SkillNode]] = []
        for branch in self.branches:
            node = self._create_attribute_node(branch.attr_idx)
            node.dependencies.append(branch.node_id)
            self._add_node(node)
            new_data.append((branch, node))

        conn_row = self._create_row()
        for branch, node in new_data:
            self._set_cell(conn_row, branch.col, branch.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node.id))
        self.rows.append(conn_row)

        skill_row = self._create_row()
        new_branches: list[Branch] = []
        for branch, node in new_data:
            self._set_cell(skill_row, branch.col, branch.sub, f"SKILL:{node.id}")
            new_branches.append(Branch(col=branch.col, sub=branch.sub,
                                       node_id=node.id, attr_idx=branch.attr_idx))
        self.rows.append(skill_row)

        self.branches = new_branches

    def _step_with_shift(self):
        """
        Non-divergence movement:
          LEFT <-> CENTER <-> RIGHT within the same column.

        Pattern (4 rows like divergence):
          - Row 1: vertical to "shift_node" at source sub
          - Row 2: skill row with shift_node and diagonal connector in target cell
          - Row 3: vertical to moved_node at target
          - Row 4: moved_node skill at target
        """
        pick = self._pick_shift()
        if not pick:
            self._step_normal()
            return

        shift_branch, target_sub = pick
        shift_col = shift_branch.col
        source_sub = shift_branch.sub

        shift_node = self._create_attribute_node(shift_branch.attr_idx)
        shift_node.dependencies.append(shift_branch.node_id)
        self._add_node(shift_node)

        moved_node = self._create_attribute_node(shift_branch.attr_idx)
        moved_node.dependencies.append(shift_node.id)
        self._add_node(moved_node)

        other_nodes: list[tuple[Branch, SkillNode]] = []
        for b in self.branches:
            if b is shift_branch:
                continue
            node = self._create_attribute_node(b.attr_idx)
            node.dependencies.append(b.node_id)
            self._add_node(node)
            other_nodes.append((b, node))

        # Row 1: verticals
        conn_row_1 = self._create_row()
        self._set_cell(conn_row_1, shift_col, source_sub,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, shift_node.id))
        for b, node in other_nodes:
            self._set_cell(conn_row_1, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node.id))
        self.rows.append(conn_row_1)

        # Row 2: skill row with diagonal into target
        skill_row_1 = self._create_row()

        # Diagonal connector is placed at the TARGET position (same style as divergence)
        if target_sub > source_sub:
            # child is to the RIGHT of the parent
            self._set_cell(skill_row_1, shift_col, target_sub,
                           self._make_connection(ConnectionType.UP_LEFT, moved_node.id))
        else:
            # child is to the LEFT of the parent
            self._set_cell(skill_row_1, shift_col, target_sub,
                           self._make_connection(ConnectionType.UP_RIGHT, moved_node.id))

        self._set_cell(skill_row_1, shift_col, source_sub, f"SKILL:{shift_node.id}")
        for b, node in other_nodes:
            self._set_cell(skill_row_1, b.col, b.sub, f"SKILL:{node.id}")

        # Optional extra parent into moved_node (double-corner chance)
        nodes_on_row = {(shift_col, source_sub): shift_node}
        for b, node in other_nodes:
            nodes_on_row[(b.col, b.sub)] = node
        self._try_add_extra_parent(skill_row_1, moved_node, (shift_col, target_sub), nodes_on_row)

        self.rows.append(skill_row_1)

        # Row 3: verticals to moved node and other branches' next nodes
        conn_row_2 = self._create_row()
        self._set_cell(conn_row_2, shift_col, target_sub,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, moved_node.id))

        other_nodes_2: list[tuple[Branch, SkillNode]] = []
        for b, prev_node in other_nodes:
            node2 = self._create_attribute_node(b.attr_idx)
            node2.dependencies.append(prev_node.id)
            self._add_node(node2)
            other_nodes_2.append((b, node2))
            self._set_cell(conn_row_2, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node2.id))
        self.rows.append(conn_row_2)

        # Row 4: skills
        skill_row_2 = self._create_row()
        self._set_cell(skill_row_2, shift_col, target_sub, f"SKILL:{moved_node.id}")
        for b, node2 in other_nodes_2:
            self._set_cell(skill_row_2, b.col, b.sub, f"SKILL:{node2.id}")
        self.rows.append(skill_row_2)

        # Update branches
        new_branches: list[Branch] = [
            Branch(col=shift_col, sub=target_sub, node_id=moved_node.id, attr_idx=shift_branch.attr_idx)
        ]
        for b, node2 in other_nodes_2:
            new_branches.append(Branch(col=b.col, sub=b.sub, node_id=node2.id, attr_idx=b.attr_idx))
        self.branches = new_branches

    def _step_with_divergence(self):
        """
        Step where one branch diverges.

        Pattern:
        - Row 1: Connection row (vertical to the diverging skill and others)
        - Row 2: Skill row with divergence (center skill + UP_RIGHT/UP_LEFT connectors)
        - Row 3: Connection row (vertical for new left/right, plus others)
        - Row 4: Skill row (new left/right skills, others continue)
        """
        diverge_branch = None
        for b in self.branches:
            if b.sub == CENTER and self._can_diverge_at(b.col):
                diverge_branch = b
                break

        if not diverge_branch:
            self._step_normal()
            return

        diverge_col = diverge_branch.col

        diverge_node = self._create_attribute_node(diverge_branch.attr_idx)
        diverge_node.dependencies.append(diverge_branch.node_id)
        self._add_node(diverge_node)

        left_attr = diverge_branch.attr_idx
        right_attr = (diverge_branch.attr_idx + 1) % len(self.attribute_types)

        left_node = self._create_attribute_node(left_attr)
        right_node = self._create_attribute_node(right_attr)
        left_node.dependencies.append(diverge_node.id)
        right_node.dependencies.append(diverge_node.id)
        self._add_node(left_node)
        self._add_node(right_node)

        other_nodes: list[tuple[Branch, SkillNode]] = []
        for b in self.branches:
            if b.col != diverge_col:
                node = self._create_attribute_node(b.attr_idx)
                node.dependencies.append(b.node_id)
                self._add_node(node)
                other_nodes.append((b, node))

        # Row 1: vertical to diverge node + others
        conn_row_1 = self._create_row()
        self._set_cell(conn_row_1, diverge_col, CENTER,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, diverge_node.id))
        for b, node in other_nodes:
            self._set_cell(conn_row_1, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node.id))
        self.rows.append(conn_row_1)

        # Row 2: diverge skill + diagonal connectors
        skill_row_1 = self._create_row()
        self._set_cell(skill_row_1, diverge_col, LEFT,
                       self._make_connection(ConnectionType.UP_RIGHT, left_node.id))
        self._set_cell(skill_row_1, diverge_col, CENTER, f"SKILL:{diverge_node.id}")
        self._set_cell(skill_row_1, diverge_col, RIGHT,
                       self._make_connection(ConnectionType.UP_LEFT, right_node.id))
        for b, node in other_nodes:
            self._set_cell(skill_row_1, b.col, b.sub, f"SKILL:{node.id}")

        # Optional extra parents into the diverged children (double-corner chance)
        nodes_on_row = {(diverge_col, CENTER): diverge_node}
        for b, node in other_nodes:
            nodes_on_row[(b.col, b.sub)] = node
        self._try_add_extra_parent(skill_row_1, left_node, (diverge_col, LEFT), nodes_on_row)
        self._try_add_extra_parent(skill_row_1, right_node, (diverge_col, RIGHT), nodes_on_row)

        self.rows.append(skill_row_1)

        # Row 3: verticals to children + others' next nodes
        conn_row_2 = self._create_row()
        self._set_cell(conn_row_2, diverge_col, LEFT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, left_node.id))
        self._set_cell(conn_row_2, diverge_col, RIGHT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, right_node.id))

        other_nodes_2: list[tuple[Branch, SkillNode]] = []
        for b, prev_node in other_nodes:
            node2 = self._create_attribute_node(b.attr_idx)
            node2.dependencies.append(prev_node.id)
            self._add_node(node2)
            other_nodes_2.append((b, node2))
            self._set_cell(conn_row_2, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node2.id))
        self.rows.append(conn_row_2)

        # Row 4: skills
        skill_row_2 = self._create_row()
        self._set_cell(skill_row_2, diverge_col, LEFT, f"SKILL:{left_node.id}")
        self._set_cell(skill_row_2, diverge_col, RIGHT, f"SKILL:{right_node.id}")
        for b, node2 in other_nodes_2:
            self._set_cell(skill_row_2, b.col, b.sub, f"SKILL:{node2.id}")
        self.rows.append(skill_row_2)

        self.branches = [
            Branch(col=diverge_col, sub=LEFT, node_id=left_node.id, attr_idx=left_attr),
            Branch(col=diverge_col, sub=RIGHT, node_id=right_node.id, attr_idx=right_attr),
            *[Branch(col=b.col, sub=b.sub, node_id=node2.id, attr_idx=b.attr_idx) for b, node2 in other_nodes_2]
        ]

    def _step_with_convergence(self):
        """
        Step where two branches in the same column converge.

        Supports side-merge: sometimes merged node lands on LEFT or RIGHT instead of CENTER,
        but only if it won't create abs adjacency (diff == 1) with any other branch.
        """
        converge_col = None
        left_branch = None
        right_branch = None

        for col in range(3):
            col_branches = [b for b in self.branches if b.col == col]
            lb = next((b for b in col_branches if b.sub == LEFT), None)
            rb = next((b for b in col_branches if b.sub == RIGHT), None)
            if lb and rb:
                converge_col = col
                left_branch = lb
                right_branch = rb
                break

        if converge_col is None:
            self._step_normal()
            return

        merge_sub = CENTER
        if random.random() < self.side_merge_chance:
            candidates: list[int] = []
            allowed = self._allowed_merge_targets_for_column(converge_col)

            if LEFT in allowed and self._can_side_merge(converge_col, LEFT, left_branch, right_branch):
                candidates.append(LEFT)
            if RIGHT in allowed and self._can_side_merge(converge_col, RIGHT, left_branch, right_branch):
                candidates.append(RIGHT)

            if candidates:
                merge_sub = random.choice(candidates)

        left_node = self._create_attribute_node(left_branch.attr_idx)
        left_node.dependencies.append(left_branch.node_id)
        self._add_node(left_node)

        right_node = self._create_attribute_node(right_branch.attr_idx)
        right_node.dependencies.append(right_branch.node_id)
        self._add_node(right_node)

        merged_node = self._create_attribute_node(left_branch.attr_idx)
        merged_node.dependencies.extend([left_node.id, right_node.id])
        self._add_node(merged_node)

        other_nodes_1: list[tuple[Branch, SkillNode]] = []
        for b in self.branches:
            if b.col != converge_col:
                node = self._create_attribute_node(b.attr_idx)
                node.dependencies.append(b.node_id)
                self._add_node(node)
                other_nodes_1.append((b, node))

        # Row 1: verticals to the two nodes + others
        conn_row_1 = self._create_row()
        self._set_cell(conn_row_1, converge_col, LEFT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, left_node.id))
        self._set_cell(conn_row_1, converge_col, RIGHT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, right_node.id))
        for b, node in other_nodes_1:
            self._set_cell(conn_row_1, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node.id))
        self.rows.append(conn_row_1)

        # Row 2: skills for the two converging nodes + others
        skill_row_1 = self._create_row()
        self._set_cell(skill_row_1, converge_col, LEFT, f"SKILL:{left_node.id}")
        self._set_cell(skill_row_1, converge_col, RIGHT, f"SKILL:{right_node.id}")
        for b, node in other_nodes_1:
            self._set_cell(skill_row_1, b.col, b.sub, f"SKILL:{node.id}")
        self.rows.append(skill_row_1)

        # Row 3: vertical connections down to the merged node (encoded on both sides) + others advance
        vert_row = self._create_row()
        self._set_cell(vert_row, converge_col, LEFT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, merged_node.id, left_node.id))
        self._set_cell(vert_row, converge_col, RIGHT,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, merged_node.id, right_node.id))

        other_nodes_2: list[tuple[Branch, SkillNode]] = []
        for b, prev_node in other_nodes_1:
            node2 = self._create_attribute_node(b.attr_idx)
            node2.dependencies.append(prev_node.id)
            self._add_node(node2)
            other_nodes_2.append((b, node2))
            self._set_cell(vert_row, b.col, b.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node2.id))
        self.rows.append(vert_row)

        # Row 4: corners + merged skill (sometimes not centered)
        merge_row = self._create_row()

        if merge_sub == CENTER:
            self._set_cell(merge_row, converge_col, LEFT,
                           self._make_connection(ConnectionType.DOWN_RIGHT, merged_node.id, left_node.id))
            self._set_cell(merge_row, converge_col, CENTER, f"SKILL:{merged_node.id}")
            self._set_cell(merge_row, converge_col, RIGHT,
                           self._make_connection(ConnectionType.DOWN_LEFT, merged_node.id, right_node.id))

        elif merge_sub == RIGHT:
            self._set_cell(merge_row, converge_col, LEFT,
                           self._make_connection(ConnectionType.DOWN_RIGHT, merged_node.id, left_node.id))
            # Visual segment only (avoid encoding a second logical edge)
            self._set_cell(merge_row, converge_col, CENTER,
                           self._make_connection(ConnectionType.STRAIGHT_HORIZONTAL, merged_node.id))
            self._set_cell(merge_row, converge_col, RIGHT, f"SKILL:{merged_node.id}")

        elif merge_sub == LEFT:
            self._set_cell(merge_row, converge_col, RIGHT,
                           self._make_connection(ConnectionType.DOWN_LEFT, merged_node.id, right_node.id))
            # Visual segment only (avoid encoding a second logical edge)
            self._set_cell(merge_row, converge_col, CENTER,
                           self._make_connection(ConnectionType.STRAIGHT_HORIZONTAL, merged_node.id))
            self._set_cell(merge_row, converge_col, LEFT, f"SKILL:{merged_node.id}")

        for b, node2 in other_nodes_2:
            self._set_cell(merge_row, b.col, b.sub, f"SKILL:{node2.id}")

        self.rows.append(merge_row)

        self.branches = [
            Branch(col=converge_col, sub=merge_sub, node_id=merged_node.id, attr_idx=left_branch.attr_idx),
            *[Branch(col=b.col, sub=b.sub, node_id=node2.id, attr_idx=b.attr_idx) for b, node2 in other_nodes_2]
        ]

    # ---------- block logic (partial / asymmetric / multi-block allowed) ----------

    def _step_with_block_layer(self) -> bool:
        """
        Performs ONE block layer if possible.
        Returns True if it actually placed any horizontal edge; else False.
        """
        eligible = self._eligible_horizontal_edges()
        if not eligible:
            self._step_normal()
            return False

        chosen: list[tuple[Branch, Branch, int]] = []
        for a, b, mid in eligible:
            if random.random() < self.block_edge_chance:
                chosen.append((a, b, mid))

        if not chosen:
            self._step_normal()
            return False

        # advance all branches
        new_nodes: list[tuple[Branch, SkillNode]] = []
        for branch in self.branches:
            node = self._create_attribute_node(branch.attr_idx)
            node.dependencies.append(branch.node_id)  # vertical parent
            self._add_node(node)
            new_nodes.append((branch, node))

        node_by_pos = {(b.col, b.sub): n for (b, n) in new_nodes}

        # add horizontal deps for chosen edges (right depends on left)
        for left_b, right_b, _mid in chosen:
            left_node = node_by_pos[(left_b.col, left_b.sub)]
            right_node = node_by_pos[(right_b.col, right_b.sub)]
            right_node.dependencies.append(left_node.id)

        # connection row (verticals)
        conn_row = self._create_row()
        for branch, node in new_nodes:
            self._set_cell(conn_row, branch.col, branch.sub,
                           self._make_connection(ConnectionType.STRAIGHT_VERTICAL, node.id))
        self.rows.append(conn_row)

        # skill row: skills + horizontal connectors at midpoints
        skill_row = self._create_row()
        for branch, node in new_nodes:
            self._set_cell(skill_row, branch.col, branch.sub, f"SKILL:{node.id}")

        for left_b, right_b, mid_abs in chosen:
            left_node = node_by_pos[(left_b.col, left_b.sub)]
            right_node = node_by_pos[(right_b.col, right_b.sub)]
            mid_col, mid_sub = self._abs_to_col_sub(mid_abs)

            if skill_row[self._get_col_key(mid_col)][mid_sub] != "AIR":
                continue

            self._set_cell(
                skill_row,
                mid_col,
                mid_sub,
                self._make_connection(ConnectionType.STRAIGHT_HORIZONTAL, right_node.id, left_node.id)
            )

        self.rows.append(skill_row)

        self.branches = [
            Branch(col=b.col, sub=b.sub, node_id=n.id, attr_idx=b.attr_idx)
            for (b, n) in new_nodes
        ]
        return True

    def _step_with_block_dynamic(self) -> int:
        """
        Run a block for >=1 layers, ending probabilistically (block_end_chance),
        capped by block_max_height. Returns number of layers used.
        """
        layers = 0
        while layers < self.block_max_height:
            did_layer = self._step_with_block_layer()
            if not did_layer:
                break
            layers += 1
            if random.random() < self.block_end_chance:
                break
        return max(1, layers) if layers > 0 else 1

    # ---------- mega node ----------

    def _add_mega_node(self):
        """Add final mega node."""
        mega = SkillNode(
            id="mega_attribute_1",
            display_name="mega_attribute_1",
            max_level=99,
            attributes=[
                Attribute(attr['attribute'], attr['base'], attr['per_level'])
                for attr in self.attribute_types
            ]
        )

        # IMPORTANT FIX:
        # _step_normal() never changes (col, sub) positions, so it cannot create new convergence opportunities.
        # Only attempt actual convergences; otherwise stop and attach mega to remaining branches.
        safety = 0
        while len(self.branches) > 1 and safety < 200:
            if self._can_converge():
                self._step_with_convergence()
            else:
                break
            safety += 1

        for b in self.branches[:3]:
            mega.dependencies.append(b.node_id)
        self._add_node(mega)

        conn_row = self._create_row()
        self._set_cell(conn_row, 1, CENTER,
                       self._make_connection(ConnectionType.STRAIGHT_VERTICAL, mega.id))
        self.rows.append(conn_row)

        skill_row = self._create_row()
        self._set_cell(skill_row, 1, CENTER, f"SKILL:{mega.id}")
        self.rows.append(skill_row)

        self.rows.append(self._create_row())

    # ---------- export ----------

    def export_skill_tree_yaml(self) -> str:
        output = "skill_tree:\n  layout:\n"

        for i, row in enumerate(self.rows, start=1):
            output += f"    # Row {i}\n"
            output += f"    row_{i}:\n"
            output += f"      col_1:\n"
            for cell in row["col_1"]:
                output += f"      - {cell}\n"
            output += f"      col_2:\n"
            for cell in row["col_2"]:
                output += f"      - {cell}\n"
            output += f"      col_3:\n"
            for cell in row["col_3"]:
                output += f"      - {cell}\n"
            output += "\n"

        return output

    def export_nodes_yaml(self) -> str:
        output = "nodes:\n"

        for node_id, node in self.nodes.items():
            output += f"  {node_id}:\n"
            output += f"    enabled: {'true' if node.enabled else 'false'}\n"
            output += f"    maxlevel: {node.max_level}\n"
            output += f"    class: {node.java_class}\n"
            output += f"    dependencies:\n"

            if node.dependencies:
                output += f"      nodes:\n"
                for dep in node.dependencies:
                    output += f"      - {dep}\n"
            else:
                output += f"      nodes: []\n"

            output += f"      levelsRequired: {node.levels_required}\n"
            output += f"      requiredLevel: {node.required_level}\n"

            if node.attributes:
                output += f"    attributes:\n"
                for attr in node.attributes:
                    output += f"    - attribute: {attr.name}\n"
                    output += f"      base: {attr.base}\n"
                    output += f"      per_level: {attr.per_level}\n"

            output += f"    displayName: {node.display_name}\n"

        return output

    def save_files(self, output_dir: str = "."):
        import os

        skill_tree_path = os.path.join(output_dir, "skill_tree.yml")
        nodes_path = os.path.join(output_dir, f"{self.profession_name}.yml")

        with open(skill_tree_path, 'w') as f:
            f.write(self.export_skill_tree_yaml())

        with open(nodes_path, 'w') as f:
            f.write(self.export_nodes_yaml())

        return skill_tree_path, nodes_path


PROFESSION_CONFIGS = {
    "mining": [
        {"name": "ore_yield", "attribute": "ORE_YIELD", "base": 2.5, "per_level": 2.5},
        {"name": "mining_speed", "attribute": "MINING_SPEED", "base": 1.0, "per_level": 1.0},
        {"name": "rare_gem_chance", "attribute": "RARE_GEM_CHANCE", "base": 0.5, "per_level": 0.5},
    ],
    "woodcutting": [
        {"name": "log_yield", "attribute": "LOG_YIELD", "base": 2.0, "per_level": 2.0},
        {"name": "chopping_speed", "attribute": "CHOPPING_SPEED", "base": 1.5, "per_level": 1.5},
        {"name": "special_wood_chance", "attribute": "SPECIAL_WOOD_CHANCE", "base": 0.3, "per_level": 0.3},
    ],
    "cooking": [
        {"name": "food_quality", "attribute": "FOOD_QUALITY", "base": 5.0, "per_level": 5.0},
        {"name": "cooking_speed", "attribute": "COOKING_SPEED", "base": 2.0, "per_level": 2.0},
        {"name": "bonus_servings", "attribute": "BONUS_SERVINGS", "base": 0.1, "per_level": 0.1},
    ],
    "herbalism": [
        {"name": "herb_yield", "attribute": "HERB_YIELD", "base": 2.0, "per_level": 2.0},
        {"name": "growth_speed", "attribute": "GROWTH_SPEED", "base": 1.0, "per_level": 1.0},
        {"name": "rare_herb_chance", "attribute": "RARE_HERB_CHANCE", "base": 0.5, "per_level": 0.5},
    ],
    "smithing": [
        {"name": "craft_quality", "attribute": "CRAFT_QUALITY", "base": 3.0, "per_level": 3.0},
        {"name": "material_efficiency", "attribute": "MATERIAL_EFFICIENCY", "base": 1.5, "per_level": 1.5},
        {"name": "rare_craft_chance", "attribute": "RARE_CRAFT_CHANCE", "base": 0.25, "per_level": 0.25},
    ],
}


def main():
    import sys
    import argparse

    parser = argparse.ArgumentParser(description='Generate profession skill trees')
    parser.add_argument('profession', nargs='?', default='mining',
                        help=f'Profession name ({", ".join(PROFESSION_CONFIGS.keys())})')
    parser.add_argument('--seed', '-s', type=int, default=None,
                        help='Random seed for reproducibility')
    parser.add_argument('--rows', '-r', type=int, default=50,
                        help='Approximate number of layout rows')
    parser.add_argument('--output', '-o', default='.',
                        help='Output directory')
    parser.add_argument('--diverge', type=float, default=0.15,
                        help='Divergence probability (0-1)')
    parser.add_argument('--converge', type=float, default=0.15,
                        help='Convergence probability (0-1)')
    parser.add_argument('--block', type=float, default=0.0,
                        help='Block pattern probability (0-1)')

    parser.add_argument('--block-end', type=float, default=0.35,
                        help='Chance each block-layer ends the block (0-1)')
    parser.add_argument('--block-max-height', type=int, default=6,
                        help='Max block height in layers (>=1)')
    parser.add_argument('--block-edge', type=float, default=0.75,
                        help='Chance an eligible horizontal edge is included in a block layer (0-1)')

    parser.add_argument('--shift', type=float, default=0.10,
                        help='Chance to do a non-divergence shift action (0-1)')
    parser.add_argument('--double-corner', type=float, default=0.15,
                        help='Chance to add an extra corner/side parent (0-1)')
    parser.add_argument('--side-merge', type=float, default=0.35,
                        help='Side-merge probability during convergence (0-1)')

    parser.add_argument('--list', '-l', action='store_true',
                        help='List available professions and exit')

    args = parser.parse_args()

    if args.list:
        print("Available professions:")
        for name, attrs in PROFESSION_CONFIGS.items():
            print(f"  {name}:")
            for attr in attrs:
                print(f"    - {attr['name']} ({attr['attribute']})")
        return

    if args.profession not in PROFESSION_CONFIGS:
        print(f"Unknown profession: {args.profession}")
        print(f"Available: {', '.join(PROFESSION_CONFIGS.keys())}")
        sys.exit(1)

    print(f"Generating {args.profession} skill tree...")
    if args.seed:
        print(f"Using seed: {args.seed}")

    gen = SkillTreeGenerator(
        args.profession,
        PROFESSION_CONFIGS[args.profession],
        seed=args.seed,
        side_merge_chance=args.side_merge,
        block_end_chance=args.block_end,
        block_max_height=args.block_max_height,
        block_edge_chance=args.block_edge,
        shift_chance=args.shift,
        double_corner_chance=args.double_corner
    )

    gen.generate_tree(
        total_rows=args.rows,
        diverge_chance=args.diverge,
        converge_chance=args.converge,
        block_chance=args.block
    )

    skill_tree_path, nodes_path = gen.save_files(args.output)

    print(f"Created: {skill_tree_path}")
    print(f"Created: {nodes_path}")
    print(f"Total nodes: {len(gen.nodes)}")
    print(f"Total rows: {len(gen.rows)}")


if __name__ == "__main__":
    main()
