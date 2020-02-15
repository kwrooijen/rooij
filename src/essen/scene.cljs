(ns essen.scene
  (:require
   [integrant.core :as ig]
   [re-frame.core :as re-frame]
   [essen.state :refer [scene-queues]]
   [essen.events.scene]))

(defn normalize-key [k]
  (if (vector? k) (ig/composite-keyword k) k))

(defmulti rule-prep
  (fn [key _ _]
    (normalize-key key)))

(defmethod ig/init-key :ir/force-resolve [_ opts]
  opts)

(defmethod rule-prep :ir/rule [[_ k] opts state]
  (swap! state assoc-in [:subs k] (:subs opts))
  opts)

(defmethod rule-prep :default [_ opts state]
  opts)

(defmulti rule-init
  (fn [key _ _] (normalize-key key)))

(defmethod rule-init :ir/rule [[_ k] opts state]
  (doseq [sub (get-in @state [:subs k])]
    (swap! state update-in [:rule sub] conj opts))
  opts)

(defmethod rule-init :ir/state [[_ k] opts state]
  opts)

(defmethod rule-init :default [_ opts state]
  opts)

(defn setup-rule
  ([system state]
   (setup-rule system state (keys system)))
  ([system state keys]
   (ig/build system keys (fn [key opts] (rule-init key opts state)))))

(defn new-state! [k opts state]
  (if-let [entity-state (k @state)]
    entity-state
    (let [entity-state (atom opts)]
      (swap! state assoc k entity-state)
      (add-watch entity-state k
                 (fn [_key _atom _old-state _new-state]
                   (doseq [f (get-in @state [:rule k])] (f))))
      entity-state)))

(defn setup
  ([config]
   (setup config (atom {})))
  ([config state]
   (doseq [[k v] config]
     (rule-prep k v state))
   ;; Shouldn't be a problem in CLJS, since we don't have multiple
   ;; threads. In Clojure this could be a problem.
   (defmethod ig/resolve-key :ir/state [[_ k] opts]
     (new-state! k opts state))
   {:state state
    :system (-> config
                (ig/prep)
                (ig/init)
                (setup-rule state))}))

(defmethod ig/init-key :essen/delta [_ opts] opts)
(defmethod ig/init-key :essen/time [_ opts] opts)

(defonce scene-states (atom {}))

(defn scene-state [k]
  (get @scene-states k))

;;
;; Scene
;;

(defmethod ig/prep-key :essen.scene/run [_ opts]
  (merge opts
         {:essen/this (ig/ref :essen/this)
          :essen/init (ig/ref :essen/init)}))

(defmethod ig/init-key :essen/init [_ opts]
  opts)

(defmethod ig/init-key :essen.scene/config [_ opts]
  opts)

(defmethod ig/init-key :essen.scene.update/list [_ opts]
  opts)

(defmethod ig/init-key :essen.scene.update/emitters [_ opts]
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

(defn scene-init [k]
  (fn [data]
    (re-frame/dispatch [:essen.events.scene/set-active-scenes])
    (swap! (scene-state k) assoc :essen/init data)))

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
         (assoc [:ir/state :essen/delta] 0)
         (assoc [:ir/state :essen/time] 0)
         (assoc :ir/force-resolve
                {:time (ig/ref :essen/time)
                 :delta (ig/ref :essen/delta)})
         (assoc :essen/init (:essen/init @(scene-state k)))
         (setup (scene-state k)))))

(defn init-update-scene [opts]
  (-> ((:essen.scene/update opts))
      (ig/prep)
      (ig/init)))

(defn apply-emitters [scene-state emitters this time delta]
  (->> emitters
       (mapv #(%1 scene-state this time delta))
       (flatten)))

(defn apply-updaters [scene-state updaters this time delta]
  (reduce #(%2 %1 this time delta) scene-state updaters))

(defn empty-queue [k]
  (swap! scene-queues assoc k []))

(defn scene-queue
  [scene-key]
  (let [current-queues (get @scene-queues scene-key)]
    (swap! scene-queues assoc scene-key [])
    current-queues))

(defn scene-update [opts k]
  (let [state (scene-state k)]
    (fn [time delta]
      (this-as this
        (reset! (:essen/time @state) time)
        (reset! (:essen/delta @state) delta)))))

(def initial-state
  (atom {:essen/queue []}))

(defmethod ig/init-key :essen/scene [[_ k] opts]
  (swap! scene-states assoc k initial-state)
  (-> (merge {:key (name k)}
             (:essen.scene/config opts))
      (assoc :init (scene-init k))
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

;;
;; Impl
;;

(defmethod ig/init-key :game/player [_ opts] opts)

(defmethod ig/init-key :rule/damage
  [_ {:state/keys [player ]}]
  (let [last-hp (atom (:hp @player))]
    #(when (> @last-hp (:hp @player))
       (-> (:sprite @player)
           (.play "adventurer/attack")
           (.. -anims (chain "adventurer/idle")))
       (println "TOOK DAMAGE"))))

(defmethod ig/init-key :rule/poisoned
  [_ {:state/keys [player time this]}]
  (let [last-time (atom nil)
        delay 1000]
    #(cond
       (nil? @last-time)
       (reset! last-time @time)

       (> (- @time @last-time) delay)
       (do (swap! player update :hp dec)
         (reset! last-time (- (* 2 @time) delay @last-time))))))

(comment
  (swap! (:game/player @(scene-state :scene/battle)) update :hp dec)
   @(scene-state :scene/battle) )
