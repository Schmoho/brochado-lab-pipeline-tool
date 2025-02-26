(ns src.unknown-client.views.msa)




;; (defn msa-viewer-scrollable
;;   [alignment row-height container-height]
;;   (let [scroll-offset (r/atom 0)
;;         total-rows    (count alignment)
;;         visible-count (.ceil js/Math (/ container-height row-height))
;;         buffer        5]
;;     (fn [alignment row-height container-height]
;;       (let [first-row            (max 0 (- (int (/ @scroll-offset row-height)) buffer))
;;             first-row            (if (< first-row 0) 0 first-row)
;;             last-row             (min total-rows (+ first-row visible-count (* 2 buffer)))
;;             top-spacer-height    (* first-row row-height)
;;             bottom-spacer-height (* (- total-rows last-row) row-height)]
;;         [:div {:style     {:height      (str container-height "px")
;;                            :overflow-y  "auto"
;;                            :white-space "nowrap"
;;                            :border      "solid black 1px"}
;;                :on-scroll (fn [e]
;;                             (reset! scroll-offset (.-scrollTop (.-target e))))}
;;          [:div
;;           #_[:div {:style {:height (str top-spacer-height "px")}}]
;;           (for [row (subvec alignment first-row last-row)]
;;             ^{:key (:name row)}
;;             [:div
;;              (into
;;               [:span {:style {:display     "inline-block"
;;                               :width       "100px"
;;                               :font-weight "bold"}}
;;               (:name row)]
;;               (for [[idx c] (map-indexed vector (:sequence row))]
;;                 ^{:key idx}
;;                 [:span {:style {:display     "inline-block"
;;                                 :width       "20px"
;;                                 :text-align  "center"
;;                                 :font-family "monospace"}}
;;                  c]))])
;;           #_[:div {:style {:height (str bottom-spacer-height "px")}}]]]))))

;; (def sample-alignment
;;   (vec
;;    (map (fn [i]
;;           {:name (str "Seq" i) :sequence (apply concat (repeat 100 "ACGT-ACGT"))})
;;         (range 100))))

;; (defn main-panel []
;;   [re-com/v-box
;;    :gap "20px"
;;    :children [[msa-viewer-scrollable sample-alignment 30 300]]])

;; (defn fseq
;;   [s]
;;   [re-com/h-box
;;    :children
;;    (map (fn [se]
;;           [re-com/v-box
;;            :children
;;            (mapv (comp str identity) se)])
;;               (:clustalo/aligned-sequence s))])

;; [re-com/v-box
;;                   :gap "20px"
;;                   :children [[msa-viewer (apply concat (repeat 10 f))]]]

;; (defn msa-viewer
;;   [alignment]
;;   (when (seq alignment)
;;     (let [seq-length (-> alignment
;;                          first
;;                          :clustalo/aligned-sequence
;;                          count)]
;;       [re-com/scroller
;;        :size "100px"
;;        :child
;;        [re-com/v-box
;;         :gap "5px"
;;         :children
;;         (cons
;;          [:div
;;           {:style {:white-space "nowrap"}}
;;           (into
;;            [:span {:style {:display    "inline-block"}} ""]
;;            (for [i (range 1 (inc seq-length))]
;;              ^{:key i}
;;              [:span {:style {:display       "inline-block"
;;                              :width         "20px"
;;                              :text-align    "center"
;;                              :font-family   "monospace"
;;                              :font-weight   "bold"}}
;;               i]))]
;;          (for [{:keys [fasta/header clustalo/aligned-sequence]} alignment]
;;            ^{:key header}
;;            [:div
;;             {:style {:white-space "nowrap"}}
;;             (into
;;              [:span {:style {:display       "inline-block"
;;                              :font-weight   "bold"
;;                              :font-family   "sans-serif"}}
;;                 header]
;;              (for [[idx c] (map-indexed vector aligned-sequence)]
;;                ^{:key idx}
;;                [:span {:style {:display       "inline-block"
;;                                :width         "20px"
;;                                :text-align    "center"
;;                                :font-family   "monospace"}}
;;                 c]))]))]])))

;; ;; Example usage:
;; (def sample-alignment
;;   [ {:fasta/header
;;    ">tr|A0A0U4NUB5|A0A0U4NUB5_PSEAI Penicillin-binding protein 1B OS=Pseudomonas aeruginosa OX=287 PE=3 GN=mrcB SV=1",
;;    :clustalo/aligned-sequence
;;    "MTRPRSPRSRNS-KARPAPGLNKWLGWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKQRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQVEQRFDSGAVYLVQNAMQRVMREGTGRSVYSQLPSSLTLAGKTGTSNDSRDSWFSGFGGDLQAVVWLGRDDNGKTPLTGATGALQVWASFMRKAHPQSLEMPMPENVVMAWVDAQTGQGSAADCPNAVQMPYIRGSEPAQGPGCGSQ--NPAGEVMDWVRGWLN",
;;    :fasta/sequence
;;    "MTRPRSPRSRNSKARPAPGLNKWLGWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKQRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQVEQRFDSGAVYLVQNAMQRVMREGTGRSVYSQLPSSLTLAGKTGTSNDSRDSWFSGFGGDLQAVVWLGRDDNGKTPLTGATGALQVWASFMRKAHPQSLEMPMPENVVMAWVDAQTGQGSAADCPNAVQMPYIRGSEPAQGPGCGSQNPAGEVMDWVRGWLN"}])
