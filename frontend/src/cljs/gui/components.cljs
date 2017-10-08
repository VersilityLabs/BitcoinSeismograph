(ns gui.components
  (:require [re-frame.core :as re-frame]))

(defn tab
  ([title value target]
   [tab title value target nil])
  ([title value target class]
   (let [active-tab (re-frame/subscribe [target])]
     (fn [title value target]
       [:li {:class (str (when (= @active-tab value) "is-active ")
                         class)}
        [:a {:on-click #(re-frame/dispatch [:config/set target value])} title]]))))

(defn info-with-tooltip
  ([kind]
   (info-with-tooltip kind :top))
  ([kind direction]
   (let [tooltip
         (case kind
           :reddit (str "Top Submission posted on Reddit at the moment, "
                        "red postings indicates a negative attitude of a author, "
                        "green posting indicates a positive attitude of the author "
                        "based on a sentiment analysis.")
           :news   (str "Latest news about Bitcoin from news.bitcoin.com, "
                        "please use the specific link provided for more news details")
           :forums (str "Current Submission with the highest view count from "
                        "Bitcointalk and forum.bitcoin.com, red postings indicates"
                        " a negative attitude of a author, green posting indicates"
                        " a positive attitude of the author based on a sentiment analysis.")
           :graphs (str "Changes in Bitcoin metrics over the last 30 days. Click on"
                        " a label below the graph to hide the corresponding line. To see"
                        " community data for a specific date, click on the corresponding"
                        " data-point on the graph.")
           "")]
     [:span.icon.is-small.tooltip.is-tooltip-multiline
      {:data-tooltip tooltip
       :class        (case direction
                       :top    "is-tooltip-top"
                       :right  "is-tooltip-right"
                       :bottom "is-tooltip-bottom"
                       :left   "is-tooltip-left")}
      [:i.fa.fa-info-circle {:aria-label "info"}]])))

(defn trend-indicator [trend & {:keys [invert?]
                                :or   [invert? false]}]
  (if invert?
    (cond
      (and (< trend 0.1)
           (> trend -0.1)) nil
      (pos? trend)         [:span.icon
                            [:i.fa.fa-caret-up {:style {:color "red"}}]]
      (neg? trend)         [:span.icon
                            [:i.fa.fa-caret-down {:style {:color "green"}}]])
    (cond
      (and (< trend 0.1)
           (> trend -0.1)) nil
      (pos? trend)         [:span.icon
                            [:i.fa.fa-caret-up {:style {:color "green"}}]]
      (neg? trend)         [:span.icon
                            [:i.fa.fa-caret-down {:style {:color "red"}}]])))
(defn level-item
  ([left right]
   [level-item nil left right])
  ([attrs left right]
   ;; FIXME: why is-marginless hardcoded?
   [:div.level.is-marginless
    attrs
    [:div.level-item-left
     left]
    [:div.level-item-right
     right]]))

(defn card-meta [items]
  (into [:div.is-flexed.are-small]
        (map (fn [i] [:div.card-meta-item i]) items)))
