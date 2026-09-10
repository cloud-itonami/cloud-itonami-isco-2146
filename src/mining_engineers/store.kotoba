(ns mining-engineers.store
  "Store protocol and in-memory implementation for mining engineer records,
  site inventory, engineer registry, and append-only audit ledger.

  Stores:
  - Engineer registry (registered mining/metallurgical engineers)
  - Mine site registry (registered mining sites)
  - Engineering records (proposals, design drafts, assessments)
  - Append-only audit ledger (immutable, all operations logged)

  Default implementation: `MemStore` (memory-backed, suitable for testing
  and local deployment). Swap for Datomic/kotoba-server without touching
  actor or governor.")

#?(:clj (defn- now-ms [] (System/currentTimeMillis)))
#?(:cljs (defn- now-ms [] (js/Date.now)))

(defprotocol Store
  "Protocol for engineer record storage and audit ledger."
  (engineer [store engineer-id]
    "Look up a registered engineer by ID. Returns map or nil.")
  (mine-site [store site-id]
    "Look up a registered mine-site by ID. Returns map or nil.")
  (records-of [store site-id]
    "All engineering records (proposals, drafts, logs) for a site.")
  (ledger [store]
    "Append-only audit trail: all operations in log order.")
  (register-engineer! [store engineer-record]
    "Register a new engineer. Appends ledger entry. Returns engineer-record.")
  (register-site! [store site-record]
    "Register a new mine site. Appends ledger entry. Returns site-record.")
  (log-record! [store record-type record-data]
    "Log an engineering record (proposal committed, flagged, escalated, etc.
    Returns the logged record.")
  (append-ledger! [store fact]
    "Append a raw ledger entry (internal use; prefer log-record!)."))

(deftype MemStore [engineers-atom sites-atom records-atom ledger-atom]
  Store
  (engineer [_ engineer-id]
    (get @engineers-atom engineer-id))

  (mine-site [_ site-id]
    (get @sites-atom site-id))

  (records-of [_ site-id]
    (filter #(= (:site-id %) site-id) @records-atom))

  (ledger [_]
    @ledger-atom)

  (register-engineer! [store engineer-record]
    (let [eng-id (:engineer-id engineer-record)]
      (swap! engineers-atom assoc eng-id engineer-record)
      (append-ledger! store {:type :engineer-registered
                             :engineer-id eng-id
                             :timestamp (now-ms)})
      engineer-record))

  (register-site! [store site-record]
    (let [site-id (:site-id site-record)]
      (swap! sites-atom assoc site-id site-record)
      (append-ledger! store {:type :site-registered
                             :site-id site-id
                             :timestamp (now-ms)})
      site-record))

  (log-record! [store record-type record-data]
    (let [record (assoc record-data :record-type record-type :timestamp (now-ms))]
      (swap! records-atom conj record)
      (append-ledger! store {:type :record-logged :record-type record-type :record-id (:id record)})
      record))

  (append-ledger! [_ fact]
    (swap! ledger-atom conj fact)))

(defn new-mem-store
  "Create a new in-memory store (zero dependencies)."
  []
  (MemStore. (atom {}) (atom {}) (atom []) (atom [])))
