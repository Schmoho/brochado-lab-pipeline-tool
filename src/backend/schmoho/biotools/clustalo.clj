(ns schmoho.biotools.clustalo
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

#_(utils/is-command-available? "clustalo")

(def mapping
  {:sequence-input   {:in         "--infile"
                      :hmm-in     "--hmm-in"
                      :dealign    "--dealign"
                      :profile1   "--profile1"
                      :profile2   "--profile2"
                      :is-profile "--is-profile"
                      :seqtype    ["--seqtype"
                                   {:protein "Protein"
                                    :rna     "RNA"
                                    :dna     "DNA"}]
                      :infmt      ["--infmt"
                                   {:fasta     "fasta",
                                    :clustal   "clustal",
                                    :msf       "msf",
                                    :phylip    "phylip",
                                    :selex     "selex",
                                    :stockholm "stockholm",
                                    :vienna    "vienna"}]}
   :clustering       {:distmat-in     "--distmat-in"
                      :distmat-out    "--distmat-out"
                      :guidetree-in   "--guidetree-in"
                      :guidetree-out  "--guidetree-out"
                      :full           "--full"
                      :full-iter      "--full-iter"
                      :cluster-size   "--cluster-size"
                      :clustering-out "--clustering-out"
                      :use-kimura     "--use-kimura"
                      :percent-id     "--percent-id"}
   :alignment-output {:out           "--outfile"
                      :outfmt        ["--outfmt"
                                      {:fasta     "fasta",
                                       :clustal   "clustal",
                                       :msf       "msf",
                                       :phylip    "phylip",
                                       :selex     "selex",
                                       :stockholm "stockholm",
                                       :vienna    "vienna"}]
                      :residuenumber "--residuenumber"
                      :wrap          "--wrap"
                      :output-order  ["--output-order"
                                      {:input-order "input-order"
                                       :tree-order  "tree-order"}]}
   :iteration        {:iter               "--iterations"
                      :max-guidetree-iter "--max-guidetree-iterations"
                      :max-hmm-iter       "--max-hmm-iterations"}
   :limits           {:maxnumseq "--maxnumseq"
                      :maxseqlen "--maxseqlen"}
   :miscellaneous    {:auto         "--auto"
                      :threads      "--threads"
                      :log          "--log"
                      :help         "--help"
                      :verbose      "--verbose"
                      :version      "--version"
                      :long-version "--long-version"
                      :force        "--force"}})

(defn accessors
  [m]
  (mapcat (fn [[k v]]
           (map (juxt (constantly k) identity)
                (keys v)))
          m))

(defn params->cmd-args
  [params]
  (->> (for [a (accessors params)]
         (let [v (get-in params a)
               f (get-in mapping a)]
           (cond 
             (string? v)  (format "%s=%s" f v)
             (keyword? v) [(first f) ((second f) v)]
             (boolean? v) f)))
       (flatten)))


(defn clustalo
  ([fasta-elements]
   (clustalo {:sequence-input {:in "-"}} fasta-elements))
  ([params fasta-elements]
   (log/info "Running ClustalO of" (count fasta-elements) "sequences")
   (let [in-string              (str/join "\n"
                                          (map #(str (:fasta/header %) "\n" (:fasta/sequence %))
                                               fasta-elements))
         {:keys [exit out err]} (apply sh/sh
                                       (concat
                                        ["clustalo"]
                                        (params->cmd-args params)
                                        [:in in-string]))]
     (if (= 0 exit)
       (->> (str/split-lines out)
            (partition-by #(str/starts-with? % ">"))
            (partition-all 2)
            (mapv (fn [[header sequence]]
                    {:fasta/header              (first header)
                     :clustalo/aligned-sequence (apply str sequence)
                     :fasta/sequence            (str/replace (apply str sequence)
                                                             "-" "")})))
       (throw (ex-info "ClustalO error"
                       {:err  err
                        :exit exit
                        :out  out}))))))


#_(clustalo
 [{:fasta/header
   ">UPI00053A1130 status=active RefSeq=WP_033885940 OS=Pseudomonas aeruginosa OX=287",
   :fasta/sequence
   "SQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKLRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFL"}
  {:fasta/header
   ">UPI001ED997A5 status=active RefSeq=WP_237755404 OS=Pseudomonas aeruginosa OX=287",
   :fasta/sequence
   "MTRPRSPRSRNSKARPAPGLNKWLSWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKLRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQV"}])
