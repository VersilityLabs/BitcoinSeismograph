(ns gui.listings
  (:require [re-frame.core :as re-frame]

            [gui.components :as components]
            [gui.format :refer [->relative-date]]
            [gui.modal :as modal]))

(defn- item-backdrop [sentiment]
  (cond
    (< 0.25 sentiment)  "is-positive"
    (> -0.25 sentiment) "is-negative"
    :else               nil))

(defn- forum-thread [data]
  (let [source (:_source data)
        scrape (peek (:scrapes source))]
    [:div.card.with-hover
     [:div.card-content.is-narrow
      {:class (item-backdrop (:polarity (:title (:sentiment source))))}
      [:p.item-title.with-pointer
       {:on-click #(modal/show {:type :thread :data source})}
       (:title source)]
      [components/card-meta
       [[:a {:href   (:permalink source)
             :target "_blank"}
         [:span.icon.is-small.has-small-margin-right
          [:i.fa.fa-eye {:aria-label "score"}]]
         (:views scrape)]

        [:a {:href   (:permalink source)
             :target "_blank"}
         [:span.icon.is-small.has-small-margin-right
          [:i.fa.fa-comments-o.fa-lg {:aria-label "comment-count"}]]
         (:replies scrape)]

        (clojure.string/replace (:community source) #"_" ".")

        [:time {:dateTime (:created_at source)}
         (->relative-date (:created_at source))]]]]]))

(defn- news [data]
  (let [source (:_source data)]
    [:div.card
     [:div.card-content.is-narrow
      [:p.item-title
       [:a {:href   (:url source)
            :target "_blank"}
        (:title source)]]
      [components/card-meta
       [[:time {:dateTime (:timestamp source)}
         (->relative-date (:timestamp source))]

        (:source source)]]]]))

(defn- submission [data]
  (let [source (assoc (:_source data) :id (:_id data))
        scrape (peek (:scrapes source))]
    [:div.card.with-hover
     [:div.card-content.is-narrow
      {:class (item-backdrop (:polarity (:title (:sentiment source))))}
      [:p.item-title.with-pointer
       {:on-click #(modal/show {:type :submission :data source})}
       (:title source)]
      [components/card-meta
       [[:a {:href   (str "https://reddit.com/r/" (:subreddit source) "/comments/" (:id source) "/")
             :target "_blank"}
         [:span.icon.is-small.has-small-margin-right
          [:i.fa.fa-level-up {:aria-label "score"}]]
         (:score scrape)]

        [:a {:href   (str "https://reddit.com/r/" (:subreddit source) "/comments/" (:id source) "/")
             :target "_blank"}
         [:span.icon.is-small.has-small-margin-right
          [:i.fa.fa-comments-o.fa-lg {:aria-label "comment-count"}]]
         (:comments scrape)]

        (str "/r/" (:subreddit source))

        [:time {:dateTime (:created_at source)}
         (->relative-date (:created_at source))]]]]]))

(defn items [key-prefix items]
  [:div.content
   (for [i items]
     (let [key (str (name key-prefix) "-" (:_id i))]
       (case (:_type i)
         "item"       ^{:key key} [news i]
         "submission" ^{:key key} [submission i]
         "thread"     ^{:key key} [forum-thread i])))])

(defn listing [type category]
  (fn [type category]
    (let [paginated-items @(re-frame/subscribe [:community/paginated-data type])]
      [:div.listing
       [items category (:items paginated-items)]
       [components/level-item
        (when (:has-prev? paginated-items)
          [:a.button.is-primary.is-inverted
           {:on-click #(re-frame/dispatch [:prev-page type])}
           [:span.icon
            [:i.fa.fa-arrow-left {:aria-label "more recent items"}]]])
        (when (:has-next? paginated-items)
          [:a.button.is-primary.is-inverted
           {:on-click #(re-frame/dispatch [:next-page type])}
           [:span.icon
            [:i.fa.fa-arrow-right {:aria-label "older items"}]]])]])))
