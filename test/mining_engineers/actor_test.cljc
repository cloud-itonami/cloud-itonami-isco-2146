(ns mining-engineers.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mining-engineers.actor :as actor]
            [mining-engineers.advisor :as advisor]
            [mining-engineers.store :as store]))

(deftest graph-construction
  (testing "Graph builds without errors"
    (let [graph (actor/build-graph (advisor/mock-advisor) #(store/new-mem-store))]
      (is (contains? graph :nodes))
      (is (contains? graph :edges))
      (is (contains? graph :start-node))
      (is (contains? graph :end-node)))))

(deftest run-request-clean-flow
  (testing "Request flows through to commit"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          graph-config {:advisor (advisor/mock-advisor) :store s}
          request {:engineer-id "eng1" :site {:site-id "site1"} :operation :draft-processing-design}
          context {}
          result (actor/run-request! graph-config request context s)]
      (is (not (:hard? (:verdict result))))
      (is (not (:escalate? (:verdict result)))))))

(deftest run-request-hard-block
  (testing "Unregistered engineer leads to :held outcome"
    (let [s (store/new-mem-store)
          graph-config {:advisor (advisor/mock-advisor) :store s}
          request {:engineer-id "unknown" :site {:site-id "site1"} :operation :log-site-data}
          context {}
          result (actor/run-request! graph-config request context s)]
      (is (= :held (:outcome result)))
      (is (:hard? (:verdict result))))))

(deftest run-request-escalation
  (testing "Safety risk flag leads to :escalated outcome"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          graph-config {:advisor (advisor/mock-advisor) :store s}
          request {:engineer-id "eng1" :site {:site-id "site1"} :operation :flag-safety-risk
                   :params {:risk-type "ventilation-concern"}}
          context {}
          result (actor/run-request! graph-config request context s)]
      (is (= :escalated (:outcome result)))
      (is (not (:hard? (:verdict result))))
      (is (:escalate? (:verdict result))))))

(deftest approve-escalated-request
  (testing "Approval converts escalated request to committed"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          graph-config {:advisor (advisor/mock-advisor) :store s}
          request {:engineer-id "eng1" :site {:site-id "site1"} :operation :flag-safety-risk}
          context {}
          escalated (actor/run-request! graph-config request context s)
          approved (actor/approve! escalated)]
      (is (= :escalated (:outcome escalated)))
      (is (= :committed (:outcome approved))))))

(deftest advisor-integration
  (testing "Advisor generates valid proposals"
    (let [s (store/new-mem-store)
          adv (advisor/mock-advisor)
          request {:engineer-id "eng1" :site {:site-id "site1"} :operation :draft-processing-design
                   :params {:ore-type "copper" :capacity-tpd 500}}
          proposal (advisor/-advise adv s request)]
      (is (= :propose (:effect proposal)))
      (is (= :draft-processing-design (:op proposal)))
      (is (number? (:confidence proposal)))
      (is (<= 0.0 (:confidence proposal) 1.0)))))

(deftest ledger-logging
  (testing "Committed proposals are logged to ledger"
    (let [s (store/new-mem-store)
          _ (store/register-engineer! s {:engineer-id "eng1"})
          _ (store/register-site! s {:site-id "site1" :risk-level :low})
          _ (store/log-record! s :proposal-committed
              {:request {:engineer-id "eng1"} :proposal {:op :log-site-data}})
          ledger (store/ledger s)]
      (is (> (count ledger) 0))
      (is (some #(= :record-logged (:type %)) ledger))
      (is (some #(= :proposal-committed (:record-type %)) ledger)))))
