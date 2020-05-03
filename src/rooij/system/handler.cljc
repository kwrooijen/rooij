(ns rooij.system.handler
  (:require [meta-merge.core :refer [meta-merge]]
            [rooij.system.core :as system]
            [rooij.util :refer [->context map-kv]]
            [taoensso.timbre :as timbre]))

(defmethod system/init-key :rooij/handler [k opts]
  (timbre/debug ::init-key opts)
  (assoc opts
         :handler/init (system/get-init-key k)
         :handler/fn nil))

(defn init [{:handler/keys [key init] :as handler}]
  (assoc handler :handler/fn (init key handler)))

(defn preprocess-handler [context handler-key handler-opts]
  (-> handler-opts
      (->> (meta-merge (:handler/ref handler-opts)))
      (dissoc :handler/ref)
      (merge context)
      (assoc :handler/key handler-key)
      (as-> $ (assoc $ :handler/fn ((:handler/init $) handler-key $)))))

(defn preprocess-handlers [scene-key entity-key component-key handlers]
  (map-kv #(preprocess-handler (->context scene-key entity-key component-key) %1 %2) handlers))
