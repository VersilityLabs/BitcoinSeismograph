(ns gui.modal
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [gui.faq :as faq]
            [gui.format :refer [->number-with-fraction
                                ->relative-date]]))

(defn show [payload]
  (-> js/document
      (.querySelector "html")
      (.-classList)
      (.add "is-clipped"))
  (re-frame/dispatch [:config/open-modal payload]))

(defn close []
  (-> js/document
      (.querySelector "html")
      (.-classList)
      (.remove "is-clipped"))
  (re-frame/dispatch [:config/close-modal]))

(defn- meta-data [type data]
  (let [scrape (peek (:scrapes data))]
    (case type
      :thread     [:div.level.are-small
                   [:div.level-item
                    [:span.icon.is-small
                     [:i.fa.fa-eye {:aria-label "score"}]]
                    [:a {:href   (:permalink data)
                         :target "_blank"}
                     (:views scrape)]]
                   [:div.level-item
                    [:span.icon.is-small
                     [:i.fa.fa-comments-o.fa-lg {:aria-label "comment-count"}]]
                    [:a {:href   (:permalink data)
                         :target "_blank"}
                     (:replies scrape)]]
                   [:div.level-item
                    (str (clojure.string/replace (:community data) #"_" ".")
                         " - "
                         (:board data))]
                   [:div.level-item
                    [:time {:dateTime (:created_at data)}
                     (->relative-date (:created_at data))]]]
      :submission [:div.level.are-small
                   [:div.level-item
                    [:span.icon.is-small
                     [:i.fa.fa-level-up {:aria-label "score"}]]
                    [:a {:href   (str "https://reddit.com/r/" (:subreddit data) "/comments/" (:id data) "/")
                         :target "_blank"}
                     (:score scrape)]]
                   [:div.level-item
                    [:span.icon.is-small
                     [:i.fa.fa-comments-o.fa-lg {:aria-label "comment-count"}]]
                    [:a {:href   (str "https://reddit.com/r/" (:subreddit data) "/comments/" (:id data) "/")
                         :target "_blank"}
                     (:comments scrape)]]
                   [:div.level-item
                    (str "/r/" (:subreddit data))]
                   [:div.level-item
                    [:time {:dateTime (:created_at data)}
                     (->relative-date (:created_at data))]]])))

(defn details [type data]
  (let [contents (reagent/atom :content)]
    (fn [type data]
      [:div.modal-content
       [:div.card
        [:header.card-header
         [:p.card-header-title
          [:a {:href   (:permalink data)
               :target "_blank"}
           (:title data)]]]
        [:div.card-content
         [meta-data type data]
         (case @contents
           :content
           [:div.content.op-body
            (if-not (:op data)
              [:p "No text available for this post"]
              (let [op-lines (map clojure.string/trim
                                  (clojure.string/split-lines (:op data)))]
                (map-indexed #(with-meta [:p %2] {:key (str "modal-" %1)})
                             op-lines)))]
           :analysis [:div
                      (let [sent (:sentiment data)]
                        [:div
                         [:p.subtitle "Sentiment Analysis"]
                         [:table.table.is-narrow
                          [:tbody
                           [:tr
                            [:th "title polarity"]
                            [:td (.format ->number-with-fraction
                                          (:polarity (:title sent)))]]
                           [:tr
                            [:th "title subjectivity"]
                            [:td (.format ->number-with-fraction
                                          (:subjectivity (:title sent)))]]
                           (when (:op sent)
                             [:tr
                              [:th "op polarity"]
                              [:td (.format ->number-with-fraction
                                            (:polarity (:op sent)))]])
                           (when (:op sent)
                             [:tr
                              [:th "op subjectivity"]
                              [:td (.format ->number-with-fraction
                                            (:subjectivity (:op sent)))]])]]])
                      [:div
                       [:p.subtitle "Keywords"]
                       [:ul.is-inline
                        (map-indexed #(with-meta [:li [:span.tag %2]]
                                        {:key (str "keyword-" %1)})
                                     (:keywords data))]]])]
        [:footer.card-footer
         [:p.card-footer-item
          (if (= @contents :content)
            "Contents"
            [:a {:on-click #(reset! contents :content)}
             "Contents"])]
         [:p.card-footer-item
          (if (= @contents :analysis)
            "Text Analysis"
            [:a {:on-click #(reset! contents :analysis)}
             "Text Analysis"])]]]])))

(defn- help [data]
  [:div.modal-card.is-extra-wide
   [:div.modal-card-head
    [:p.modal-card-title
     "Frequently asked questions"]]
   [:div.modal-card-body
    [faq/faq]]])

(defn modal []
  (let [modal-state (re-frame/subscribe [:config/modal])]
    (fn []
      (when-let [{:keys [type data]} @modal-state]
        [:div.modal.is-active
         [:div.modal-background
          {:on-click close}]
         (case type
           :help [help data]
           [details type data])
         [:button.modal-close
          {:on-click close}]]))))
