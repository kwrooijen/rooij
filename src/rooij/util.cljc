(ns rooij.util
  (:require
   [clojure.walk :refer [postwalk]]
   [integrant-tools.core :as it]))

(defn top-key [k]
  (if ^boolean (coll? k) (last k) k))

(defn bottom-key [k]
  (if ^boolean (coll? k) (first k) k))

(defn derive-composite-all
  "Recursively apply `it/derive-composite` on all map keys."
  [m]
  (let [f (fn [[k v]]
            (when (coll? k)
              (try (it/derive-composite k)
                   (catch #?(:clj Throwable :cljs :default) _
                     ;; This means this key already derives from the parent
                     nil)))
            [k v])]
    (doall
     (doall
      (postwalk (fn [x] (if ^boolean (map? x) (into {} (map f x)) x)) m)))))
