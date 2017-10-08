(ns gui.footer
  (:require [gui.vars :as vars]))

(defn footer []
  [:div.footer
   [:div.container
    [:div.columns
     [:div.column.is-4
      "The Bitcoin Seismograph is a joint open source research project by "
      "Marcel Morisse"
      ", PhD student at the Department of Informatics, "
      "Universität Hamburg, and the "
      [:a {:href   "https://versility.com"
           :target "_blank"}
       "Versility Labs GmbH"]]
     [:div.column.is-6
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
       "."]
      [:p [:a {:href   "impressum.html"
               :target "_blank"}
           "Impressum - Contact Details"]]]
     [:div.column.has-text-right
      [:a.is-unstyled {:href   "https://github.com/VersilityLabs/BitcoinSeismograph"
                       :target "_blank"}
       [:span.icon.is-large
        [:i.fa.fa-github.fa-3x {:aria-label "github"}]]]]]]])
