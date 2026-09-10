(ns mining-engineers.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mining-engineers.governor :as governor]
            [mining-engineers.store :as store]))

(deftest hard-violations-unregistered-engineer
  (testing "Blocks proposals from unregistered engineers"
    (let [s (store/new-mem-store)
          request {:engineer-id "unknown-eng" :site {:site-id "site1"}}
          context {}
          proposal {:op :draft-processing-design :effect :propose :confidence 0.9}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict))
      (is (some #(= :no-engineer (:rule %)) (:violations verdict))))))

(deftest hard-violations-unregistered-site
  (testing "Blocks proposals for unregistered mine sites"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          request {:engineer-id "eng1" :site {:site-id "unknown-site"}}
          context {}
          proposal {:op :log-site-data :effect :propose :confidence 0.9}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict))
      (is (some #(= :no-site (:rule %)) (:violations verdict))))))

(deftest hard-violations-no-actuation
  (testing "Blocks proposals with effect other than :propose"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          proposal {:op :draft-processing-design :effect :direct-write :confidence 0.9}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict))
      (is (some #(= :no-actuation (:rule %)) (:violations verdict))))))

(deftest hard-violations-licensed-engineer-exclusive
  (testing "Blocks licensed-engineer-exclusive operations"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          test-ops [:issue-certified-design :certify-compliance :extract :blast
                    :mine-safety-auth :equipment-sequence :ventilation-auth]]
      (doseq [op test-ops]
        (let [proposal {:op op :effect :propose :confidence 0.9}
              verdict (governor/check request context proposal s)]
          (is (not (:ok? verdict)) (str "Failed for op: " op))
          (is (:hard? verdict) (str "Not hard-block for op: " op))
          (is (some #(= :licensed-engineer-exclusive-blocked (:rule %))
                    (:violations verdict)) (str "No exclusive-op block for: " op)))))))

(deftest escalation-safety-risk
  (testing "All :flag-safety-risk operations escalate"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          proposal {:op :flag-safety-risk :effect :propose :confidence 0.95}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (not (:hard? verdict)))
      (is (:escalate? verdict)))))

(deftest escalation-high-risk-site
  (testing "Operations on high-risk sites escalate"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :high})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          proposal {:op :log-site-data :effect :propose :confidence 0.9}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (not (:hard? verdict)))
      (is (:escalate? verdict)))))

(deftest escalation-low-confidence
  (testing "Low confidence proposals escalate"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          proposal {:op :draft-processing-design :effect :propose :confidence 0.5}
          verdict (governor/check request context proposal s)]
      (is (not (:ok? verdict)))
      (is (not (:hard? verdict)))
      (is (:escalate? verdict)))))

(deftest clean-flow-low-risk
  (testing "Valid proposals on low-risk sites with high confidence proceed to commit"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          request {:engineer-id "eng1" :site {:site-id "site1"}}
          context {}
          proposal {:op :draft-processing-design :effect :propose :confidence 0.85}
          verdict (governor/check request context proposal s)]
      (is (:ok? verdict))
      (is (not (:hard? verdict)))
      (is (not (:escalate? verdict))))))
