(ns schmoho.biodb.uniprot.formats
  (:require
   [clojure.string :as str]
   [schmoho.formats.fasta :refer [->fasta]]))

(defmethod ->fasta 
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [protein]
  (let [header
        (str ">"
             ({"UniProtKB unreviewed (TrEMBL)"   "tr|"
               "UniProtKB reviewed (Swiss-Prot)" "sp|"} (:entryType protein))
             (:primaryAccession protein)
             (str/join " "
                       [(:uniProtkbId protein)
                        (-> protein :proteinDescription :recommendedName :fullName :value)
                        (str "OS=" (-> protein :organism :scientificName))
                        (str "OX=" (-> protein :organism :taxonId))
                        (str "PE=" (-> protein :proteinExistence (str/split #":" 2) first))
                        (str "GN=" (-> protein :genes first :geneName :value))
                        (str "SV=" (-> protein :entryAudit :sequenceVersion))]))]
    {:fasta/header header
     :fasta/sequence (-> protein :sequence :value)}))

(defmethod ->fasta 
  {:biodb/source :uniprot
   :uniprot/type :uniparc-entry}
  [protein]
  (let [header (str ">"
                    (str/join
                     " "
                     [(-> protein :uniParcId)
                      (str "status=" (if (some :active (:uniParcCrossReferences protein)) "active" "inactive"))
                      (str/join " "
                                (for [{:keys [database id]} (filter :active (:uniParcCrossReferences protein))]
                                  (str database "=" id)))
                      (str "OS="  (->> (:uniParcCrossReferences protein)
                                       (filter :active)
                                       (map (comp :scientificName :organism))
                                       (filter some?)
                                       first))
                      (str "OX="  (->> (:uniParcCrossReferences protein)
                                       (filter :active)
                                       (map (comp :taxonId :organism))
                                       (filter some?)
                                       first))]))]
    {:fasta/header header
     :fasta/sequence (-> protein :sequence :value)}))
