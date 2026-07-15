# cloud-itonami-isco-3313

Open Occupation Blueprint for **ISCO-08 3313**: Accounting Associate Professionals.

This repository designs a forkable OSS business for an independent accounting support practice: a document intake and archival robot manages transaction records under a governor-gated actor, so the practice keeps its own posting records instead of renting a closed bookkeeping SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document intake and archival robot performs receipt scanning, ledger-entry filing and physical archival under an actor that proposes
actions and an independent **Accounting Support Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
ledger posting above the client's registered transaction-amount ceiling) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client transaction batch + chart of accounts + reconciliation policy
        |
        v
Accounting Advisor -> Accounting Support Governor -> post entry/reconcile, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3313`). Required capabilities:

- :robotics
- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
