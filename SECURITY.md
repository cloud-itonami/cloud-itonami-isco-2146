# Security Policy

## Reporting Vulnerabilities

If you discover a security vulnerability in this actor or its dependencies, please report it responsibly by opening a confidential security advisory on GitHub. Do not open a public issue.

## Security Considerations

### Hard Blocks Are Irreversible

The governor's hard-block verdict (`:hard? true`) is final and cannot be overridden. Any change to hard-block rules must go through design review and testing before deployment.

### Audit Ledger Integrity

All records committed to the audit ledger are append-only and immutable. Do not write code that modifies or deletes ledger entries.

### Scope Boundary Enforcement

The following operations are permanently blocked:
- `:issue-certified-design` — Only licensed engineers can issue final designs
- `:certify-compliance` — Only licensed engineers can certify regulatory compliance
- `:extract`, `:blast` — Extraction and blasting are mining operator-exclusive
- `:mine-safety-auth` — Mine safety determinations are exclusive to licensed professionals
- `:ventilation-auth`, `:equipment-sequence` — Direct mine operations are blocked

Any code change that attempts to weaken these blocks is a security violation.

### Human-in-the-Loop for Safety Flags

All `:flag-safety-risk` operations must escalate. Code that suppresses or silently logs safety flags is a security violation.

## Dependency Updates

Keep dependencies (especially `langgraph` and test runners) up to date. Report any supply-chain security concerns to the maintainers.

## License

AGPL-3.0-or-later.
