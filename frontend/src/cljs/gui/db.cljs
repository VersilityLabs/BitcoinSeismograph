(ns gui.db)

(def initial-db
  {:initializing?            false
   :welcome                  true
   :config/currency          :EUR
   :config/active-metric-tab :price
   :config/active-chart-tab  :price
   :config/items-per-page    5
   :dashboard/style          :data-focus})
