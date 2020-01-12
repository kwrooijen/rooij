(ns essen.scene
  (:require
   [integrant.core :as ig]))

(defonce custom-methods (atom {}))
(defonce scene-states (atom {}))

(defn scene-state [k]
  (get @scene-states k))

;; TODO Maybe create ig/init-key for these?
;; Then the end user can create methods that way
(def method-collection
  {[:image 3]                    #(.image %1 %2 %3)
   [:image 4]                    #(.image %1 %2 %3 %4)
   [:set-origin 2]               #(.setOrigin %1 %2)
   [:set-flip-x 2]               #(.setFlipX %1 %2)
   [:set-flip-y 2]               #(.setFlipY %1 %2)
   [:set-bounds 5]               #(.setBounds %1 %2 %3 %4 %5)
   [:create-cursor-keys 1]       #(.createCursorKeys %1)
   [:start-follow 5]             #(.startFollow %1 %2 %3 %4 %5)
   [:set-collide-world-bounds 2] #(.setCollideWorldBounds %1 %2)
   [:set-active 2]               #(.setActive %1 %2)
   [:group 1]                    #(.group (clj->js %1))
   [:group 2]                    #(.group (clj->js %1) (clj->js %2))
   [:add 2]                      #(.add %1 %2)
   [:set-depth 2]                #(.setDepth %1 %2)
   [:create 2]                   #(.create %1 %2)
   [:create 3]                   #(.create %1 %2 %3)
   [:create 4]                   #(.create %1 %2 %3 %4)
   [:create 5]                   #(.create %1 %2 %3 %4 %5)
   [:create 6]                   #(.create %1 %2 %3 %4 %5 %6)
   [:create 7]                   #(.create %1 %2 %3 %4 %5 %6 %7)
   [:set-visible 2]              #(.setVisible %1 %2)})

(def essen-scene-key-collection
  {:load           #(.. % -load)
   :apply          #(%)
   :add            #(.. % -add)
   :lights         #(.. % -lights)
   :physics.world  #(.. % -physics -world)
   :physics.add    #(.. % -physics -add)
   :input.keyboard #(.. % -input -keyboard)
   :cameras.main   #(.. % -cameras -main)})

(doall
 (for [[k _] essen-scene-key-collection]
   (do
     (derive (keyword :es.obj k) :es.obj/key)
     (derive (keyword :es.obj-do k) :es.obj-do/key)
     (derive (keyword :es.obj-fn k) :es.obj-fn/key))))

(defmethod ig/init-key :essen/this [_ opts]
  opts)

;;
;; ::es.obj/key
;;

(def method->method-key
  (juxt first count))

(defn methods->fargs [methods]
  (let [keys (map method->method-key methods)
        fns (map (partial get (merge method-collection @custom-methods)) keys)
        args (map rest methods)]
    (map vector fns args)))

(defn this->obj [this k]
  ((get essen-scene-key-collection (keyword (name k))) this))

(defn apply-method [obj-acc [method args]]
  (apply method (cons obj-acc args)))

(defn apply-fargs [obj fargs]
  ;; TODO Add proper error message if method does not exist
  ;; TODO Check if we can provide a proper error message if method exists, but
  ;; errors
  (reduce apply-method obj fargs))


(defn apply-method-do [obj-acc [method args]]
  (apply method (cons obj-acc args))
  obj-acc)

(defn apply-fargs-do [obj fargs]
  ;; TODO Add proper error message if method does not exist
  ;; TODO Check if we can provide a proper error message if method exists, but
  ;; errors
  (reduce apply-method-do obj fargs))

(defmethod ig/prep-key :es.obj/key [[_ k] opts]
  {:essen/methods opts
   :essen/this (ig/ref :essen/this)})

(defmethod ig/prep-key :es.obj-do/key [[_ k] opts]
  {:essen/methods opts
   :essen/this (ig/ref :essen/this)})

(defmethod ig/prep-key :es.obj-fn/key [[_ k] opts]
  {:essen/methods opts})

;; TODO create version that returns a function instead of call?
(defmethod ig/init-key :es.obj/key [[k _] {:essen/keys [this methods] :as opts}]
  (if (keyword-identical? :es.obj/apply k)
    (apply-fargs (first methods)
                 (methods->fargs (rest methods)))
    (apply-fargs (this->obj this k)
                 (methods->fargs methods))))

(defmethod ig/init-key :es.obj-do/key [[k _] {:essen/keys [this methods] :as opts}]
  (if (keyword-identical? :es.obj-do/apply k)
    (let [obj (first methods)]
      (apply-fargs-do obj (methods->fargs (rest methods)))
      obj)
    (let [obj (this->obj this k)]
      (apply-fargs-do obj (methods->fargs methods))
      obj)))

(defmethod ig/init-key :es.obj-fn/key [[k _] {:essen/keys [methods] :as opts}]
  (if (keyword-identical? :es.obj-fn/apply k)
    (let [fargs (methods->fargs (rest methods))]
      (fn [_this]
        (apply-fargs (first methods) fargs)))
    (let [this->obj (get essen-scene-key-collection (keyword (name k)))
          fargs (methods->fargs methods)]
      (fn [this]
        (apply-fargs (this->obj this) fargs)))))

;;
;; :essen.scene/state
;;

(defmethod ig/init-key :essen.scene.state/atom [_ opts]
  opts)

(defmethod ig/prep-key :essen.scene/state [[_ k] opts]
  {:data opts
   :state (ig/ref :essen.scene.state/atom)})

(defmethod ig/init-key :essen.scene/state [[_ k] {:keys [data state]}]
  (swap! state assoc k data))

;;
;; Scene
;;

(defmethod ig/init-key :essen.scene/config [_ opts]
  opts)

(defmethod ig/init-key :essen.scene.update/list [_ opts]
  opts)


(defmethod ig/prep-key :essen.scene/create [_ opts]
  (assoc opts :essen.scene/create (ig/refset :essen.scene/key)))

(defmethod ig/init-key :essen.scene/create [_ opts]
  opts)

(defmethod ig/prep-key :essen/scene [[_ k] opts]
  (-> opts
      (update :essen.scene/preload (fn [preload] (fn [] preload)))
      (update :essen.scene/create (fn [create] (fn [] create)))
      (update :essen.scene/update (fn [update] (fn [] update)))))

(defn scene-preload [opts]
  #(this-as this
     (-> ((:essen.scene/preload opts))
         (assoc :essen/this this)
         (ig/prep)
         (ig/init))))

(defn scene-create [opts k]
  #(this-as this
     (-> ((:essen.scene/create opts))
         (assoc :essen/this this)
         (assoc :essen.scene.state/atom (scene-state k))
         (ig/prep)
         (ig/init))))

(defn scene-updaters [opts]
  (-> ((:essen.scene/update opts))
      (ig/prep)
      (ig/init)
      (:essen.scene.update/list)))

(defn scene-update [opts k]
  (let [updaters (scene-updaters opts)
        state (scene-state k)]
    (fn [delta]
      (this-as this
        (reset! state (reduce #(%2 %1 delta this) @state updaters))))))

(defmethod ig/init-key :essen/scene [[_ k] opts]
  (swap! scene-states assoc k (atom {}))
  (-> (merge {:key (name k)}
             (:essen.scene/config opts))
      (cond->
          ((:essen.scene/preload opts))
          (assoc :preload (scene-preload opts)))
      (cond->
          ((:essen.scene/create opts))
          (assoc :create (scene-create opts k)))
      (cond->
          ((:essen.scene/update opts))
          (assoc :update (scene-update opts k)))))

;; TODO reset scene?
;; Preserve state of the atom
(defmethod ig/suspend-key! :essen/scene [_ opts])

(defmethod ig/resume-key :essen/scene [k opts old-opts old-impl]
  (ig/init-key k opts))
