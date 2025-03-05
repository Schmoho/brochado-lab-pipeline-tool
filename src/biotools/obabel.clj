(ns biotools.obabel
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]
   [utils :as utils]))

#_(utils/is-command-available? "obabel")

(defn obabel-version [] (:out (sh/sh "obabel" "-V")))

(def obabel-3d-conformer-args ["-opdbqt" "--gen3D" "--best"])

(defmulti produce-obabel-3d-conformer! (fn [sdf args] (type sdf)))

(defmethod produce-obabel-3d-conformer! java.lang.String
  [sdf args]
  (let [input-sdf-file (utils/create-temp-file sdf)]
    (slurp (produce-obabel-3d-conformer! input-sdf-file args))))

(defmethod produce-obabel-3d-conformer! java.io.File
  [input-sdf-file args]
  (let [output-pdbqt-file (utils/create-temp-file "pdbqt")
        {:keys [exit out err]
         :as   return}
        (apply (partial sh/sh "obabel" (.getAbsolutePath input-sdf-file) "-O" (.getAbsolutePath output-pdbqt-file))
               args)]
    (if (or (not= 0 exit)
            (and (not-empty err)
                 (str/includes? err "Open Babel Error")))
      (throw (ex-info (str "Error when using OBabel: " err)
                      {:input-sdf-file input-sdf-file
                       :return         return
                       :args           args}))
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
        {:keys [exit out err]
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


