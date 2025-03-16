(ns schmoho.dasudopit.client.panels.results.msa.plots)

(def spec
  {:description
  "reference: https://dash.plotly.com/dash-bio/alignmentchart",
  :zoomLimits [1 396],
  :xDomain {:interval [350 396]},
  :assembly "unknown",
  :style {:outline "lightgray"},
  :views
  [{:linkingId "-",
    :spacing 30,
    :tracks
    [{:y {:field "gap", :type "quantitative", :axis "right"},
      :stroke {:value "white"},
      :color {:value "gray"},
      :mark "bar",
      :width 800,
      :title "Gap",
      :strokeWidth {:value 0},
      :xe {:field "end", :type "genomic", :axis "none"},
      :x {:field "start", :type "genomic", :axis "none"},
      :height 100,
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.gap.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}
     {:y {:field "conservation", :type "quantitative", :axis "right"},
      :stroke {:value "white"},
      :color {:field "conservation", :type "quantitative"},
      :mark "bar",
      :width 800,
      :title "Conservation",
      :strokeWidth {:value 0},
      :xe {:field "end", :type "genomic", :axis "none"},
      :x {:field "start", :type "genomic", :axis "none"},
      :height 150,
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.conservation.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}
     {:stroke {:value "white"},
      :color
      {:field "base",
       :type "nominal",
       :range
       ["#d60000"
        "#018700"
        "#b500ff"
        "#05acc6"
        "#97ff00"
        "#ffa52f"
        "#ff8ec8"
        "#79525e"
        "#00fdcf"
        "#afa5ff"
        "#93ac83"
        "#9a6900"
        "#366962"
        "#d3008c"
        "#fdf490"
        "#c86e66"
        "#9ee2ff"
        "#00c846"
        "#a877ac"
        "#b8ba01"],
       :legend true},
      :alignment "overlay",
      :tracks
      [{:mark "rect"}
       {:mark "text",
        :x {:field "start", :type "genomic"},
        :xe {:field "end", :type "genomic"},
        :color {:value "black"},
        :size {:value 12},
        :visibility
        [{:measure "zoomLevel",
          :target "track",
          :threshold 10,
          :operation "LT",
          :transitionPadding 100}]}],
      :width 800,
      :strokeWidth {:value 0},
      :x {:field "pos", :type "genomic", :axis "bottom"},
      :height 500,
      :row {:field "name", :type "nominal", :legend true},
      :text {:field "base", :type "nominal"},
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.fasta.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}]}
   {:static true,
    :xDomain {:interval [0 396]},
    :alignment "overlay",
    :tracks
    [{:stroke {:value "white"},
      :color
      {:field "base",
       :type "nominal",
       :range
       ["#d60000"
        "#018700"
        "#b500ff"
        "#05acc6"
        "#97ff00"
        "#ffa52f"
        "#ff8ec8"
        "#79525e"
        "#00fdcf"
        "#afa5ff"
        "#93ac83"
        "#9a6900"
        "#366962"
        "#d3008c"
        "#fdf490"
        "#c86e66"
        "#9ee2ff"
        "#00c846"
        "#a877ac"
        "#b8ba01"],
       :legend false},
      :mark "rect",
      :width 800,
      :strokeWidth {:value 0},
      :x {:field "pos", :type "genomic", :axis "none"},
      :height 150,
      :row {:field "name", :type "nominal", :legend false},
      :text {:field "base", :type "nominal"},
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.fasta.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}
     {:y {:field "conservation", :type "quantitative", :axis "none"},
      :stroke {:value "white"},
      :color {:field "conservation", :type "quantitative"},
      :mark "bar",
      :width 800,
      :strokeWidth {:value 0},
      :xe {:field "end", :type "genomic", :axis "none"},
      :x {:field "start", :type "genomic", :axis "none"},
      :height 150,
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.conservation.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}
     {:y {:field "gap", :type "quantitative", :axis "none"},
      :stroke {:value "white"},
      :color {:value "gray"},
      :mark "bar",
      :width 800,
      :strokeWidth {:value 0},
      :xe {:field "end", :type "genomic", :axis "none"},
      :x {:field "start", :type "genomic", :axis "none"},
      :height 150,
      :data
      {:url
       "https://raw.githubusercontent.com/sehilyi/gemini-datasets/master/data/alignment_viewer_p53.gap.csv",
       :type "csv",
       :genomicFields ["pos"],
       :sampleLength 99999}}
     {:mark "brush",
      :x {:linkingId "-"},
      :color {:value "black"},
      :stroke {:value "black"},
      :strokeWidth {:value 1},
      :opacity {:value 0.3}}],
    :width 800,
    :height 150}]})
