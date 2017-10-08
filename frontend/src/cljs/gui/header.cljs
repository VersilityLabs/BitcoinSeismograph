(ns gui.header
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [gui.modal :as modal]
            [gui.vars :as vars]))

(defn- currency-selector []
  (let [current (re-frame/subscribe [:config/currency])]
    (fn []
      [:div.field.is-horizontal
       [:div.field-body
        [:p.control
         [:span.select.is-small
          [:select#currency-selector
           {:on-change #(let [new-currency (-> % .-target .-value)]
                          (re-frame/dispatch [:config/set
                                              :config/currency
                                              (keyword new-currency)]))
            :value     (name @current)}
           [:option {:value "CNY"} "¥"]
           [:option {:value "EUR"} "€"]
           [:option {:value "USD"} "$"]]]]]])))

(defn header []
  (let [menu-is-toggled? (reagent/atom false)
        warning-level    (re-frame/subscribe [:dashboard/warning-level])]
    (fn []
      [:nav.navbar
       {:class (case @warning-level
                 :warning "is-yellow"
                 :alert   "is-red"
                 nil)}
       [:div.navbar-brand
        [:a.navbar-item.is-paddingless
         {:href "#dashboard"}
         [:img {:src   "assets/logo_no-text.svg"
                :style {:width "120px"}
                :alt   "Logo"}]]
        [:div.navbar-burger
         {:data-target "header-menu"
          :on-click    #(swap! menu-is-toggled? not)
          :class       (when @menu-is-toggled? "is-active")}
         [:span]
         [:span]
         [:span]]]

       [:div#header-menu.navbar-menu
        {:class (when @menu-is-toggled? "is-active")}
        [:div.navbar-start
         [:div.navbar-item
          [:strong "Bitcoin Seismograph"]]
         [:div.navbar-item.no-hover
          [currency-selector]]]
        [:div.navbar-end
         [:a.navbar-item
          {:href   vars/feedback-url
           :target "_blank"}
          "Feedback"]
         [:a.navbar-item
          {:on-click #(re-frame/dispatch [:dashboard/toggle-style])}
          (case @(re-frame/subscribe [:dashboard/style])
            :community-focus "Data-centric view"
            "Community-centric view")]
         [:a.navbar-item
          {:on-click #(modal/show {:type :help :data nil})}
          [:i.fa.fa-question-circle-o.fa-lg {:aria-label "help"}]]]]])))
