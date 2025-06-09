(ns schmoho.biotools.obabel
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schmoho.utils.file :as utils]))

#_(utils/is-command-available? "obabel")

(defn obabel-version [] (:out (sh/sh "obabel" "-V")))

(def obabel-3d-conformer-args ["-opdbqt" "--gen3D" "--best"])

(defmulti produce-obabel-3d-conformer! (fn [sdf] (type sdf)))

(defmethod produce-obabel-3d-conformer! java.lang.String
  ([sdf]
   (let [input-sdf-file (utils/create-temp-file "sdf")]
     (spit input-sdf-file sdf)
     (let [result-pdbqt-file (produce-obabel-3d-conformer! input-sdf-file)]
       (slurp result-pdbqt-file)))))

(defmethod produce-obabel-3d-conformer! java.io.File
  [input-sdf-file]
  (let [output-pdbqt-file (utils/create-temp-file "pdbqt")
        {:keys [exit _out err]
         :as   return}
        (apply (partial sh/sh "obabel" (.getAbsolutePath input-sdf-file) "-O" (.getAbsolutePath output-pdbqt-file))
               obabel-3d-conformer-args)]
    (if (or (not= 0 exit)
            (and (not-empty err)
                 (str/includes? err "Open Babel Error")))
      (throw (ex-info (str "Error when using OBabel: " err)
                      {:input-sdf-file input-sdf-file
                       :return         return
                       :args           obabel-3d-conformer-args}))
      output-pdbqt-file)))

(defmulti hydrogenate! type)

(defmethod hydrogenate! java.util.List
  [pdb]
  (->> (str/join "\n" pdb)
       hydrogenate!
       str/split-lines))

(defmethod hydrogenate! java.lang.String
  [pdb]
  (let [input-file (utils/create-temp-file "pdb")]
    (spit input-file pdb)
    (slurp (hydrogenate! input-file))))

(defmethod hydrogenate! java.io.File
  [pdb]
  (log/info "obabel hydrogens" pdb)
  (let [output-file (utils/create-temp-file "pdb")
        {:keys [exit _out err]
         :as   return}
        (sh/sh "obabel" (.getAbsolutePath pdb) "-O" (.getAbsolutePath output-file) "-h")]
    (if (or (not= 0 exit)
            (and (not-empty err)
                 (str/includes? err "Open Babel Error")))
      (throw (ex-info (str "Error when using OBabel: " err)
                      {:path-to-input-pdb  (.getAbsolutePath pdb)
                       :return             return}))
      output-file)))

#_(hydrogenate!
   "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9.pdb"
   "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9-hydro.pdb")


(defmulti charge! type)

(defmethod charge! java.util.List
  [pdb]
  (->> (str/join "\n" pdb)
       charge!
       str/split-lines))

(defmethod charge! java.lang.String
  [pdb]
  (let [input-file (utils/create-temp-file "pdb")]
    (spit input-file pdb)
    (slurp (charge! input-file))))

(defmethod charge! java.io.File
  [pdb]
  (log/info "obabel hydrogens" pdb)
  (let [output-file (utils/create-temp-file "pdb")
        {:keys [exit _out err]
         :as   return}
        (sh/sh "obabel" (.getAbsolutePath pdb) "-O" (.getAbsolutePath output-file) "--partialcharge" "gasteiger")]
    (if (or (not= 0 exit)
            (and (not-empty err)
                 (str/includes? err "Open Babel Error")))
      (throw (ex-info (str "Error when using OBabel: " err)
                      {:path-to-input-pdb  (.getAbsolutePath pdb)
                       :return             return}))
      output-file)))

