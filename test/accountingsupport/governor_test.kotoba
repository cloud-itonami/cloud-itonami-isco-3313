(ns accountingsupport.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [accountingsupport.store :as store]
            [accountingsupport.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Accounting"})
    (store/register-account! st {:account-id "A-1" :client-id "client-1"
                                 :name "account-042"
                                 :max-transaction-amount 5000})
    st))

(defn- post-op [amount attached?]
  {:op :approve-transaction-posting :effect :propose :account-id "A-1"
   :transaction-amount amount :source-document-attached? attached?
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-ceiling-and-attached
  (let [st (fresh-store)
        v (governor/check req {} (post-op 2000 true) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling-boundary
  (testing "the transaction-amount ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (post-op 5000 true) st)]
      (is (:ok? v)))))

(deftest hard-on-transaction-exceeds-ceiling
  (testing "posting a transaction beyond the client's registered ceiling is unauthorized posting, not routine bookkeeping"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (post-op 50000 true) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :transaction-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-missing-source-document
  (testing "posting a transaction without an attached source document is an invented transaction, not efficient service"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (post-op 2000 false) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :missing-source-document (:rule %)) (:violations v))))))

(deftest hard-on-unknown-account
  (let [st (fresh-store)
        v (governor/check req {} (assoc (post-op 2000 true) :account-id "A-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-account (:rule %)) (:violations v)))))

(deftest hard-on-foreign-account
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (post-op 2000 true) st)]
      (is (:hard? v))
      (is (some #(= :account-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (post-op 2000 true) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (post-op 2000 true) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-over-ceiling-posting-even-at-high-confidence
  (testing "no ledger posting above the client's registered transaction-amount ceiling without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-over-ceiling-posting :effect :propose
                                    :account-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-period-close-even-at-high-confidence
  (testing "closing an accounting period always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-period-close :effect :propose
                                    :account-id "A-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (post-op 2000 true) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
