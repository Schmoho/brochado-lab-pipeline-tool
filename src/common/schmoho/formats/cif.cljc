(ns schmoho.formats.cif
  (:require
   [clojure.string :as str]))

(def plddt-block
  ["loop_"
   "_ma_qa_metric.id"
   "_ma_qa_metric.mode"
   "_ma_qa_metric.name"
   "_ma_qa_metric.software_group_id"
   "_ma_qa_metric.type"
   "1 global pLDDT 1 pLDDT"
   "2 local  pLDDT 1 pLDDT"
   "#"
   "_ma_qa_metric_global.metric_id    1"
   "_ma_qa_metric_global.metric_value 87.87"
   "_ma_qa_metric_global.model_id     1"
   "_ma_qa_metric_global.ordinal_id   1"
   "#"
   "loop_"
   "_ma_qa_metric_local.label_asym_id"
   "_ma_qa_metric_local.label_comp_id"
   "_ma_qa_metric_local.label_seq_id"
   "_ma_qa_metric_local.metric_id"
   "_ma_qa_metric_local.metric_value"
   "_ma_qa_metric_local.model_id"
   "_ma_qa_metric_local.ordinal_id"])

(defn match-block?
  "Returns true if the next lines in the sequence match the entire block."
  [lines block]
  (let [candidate (map str/trim (take (count block) lines))]
    (= candidate block)))

(defn skip-to-block
  "Recursively drops lines until the complete block is found.
   Returns the lazy sequence of lines immediately following the block, or nil if not found."
  [lines block]
  (when (seq lines)
    (if (match-block? lines block)
      (drop (count block) lines)
      (recur (rest lines) block))))

(defn extract-after-block
  "Opens the given file, skips until the block occurs, then collects lines
   until a line starting with '#' is found. Returns the lines as a list."
  [lines block]
  (let [after-block (skip-to-block lines block)]
      (if after-block
        (doall (take-while #(and (not (.startsWith % "#"))
                                 (not (.startsWith % "loop_"))
                                 (not (.startsWith % "_"))) after-block))
        [])))




#_(defn extract-after-block
  "Opens the given file, skips until the block occurs, then collects lines
   until a line starting with '#' is found. Returns the lines as a list."
  [filename]
  (with-open [rdr (io/reader filename)]
    (let [lines (line-seq rdr)
          after-block (skip-to-block lines block)]
      (if after-block
        (doall (take-while #(not (.startsWith % "#")) after-block))
        []))))
