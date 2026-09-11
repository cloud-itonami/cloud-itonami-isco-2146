# cloud-itonami-isco-2146

Open Occupation Blueprint for **ISCO-08 2146**: Mining Engineers, Metallurgists and Related Professionals.

This repository designs an autonomous LLM/advisor behind an independent Governor with a langgraph-clj StateGraph and append-only audit ledger. The actor drafts and prepares engineering analysis material (ore-processing design calculations, mine-plan review drafts) for a licensed mining engineer's professional review and sign-off, enabling the engineer to maintain independent records and engineering determinations instead of relying on closed proprietary software.

## Critical domain boundary: ENGINEERING SUPPORT, NOT LICENSED ENGINEER'S AUTHORITY

**This actor supports a LICENSED MINING ENGINEER's professional workflow — NOT independent issuance of certified engineering designs, blasting/extraction authorization, or mine-safety determinations.**

Scope: Licensed engineer support operations
- ✓ Ore-processing design draft calculations and metallurgical analysis
- ✓ Mine-plan review and impact assessment preparation
- ✓ Site/ore assessment data logging and organization
- ✓ Safety risk flagging (always escalated to human review)
- ✓ Client/regulator review session scheduling

Out of scope: Licensed engineer-exclusive decisions (hard-blocked, no override path)
- ✗ Issue final certified engineering design or calculations (exclusive to licensed engineer)
- ✗ Authorize blasting or extraction operations
- ✗ Make mine-safety determinations (ventilation adequacy, hazard classification, safety authority)
- ✗ Certify compliance or regulatory sign-off
- ✗ Direct actuation of any mining operation

This boundary is enforced as a hard invariant in the governor (`mining-engineer-governor`): any proposal flagged with licensed-engineer-exclusive ops (`:issue-certified-design`, `:certify-compliance`, `:extract`, `:blast`, `:mine-safety-auth`, etc.) is instantly blocked with no override path.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the domain work**. Here an analysis robot performs ore-processing calculations, site data logging, and plan review preparation under an actor that proposes actions and an independent **Mining Engineer Governor** that gates them. The governor never dispatches operations itself; `:flag-safety-risk` logging or high-risk site operations require human sign-off. Licensed engineer authorization, blasting/extraction, and mine-safety decisions are engineer-exclusive and permanently blocked.

## Core Contract

```text
ore samples + site data + processing requirements
        |
        v
Engineer Advisor -> Engineer Governor -> design drafts/analysis/logging, or human sign-off
        |
        v
engineering analysis (gated) + operating records + audit ledger
```

No automated advice can issue a certified engineering design, authorize operations the governor refuses, suppress an operating record, or disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2146`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

## Proposal Operations

All proposal operations carry `:effect :propose` (never direct actuation):

- **`:draft-processing-design`** — Ore-processing or metallurgical design draft (candidate design flows, material balances, equipment sizing). The draft is prepared FOR the licensed engineer's review and sign-off, never issued directly as a final design.

- **`:log-site-data`** — Site/ore assessment data logging: core samples, assay results, geological surveys, mine geometry, etc. Organizes raw data for engineer analysis.

- **`:flag-safety-risk`** — Surface a mine-safety or process-safety risk (e.g., unstable ore body, hazardous material exposure, inadequate ventilation observed in plan). Always escalates to human sign-off; never suppresses the flag.

- **`:request-client-review`** — Propose scheduling a client/regulator review session, providing draft materials and preliminary findings for external validation.

## Governor Rules

The `MiningEngineerGovernor` is an independent system that gates all proposals:

### Hard Violations (permanent block, `:hold`)
1. **Project provenance** — the request's project must be registered
2. **No actuation** — proposal `:effect` must be `:propose` (never `:direct-write` or similar)
3. **No licensed-engineer ops** — any proposal flagged with operations exclusive to licensed engineers (`:issue-certified-design`, `:certify-compliance`, `:extract`, `:blast`, `:mine-safety-auth`, `:equipment-sequence`, `:ventilation-auth`) is an instant hard block with no override path
4. **No site-mining decisions** — proposals to make extraction/blasting/production decisions are blocked

### Escalation Rules (always human sign-off, `:request-approval`)
1. **`:flag-safety-risk` operation** — ALL safety risk flags escalate
2. **High-risk site** — any operation on a site flagged `:risk-level :high` escalates
3. **Low confidence** — proposal confidence < 0.6 (on a 0.0–1.0 scale)

### Clean Flow
Proposals that pass all checks proceed to `:commit` without interruption.

## Architecture

```
:intake → :advise → :govern → :decide ─┬─→ :commit           (ok)
                                        ├─→ :request-approval  (escalate)
                                        └─→ :hold              (hard)
```

- `src/mining_engineers/store.kotoba` — `Store` protocol + `MemStore`:
  registered engineers, registered mine-sites, committed records, append-only audit ledger.
- `src/mining_engineers/advisor.kotoba` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes an engineering operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a committed design, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/mining_engineers/governor.kotoba` — `MiningEngineerGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered engineer, unregistered mine-site, a proposal whose `:effect`
  isn't `:propose`, or any licensed-engineer-exclusive op) always route to `:hold`.
  Escalation invariants (`:flag-safety-risk`, high-risk site operations, or low advisor
  confidence) always route to `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval (`actor/approve!`).
- `src/mining_engineers/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section): a real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```bash
kbb -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
