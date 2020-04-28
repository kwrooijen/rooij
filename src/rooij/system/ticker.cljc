(ns rooij.system.ticker
  (:require
   [integrant.core :as ig]
   [meta-merge.core :refer [meta-merge]]
   [rooij.state :as state]
   [rooij.system.core :as system]
   [rooij.util :refer [top-key]]
   [taoensso.timbre :as timbre]))

(defn path
  ([entity-key component-key]
   [:scene/entities entity-key
    :entity/components component-key
    :component/tickers])
  ([entity-key component-key ticker]
   [:scene/entities entity-key
    :entity/components component-key
    :component/tickers ticker]))

(defn add!
  ([{:context/keys [scene-key entity-key component-key]} ticker]
   (add! scene-key entity-key component-key ticker))
  ([scene-key entity-key component-key ticker-key]
   (swap! (state/get-scene-post-events scene-key) conj
          {:add/path (path entity-key component-key)
           :add/key ticker-key
           :event/type :add/ticker})))

(defn remove!
  ([{:context/keys [scene-key entity-key component-key]} ticker]
   (remove! scene-key entity-key component-key ticker))
  ([scene-key entity-key component-key ticker-key]
   (swap! (state/get-scene-post-events scene-key) conj
          {:remove/path (path entity-key component-key)
           :remove/key ticker-key
           :event/type :remove/ticker})))

(defn init [{:ticker/keys [key opts] :as ticker}]
  (-> ticker
      (assoc :ticker/fn (ig/init-key key opts))
      (update :ticker/subs meta-merge (:ticker/subs opts))))

(defmethod system/init-key :rooij/ticker [k opts]
  (timbre/debug ::init-key opts)
  (assoc opts
         :ticker/opts opts
         :ticker/key (top-key k)
         :ticker/init (system/get-init-key k)
         :ticker/fn nil))
