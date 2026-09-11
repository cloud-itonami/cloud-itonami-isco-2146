# Contributing

We welcome contributions to improve the Mining Engineers actor implementation. Before making changes, please review the domain boundaries documented in `README.md` and `GOVERNANCE.md`.

## Design Review

Any proposal that extends the actor's operations or modifies the governor rules must include a design review demonstrating:

1. **Scope Alignment** — The proposed operation or rule aligns with ISCO-08 2146 occupational standards and does not violate licensed engineer authority.

2. **Safety Justification** — If the proposal affects safety-critical operations or escalation logic, explain why the change maintains or improves safety posture.

3. **Audit Trail** — Any ledger format changes must preserve append-only semantics.

## Testing

All changes must be accompanied by tests. Run the test suite before submitting:

```bash
kbb -M:test
```

## Code Style

- Follow standard Clojure conventions (kebab-case for symbols, SCREAMING_SNAKE_CASE for constants).
- Include docstrings for public functions and protocols.
- Keep governor and advisor logic pure (no side effects except in store operations).

## License

By contributing, you agree that your contributions will be licensed under the AGPL-3.0-or-later license.
