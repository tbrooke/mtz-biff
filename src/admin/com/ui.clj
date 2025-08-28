(ns admin.com.ui
  (:require [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [admin.com.settings :as settings]
            [com.biffweb :as biff]
            [ring.middleware.anti-forgery :as csrf]
            [ring.util.response :as ring-response]
            [rum.core :as rum]))

(defn static-path [path]
  (if-some [last-modified (some-> (io/resource (str "public" path))
                                  ring-response/resource-data
                                  :last-modified
                                  (.getTime))]
    (str path "?t=" last-modified)
    path))

(defn base [{:keys [::recaptcha] :as ctx} & body]
  (apply
   biff/base-html
   (-> ctx
       (merge #:base{:title settings/app-name
                     :lang "en-US"
                     :icon "/img/glider.png"
                     :description (str settings/app-name " Description")
                     :image "https://clojure.org/images/clojure-logo-120b.png"})
       (update :base/head (fn [head]
                            (concat [[:link {:rel "stylesheet" :href (static-path "/css/main.css")}]
                                     [:script {:src (static-path "/js/main.js")}]
                                     [:script {:src "https://unpkg.com/htmx.org@2.0.4"}]
                                     [:script {:src "https://unpkg.com/htmx-ext-ws@2.0.1/ws.js"}]
                                     [:script {:src "https://unpkg.com/hyperscript.org@0.9.13"}]
                                     (when recaptcha
                                       [:script {:src "https://www.google.com/recaptcha/api.js"
                                                 :async "async" :defer "defer"}])]
                                    head))))
   body))

(defn navbar [{:keys [session]}]
  (when session
    [:nav.bg-gray-800.text-white.p-4
     [:div.max-w-6xl.mx-auto.flex.justify-between.items-center
      [:div.flex.space-x-4
       [:a.hover:text-gray-300 {:href "/app"} "Dashboard"]
       [:a.hover:text-gray-300 {:href "/posts"} "Blog Posts"]
       [:a.hover:text-gray-300 {:href "/admin"} "Admin"]
       [:a.hover:text-gray-300 {:href "/uploads"} "Media"]
       [:a.hover:text-gray-300 {:href "/database-test"} "DB Test"]]
      [:div.text-sm
       "Signed in as " (get-in session [:user/email] "user")]]]))

(defn page [ctx & body]
  (base
   ctx
   (navbar ctx)
   [:.flex-grow]
   [:.p-3.mx-auto.max-w-6xl.w-full
    (when (bound? #'csrf/*anti-forgery-token*)
      {:hx-headers (cheshire/generate-string
                    {:x-csrf-token csrf/*anti-forgery-token*})})
    body]
   [:.flex-grow]
   [:.flex-grow]))

(defn on-error [{:keys [status ex] :as ctx}]
  {:status status
   :headers {"content-type" "text/html"}
   :body (rum/render-static-markup
          (page
           ctx
           [:h1.text-lg.font-bold
            (if (= status 404)
              "Page not found."
              "Something went wrong.")]))})
