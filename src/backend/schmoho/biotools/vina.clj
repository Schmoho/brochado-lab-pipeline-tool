(ns schmoho.biotools.vina
  (:require
   [clojure.string :as str]
   [schmoho.utils.file :as utils]))

(utils/is-command-available? "vina")

(defn vina-box-config
  [point box-size]
  (str/join "\n"
            [(str "center_x = " (:x point))
             (str "center_y = " (:y point))
             (str "center_z = " (:z point))
             (str "size_x = " box-size)
             (str "size_y = " box-size)
             (str "size_z = " box-size)]))
