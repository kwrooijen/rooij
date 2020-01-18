(ns cardo.core
  (:require
   [essen.core]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [integrant.core :as ig]
   [cardo.views :as views]
   [cardo.db :as db]

   [cardo.config :as config]
   [cardo.config.battle :as config.battle]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
    (.getElementById js/document "interface")))

(defn ^:export init []
  (re-frame/dispatch-sync [::initialize-db])
  (dev-setup)
  (mount-root)
  (essen.core/init (merge config/config
                          config.battle/config)))

(defn stop []
  (essen.core/suspend!))

(defn start []
  (essen.core/resume (merge config/config
                            config.battle/config)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-bg [obj x y flip-x flip-y]
  (.. obj
      (image x y "bg")
      (setOrigin 0)
      (setFlipX flip-x)
      (setFlipY flip-y)))

(defn create-anim [anims k prefix end repeat framerate]
  (let [frames (.generateFrameNames anims "atlas" #js {:prefix prefix
                                                       :end end
                                                       :zeroPad 2})]
    (.create anims #js {:key k
                        :frames frames
                        :frameRate framerate
                        :repeat repeat})))

(essen.core/custom-methods!
 {[:set-bg 5] set-bg
  [:create-anim 6] create-anim})

(defmethod ig/init-key :my/updater [_ opts]
  (fn [{:game/keys [cursor adventurer] :as state} delta this]
    (when (.. cursor -space -isDown)
      (re-frame/dispatch [:set-component/attack 1])
      (set! (.-delay (:attack/timer state)) 600))
    state))

(defmethod ig/init-key :adventurer/timer [_ {:keys [adventurer]}]
  (fn []
    (.play adventurer  "adventurer/attack")
    (.. adventurer -anims (chain "adventurer/idle"))))
