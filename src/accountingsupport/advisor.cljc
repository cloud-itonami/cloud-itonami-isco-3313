(ns accountingsupport.advisor
  "Accounting Advisor — the advisor named in this repository's README,
  proposing an accounting-support operation (post a transaction,
  approve an over-ceiling posting, approve a period close) from a
  client transaction batch, chart of accounts and reconciliation
  policy. Swappable mock/llm; the advisor ONLY proposes —
  `accountingsupport.governor` checks the transaction-amount ceiling
  and source-document attachment independently and always escalates
  over-ceiling-posting and period-close decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-transaction-posting|:approve-over-ceiling-posting|:approve-period-close
               :effect :propose :account-id str :transaction-amount
               number :source-document-attached? boolean :stake kw
               :confidence n :rationale str}"
  ;; clojure.edn, not clojure.core/read-string: this parses untrusted
  ;; advisor output, and the core reader executes #=(...) at read time.
  (:require [clojure.edn :as edn]))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake account-id transaction-amount source-document-attached?] :as request}]
  {:op op
   :effect :propose
   :account-id account-id
   :transaction-amount transaction-amount
   :source-document-attached? (boolean source-document-attached?)
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an accounting-support advisor. Given a request, propose an
   :op, the :account-id, :transaction-amount and whether a source
   document is attached, an honest :confidence and a :stake. Never
   propose a transaction beyond the account's registered ceiling, or a
   posting without an attached source document — the governor checks
   both against the registered account record. Over-ceiling postings
   and period closes always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
