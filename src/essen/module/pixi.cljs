(ns essen.module.pixi
  (:require
   ["pixi.js" :as PIXI]
   [clojure.spec.alpha :as s]
   [integrant.core :as ig]
   [essen.system.scene :as scene]
   [essen.module.pixi.state :as state :refer [sheets textures animations]]
   [essen.module.pixi.render]
   [essen.module.pixi.component.sprite :as component.sprite]
   [com.rpl.specter :as specter :refer [MAP-VALS] :refer-macros [transform]]))

(defn js-keys->clj-keys [o]
  (transform [MAP-VALS] clj->js (js->clj o)))

(defn- spritesheet-loaded
  [{::keys [spritesheet name transition] :context/keys [scene] :as opts}]
  (let [sheet
        (-> (.-shared PIXI/Loader)
            (.-resources)
            (aget spritesheet))]
    (swap! sheets assoc name sheet)
    (swap! textures assoc name (js-keys->clj-keys (.-textures sheet)))
    (swap! animations assoc name (js-keys->clj-keys (.-animations (.-spritesheet sheet)))))

  (scene/halt! scene)
  (scene/start! transition))

(defmethod ig/pre-init-spec ::load-spritesheet [_]
  (s/keys :req [::spritesheet
                ::name
                ::transition]))

(defmethod ig/init-key ::load-spritesheet [_ {::keys [spritesheet] :as opts}]
  (-> (.-shared PIXI/Loader)
      (.add spritesheet)
      (.load (partial spritesheet-loaded opts))))

(def config
  (merge
   component.sprite/config))

(derive :essen.module/pixi :essen.module/render)
(derive :essen.module.spawn/pixi :essen.module.spawn/render)

