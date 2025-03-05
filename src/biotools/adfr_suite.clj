(ns biotools.adfr-suite
    (:require
   [biotools.utils :refer :all]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

;; Frankly, the ADFRSuite is an intransparent mess.
;; There is no actual documentation available on it anywhere.
;; This program uses it for the assignment of Gasteiger Charges
;; (which it does implicitly) and the production
;; of a PDBQT file.
;; It would be best to replace this with a software that
;; has documentation and transparent usage. ¯\_(ツ)_/¯
;; https://ccsb.scripps.edu/adfr/
;; https://autodock-vina.readthedocs.io/en/latest/docking_requirements.html#adfr-software-suite

(is-command-available? "prepare_receptor")

(defn produce-pbdqt!
  [path-to-input-pdb path-to-output-pdbqt]
  (sh/sh "prepare_receptor"
         "-r" path-to-input-pdb
         "-o" path-to-output-pdbqt))

