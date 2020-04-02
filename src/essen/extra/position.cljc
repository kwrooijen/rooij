(ns essen.extra.position
  (:require
   [essen.core :as essen]
   [integrant.core :as ig]))

(defmethod ig/init-key :component.essen/position [_ {:position/keys [x y]}]
  ;; TODO separate context + rename scene / entity to scene-key and entity-key
  (fn [{:context/keys [scene-key entity-key]}]
    (essen/emit!
     {:event/scene scene-key
      :event/entity entity-key
      :event/handler :handler.essen.position/set
      :event/content {:position/x x :position/y y}})
    {:position/x x
     :position/y y}))

(defmethod ig/init-key :handler.essen.position/set [_ _opts]
  (fn [_context event _state]
    (select-keys event [:position/x :position/y])))

(def config
  {[:essen/component :component.essen/position]
   {:component/handlers [(ig/ref :handler.essen.position/set)]}

   [:essen/handler :handler.essen.position/set] {}})
