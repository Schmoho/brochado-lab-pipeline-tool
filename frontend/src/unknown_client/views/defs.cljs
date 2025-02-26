(ns unknown-client.views.defs)

(def blast-dbs
  [{:id :uniprot-bacteria :label "Uniprot Bacteria"}
   {:id :uniprot-complete-microbial-proteomes :label "Uniprot Complete Microbial Proteomes"}
   {:id :uniprotkb :label "UniprotKB "}
   {:id :uniparc :label "UniParc"}
   {:id :uniref-100 :label "Uniref 100"}
   {:id :uniref-50 :label "Uniref 50"}
   {:id :uniref-90 :label "Uniref 90"}
   {:id :swissprot :label "Swissprot"}
   {:id :trembl :label "TrEMBL"}
   {:id :uniprot-pdb :label "Uniprot PDB"}
   {:id :uniprot-archaea :label "Uniprot Archaea"}
   {:id :uniprot-arthropoda :label "Uniprot Arthropoda"}
   {:id :uniprot-eukaryota :label "Uniprot Eukaryota"}
   {:id :uniprot-fungi :label "Uniprot Fungi"}
   {:id :uniprot-human :label "Uniprot Human"}
   {:id :uniprot-mammals :label "Uniprot Mammals"}
   {:id :uniprot-nematoda :label "Uniprot Nematoda"}         
   {:id :uniprot-rodents :label "Uniprot Rodents"}
   {:id :uniprot-vertebrates :label "Uniprot Vertebrates"}
   {:id :uniprot-viridiplantae :label "Uniprot Viridiplantae"}
   {:id :uniprot-viruses :label "Uniprot Viruses"}])

(def uniref-cluster-types
  [{:id :uniref-100 :label "UniRef 100"}
   {:id :uniref-90 :label "UniRef 90"}
   {:id :uniref-50 :label "UniRef 50"}])

(def insane-taxonomic-levels
  [{:id :no-rank :label "no rank"}
   {:id :phylum :label "phylum"}
   {:id :class :label "class"}
   {:id :order :label "order"}
   {:id :family :label "family"}
   {:id :superkingdom :label "superkingdom"}
   {:id :kingdom :label "kingdom"}
   {:id :clade :label "clade"}
   {:id :subkingdom :label "subkingdom"}
   {:id :subphylum :label "subphylum"}
   {:id :subclass :label "subclass"}
   {:id :suborder :label "suborder"}
   {:id :subfamily :label "subfamily"}
   {:id :tribe :label "tribe"}])

(def all-taxonomic-levels
  (concat [{:id :species :label "species"}
           {:id :strain :label "strain"}]
          insane-taxonomic-levels))
