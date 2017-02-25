(ns triggerfish.server.midi-control-adapters)

;;midi-val -> real-val 
(defn dial [{:keys [min max]} val]
  (let [rng (- max min)
        ratio (/ rng 127)]
    (+ min (* ratio val))))

(def adapters
  {:dial dial})
