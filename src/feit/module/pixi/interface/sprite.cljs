(ns feit.module.pixi.interface.sprite
  (:require
   ["pixi.js" :as PIXI]
   [feit.module.pixi.util.position :as util.position]
   [feit.api :refer [emit!]]
   [feit.interface.general-2d.position :refer [FeitGeneral2DPosition]]
   [feit.interface.graphics-2d.sprite :refer [FeitGraphics2DSprite flip]]
   [feit.module.pixi.state :as state]))

(defn -play [{:keys [sprite] :as this} spritesheet [animation & chain]]
  (set! (.-textures sprite) (state/spritesheet-animation-texture spritesheet animation))
  (set! (.-running-animation sprite) animation)
  (set! (.-loop sprite) false)
  (set! (.-onComplete sprite)
        (fn []
          (when (seq chain)
            (-play this spritesheet chain))))
  (.play sprite))

(defn -play-loop [{:keys [sprite] :as this} spritesheet [animation & chain]]
  (set! (.-textures sprite) (state/spritesheet-animation-texture spritesheet animation))
  (set! (.-running-animation sprite) animation)
  (set! (.-loop sprite) (not (boolean (seq chain))))
  (when (seq chain)
    (set! (.-onComplete sprite) #(-play-loop this spritesheet chain)))
  (.play sprite))

(defn ->vec [v]
  (if (coll? v) v [v]))

(defrecord PixiGraphics2DSprite [^js sprite initial-textures x y flip]
  FeitGraphics2DSprite
  (play [this spritesheet animations]
    (-play this spritesheet (->vec animations))
    this)

  (play-loop [this spritesheet animation opts]
    (when-not (= (.-running-animation sprite) animation)
      (-play-loop this spritesheet (->vec animation)))
    this)

  (stop-loop [this animation]
    (when (= (.-running-animation sprite) animation)
      (set! (.-textures sprite) initial-textures)
      (set! (.-loop sprite) true)
      (set! (.-running-animation sprite) :initial)
      (.play sprite))
    this)

  (flip [this x y]
    (when (or (and x (pos-int? (.. sprite -scale -x)))
              (and (not x) (neg-int? (.. sprite -scale -x))))
      (set! (.. sprite -scale -x) (* -1 (.. sprite -scale -x))))
    (when (or (and y (pos-int? (.. sprite -scale -y)))
              (and (not y) (neg-int? (.. sprite -scale -y))))
      (set! (.. sprite -scale -y) (* -1 (.. sprite -scale -y))))
    (-> this
        (assoc-in [:flip :x] x)
        (assoc-in [:flip :y] y)))

  (halt! [this]
    (.destroy (:body this))))

(extend-protocol FeitGeneral2DPosition
  PixiGraphics2DSprite
  (set-position [{:keys [sprite] :as this} x y angle opts]
    (set! (.. sprite -position -x) (util.position/x-with-anchor sprite x opts))
    (set! (.. sprite -position -y) (util.position/y-with-anchor sprite y opts))
    (set! (.. sprite -rotation) angle)
    (assoc this :x x :y y)))

(defn spritesheet-animated-sprite [{:spritesheet/keys [name animation]}]
  (let [textures (state/spritesheet-animation-texture name animation)
        sprite  (PIXI/AnimatedSprite. textures)]
    (.play sprite)
    (map->PixiGraphics2DSprite
     {:sprite sprite
      :initial-textures textures
      :flip {:x false :y false}})))

(defn spritesheet-static-sprite [{:spritesheet/keys [name texture]}]
  (let [texture (state/spritesheet-static-texture name texture)]
    (map->PixiGraphics2DSprite
     {:sprite (PIXI/AnimatedSprite. #js [texture])
      :initial-textures [texture]
      :flip {:x false :y false}})))

(defn texture-static-sprite [{:texture/keys [name]}]
  (let [texture (state/texture name)]
    (map->PixiGraphics2DSprite
     {:sprite (PIXI/AnimatedSprite. #js [texture])
      :initial-textures [texture]
      :flip {:x false :y false}})))

(defn ->sprite [opts]
  (cond
    (:spritesheet/animation opts) (spritesheet-animated-sprite opts)
    (:spritesheet/texture opts)   (spritesheet-static-sprite opts)
    (:texture/name opts)          (texture-static-sprite opts)))

(defn- add-on-click-events!
  [sprite {:handler/keys [on-click] :as opts}]
  (when on-click
    (let [[handler extra-opts] on-click]
      (set! (.-interactive sprite) true)
      (.on sprite "mousedown"
           (fn [event-data]
             (->> [:context/scene-key
                   :context/entity-key
                   :context/component-key]
                  (select-keys opts)
                  (merge {:event/data event-data} extra-opts)
                  (emit! opts handler)))))))

(defn make [{:context/keys [scene-key]
             :position/keys [x y]
             :sprite/keys [scale fps]
             flip-x :flip/x
             flip-y :flip/y
             :as opts}]
  (let [{:keys [sprite] :as state} (->sprite opts)]
    (add-on-click-events! sprite opts)
    (set! (.-animationSpeed sprite) (/ (or fps 15) 60))
    (set! (.-x sprite) x)
    (set! (.-y sprite) y)
    (set! (.-y sprite) y)
    ;; TODO Add scaling option
    (set! (.. sprite -scale -x) (or scale 1))
    (set! (.. sprite -scale -y) (or scale 1))
    (flip state flip-x flip-y)
    (.set (.-anchor sprite) 0.5)
    (.addChild (state/get-scene scene-key) sprite)
    (assoc state :x x :y y)))
