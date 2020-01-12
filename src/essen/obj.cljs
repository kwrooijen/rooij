(ns essen.obj
  (:require
   [integrant.core :as ig]))

(defonce custom-methods (atom {}))

;; TODO Maybe create ig/init-key for these?
;; Then the end user can create methods that way
(def method-collection
  {[:add 2]                      #(.add %1 %2)
   [:create 2]                   #(.create %1 %2)
   [:create 3]                   #(.create %1 %2 %3)
   [:create 4]                   #(.create %1 %2 %3 %4)
   [:create 5]                   #(.create %1 %2 %3 %4 %5)
   [:create 6]                   #(.create %1 %2 %3 %4 %5 %6)
   [:create 7]                   #(.create %1 %2 %3 %4 %5 %6 %7)
   [:create-cursor-keys 1]       #(.createCursorKeys %1)
   [:group 1]                    #(.group (clj->js %1))
   [:group 2]                    #(.group (clj->js %1) (clj->js %2))
   [:image 3]                    #(.image %1 %2 %3)
   [:image 4]                    #(.image %1 %2 %3 %4)
   [:set-active 2]               #(.setActive %1 %2)
   [:set-bounds 5]               #(.setBounds %1 %2 %3 %4 %5)
   [:set-collide-world-bounds 2] #(.setCollideWorldBounds %1 %2)
   [:set-depth 2]                #(.setDepth %1 %2)
   [:set-flip-x 2]               #(.setFlipX %1 %2)
   [:set-flip-y 2]               #(.setFlipY %1 %2)
   [:set-origin 2]               #(.setOrigin %1 %2)
   [:set-visible 2]              #(.setVisible %1 %2)
   [:start-follow 5]             #(.startFollow %1 %2 %3 %4 %5)})

(def essen-scene-key-collection
  {:add            #(.. % -add)
   :apply          #(%)
   :cameras.main   #(.. % -cameras -main)
   :input.keyboard #(.. % -input -keyboard)
   :lights         #(.. % -lights)
   :load           #(.. % -load)
   :physics.add    #(.. % -physics -add)
   :physics.world  #(.. % -physics -world)})

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

(defn process-methods [methods k]
  (if (keyword-identical? :es.obj/apply k)
    (rest methods)
    methods))

(defn methods->fargs [methods k]
  (let [methods (process-methods methods k)
        keys (map method->method-key methods)
        fns (map (partial get (merge method-collection @custom-methods)) keys)
        args (map rest methods)]
    (map vector fns args)))

(defn get-obj [this k methods]
  (if (keyword-identical? :es.obj/apply k)
    (first methods)
    ((get essen-scene-key-collection (keyword (name k))) this)))

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
  (apply-fargs (get-obj this k methods)
               (methods->fargs methods k)))

(defmethod ig/init-key :es.obj-do/key [[k _] {:essen/keys [this methods] :as opts}]
  (let [obj (get-obj this k methods)]
    (apply-fargs-do obj (methods->fargs methods k))
    obj))

(defmethod ig/init-key :es.obj-fn/key [[k _] {:essen/keys [methods] :as opts}]
  (if (keyword-identical? :es.obj-fn/apply k)
    (let [fargs (methods->fargs methods k)]
      (fn [_this]
        (apply-fargs (first methods) fargs)))
    (let [this->obj (get essen-scene-key-collection (keyword (name k)))
          fargs (methods->fargs methods k)]
      (fn [this]
        (apply-fargs (this->obj this) fargs)))))