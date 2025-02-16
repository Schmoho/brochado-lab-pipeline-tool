(ns graph.load.presets)

(def ncbi-taxes
  ["208963"
   "208964"
   "652611"])

(def databases
  [{:category "Sequence databases"
    :dbUrl    "https://www.uniprot.org/uniprotkb/%s",
    :abbrev   "UniprotKB",
    :name     "UniprotKB"
    :servers  ["https://www.uniprot.org/uniprotkb"],}
   {:category "Taxonomy databases"
    :dbUrl    "https://www.uniprot.org/taxonomy/%s",
    :abbrev   "UniprotTaxonomy",
    :name     "Taxonomy"
    :servers  ["https://www.uniprot.org/uniprotkb"],}
   {:category "Taxonomy databases",
    :dbUrl    "https://www.ncbi.nlm.nih.gov/taxonomy/%s",
    :abbrev   "NCBITaxonomy",
    :name     "Taxonomy",
    :servers  ["https://www.ncbi.nlm.nih.gov/taxonomy"],}])
