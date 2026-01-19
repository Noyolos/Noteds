# AGENT.md

## Purpose
- This file defines workflow and guardrails only.
- Project facts live in `ai/PROJECT.md` and `ai/CONTRACTS.md`.

## Workflow
- Plan -> Minimal diff -> Smoke tests -> Report.

## Guardrails
- Always search and trace call chains before editing (use `rg` first).
- Minimal diffs; no unrelated refactors or formatting-only churn.
- Update `ai/PROJECT.md` and `ai/CONTRACTS.md` when behavior or contracts change.
- Remove any duplicated project-detail text outside `ai/` (replace with links).
- Never include secrets; only env var names.

## Reporting (required)
- Files changed.
- Commands run.
- Results and failures.
- TODOs/unknowns to verify.
