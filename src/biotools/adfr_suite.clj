(ns biotools.adfr-suite
  (:require
   [utils :as utils]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

;; Frankly, the ADFRSuite is an intransparent mess.
;; There is no actual documentation available on it anywhere,
;; and it is not possible to query the program for its version.
;; This program uses it for the assignment of Gasteiger Charges
;; (which it does implicitly) and the production
;; of a PDBQT file.
;; It would be best to replace this with a software that
;; has documentation and transparent usage. ¯\_(ツ)_/¯
;; https://ccsb.scripps.edu/adfr/
;; https://autodock-vina.readthedocs.io/en/latest/docking_requirements.html#adfr-software-suite

(utils/is-command-available? "prepare_receptor")

(defmulti produce-pdbqt! type)

(defmethod produce-pdbqt! java.util.List
  [pdb-lines]
  (->> (str/join "\n" pdb-lines)
       (produce-pdbqt!)
       (str/split-lines)))

(defmethod produce-pdbqt! java.lang.String
  [pdb]
  (let [input-file (utils/create-temp-file "pdb")]
    (spit input-file pdb)
    (slurp (produce-pdbqt! input-file))))

(defmethod produce-pdbqt! java.io.File
  [input-pdb]
  (let [output-pdbqt (utils/create-temp-file "pdbqt")]
    (sh/sh "prepare_receptor"
           "-r" (.getAbsolutePath input-pdb)
           "-o" (.getAbsolutePath output-pdbqt))
    output-pdbqt))
