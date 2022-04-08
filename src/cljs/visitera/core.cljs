(ns visitera.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [visitera.ajax :as ajax]
    [visitera.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "visitera"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn map-component
  []
  (let [create (fn [this]
                 ; Define globals
                 (def am4core (.-am4core js/window))
                 (def am4maps (.-am4maps js/window))
                 (def am4geodata_worldLow (.-am4geodata_worldLow js/window))

                 ; Create map instance
                 (def chart (.create am4core "chartdiv" (.-MapChart am4maps)))

                 ; Set map definition
                 (set! (.-geodata chart) am4geodata_worldLow)

                 ; Set projection
                 (set! (.-projection chart) (new (.-Miller (.-projections am4maps))))

                 ; Create map polygon series
                 (def polygonSeries (.push (.-series chart) (new (.-MapPolygonSeries am4maps))))

                 ; Make map load polygon (like country names) data from GeoJSON
                 (set! (.-useGeodata polygonSeries) true)

                 ; Configure series
                 (def polygonTemplate (.. polygonSeries -mapPolygons -template))
                 (set! (.-tooltipText polygonTemplate) "{name}")
                 (set! (.-fill polygonTemplate) (.color am4core "#74B266"))

                 ; Remove Antarctica
                 (set! (.-exclude polygonSeries) #js ["AQ"])

                 ; add some data
                 (def testData
                   #js
                    [#js {:id    "CN"
                          :value 100
                          :fill  (.color am4core "#F05C5C")}
                     #js {:id    "TW"
                          :value 50
                          :fill  (.color am4core "#F05C5C")}
                     #js {:id    "HK"
                          :value 50
                          :fill  (.color am4core "#F05C5C")}
                     #js {:id    "MO"
                          :value 50
                          :fill  (.color am4core "#F05C5C")}
                     ])
                 (set! (.-data polygonSeries) testData)

                 ; Bind "fill" property to "fill" key in data
                 (set! (.. polygonTemplate -propertyFields -fill) "fill"))]

    (r/create-class
     {:display-name  "map-component"
      :reagent-render (fn []
                        [:div {:id "chartdiv"
                               :style {:width "100%"
                                       :height "calc(100vh - 5rem)"}}])
      :component-did-mount
      create})))

(defn home-page []
  [map-component])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch [:fetch-user-countries])
  (mount-components))
