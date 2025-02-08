(ns plip
  (:require
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.zip :as zip]
   [clojure.string :as str]))

(defn iter-zip [zipper]
  (->> zipper
       (iterate zip/next)
       (take-while (complement zip/end?))))

(defn loc-tag?
  [loc tag]
  (-> loc zip/node :tag (= tag)))


(defn plip-stats
  [report]
  (let [top-level              (->> (xml/parse (io/reader (io/file report)))
                                    zip/xml-zip
                                    zip/down
                                    iter-zip)
        bs-residues            (zip/children (first (filter #(loc-tag? % :bs_residues) top-level)))
        in-contact-residues    (->> bs-residues
                                    (filter #(= (-> % :attrs :contact) "True"))
                                    (map (fn [bs-residue]
                                           (str/join " "
                                                     [(-> bs-residue :attrs :aa)
                                                      (-> bs-residue :content first (str/replace "A" ""))
                                                      (-> bs-residue :attrs :min_dist)])))
                                    (sort-by #(-> % (str/split #" ") second (Integer/parseInt) )))
        interactions           (zip/children (first (filter #(loc-tag? % :interactions) top-level)))
        interaction-type-count (->> interactions
                                    (map (juxt :tag
                                               (comp count zip/children zip/xml-zip)
                                               (comp (partial map
                                                              (comp
                                                               (partial map (comp first :content))
                                                               (partial filter #(#{:resnr :restype :dist :dist_h-a :dist_d-a}
                                                                                 (-> % :tag)))))
                                                     (partial map :content)
                                                     zip/children
                                                     zip/xml-zip)))
                                    (filter (comp #(< 0 %) second))
                                    (map (fn [[interaction-type n interactions]]
                                           (let [interaction (-> interaction-type name (str/replace "_" " ") (str ":"))
                                                 max-length  25
                                                 padding     (- max-length (count interaction))]
                                             [interaction-type
                                              (map (comp #(str/join " " %)
                                                         #(let [[resnr res & dists] %]
                                                            (concat [res resnr] dists)))
                                                   interactions)]))))]
    [in-contact-residues
     interaction-type-count]))

(plip-stats "output/docking/BW25113/joseph/plip/report.xml")
(plip-stats "output/docking/PA14/joseph/plip/report.xml")

(defn print-stats
  [stats]
  (println)
  (println "Residue + Minimal Distance")
  (doseq [residue (first stats)]
    (println residue))
  (println "\nInteraction Type Counts")
  (doseq [[interaction-type is] (second stats)]
    (println interaction-type)
      (doseq [i is]
        (println i))))

;; (def report "output/docking/BW25113/joseph/plip/report.xml")

;; (print-stats (plip-stats "output/docking/BW25113/uniprot/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/BW25113/joseph/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/BW25113/pbp1b-complexed/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/BW25113/pbp1b-complexed-pruned/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/BW25113/pbp1b-lpox-complex/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/BW25113/pbp1b-lpox-complex-pruned/plip/report.xml"))

;; (print-stats (plip-stats "output/docking/14028s/uniprot/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/14028s/joseph/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/14028s/pbp1b-complexed/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/14028s/pbp1b-complexed-pruned/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/14028s/pbp1b-lpox-complex/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/14028s/pbp1b-lpox-complex-pruned/plip/report.xml"))

;; (print-stats (plip-stats "output/docking/PA14/uniprot/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/PA14/joseph/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/PA14/pbp1b-complexed/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/PA14/pbp1b-complexed-pruned/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/PA14/pbp1b-lpox-complex/plip/report.xml"))
;; (print-stats (plip-stats "output/docking/PA14/pbp1b-lpox-complex-pruned/plip/report.xml"))

