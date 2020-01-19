(ns cardo.methods
  (:require
   [essen.core]))

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