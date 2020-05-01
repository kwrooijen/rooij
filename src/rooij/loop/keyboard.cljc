(ns rooij.loop.keyboard)

(defn process [{:context/keys [scene] :as context} {:input-event/keys [key type]}]
  (when-let [keyboard-key (get-in scene [:scene/keyboard [type key]])]
    ((:keyboard/fn keyboard-key) context)))
