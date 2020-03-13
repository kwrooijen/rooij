(ns essen.system.component
  (:require
   [integrant.core :as ig]
   [essen.util :refer [vec->map]]
   [essen.state :refer [persistent-components]]
   [essen.system :as es]))

(defmulti persistent-resume
  (fn [key _opts _state]
    (#'ig/normalize-key key)))

(defmethod persistent-resume :default [_key _opts state] state)

(defn path
  ([entity]
   [:scene/entities entity
    :entity/components])
  ([entity component]
   [:scene/entities entity
    :entity/components component]))

(defn get-persistent-state [k opts]
  (when-let [state (get @persistent-components (last k))]
    (persistent-resume k opts (:component/state state))))

(defmethod es/init-key :essen/component [k opts]
  (-> opts
      (select-keys [:component/tickers :component/handlers :component/reactors])
      (assoc :component/key (last k)
             :component/state (or (get-persistent-state k opts)
                                  (ig/init-key k opts))
             :component/persistent (:persistent (meta k)))
      (update :component/tickers vec->map :ticker/key)
      (update :component/handlers vec->map :handler/key)
      (update :component/reactors vec->map :reactor/key)))
