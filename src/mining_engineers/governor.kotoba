(ns mining-engineers.governor
  "MiningEngineerGovernor — the independent safety/authority layer for the
  ISCO-08 2146 mining engineers, metallurgists and related professionals actor.
  Wired as its own `:govern` node in `mining-engineers.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of engineer/site provenance
  or licensed-engineer-exclusive operations, so this MUST be a separate system
  able to reject a proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  CRITICAL DOMAIN NOTE: This actor drafts and prepares engineering analysis
  material (ore-processing design calculations, mine-plan review drafts) FOR
  a LICENSED MINING ENGINEER's professional review and sign-off. It does NOT
  issue final certified engineering designs, authorize blasting/extraction,
  or determine mine-safety authority. Those remain the ENGINEER's exclusive
  authority. Scope boundaries:
    ✓ Ore-processing design draft calculations
    ✓ Mine-plan review and impact assessment preparation
    ✓ Site/ore assessment data logging
    ✓ Safety risk flagging (always escalated)
    ✓ Client/regulator review session scheduling
    ✗ Issue final certified engineering design
    ✗ Authorize blasting or extraction
    ✗ Make mine-safety determinations
    ✗ Certify compliance or regulatory sign-off
    ✗ Direct actuation of mining operations

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. engineer provenance     — the request's engineer must be registered.
    2. site provenance         — ops must involve a registered mine-site.
    3. no-actuation            — proposal :effect must be :propose.
    4. no-licensed-ops         — any proposal flagged :op with engineer-exclusive
                               ops (:issue-certified-design/:certify-compliance/
                               :extract/:blast/:mine-safety-auth/:equipment-sequence/
                               :ventilation-auth) is an instant hard block with
                               no override path.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    5. :op :flag-safety-risk   — ALL safety risk flags escalate.
    6. high-risk site          — any operation on a site flagged :risk-level :high
                               escalates.
    7. low confidence          — < `confidence-floor`."
  (:require [mining-engineers.store :as store]))

(def confidence-floor 0.6)

;; Ops that are hard-blocked (exclusive to licensed engineer; not in drafting/support domain)
(def ^:private licensed-engineer-exclusive-ops
  #{:issue-certified-design :certify-compliance :extract :blast
    :mine-safety-auth :equipment-sequence :ventilation-auth
    :set-production-target})

;; Ops that always escalate
(def ^:private always-escalate-ops #{:flag-safety-risk})

(defn- hard-violations [{:keys [proposal site-id]} engineer-record site-record]
  (cond-> []
    (nil? engineer-record)
    (conj {:rule :no-engineer :detail "unregistered engineer"})

    (and site-id (nil? site-record))
    (conj {:rule :no-site :detail "unregistered mine-site"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct write)"})

    (contains? licensed-engineer-exclusive-ops (:op proposal))
    (conj {:rule :licensed-engineer-exclusive-blocked
           :detail "certified design issuance, compliance certification, extraction/blasting/safety authority are LICENSED ENGINEER-exclusive; not in support/drafting scope"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `mining-engineers.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [engineer-record (store/engineer store (:engineer-id request))
        site-id (get-in request [:site :site-id])
        site-record (when site-id (store/mine-site store site-id))
        hard (hard-violations {:proposal proposal :site-id site-id} engineer-record site-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        safety-op? (contains? always-escalate-ops (:op proposal))
        high-risk? (and site-record (= :high (:risk-level site-record)))]
    {:ok? (and (not hard?) (not low?) (not safety-op?) (not high-risk?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? safety-op? high-risk?))}))
