(ns mining-engineers.advisor
  "Advisor protocol and implementations for mining engineer proposals.

  The Advisor is responsible for generating proposals (draft designs, site logs, etc.)
  in response to a request. It does NOT commit records or make governor-level decisions.

  Two implementations:
  - `mock-advisor`: deterministic, default, zero dependencies
  - `llm-advisor`: wraps a langchain ChatModel (TBD integration)

  Both implementations MUST:
  - Always produce a `:propose` effect (never direct write)
  - Include a confidence score (0.0–1.0)
  - Return nil + confidence 0.0 on parse/semantic errors (forces escalation)
  ")

#?(:clj (defn- now-ms [] (System/currentTimeMillis)))
#?(:cljs (defn- now-ms [] (js/Date.now)))

(defprotocol Advisor
  "Protocol for generating engineering proposals."
  (-advise [advisor store request]
    "Given a request (map with :site-id, :engineer-id, :operation, :params),
    return a proposal map: {:op :op-type
                             :effect :propose
                             :details {...}
                             :confidence <0.0-1.0>
                             :reasoning \"...\"}
    On parse/semantic error, return {:confidence 0.0 :error \"...\"}"))

(deftype MockAdvisor []
  Advisor
  (-advise [_ store request]
    (let [{:keys [operation engineer-id params]} request
          site-id (get-in request [:site :site-id])]
      (cond
        (nil? site-id)
        {:effect :propose :op operation :confidence 0.0 :error "missing site-id"}

        (nil? engineer-id)
        {:effect :propose :op operation :confidence 0.0 :error "missing engineer-id"}

        ;; :draft-processing-design
        (= operation :draft-processing-design)
        {:effect :propose
         :op :draft-processing-design
         :confidence 0.85
         :details {:ore-type (:ore-type params "unknown")
                   :processing-route (:processing-route params "standard")
                   :equipment-candidates (:equipment-candidates params [])
                   :draft-capacity-tpd (:capacity-tpd params 100)}
         :reasoning "Draft processing design generated from site data and parameters"}

        ;; :log-site-data
        (= operation :log-site-data)
        {:effect :propose
         :op :log-site-data
         :confidence 0.95
         :details {:data-type (:data-type params "assay")
                   :logged-at (:timestamp params (now-ms))
                   :summary (:summary params {})}
         :reasoning "Site data logged for analyst review"}

        ;; :flag-safety-risk
        (= operation :flag-safety-risk)
        {:effect :propose
         :op :flag-safety-risk
         :confidence 1.0
         :details {:risk-type (:risk-type params "unspecified")
                   :severity (:severity params :medium)
                   :description (:description params "")}
         :reasoning "Safety risk flagged for escalation"}

        ;; :request-client-review
        (= operation :request-client-review)
        {:effect :propose
         :op :request-client-review
         :confidence 0.8
         :details {:review-scope (:review-scope params [])
                   :proposed-date (:proposed-date params nil)
                   :stakeholders (:stakeholders params [])}
         :reasoning "Client review session proposed"}

        :else
        {:effect :propose :op operation :confidence 0.0 :error (str "unknown operation: " operation)}))))

(defn mock-advisor
  "Create a mock advisor (deterministic, suitable for testing)."
  []
  (MockAdvisor.))

;; LLM advisor would be integrated here (TBD):
;; (deftype LLMAdvisor [model]
;;   Advisor
;;   (-advise [_ store request]
;;     ;; Call langchain model, parse response, extract proposal with confidence
;;     ))
;;
;; (defn llm-advisor [model] (LLMAdvisor. model))
