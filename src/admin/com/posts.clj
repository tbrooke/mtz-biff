(ns admin.com.posts
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [admin.com.ui :as ui]
            [xtdb.api :as xt]
            [clojure.string :as str]))

(defn generate-slug [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-")))

(defn posts-list [{:keys [biff/db] :as ctx}]
  (let [posts (q db
                 '{:find (pull post [*])
                   :where [[post :db/doc-type :blog-entry]]
                   :order-by [[post :blog-entry/created-at :desc]]})]
    (ui/page
     ctx
     [:div.flex.justify-between.items-center.mb-6
      [:h1.text-2xl.font-bold.text-gray-800 "Blog Posts"]
      [:a.btn.btn-primary {:href "/posts/new"} "New Post"]]

     (if (empty? posts)
       [:div.bg-gray-50.p-8.rounded-lg.text-center
        [:p.text-gray-600.mb-4 "No blog posts yet."]
        [:a.btn.btn-primary {:href "/posts/new"} "Create Your First Post"]]

       [:div.bg-white.shadow.rounded-lg
        [:table.w-full
         [:thead.bg-gray-50
          [:tr
           [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Title"]
           [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Author"]
           [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Date"]
           [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Actions"]]]
         [:tbody.bg-white.divide-y.divide-gray-200
          (for [post posts]
            [:tr
             [:td.px-6.py-4.whitespace-nowrap
              [:div.text-sm.font-medium.text-gray-900 (:blog-entry/title post)]
              [:div.text-sm.text-gray-500 (:blog-entry/slug post)]]
             [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900
              (:blog-entry/author post)]
             [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-500
              (when (:blog-entry/created-at post)
                (biff/format-date (:blog-entry/created-at post) "dd MMM yyyy"))]
             [:td.px-6.py-4.whitespace-nowrap.text-sm.font-medium
              [:div.flex.gap-2
               [:a.text-blue-600.hover:text-blue-900
                {:href (str "/posts/edit/" (:xt/id post))} "Edit"]
               [:a.text-green-600.hover:text-green-900
                {:href (str "/posts/view/" (:xt/id post))} "View"]
               [:form.inline {:method "post" :action (str "/posts/delete/" (:xt/id post))
                              :onsubmit "return confirm('Are you sure you want to delete this post?')"}
                [:button.text-red-600.hover:text-red-900 {:type "submit"} "Delete"]]]]])]]]))))

(defn post-form [ctx post]
  (ui/page
   ctx
   [:div.max-w-4xl.mx-auto
    [:div.flex.justify-between.items-center.mb-6
     [:h1.text-2xl.font-bold.text-gray-800
      (if post "Edit Post" "New Post")]
     [:a.btn.btn-secondary {:href "/posts"} "← Back to Posts"]]

    (biff/form
     {:action (if post
                (str "/posts/save/" (:xt/id post))
                "/posts/create")
      :method "post"}
     [:div.bg-white.shadow.rounded-lg.p-6
      [:div.grid.grid-cols-1.gap-6
       [:div
        [:label.block.text-sm.font-medium.text-gray-700 {:for "title"} "Title"]
        [:input.mt-1.block.w-full.rounded-md.border-gray-300.shadow-sm
         {:type "text"
          :id "title"
          :name "title"
          :required true
          :value (or (:blog-entry/title post) "")}]]

       [:div
        [:label.block.text-sm.font-medium.text-gray-700 {:for "slug"} "Slug"]
        [:input.mt-1.block.w-full.rounded-md.border-gray-300.shadow-sm
         {:type "text"
          :id "slug"
          :name "slug"
          :value (or (:blog-entry/slug post) "")}]
        [:p.mt-1.text-sm.text-gray-500 "Leave empty to auto-generate from title"]]

       [:div
        [:label.block.text-sm.font-medium.text-gray-700 {:for "author"} "Author"]
        [:input.mt-1.block.w-full.rounded-md.border-gray-300.shadow-sm
         {:type "text"
          :id "author"
          :name "author"
          :required true
          :value (or (:blog-entry/author post) "")}]]

       [:div
        [:label.block.text-sm.font-medium.text-gray-700 {:for "content"} "Content"]
        [:textarea.mt-1.block.w-full.rounded-md.border-gray-300.shadow-sm
         {:id "content"
          :name "content"
          :rows "12"
          :required true}
         (or (:blog-entry/content post) "")]]

       [:div
        [:label.block.text-sm.font-medium.text-gray-700 {:for "excerpt"} "Excerpt"]
        [:textarea.mt-1.block.w-full.rounded-md.border-gray-300.shadow-sm
         {:id "excerpt"
          :name "excerpt"
          :rows "3"}
         (or (:blog-entry/excerpt post) "")]
        [:p.mt-1.text-sm.text-gray-500 "Short description for listings"]]

       [:div.flex.items-center
        [:input.h-4.w-4.text-blue-600.focus:ring-blue-500.border-gray-300.rounded
         {:type "checkbox"
          :id "featured"
          :name "featured"
          :checked (boolean (:blog-entry/featured post))}]
        [:label.ml-2.block.text-sm.text-gray-900 {:for "featured"} "Featured post"]]]]

     [:div.mt-6.flex.justify-end.gap-3
      [:a.btn.btn-secondary {:href "/posts"} "Cancel"]
      [:button.btn.btn-primary {:type "submit"}
       (if post "Update Post" "Create Post")]])]))

(defn new-post [ctx]
  (post-form ctx nil))

(defn edit-post [{:keys [biff/db path-params] :as ctx}]
  (let [post-id (parse-uuid (:post-id path-params))
        post (xt/entity db post-id)]
    (if post
      (post-form ctx post)
      {:status 404
       :headers {"content-type" "text/html"}
       :body "Post not found"})))

(defn view-post [{:keys [biff/db path-params] :as ctx}]
  (let [post-id (parse-uuid (:post-id path-params))
        post (xt/entity db post-id)]
    (if post
      (ui/page
       ctx
       [:div.max-w-4xl.mx-auto
        [:div.flex.justify-between.items-center.mb-6
         [:div
          [:h1.text-3xl.font-bold.text-gray-800 (:blog-entry/title post)]
          [:p.text-gray-600.mt-2
           "By " (:blog-entry/author post)
           " • " (when (:blog-entry/created-at post)
                   (biff/format-date (:blog-entry/created-at post) "MMMM dd, yyyy"))]]
         [:div.flex.gap-2
          [:a.btn.btn-secondary {:href "/posts"} "← Back to Posts"]
          [:a.btn.btn-primary {:href (str "/posts/edit/" (:xt/id post))} "Edit"]]]

        [:div.bg-white.shadow.rounded-lg.p-6
         [:div.prose.max-w-none
          (when (:blog-entry/excerpt post)
            [:div.text-lg.text-gray-700.mb-6.italic (:blog-entry/excerpt post)])
          [:div.whitespace-pre-wrap (:blog-entry/content post)]]]])
      {:status 404
       :headers {"content-type" "text/html"}
       :body "Post not found"})))

(defn create-post [{:keys [session params] :as ctx}]
  (let [slug (if (str/blank? (:slug params))
               (generate-slug (:title params))
               (:slug params))
        post-doc {:db/doc-type :blog-entry
                  :xt/id (random-uuid)
                  :blog-entry/title (:title params)
                  :blog-entry/slug slug
                  :blog-entry/author (:author params)
                  :blog-entry/content (:content params)
                  :blog-entry/excerpt (:excerpt params)
                  :blog-entry/featured (= "on" (:featured params))
                  :blog-entry/user (:uid session)
                  :blog-entry/created-at (java.util.Date.)
                  :blog-entry/updated-at (java.util.Date.)}]
    (biff/submit-tx ctx [post-doc])
    {:status 303
     :headers {"location" "/posts"}}))

(defn save-post [{:keys [biff/db session params path-params] :as ctx}]
  (let [post-id (parse-uuid (:post-id path-params))
        existing-post (xt/entity db post-id)]
    (if existing-post
      (let [slug (if (str/blank? (:slug params))
                   (generate-slug (:title params))
                   (:slug params))
            post-doc {:db/op :update
                      :xt/id post-id
                      :blog-entry/title (:title params)
                      :blog-entry/slug slug
                      :blog-entry/author (:author params)
                      :blog-entry/content (:content params)
                      :blog-entry/excerpt (:excerpt params)
                      :blog-entry/featured (= "on" (:featured params))
                      :blog-entry/updated-at (java.util.Date.)}]
        (biff/submit-tx ctx [post-doc])
        {:status 303
         :headers {"location" "/posts"}})
      {:status 404
       :headers {"content-type" "text/html"}
       :body "Post not found"})))

(defn delete-post [{:keys [biff/db path-params] :as ctx}]
  (let [post-id (parse-uuid (:post-id path-params))
        existing-post (xt/entity db post-id)]
    (if existing-post
      (do
        (biff/submit-tx ctx [{:xt/id post-id :db/op :delete}])
        {:status 303
         :headers {"location" "/posts"}})
      {:status 404
       :headers {"content-type" "text/html"}
       :body "Post not found"})))

(def module
  {:routes ["/posts" {:middleware [mid/wrap-signed-in]}
            ["" {:get posts-list}]
            ["/new" {:get new-post}]
            ["/create" {:post create-post}]
            ["/view/:post-id" {:get view-post}]
            ["/edit/:post-id" {:get edit-post}]
            ["/save/:post-id" {:post save-post}]
            ["/delete/:post-id" {:post delete-post}]]})
