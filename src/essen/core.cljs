(ns essen.core
  (:require
   [clojure.spec.alpha :as s]
   [essen.state :refer [input-messages messages game state systems]]
   [essen.system]
   [essen.system.component]
   [essen.system.entity]
   [essen.system.handler]
   [essen.system.keyboard]
   [essen.system.middleware]
   [essen.system.reactor]
   [essen.system.scene]
   [essen.system.ticker]
   [essen.util :refer [spy]]
   [integrant-tools.core :as it]
   [integrant.core :as ig]
   [spec-signature.core :refer-macros [sdef]]))

(defn- save-state! [scene-key]
  (swap! state assoc-in [:essen/scenes scene-key]
         (-> (get-in @systems [:essen/scenes scene-key])
             (it/find-derived-value scene-key)
             (atom))))

(defn- reset-events! [scene-key]
  (swap! messages assoc scene-key (atom []))
  (swap! input-messages assoc scene-key (atom [])))

(defn- save-system! [system scene-key]
  (swap! systems assoc-in [:essen/scenes scene-key] system))

(defn- render-run [scene-key type]
  ((-> @game :essen.module/render type) (:essen/config @game) scene-key))

(defmethod ig/init-key :essen/const [_ opts] opts)

(defn setup [{:keys [:essen/config :essen.module/render] :as game-config}]
  (reset! game game-config)
  ((:essen/setup render) config))

(defn emit!
  ([scene entity route content]
   (swap! (get @messages scene)
          conj {:message/entity entity
                :message/route route
                :message/content content})))

(defn start-scene [scene-key]
  (reset-events! scene-key)
  (-> (:essen/config @game)
      (it/prep [:it/prep-meta :ig/prep] [scene-key])
      (it/init [:essen/init] [scene-key])
      (save-system! scene-key))
  (save-state! scene-key)
  (render-run scene-key :essen/stage-start))

(defn resume-scene [scene-key]
  (reset-events! scene-key)
  (as-> (get-in @systems [:essen/scenes scene-key]) system
    (ig/resume (:essen/config @game) system [scene-key])
    (save-system! system scene-key))
  (save-state! scene-key)
  (render-run scene-key :essen/stage-resume))

(defn suspend-scene [scene-key]
  (swap! systems update-in [:essen/scenes scene-key]
         #(ig/suspend! % [scene-key]))
  (render-run scene-key :essen/stage-suspend))

(defn scenes []
  (set (keys (:essen/scenes @state))))

(defn suspend! []
  (doseq [scene (scenes)]
    (suspend-scene scene)))

(defn resume [config]
  (swap! game assoc :essen/config config)
  (doseq [scene (scenes)]
    (start-scene scene)))
