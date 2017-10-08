(ns gui.app
  (:require [goog.events :as ge]

            [cljsjs.c3]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]

            [gui.events]
            [gui.fx]
            [gui.subs]

            [gui.chart :as chart]
            [gui.dashboard :as dashboard]
            [gui.footer :as footer]
            [gui.header :as header]
            [gui.modal :as modal]
            [gui.welcome :as welcome]))

(ge/listen js/window
           ge/EventType.KEYDOWN
           (fn [e]
             (if (= (.-key e) "Escape")
               (modal/close))))

(defn root []
  (let [initialized?     (re-frame/subscribe [:initialized?])
        welcome-visible? (re-frame/subscribe [:config/welcome-visible?])]
    (fn []
      (if-not @initialized?
        [:div.hero
         [:div.hero-body
          [:div.content.has-text-centered
           [:span.icon.is-large
            [:i.fa.fa-circle-o-notch.fa-spin.fa-3x.fa-fw]]]]]
        [:div
         [header/header]
         [welcome/msg @welcome-visible?]
         [:div.wrapper
          {:class (when-not @welcome-visible?
                    "has-padding-for-nav")}
          [modal/modal]
          [dashboard/dashboard]
          [chart/charts]
          [chart/community-data]]
         [footer/footer]]))))

(defn init []
  (re-frame/dispatch-sync [:initialize-db])
  ;; FIXME: this is just a workaround for reloads during dev
  (when-not @(re-frame/subscribe [:initialized?])
    (re-frame/dispatch [:load/initial-data]))
  (re-frame/clear-subscription-cache!)
  (reagent/render-component [root]
                            (.getElementById js/document "container")))
