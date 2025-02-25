(ns pipeline.structure
  (:require
   [biodb.afdb :as afdb]
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot.core]
   [biotools.clustalo :as clustalo]
   [camel-snake-kebab.core :as csk]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [graph.accrete.core]
   [graph.cypher :as cypher]
   [graph.mapping.uniprot.blast :as mapping.uniprot.blast]))




;; protein-id + ligand
;; ;; -> besorge AFDB
;; ;; ;; -> pLDDT
;; ;; ;; -> preprocess
;; ;; -> besorge PubChem
;; ;; ;; -> erstelle 3D conformer

(def pa-mrcb (api.uniprot/uniprotkb-entry "A0A0H2ZHP9"))

(def cif
  (->> (:uniProtKBCrossReferences pa-mrcb)
      (filter (comp #(= "AlphaFoldDB" %) :database))
      (map (comp afdb/get-structure-files :id))
      first
      :cif
      first))

#_(formats.cif/extract-after-block
 (str/split-lines cif)
 formats.cif/plddt-block)
