(ns essen.state
  #?(:cljs
     (:refer-clojure :exclude [atom]))
  #?(:cljs
     (:require
      [reagent.core :as r])))

#?(:cljs
   (defn atom [v]
     (r/atom v)))

(defonce config (atom {}))

(defonce system (atom {}))

(defonce ^:private state (atom {}))

(defonce ^:private scenes (atom {}))

(defonce ^:private events (atom {}))

(defonce ^:private input-events (atom {}))

(defonce ^:private persistent-components (atom {}))

(defn reset-events! [scene-key]
  (swap! events assoc scene-key (atom []))
  (swap! input-events assoc scene-key (atom [])))

(defn get-scenes []
  @scenes)

(defn get-scene [scene-key]
  (get @scenes scene-key))

(defn get-scene-events [scene-key]
  (get @events scene-key))

(defn save-scene! [scene]
  (swap! scenes assoc (:scene/key scene) (atom scene)))

(defn reset-state! [scene-key]
  (swap! state update :essen/scenes dissoc scene-key))

(defn get-component [entity-key component-key]
  (get @persistent-components [entity-key component-key]))

(defn save-component! [state entity-key component-key]
  (swap! persistent-components assoc [entity-key component-key] state))

(defn get-input-events
  ([] @input-events)
  ([scene-key] (get @input-events scene-key)))
