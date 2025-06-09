(ns schmoho.dasudopit.server.handler.pipelines
  (:require
   [clojure.tools.logging :as log]
   [schmoho.dasudopit.pipeline.taxonomy :as pipeline.taxonomy]
   [schmoho.formats.pdb :as pdb]
   [schmoho.utils.file :as utils]
   [schmoho.utils.core :refer [cool-select-keys]]
   [fast-edn.core :as edn]
   [schmoho.dasudopit.db :as db]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [schmoho.biotools.obabel :as obabel])
  (:import [java.util.zip ZipOutputStream ZipEntry]))


(defn start-msa-handler
  [request]
  (tap> request)
  (let [uuid (random-uuid)
        form (-> request :body-params (assoc :pipeline/uuid uuid))]
    (log/info "Run job with UUID" uuid)
    (log/info "Write params.")
    (utils/write!
     (format "data/results/msa/%s/params.edn" (str uuid)))
    (future (try
              (pipeline.taxonomy/pipeline form)
              (catch Throwable t
                (log/error t))))
    {:status 200
     :body   {:job-id uuid}}))

(defn- target-data
  [docking-data]
  (->> (for [t (:selected-taxons docking-data)]
         [t {:preprocessing
             (cool-select-keys docking-data
                               {:cut?         [:cut? t]
                                :plddt-cutoff [:plddt-cutoffs t]
                                :hydrogenate? [:hydrogenate? t]
                                :charges?     [:charges? t]})
             :structure
             (cool-select-keys docking-data
                               {:protein      [:selected-proteins t :id]
                                :structure    [:selected-structures t]
                                :binding-site [:selected-binding-sites t first]})}])
       (into {})))

(defn- pathify-struc
  [structure-data]
  (let [source (-> structure-data :structure :source)
        id     (-> structure-data :structure :id)]
    (str "data/structure/"
         (:protein structure-data)
         "/"
         (if (= :afdb source)
           "afdb"
           (str (name source) "/" id)))))

#_(->> (target-data d)
     vals
     (map (comp
           (fn [f]
               (pdb/filter-tail-regions #(> 70 (:temperature-factor %)) f))
           obabel/charge!
           obabel/hydrogenate!
           io/file
           #(str % "/structure.pdb")
           pathify-struc
           :structure))
     first)

(defmacro ^:private with-entry
  [zip entry-name & body]
  `(let [^ZipOutputStream zip# ~zip]
     (.putNextEntry zip# (ZipEntry. ~entry-name))
     ~@body
     (flush)
     (.closeEntry zip#)))

(defn write-to-zip
  [out-name files]
  (with-open [file (io/output-stream out-name)
              zip  (ZipOutputStream. file)
              wrt  (io/writer zip)]
    (binding [*out* wrt]
      (doseq [{:keys [file-name content]} files]
        (doto zip
          (with-entry file-name
              (println content)))))))

(defn start-docking
  [request]
  (tap> request)
  (let [form (:body-params request)]
    (log/info form)
    {:status 200}))


(spit
 "lol.py"
 (str/split-lines (generate-vina-runner {:receptors [["a" "afile"] ["b" "bfile"]]
                         :ligands [["x" "xfile"] ["y" "yfile"]]
                         :configs ["ca" "cb"]})))


(defn generate-vina-runner
  "Generates a cross-platform Python script that runs AutoDock Vina for each receptor with all ligands using configs.
  Uses standard vina command on Linux/macOS. Uses full path on Windows."
  [{:keys [receptors ligands configs]}]
  (let [lines (atom [])]
    (swap! lines conj "import subprocess")
    (swap! lines conj "import os")
    (swap! lines conj "import platform")
    (swap! lines conj "")
    (swap! lines conj "if platform.system() == \"Windows\":")
    (swap! lines conj "    vina_cmd = '\"C:\\\\Program Files (x86)\\\\The Scripps Research Institute\\\\Vina\\\\vina.exe\"'")
    (swap! lines conj "else:")
    (swap! lines conj "    vina_cmd = 'vina'")
    (swap! lines conj "")
    (doseq [[rname rfile] receptors]
      (let [cfg (get configs rname)]
        (doseq [[lname lfile] ligands]
          (let [out-file (str rname "_" lname "_out.pdbqt")
                log-file (str rname "_" lname "_log.txt")
                cmd (str "f\"{vina_cmd} --receptor \\\"" rfile
                         "\\\" --ligand \\\"" lfile
                         "\\\" --config \\\"" cfg
                         "\\\" --out \\\"" out-file
                         "\\\" --log \\\"" log-file "\\\"\"")]
            (swap! lines conj (str "subprocess.run(" cmd ", shell=True)"))))))
    (clojure.string/join "\n" @lines)))
