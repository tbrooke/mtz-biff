(ns admin.com.app
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [admin.com.ui :as ui]
            [admin.com.settings :as settings]
            [xtdb.api :as xt]))




(defn app [{:keys [session biff/db] :as ctx}]
  (let [{:user/keys [email]} (xt/entity db (:uid session))
        recent-posts (q db
                        '{:find (pull post [*])
                          :where [[post :post/title]]
                          :order-by [[post :post/created-at :desc]]})]
    (ui/page
     {}
     [:div.flex.justify-between.items-center.mb-6
      [:div
       [:h1.text-3xl.font-bold.text-gray-800 "Admin Dashboard"]
       [:p.text-gray-600 "Signed in as " [:span.font-semibold email]]]
      (biff/form
       {:action "/auth/signout"
        :class "inline"}
       [:button.text-red-500.hover:text-red-700.text-sm {:type "submit"}
        "Sign out"])]
     
     ;; Quick Stats
     [:div.grid.grid-cols-1.md:grid-cols-3.gap-6.mb-8
      [:div.bg-blue-50.p-6.rounded-lg.border
       [:h3.text-lg.font-semibold.text-blue-800 "Blog Posts"]
       [:p.text-2xl.font-bold.text-blue-900 (count recent-posts)]
       [:a.text-blue-600.hover:text-blue-800.text-sm {:href "/posts"} "Manage Posts →"]]
      
      [:div.bg-green-50.p-6.rounded-lg.border
       [:h3.text-lg.font-semibold.text-green-800 "Events"]
       [:p.text-2xl.font-bold.text-green-900 "0"]
       [:a.text-green-600.hover:text-green-800.text-sm {:href "/events"} "Manage Events →"]]
      
      [:div.bg-purple-50.p-6.rounded-lg.border
       [:h3.text-lg.font-semibold.text-purple-800 "Media"]
       [:p.text-2xl.font-bold.text-purple-900 "0"]
       [:a.text-purple-600.hover:text-purple-800.text-sm {:href "/uploads"} "Manage Media →"]]]
     
     ;; Recent Content
     [:div.mb-8
      [:div.flex.justify-between.items-center.mb-4
       [:h2.text-xl.font-semibold.text-gray-800 "Recent Blog Posts"]
       [:a.btn.btn-primary {:href "/posts/new"} "New Post"]]
      
      (if (empty? recent-posts)
        [:div.bg-gray-50.p-8.rounded-lg.text-center
         [:p.text-gray-600 "No blog posts yet."]
         [:a.btn.btn-primary.mt-4 {:href "/posts/new"} "Create Your First Post"]]
        [:div.bg-white.shadow.rounded-lg
         (for [post (take 5 recent-posts)]
           [:div.p-4.border-b.last:border-b-0
            [:div.flex.justify-between.items-start
             [:div
              [:h3.font-semibold.text-gray-800 (:post/title post)]
              [:p.text-gray-600.text-sm.mt-1 
               "Created " (when (:post/created-at post)
                            (biff/format-date (:post/created-at post) "dd MMM yyyy"))]]
             [:div.flex.gap-2
              [:a.text-blue-600.hover:text-blue-800.text-sm 
               {:href (str "/posts/edit/" (:xt/id post))} "Edit"]
              [:a.text-green-600.hover:text-green-800.text-sm 
               {:href (str "/posts/view/" (:xt/id post))} "View"]]]])])]
     
     ;; Quick Actions
     [:div
      [:h2.text-xl.font-semibold.text-gray-800.mb-4 "Quick Actions"]
      [:div.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-4.gap-4
       [:a.block.p-4.bg-white.border.rounded-lg.hover:shadow-md.transition-shadow
        {:href "/posts/new"}
        [:div.text-blue-600.text-2xl.mb-2 "📝"]
        [:h3.font-semibold.text-gray-800 "New Blog Post"]
        [:p.text-gray-600.text-sm "Write a new article"]]
       
       [:a.block.p-4.bg-white.border.rounded-lg.hover:shadow-md.transition-shadow
        {:href "/events/new"}
        [:div.text-green-600.text-2xl.mb-2 "📅"]
        [:h3.font-semibold.text-gray-800 "New Event"]
        [:p.text-gray-600.text-sm "Schedule an event"]]
       
       [:a.block.p-4.bg-white.border.rounded-lg.hover:shadow-md.transition-shadow
        {:href "/uploads"}
        [:div.text-purple-600.text-2xl.mb-2 "🖼️"]
        [:h3.font-semibold.text-gray-800 "Upload Media"]
        [:p.text-gray-600.text-sm "Add images or files"]]
       
       [:a.block.p-4.bg-white.border.rounded-lg.hover:shadow-md.transition-shadow
        {:href "/settings"}
        [:div.text-gray-600.text-2xl.mb-2 "⚙️"]
        [:h3.font-semibold.text-gray-800 "Settings"]
        [:p.text-gray-600.text-sm "Configure the site"]]]])))



(def about-page
  (ui/page
   {:base/title (str "About " settings/app-name)}
   [:p "This app was made with "
    [:a.link {:href "https://biffweb.com"} "Biff"] "."]))

(defn echo [{:keys [params]}]
  {:status 200
   :headers {"content-type" "application/json"}
   :body params})

(def module
  {:static {"/about/" about-page}
   :routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app}]]
   :api-routes [["/api/echo" {:post echo}]]})
