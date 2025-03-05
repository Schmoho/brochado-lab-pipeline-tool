(ns biotools.obabel
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]
   [utils :as utils]))

#_(utils/is-command-available? "obabel")

(defn obabel-version
  []
  (:out (sh/sh "obabel" "-V")))

(def obabel-3d-conformer-args ["-opdbqt" "--gen3D" "--best"])

(defn produce-obabel-3d-conformer!
  [input-sdf-file output-pdbqt-file args]
  (let [input-sdf-file (if (= java.io.File (type input-sdf-file))
                         (.getAbsolutePath input-sdf-file)
                         input-sdf-file)
        output-pdbqt-file (if (= java.io.File (type output-pdbqt-file))
                         (.getAbsolutePath output-pdbqt-file)
                         output-pdbqt-file)
        {:keys [exit out err]
         :as   return}
        (apply (partial sh/sh "obabel" input-sdf-file "-O" output-pdbqt-file)
               args)]
    (if (or (not= 0 exit)
            (and (not-empty err)
                 (str/includes? err "Open Babel Error")))
      (throw (ex-info (str "Error when using OBabel: " err)
                      {:input-sdf-file    input-sdf-file
                       :output-pdbqt-file output-pdbqt-file
                       :return            return}))
      (if (empty? out)
        true
        out))))

(defn hydrogenate!
  ([path-to-pdb] (hydrogenate! path-to-pdb path-to-pdb))
  ([path-to-input-pdb path-to-output-pdb]
   (log/info "obabel hydrogens"
             path-to-input-pdb
             path-to-output-pdb)
   (let [{:keys [exit out err]
          :as   return}
         (sh/sh "obabel" path-to-input-pdb "-O" path-to-output-pdb "-h")]
     (if (or (not= 0 exit)
             (and (not-empty err)
                  (str/includes? err "Open Babel Error")))
       (throw (ex-info (str "Error when using OBabel: " err)
                       {:path-to-input-pdb  path-to-input-pdb
                        :path-to-output-pdb path-to-output-pdb
                        :return             return}))
       (if (empty? out)
         true
         out)))))

#_(hydrogenate!
   "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9.pdb"
   "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9-hydro.pdb")


