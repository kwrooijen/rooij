(ns essen.system.middleware
  (:require
   [essen.system :as es]
   [essen.util :refer [top-key]]
   [essen.state :refer [get-scene]]
   [integrant.core :as ig]))

(defn path
  ([entity component handler]
   [:scene/entities entity
    :entity/components component
    :component/handlers handler
    :handler/middleware])
  ([entity component handler middleware]
   [:scene/entities entity
    :entity/components component
    :component/handlers handler
    :handler/middleware middleware]))

(defn add!
  ([{:context/keys [scene-key entity-key component]} handler middleware opts]
   (add! scene-key entity-key component handler middleware opts))
  ([scene entity component handler middleware opts]
   (let [context {:context/scene-key scene
                  :context/entity-key entity}
         opts (merge opts context)]
     (swap! (get-scene scene)
            assoc-in
            (path entity component handler middleware)
            {:middleware/key middleware
             :middleware/fn (ig/init-key middleware opts)}))))

(defn remove!
  ([{:context/keys [scene-key entity-key component]} handler middleware]
   (remove! scene-key entity-key component handler middleware))
  ([scene-key entity-key component handler middleware]
   (swap! (get-scene scene-key)
          update-in
          (path entity-key component handler)
          dissoc middleware)))

(defmethod es/init-key :essen/middleware [k opts]
  (assoc opts
         :middleware/key (top-key k)
         :middleware/fn (ig/init-key k opts)))
