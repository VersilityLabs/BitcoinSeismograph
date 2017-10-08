(ns gui.welcome
  (:require [re-frame.core :as re-frame]

            [gui.vars :as vars]))

(defn msg [visible?]
  (let [warning-level (re-frame/subscribe [:dashboard/warning-level])]
    (fn [visible?]
      (when visible?
        [:div.message-wrapper.has-padding-for-nav
         [:div.message
          ;; FIXME: bulma used .is-danger instead of .is-alert!
          {:class (case @warning-level
                    :alert   "is-danger"
                    :warning "is-warning"
                    nil)}
          [:div.message-header
           "About"
           [:button.delete
            {:on-click #(re-frame/dispatch [:display/hide-welcome])}]]
          [:div.message-body
           [:div.content
            [:p
             "The Bitcoin Seismograph aims to provide you with an overview"
             " of the Bitcoin ecosystem, its markets and the blockchain itself."]
            (case @warning-level
              :alert   [:p
                        [:strong "There are unexpectedly strong fluctuations in the Bitcoin ecosystem."]
                        " Check the ongoing "
                        [:a {:on-click #(re-frame/dispatch [:dashboard/set-style
                                                            :community-focus])}
                         "community discussions"]
                        " to get a more detailed understanding "
                        "of the situation."]
              :warning [:p
                        "Our algorithms have reported "
                        [:strong "some unexpected "
                         [:a {:on-click #(re-frame/dispatch [:dashboard/set-style
                                                             :community-focus])}
                          "Bitcoin developments"]
                         "."]]
              nil      [:p
                        "Currently "
                        [:strong "everything looks normal"]
                        ", our algorithms haven't "
                        "identified anything out of the ordinary."])
            [:p
             "We need your "
             [:a {:href   vars/feedback-url
                  :target "_blank"}
              "feedback"]
             " to fine-tune and improve our algorithms further."
             " Use of the Bitcoin Seismograph is subject to our "
             [:a {:href   "termsofuse.html"
                  :target "_blank"}
              "Terms of Use"]
             " and "
             [:a {:href   "privacypolicy.html"
                  :target "_blank"}
              "Privacy Policy"]
             "."]]]]]))))
