(ns accountingsupport.store
  "SSoT for the ISCO-08 3313 independent accounting support practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a document intake and
  archival robot performs receipt scanning, ledger-entry filing and
  physical archival under this advisor/governor pair, which never
  dispatches hardware itself and never posts a ledger entry above the
  client's registered transaction-amount ceiling). Modeled on
  cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client  — a registered small business/sole proprietor
              (:client-id, :name)
    account — a registered client account {:account-id :client-id
              :name :max-transaction-amount number}.
              `:max-transaction-amount` is the registered ceiling a
              proposed transaction amount must not exceed — posting a
              transaction beyond the client's registered ceiling is
              unauthorized posting, not routine bookkeeping.
    record  — a committed operating record (a posted transaction) —
              written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (account [s account-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-account! [s a])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (account [_ account-id] (get-in @a [:accounts account-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-account! [s acct]
    (swap! a assoc-in [:accounts (:account-id acct)] acct) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :accounts {} :records [] :ledger []}
                                   seed)))))
