(ns essen.core
  (:require
   [clojure.spec.alpha :as s]
   [essen.state :as state :refer [input-messages messages game systems]]
   [essen.util :refer [spy]]
   [essen.system]
   [essen.system.component]
   [essen.system.entity]
   [essen.system.handler]
   [essen.system.keyboard]
   [essen.system.middleware]
   [essen.system.reactor]
   [essen.system.scene]
   [essen.system.ticker]
   [integrant-tools.core :as it]
   [integrant.core :as ig]
   [spec-signature.core :refer-macros [sdef]]))

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
  (-> (state/config)
      (it/prep [:it/prep-meta :ig/prep] [scene-key])
      (it/init [:essen/init] [scene-key])
      (state/save-system! scene-key))
  (state/reset-events! scene-key)
  (state/save-state! scene-key)
  (render-run scene-key :essen/stage-start))

(defn resume-scene [scene-key]
  ;; FIXME This doesn't work
  ;; Would be nice to keep the state, but replacee the :*/fn functions.
  (-> (state/config)
      (it/prep [:it/prep-meta :ig/prep] [scene-key])
      (ig/resume (state/system scene-key) [scene-key])
      (state/save-system!  scene-key))
  (state/reset-events! scene-key)
  (state/save-state! scene-key)
  (render-run scene-key :essen/stage-resume))

(defn suspend-scene [scene-key]
  (ig/suspend! (get-in @systems [:essen/scenes scene-key]) [scene-key])
  (render-run scene-key :essen/stage-suspend))

(defn scenes []
  (set (keys (:essen/scenes @state/state))))

(defn suspend! []
  (doseq [scene (scenes)]
    (suspend-scene scene)))

(defn resume [config]
  (swap! game assoc :essen/config config)
  (doseq [scene (scenes)]
    ;; FIXME `resume-scene` needs to eb fixed, and should be called here
    (start-scene scene)))
