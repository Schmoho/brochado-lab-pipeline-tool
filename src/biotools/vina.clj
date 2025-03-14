(ns biotools.vina
  (:require
   [utils :refer :all]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(is-command-available? "vina")

(defn vina-box-config
  [point box-size]
  (str/join "\n"
            [(str "center_x = " (:x point))
             (str "center_y = " (:y point))
             (str "center_z = " (:z point))
             (str "size_x = " box-size)
             (str "size_y = " box-size)
             (str "size_z = " box-size)]))


(defn vina
  [receptor-path config-path ligand-path out-path]
  (sh/sh "vina"
         "--receptor" receptor-path
         "--ligand" ligand-path
         "--config" config-path
         "--exhaustiveness" "32"
         #_#_"--cpu" "16"
         "--out" out-path))

(defn parsed-vina-output
  [output]
  (let [table-start  (str/index-of output "mode |")
        table-end    (str/index-of output "Writing output")
        table-block  (subs output table-start table-end)
        table-lines  (->> (str/split table-block #"\n")
                          (drop 3))
        parsed-lines (map (fn [line]
                            (let [[mode affinity rmsd-lb rmsd-ub] 
                                  (str/split (str/trim line) #"\s+")]
                              {:mode     (Integer/parseInt mode)
                               :affinity (Double/parseDouble affinity)
                               :rmsd-lb  (Double/parseDouble rmsd-lb)
                               :rmsd-ub  (Double/parseDouble rmsd-ub)}))
                          (remove str/blank? table-lines))]
    parsed-lines))


;; (defn run-docking!
;;   [receptor config out-path]
;;   (.mkdirs (io/file out-path))
;;   (->> (for [index (range 1 4)]
;;          (let [{:keys [exit out err]}
;;                (vina (.getPath (io/file receptor))
;;                      (.getPath (io/file config))
;;                      "resources/drugs/Cefotaxime - 5742673.3D.pdbqt"
;;                      (format "%s/docked-%s-%d.pdbqt"
;;                              out-path
;;                              (.getName (io/file receptor))
;;                              index))]
;;            (spit (str out-path "/dock-" (.getName (io/file receptor)) "-" index ".log")
;;                  (if (= 0 exit)
;;                    out
;;                    err))
;;            [index
;;             (if (= 0 exit)
;;               (parsed-vina-output out)
;;               err)]))
;;        (apply concat)))

#_(produce-obabel-3d-conformer! "drugs/Cefotaxime - 5742673.sdf" "drugs/Cefotaxime - 5742673.3D.pdbqt")

;; (defn docking-scores
;;   [folder]
;;   (->> (utils/files-with-ending folder "log")
;;        (concat)
;;        (map (comp :affinity
;;                   first
;;                   parsed-vina-output
;;                   slurp))))

#_(spit
 "output/docking-scores.json"
 (json/generate-string
  (->> (for [org [ecoli pseudomonas salmonella]]
         [org
          {:uniprot                   (docking-scores (format "output/docking/%s/uniprot" org))
           :joseph                    (docking-scores (format "output/docking/%s/joseph" org))
           :pbp1b-complexed           (docking-scores (format "output/docking/%s/pbp1b-complexed" org))
           :pbp1b-complexed-pruned    (docking-scores (format "output/docking/%s/pbp1b-complexed-pruned" org))
           :pbp1b-lpox-complex        (docking-scores (format "output/docking/%s/pbp1b-lpox-complex" org))
           :pbp1b-lpox-complex-pruned (docking-scores (format "output/docking/%s/pbp1b-lpox-complex-pruned" org))}])
       (into {}))))


;; (defn dock!
;;   ([protein
;;     ligand-binding-site-index
;;     alignment-reference
;;     output-folder]
;;    (.mkdirs (io/file output-folder))
;;    (let [protein-base-name (-> (io/file protein)
;;                                (.getName)
;;                                (str/replace ".pdb" ""))
;;          aligned-file      (io/file (str (.getAbsolutePath (io/file output-folder))
;;                                          "/"
;;                                          (io/file (str protein-base-name ".aligned.pdb"))))]
;;      (log/info (str "Aligning pdb with output file " aligned-file))
;;      (align/align-3d-structure!
;;       protein
;;       alignment-reference
;;       output-folder
;;       aligned-file)

;;      (dock! aligned-file
;;             ligand-binding-site-index
;;             output-folder)))
;;   ([protein
;;     ligand-binding-site-index
;;     output-folder]
;;    (let [protein-base-name (-> (io/file protein)
;;                                (.getName)
;;                                (str/replace ".pdb" ""))
;;          config-file       (io/file (str (.getAbsolutePath (io/file output-folder))
;;                                          "/"
;;                                          protein-base-name
;;                                          ".config"))]
;;      (log/info (str "Producing config at "
;;                     config-file))
;;      (write-vina-box-config!
;;       25
;;       (pdb/parsed-pdb (slurp  protein))
;;       ligand-binding-site-index
;;       config-file)
     
;;      (let [pdbqt-file (str (.getAbsolutePath (io/file output-folder))
;;                            "/"
;;                            protein-base-name
;;                            ".pdbqt")]
;;        (log/info (str "Producing pdbqt at "
;;                       pdbqt-file))
;;        (produce-pbdqt!
;;         (.getAbsolutePath (io/file protein))
;;         pdbqt-file)

;;        (log/info (str "Running docking with output-folder "
;;                     output-folder))
;;        (run-docking!
;;         pdbqt-file
;;         config-file
;;         output-folder)))))

#_(do
  #_(dock! (io/resource "BW25113/uniprot/pbp1b.hydrogenated.pdb")
       510
       "output/docking/BW25113/uniprot")
  #_(dock! (io/resource "BW25113/joseph/pbp1b.pdb")
         510
         "output/docking/BW25113/joseph")
  #_(dock! (io/resource "BW25113/pbp1b-complexed/pbp1b.hydrogenated.pdb")
         510
         "output/docking/BW25113/pbp1b-complexed")
  #_(dock! (io/resource "BW25113/pbp1b-complexed-pruned/pbp1b.hydrogenated.pdb")
         439
         "output/docking/BW25113/pbp1b-complexed-pruned")
  #_(dock! (io/resource "BW25113/pbp1b-lpox-complex/pbp1b-lpox-complex.hydrogenated.pdb")
         510
         "output/docking/BW25113/pbp1b-lpox-complex")
  #_(dock! (io/resource "BW25113/pbp1b-lpox-complex-pruned/pbp1b-lpox-complex.hydrogenated.pdb")
         439
         "output/docking/BW25113/pbp1b-lpox-complex-pruned"))


#_(do
  #_(dock! (io/resource "PA14/uniprot/pbp1b.hydrogenated.pdb")
         468
         (io/resource "BW25113/uniprot/pbp1b.hydrogenated.pdb")
         "output/docking/PA14/uniprot")
  #_(dock! (io/resource "PA14/joseph/pbp1b.pdb")
         468
         (io/resource "BW25113/joseph/pbp1b.pdb")
         "output/docking/PA14/joseph")
  #_(dock! (io/resource "PA14/pbp1b-complexed/pbp1b.hydrogenated.pdb")
         468
         (io/resource "BW25113/pbp1b-complexed/pbp1b.hydrogenated.pdb")
         "output/docking/PA14/pbp1b-complexed")
  (dock! (io/resource "PA14/pbp1b-complexed-pruned/pbp1b.hydrogenated.pdb")
         449
         (io/resource "BW25113/pbp1b-complexed-pruned/pbp1b.hydrogenated.pdb")
         "output/docking/PA14/pbp1b-complexed-pruned")
  #_(dock! (io/resource "PA14/pbp1b-lpox-complex/pbp1b-lpox-complex.hydrogenated.pdb")
         468
         (io/resource "BW25113/pbp1b-lpox-complex/pbp1b-lpox-complex.hydrogenated.pdb")
         "output/docking/PA14/pbp1b-lpox-complex")
  (dock! (io/resource "PA14/pbp1b-lpox-complex-pruned/pbp1b-lpox-complex.hydrogenated.pdb")
         449
         (io/resource "BW25113/pbp1b-lpox-complex-pruned/pbp1b-lpox-complex.hydrogenated.pdb")
         "output/docking/PA14/pbp1b-lpox-complex-pruned"))

#_(do
  #_(dock! (io/resource "14028s/uniprot/pbp1b.hydrogenated.pdb")
         505
         (io/resource "BW25113/uniprot/pbp1b.hydrogenated.pdb")
         "output/docking/14028s/uniprot")
  #_(dock! (io/resource "14028s/joseph/pbp1b.pdb")
         505
         (io/resource "BW25113/joseph/pbp1b.pdb")
         "output/docking/14028s/joseph")
  #_(dock! (io/resource "14028s/pbp1b-complexed/pbp1b.hydrogenated.pdb")
         505
         (io/resource "BW25113/pbp1b-complexed/pbp1b.hydrogenated.pdb")
         "output/docking/14028s/pbp1b-complexed")
  (dock! (io/resource "14028s/pbp1b-complexed-pruned/pbp1b.hydrogenated.pdb")
         442
         (io/resource "BW25113/pbp1b-complexed-pruned/pbp1b.hydrogenated.pdb")
         "output/docking/14028s/pbp1b-complexed-pruned")
  #_(dock! (io/resource "14028s/pbp1b-lpox-complex/pbp1b-lpox-complex.hydrogenated.pdb")
         505
         (io/resource "BW25113/pbp1b-lpox-complex/pbp1b-lpox-complex.hydrogenated.pdb")
         "output/docking/14028s/pbp1b-lpox-complex")
  (dock! (io/resource "14028s/pbp1b-lpox-complex-pruned/pbp1b-lpox-complex.hydrogenated.pdb")
         442
         (io/resource "BW25113/pbp1b-lpox-complex-pruned/pbp1b-lpox-complex.hydrogenated.pdb")
         "output/docking/14028s/pbp1b-lpox-complex-pruned"))


;; (let [org "BW25113"
;;       site 512]
;;   (doseq [i (range 1 6)
;;           j (range 1 6)]
;;     (do
;;       (hydrogenate!
;;        (.getAbsolutePath (io/file (io/resource (format "%s/0%s/ec_0%s_%s.pdb" org i i j))))
;;        (format "resources/%s/0%s/ec_0%s_%s.hydro.pdb" org i i j))
;;       (dock! (io/resource (format "%s/0%s/ec_0%s_%s.hydro.pdb" org i i j))
;;              site
;;              (format "output/docking/%s/0%s-hydro/%s" org i j)))))


;; (let [org "BW25113"]
;;   (for [i (range 1 6)]
;;     [(format "output/docking/%s/0%s-hydro" org i)
;;      (for [j (range 1 6)]
;;        (docking-scores (format "output/docking/%s/0%s-hydro/%s" org i j)))]))


;; (doseq [mutated-file-basename
;;          (->> (file-seq (io/file (io/resource "BW25113/mutated")))
;;               (filter #(.isFile %))
;;               (map (comp #(str/replace % ".pdb" "")
;;                          #(.getName %))))]
;;    (do
;;      (hydrogenate!
;;       (-> (format "BW25113/mutated/%s.pdb"
;;                   mutated-file-basename)
;;           io/resource
;;           io/file
;;           .getAbsolutePath)
;;       (format "resources/BW25113/mutated/%s.hydro.pdb"
;;               mutated-file-basename))
;;      (dock! (io/resource (format "BW25113/mutated/%s.hydro.pdb"
;;                                  mutated-file-basename))
;;             510
;;             (format "output/docking/BW25113/mutated/%s"
;;                     mutated-file-basename))))

;; (doseq [mutated-file-basename
;;          (->> (file-seq (io/file (io/resource "BW25113/mutated2")))
;;               (filter #(.isFile %))
;;               (map (comp #(str/replace % ".pdb" "")
;;                          #(.getName %))))]
;;    (do
;;      (hydrogenate!
;;       (-> (format "BW25113/mutated2/%s.pdb"
;;                   mutated-file-basename)
;;           io/resource
;;           io/file
;;           .getAbsolutePath)
;;       (format "resources/BW25113/mutated2/%s.hydro.pdb"
;;               mutated-file-basename))
;;      (dock! (io/resource (format "BW25113/mutated2/%s.hydro.pdb"
;;                                  mutated-file-basename))
;;             510
;;             (format "output/docking/BW25113/mutated2/%s"
;;                     mutated-file-basename))))



;; (->> (slurp "output/asdf.blc")
;;      str/split-lines
;;      (drop 3)
;;      (map (fn [beide]
;;             [(str (second beide)) beide]))
;;      (filter (fn [[pa _]]
;;                (not= "-" pa)))
;;      (map-indexed (fn [index alignment]
;;                     [(inc index) alignment]))
;;      (filter (fn [[index alignment]]
;;                (#{468 471 508 509 511 527 528 529 530 566 655 657 659 691 692} index)))
;;      (filter (fn [[index [_ alignment]]]
;;                (not= (first alignment)
;;                      (second alignment)))))

;; #{468 471 508 509 511 527 528 529 530 566 655 657 659 691 692}

;; (doseq [mutated-file-basename
;;          (->> (file-seq (io/file (io/resource "PA14/mutated-ala")))
;;               (filter #(.isFile %))
;;               (map (comp #(str/replace % ".pdb" "")
;;                          #(.getName %))))]
;;    (do
;;      (hydrogenate!
;;       (-> (format "PA14/mutated-ala/%s.pdb"
;;                   mutated-file-basename)
;;           io/resource
;;           io/file
;;           .getAbsolutePath)
;;       (format "resources/PA14/mutated-ala/%s.hydro.pdb"
;;               mutated-file-basename))
;;      (dock! (io/resource (format "PA14/mutated-ala/%s.hydro.pdb"
;;                                  mutated-file-basename))
;;             510
;;             (format "output/docking/PA14/mutated-ala/%s"
;;                     mutated-file-basename))))
