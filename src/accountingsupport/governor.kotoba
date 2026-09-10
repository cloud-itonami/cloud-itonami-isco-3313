(ns accountingsupport.governor
  "AccountingSupportGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every transaction posting an advisor may propose for an account.
  The governor never dispatches hardware itself and never posts a
  ledger entry above the client's registered transaction-amount
  ceiling. Modeled on cloud-itonami-isco-4311's bookkeeping.governor.
  Task twist: a proposed transaction amount is an arithmetic ceiling
  against the account's registered transaction-amount ceiling, and a
  posting cannot proceed until a source document has been attached to
  that specific proposal.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the small business/sole proprietor
                                must be registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never posts a ledger entry above the
                                registered transaction-amount ceiling;
                                it only gates what the advisor may
                                post).
    3. account basis          — a posting proposal must cite a
                                REGISTERED account belonging to this
                                client.
    4. transaction-amount ceiling — the proposed transaction amount
                                must not exceed the account's
                                registered `:max-transaction-amount`
                                (posting beyond the client's
                                registered ceiling is unauthorized
                                posting, not routine bookkeeping).
    5. source-document attached — the proposal must have
                                `:source-document-attached?` true
                                before any transaction can be posted
                                (posting a transaction without an
                                attached source document is an
                                invented transaction, not efficient
                                service).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-over-ceiling-posting (no ledger posting above the
                                client's registered transaction-amount
                                ceiling without the governor gate).
    7. :op :approve-period-close (closing an accounting period always
                                requires human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [accountingsupport.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-over-ceiling-posting
                                     :approve-period-close})

(defn- hard-violations [{:keys [request proposal]} client-record a]
  (let [{:keys [op transaction-amount source-document-attached?]} proposal
        post? (= :approve-transaction-posting op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は登録上限超過の記帳を直接実行しない）"})

      (and post? (nil? a))
      (conj {:rule :unknown-account :detail "未登録 account への記帳提案は不可"})

      (and post? a (not= (:client-id a) (:client-id request)))
      (conj {:rule :account-wrong-client :detail "account が別 client のもの"})

      (and post? a (number? transaction-amount) (> transaction-amount (:max-transaction-amount a)))
      (conj {:rule :transaction-exceeds-ceiling
             :detail (str "取引額 " transaction-amount " > 登録済み上限 "
                          (:max-transaction-amount a) "（登録上限を超える記帳は無許可記帳であって通常の記帳業務ではない）")})

      (and post? (not source-document-attached?))
      (conj {:rule :missing-source-document
             :detail "原始証憑が添付されていない取引の記帳は架空取引であって効率的サービスではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `accountingsupport.store/Store`. Pure — never
  mutates the store, never posts a ledger entry above the registered
  ceiling."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        a (some->> (:account-id proposal) (store/account store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record a)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
