(ns admin.com.admin
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [admin.com.ui :as ui]
            [rum.core :as rum]
            [xtdb.api :as xt]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

;; Helper functions
(defn slugify [text]
  "Convert text to URL-friendly slug"
  (-> text
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-")
      (str/replace #"-+" "-")
      str/trim))

(defn format-date [date-str]
  "Format date string for display"
  (if (string? date-str)
    date-str
    (str date-str)))

;; Sermon Management
(defn sermon-form [{:keys [sermon action]}]
  (let [{:sermon/keys [title speaker date scripture-text summary
                      video-url audio-url series-name series-part
                      topic duration featured]} sermon
        is-edit (= action :edit)]
    [:form.space-y-4 {:action (if is-edit 
                                "/admin/sermons/update" 
                                "/admin/sermons/create")
                      :method "post"}
     (when is-edit
       [:input {:type "hidden" :name "id" :value (str (:xt/id sermon))}])
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "title"} "Title *"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "text" :name "title" :id "title" :required true
        :value (or title "")}]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "speaker"} "Speaker *"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "text" :name "speaker" :id "speaker" :required true
        :value (or speaker "")}]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "date"} "Date *"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "date" :name "date" :id "date" :required true
         :value (or date "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "topic"} "Topic"]
       [:select.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:name "topic" :id "topic"}
        [:option {:value ""} "Select Topic"]
        (for [topic-option [:salvation :faith :hope :love :discipleship :worship 
                           :prayer :service :missions :family :stewardship]]
          [:option {:value (name topic-option)
                   :selected (= topic topic-option)}
           (str/capitalize (name topic-option))])]]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "scripture-text"} "Scripture Text *"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "text" :name "scripture-text" :id "scripture-text" :required true
        :value (or scripture-text "")}]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "summary"} "Summary"]
      [:textarea.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:name "summary" :id "summary" :rows 4
        :value (or summary "")}]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "series-name"} "Series Name"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "text" :name "series-name" :id "series-name"
         :value (or series-name "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "series-part"} "Series Part"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "number" :name "series-part" :id "series-part"
         :value (or series-part "")}]]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "video-url"} "Video URL"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "url" :name "video-url" :id "video-url"
         :value (or video-url "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "audio-url"} "Audio URL"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "url" :name "audio-url" :id "audio-url"
         :value (or audio-url "")}]]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "duration"} "Duration (minutes)"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "number" :name "duration" :id "duration"
         :value (or duration "")}]]
      
      [:div.flex.items-center.pt-6
       [:input.mr-2 {:type "checkbox" :name "featured" :id "featured"
                     :checked (boolean featured)}]
       [:label.text-sm.font-medium.text-gray-700 {:for "featured"} "Featured"]]]
     
     [:div.flex.justify-end.space-x-3
      [:a.btn.bg-gray-500.hover:bg-gray-700.text-white.px-4.py-2.rounded
       {:href "/admin/sermons"} "Cancel"]
      [:button.btn.bg-blue-500.hover:bg-blue-700.text-white.px-4.py-2.rounded
       {:type "submit"}
       (if is-edit "Update Sermon" "Create Sermon")]]]))

(defn sermons-list [{:keys [biff/db]}]
  (let [sermons (q db '{:find (pull sermon [*])
                       :where [[sermon :sermon/title]]
                       :order-by [[sermon :sermon/created-at :desc]]})]
    [:div.space-y-4
     [:div.flex.justify-between.items-center
      [:h2.text-2xl.font-bold "Sermons"]
      [:a.btn.bg-green-500.hover:bg-green-700.text-white.px-4.py-2.rounded
       {:href "/admin/sermons/new"} "New Sermon"]]
     
     (if (empty? sermons)
       [:p.text-gray-500 "No sermons found."]
       [:div.overflow-x-auto
        [:table.min-w-full.bg-white.border.border-gray-300
         [:thead.bg-gray-50
          [:tr
           [:th.px-4.py-2.border-b.text-left "Title"]
           [:th.px-4.py-2.border-b.text-left "Speaker"] 
           [:th.px-4.py-2.border-b.text-left "Date"]
           [:th.px-4.py-2.border-b.text-left "Topic"]
           [:th.px-4.py-2.border-b.text-left "Featured"]
           [:th.px-4.py-2.border-b.text-center "Actions"]]]
         [:tbody
          (for [sermon sermons]
            (let [{:sermon/keys [title speaker date topic featured]} sermon]
              [:tr {:key (str (:xt/id sermon))}
               [:td.px-4.py-2.border-b title]
               [:td.px-4.py-2.border-b speaker]
               [:td.px-4.py-2.border-b (format-date date)]
               [:td.px-4.py-2.border-b (when topic (str/capitalize (name topic)))]
               [:td.px-4.py-2.border-b (if featured "Yes" "No")]
               [:td.px-4.py-2.border-b.text-center
                [:div.flex.justify-center.space-x-2
                 [:a.text-blue-600.hover:text-blue-800
                  {:href (str "/admin/sermons/" (:xt/id sermon) "/edit")}
                  "Edit"]
                 [:form.inline {:action (str "/admin/sermons/" (:xt/id sermon) "/delete")
                               :method "post"
                               :onsubmit "return confirm('Are you sure?')"}
                  [:button.text-red-600.hover:text-red-800 {:type "submit"} "Delete"]]]]]))]]])]))

;; Event Management
(defn event-form [{:keys [event action]}]
  (let [{:event/keys [title description start-date end-date event-type
                     location contact-person contact-email contact-phone
                     max-attendees registration-required registration-url
                     registration-deadline cost featured all-day]} event
        is-edit (= action :edit)]
    [:form.space-y-4 {:action (if is-edit 
                                "/admin/events/update" 
                                "/admin/events/create")
                      :method "post"}
     (when is-edit
       [:input {:type "hidden" :name "id" :value (str (:xt/id event))}])
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "title"} "Title *"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "text" :name "title" :id "title" :required true
        :value (or title "")}]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "description"} "Description *"]
      [:textarea.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:name "description" :id "description" :rows 4 :required true
        :value (or description "")}]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "event-type"} "Event Type *"]
      [:select.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:name "event-type" :id "event-type" :required true}
       [:option {:value ""} "Select Event Type"]
       (for [type-option [:service :meeting :social :outreach :study 
                         :conference :retreat :fundraiser]]
         [:option {:value (name type-option)
                  :selected (= event-type type-option)}
          (str/capitalize (name type-option))])]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "start-date"} "Start Date *"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "datetime-local" :name "start-date" :id "start-date" :required true
         :value (or start-date "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "end-date"} "End Date"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "datetime-local" :name "end-date" :id "end-date"
         :value (or end-date "")}]]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "contact-person"} "Contact Person"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "text" :name "contact-person" :id "contact-person"
         :value (or contact-person "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "contact-email"} "Contact Email"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "email" :name "contact-email" :id "contact-email"
         :value (or contact-email "")}]]]
     
     [:div.grid.grid-cols-2.gap-4
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "contact-phone"} "Contact Phone"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "tel" :name "contact-phone" :id "contact-phone"
         :value (or contact-phone "")}]]
      
      [:div
       [:label.block.text-sm.font-medium.text-gray-700 {:for "max-attendees"} "Max Attendees"]
       [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
        {:type "number" :name "max-attendees" :id "max-attendees"
         :value (or max-attendees "")}]]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "registration-url"} "Registration URL"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "url" :name "registration-url" :id "registration-url"
        :value (or registration-url "")}]]
     
     [:div
      [:label.block.text-sm.font-medium.text-gray-700 {:for "registration-deadline"} "Registration Deadline"]
      [:input.mt-1.block.w-full.border-gray-300.rounded-md.shadow-sm
       {:type "datetime-local" :name "registration-deadline" :id "registration-deadline"
        :value (or registration-deadline "")}]]
     
     [:div.flex.space-x-6
      [:div.flex.items-center
       [:input.mr-2 {:type "checkbox" :name "registration-required" :id "registration-required"
                     :checked (boolean registration-required)}]
       [:label.text-sm.font-medium.text-gray-700 {:for "registration-required"} "Registration Required"]]
      
      [:div.flex.items-center
       [:input.mr-2 {:type "checkbox" :name "featured" :id "featured"
                     :checked (boolean featured)}]
       [:label.text-sm.font-medium.text-gray-700 {:for "featured"} "Featured"]]
      
      [:div.flex.items-center
       [:input.mr-2 {:type "checkbox" :name "all-day" :id "all-day"
                     :checked (boolean all-day)}]
       [:label.text-sm.font-medium.text-gray-700 {:for "all-day"} "All Day Event"]]]
     
     [:div.flex.justify-end.space-x-3
      [:a.btn.bg-gray-500.hover:bg-gray-700.text-white.px-4.py-2.rounded
       {:href "/admin/events"} "Cancel"]
      [:button.btn.bg-blue-500.hover:bg-blue-700.text-white.px-4.py-2.rounded
       {:type "submit"}
       (if is-edit "Update Event" "Create Event")]]]))

(defn events-list [{:keys [biff/db]}]
  (let [events (q db '{:find (pull event [*])
                      :where [[event :event/title]]
                      :order-by [[event :event/created-at :desc]]})]
    [:div.space-y-4
     [:div.flex.justify-between.items-center
      [:h2.text-2xl.font-bold "Events"]
      [:a.btn.bg-green-500.hover:bg-green-700.text-white.px-4.py-2.rounded
       {:href "/admin/events/new"} "New Event"]]
     
     (if (empty? events)
       [:p.text-gray-500 "No events found."]
       [:div.overflow-x-auto
        [:table.min-w-full.bg-white.border.border-gray-300
         [:thead.bg-gray-50
          [:tr
           [:th.px-4.py-2.border-b.text-left "Title"]
           [:th.px-4.py-2.border-b.text-left "Type"]
           [:th.px-4.py-2.border-b.text-left "Start Date"]
           [:th.px-4.py-2.border-b.text-left "Featured"]
           [:th.px-4.py-2.border-b.text-center "Actions"]]]
         [:tbody
          (for [event events]
            (let [{:event/keys [title event-type start-date featured]} event]
              [:tr {:key (str (:xt/id event))}
               [:td.px-4.py-2.border-b title]
               [:td.px-4.py-2.border-b (when event-type (str/capitalize (name event-type)))]
               [:td.px-4.py-2.border-b (format-date start-date)]
               [:td.px-4.py-2.border-b (if featured "Yes" "No")]
               [:td.px-4.py-2.border-b.text-center
                [:div.flex.justify-center.space-x-2
                 [:a.text-blue-600.hover:text-blue-800
                  {:href (str "/admin/events/" (:xt/id event) "/edit")}
                  "Edit"]
                 [:form.inline {:action (str "/admin/events/" (:xt/id event) "/delete")
                               :method "post"
                               :onsubmit "return confirm('Are you sure?')"}
                  [:button.text-red-600.hover:text-red-800 {:type "submit"} "Delete"]]]]]))]]])]))

;; Main Admin Dashboard
(defn admin-dashboard [{:keys [biff/db]}]
  (let [user-count (count (q db '{:find [user]
                                 :where [[user :user/email]]}))
        sermon-count (count (q db '{:find [sermon]
                                   :where [[sermon :sermon/title]]}))
        event-count (count (q db '{:find [event]
                                  :where [[event :event/title]]}))
        blog-count (count (q db '{:find [blog]
                                 :where [[blog :blog-entry/title]]}))
        recent-sermons (q db '{:find (pull sermon [:xt/id :sermon/title :sermon/created-at])
                              :where [[sermon :sermon/title]]
                              :order-by [[sermon :sermon/created-at :desc]]
                              :limit 5})
        recent-events (q db '{:find (pull event [:xt/id :event/title :event/created-at])
                             :where [[event :event/title]]
                             :order-by [[event :event/created-at :desc]]
                             :limit 5})]
    [:div.space-y-6
     [:h1.text-3xl.font-bold "Admin Dashboard"]
     
     ;; Stats Cards
     [:div.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-4.gap-4
      [:div.bg-blue-50.border.border-blue-200.rounded-lg.p-4
       [:div.flex.items-center
        [:div
         [:p.text-2xl.font-bold.text-blue-600 user-count]
         [:p.text-sm.text-blue-800 "Users"]]]]
      
      [:div.bg-green-50.border.border-green-200.rounded-lg.p-4
       [:div.flex.items-center
        [:div
         [:p.text-2xl.font-bold.text-green-600 sermon-count]
         [:p.text-sm.text-green-800 "Sermons"]]]]
      
      [:div.bg-purple-50.border.border-purple-200.rounded-lg.p-4
       [:div.flex.items-center
        [:div
         [:p.text-2xl.font-bold.text-purple-600 event-count]
         [:p.text-sm.text-purple-800 "Events"]]]]
      
      [:div.bg-yellow-50.border.border-yellow-200.rounded-lg.p-4
       [:div.flex.items-center
        [:div
         [:p.text-2xl.font-bold.text-yellow-600 blog-count]
         [:p.text-sm.text-yellow-800 "Blog Posts"]]]]]
     
     ;; Quick Actions
     [:div.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-4.gap-4
      [:a.block.bg-white.border.border-gray-200.rounded-lg.p-4.hover:bg-gray-50
       {:href "/admin/sermons/new"}
       [:div.text-center
        [:div.text-2xl.mb-2 "📝"]
        [:p.font-semibold "New Sermon"]]]
      
      [:a.block.bg-white.border.border-gray-200.rounded-lg.p-4.hover:bg-gray-50
       {:href "/admin/events/new"}
       [:div.text-center
        [:div.text-2xl.mb-2 "📅"]
        [:p.font-semibold "New Event"]]]
      
      [:a.block.bg-white.border.border-gray-200.rounded-lg.p-4.hover:bg-gray-50
       {:href "/admin/blog/new"}
       [:div.text-center
        [:div.text-2xl.mb-2 "✍️"]
        [:p.font-semibold "New Blog Post"]]]
      
      [:a.block.bg-white.border.border-gray-200.rounded-lg.p-4.hover:bg-gray-50
       {:href "/database-test"}
       [:div.text-center
        [:div.text-2xl.mb-2 "🔧"]
        [:p.font-semibold "Database Test"]]]]
     
     ;; Recent Content
     [:div.grid.grid-cols-1.lg:grid-cols-2.gap-6
      [:div.bg-white.border.border-gray-200.rounded-lg.p-4
       [:h3.text-lg.font-semibold.mb-4 "Recent Sermons"]
       (if (empty? recent-sermons)
         [:p.text-gray-500 "No sermons yet."]
         [:ul.space-y-2
          (for [sermon recent-sermons]
            [:li {:key (str (:xt/id sermon))}
             [:a.text-blue-600.hover:text-blue-800
              {:href (str "/admin/sermons/" (:xt/id sermon) "/edit")}
              (:sermon/title sermon)]])])
       [:div.mt-4
        [:a.text-sm.text-blue-600.hover:text-blue-800
         {:href "/admin/sermons"}
         "View all sermons →"]]]
      
      [:div.bg-white.border.border-gray-200.rounded-lg.p-4
       [:h3.text-lg.font-semibold.mb-4 "Recent Events"]
       (if (empty? recent-events)
         [:p.text-gray-500 "No events yet."]
         [:ul.space-y-2
          (for [event recent-events]
            [:li {:key (str (:xt/id event))}
             [:a.text-blue-600.hover:text-blue-800
              {:href (str "/admin/events/" (:xt/id event) "/edit")}
              (:event/title event)]])])
       [:div.mt-4
        [:a.text-sm.text-blue-600.hover:text-blue-800
         {:href "/admin/events"}
         "View all events →"]]]]]))

;; Route Handlers
(defn admin-page [ctx]
  (ui/page
   {:base/title "Admin Dashboard"}
   (admin-dashboard ctx)))

(defn sermons-page [ctx]
  (ui/page
   {:base/title "Manage Sermons"}
   (sermons-list ctx)))

(defn new-sermon-page [_ctx]
  (ui/page
   {:base/title "New Sermon"}
   [:div.max-w-4xl.mx-auto.p-6
    [:h1.text-3xl.font-bold.mb-6 "New Sermon"]
    (sermon-form {:action :create})]))

(defn edit-sermon-page [{:keys [path-params biff/db] :as ctx}]
  (let [sermon-id (parse-uuid (:id path-params))
        sermon (xt/entity db sermon-id)]
    (if sermon
      (ui/page
       {:base/title "Edit Sermon"}
       [:div.max-w-4xl.mx-auto.p-6
        [:h1.text-3xl.font-bold.mb-6 "Edit Sermon"]
        (sermon-form {:sermon sermon :action :edit})])
      {:status 404
       :body "Sermon not found"})))

(defn create-sermon [{:keys [params session] :as ctx}]
  (try
    (let [sermon-id (random-uuid)
          slug (slugify (:title params))
          sermon-data {:db/doc-type :sermon
                      :xt/id sermon-id
                      :sermon/user (:uid session)
                      :sermon/title (:title params)
                      :sermon/slug slug
                      :sermon/speaker (:speaker params)
                      :sermon/date (:date params)
                      :sermon/scripture-text (:scripture-text params)
                      :sermon/created-at :db/now
                      :sermon/updated-at :db/now}
          sermon-data (cond-> sermon-data
                        (:summary params) (assoc :sermon/summary (:summary params))
                        (:topic params) (assoc :sermon/topic (keyword (:topic params)))
                        (:series-name params) (assoc :sermon/series-name (:series-name params))
                        (:series-part params) (assoc :sermon/series-part (parse-long (:series-part params)))
                        (:video-url params) (assoc :sermon/video-url (:video-url params))
                        (:audio-url params) (assoc :sermon/audio-url (:audio-url params))
                        (:duration params) (assoc :sermon/duration (parse-long (:duration params)))
                        (:featured params) (assoc :sermon/featured true))]
      (biff/submit-tx ctx [sermon-data])
      {:status 303
       :headers {"location" "/admin/sermons"}})
    (catch Exception e
      (log/error e "Failed to create sermon")
      {:status 400
       :body "Failed to create sermon"})))

(defn update-sermon [{:keys [params session] :as ctx}]
  (try
    (let [sermon-id (parse-uuid (:id params))
          slug (slugify (:title params))
          sermon-data {:db/op :update
                      :db/doc-type :sermon
                      :xt/id sermon-id
                      :sermon/title (:title params)
                      :sermon/slug slug
                      :sermon/speaker (:speaker params)
                      :sermon/date (:date params)
                      :sermon/scripture-text (:scripture-text params)
                      :sermon/updated-at :db/now}
          sermon-data (cond-> sermon-data
                        (:summary params) (assoc :sermon/summary (:summary params))
                        (:topic params) (assoc :sermon/topic (keyword (:topic params)))
                        (:series-name params) (assoc :sermon/series-name (:series-name params))
                        (:series-part params) (assoc :sermon/series-part (parse-long (:series-part params)))
                        (:video-url params) (assoc :sermon/video-url (:video-url params))
                        (:audio-url params) (assoc :sermon/audio-url (:audio-url params))
                        (:duration params) (assoc :sermon/duration (parse-long (:duration params)))
                        (:featured params) (assoc :sermon/featured true))]
      (biff/submit-tx ctx [sermon-data])
      {:status 303
       :headers {"location" "/admin/sermons"}})
    (catch Exception e
      (log/error e "Failed to update sermon")
      {:status 400
       :body "Failed to update sermon"})))

(defn delete-sermon [{:keys [path-params] :as ctx}]
  (try
    (let [sermon-id (parse-uuid (:id path-params))]
      (biff/submit-tx ctx [{:db/op :delete :xt/id sermon-id}])
      {:status 303
       :headers {"location" "/admin/sermons"}})
    (catch Exception e
      (log/error e "Failed to delete sermon")
      {:status 400
       :body "Failed to delete sermon"})))

;; Event handlers (similar structure)
(defn events-page [ctx]
  (ui/page
   {:base/title "Manage Events"}
   (events-list ctx)))

(defn new-event-page [_ctx]
  (ui/page
   {:base/title "New Event"}
   [:div.max-w-4xl.mx-auto.p-6
    [:h1.text-3xl.font-bold.mb-6 "New Event"]
    (event-form {:action :create})]))

(defn edit-event-page [{:keys [path-params biff/db] :as ctx}]
  (let [event-id (parse-uuid (:id path-params))
        event (xt/entity db event-id)]
    (if event
      (ui/page
       {:base/title "Edit Event"}
       [:div.max-w-4xl.mx-auto.p-6
        [:h1.text-3xl.font-bold.mb-6 "Edit Event"]
        (event-form {:event event :action :edit})])
      {:status 404
       :body "Event not found"})))

(defn create-event [{:keys [params session] :as ctx}]
  (try
    (let [event-id (random-uuid)
          slug (slugify (:title params))
          event-data {:db/doc-type :event
                     :xt/id event-id
                     :event/user (:uid session)
                     :event/title (:title params)
                     :event/slug slug
                     :event/description (:description params)
                     :event/start-date (:start-date params)
                     :event/event-type (keyword (:event-type params))
                     :event/created-at :db/now
                     :event/updated-at :db/now}
          event-data (cond-> event-data
                       (:end-date params) (assoc :event/end-date (:end-date params))
                       (:contact-person params) (assoc :event/contact-person (:contact-person params))
                       (:contact-email params) (assoc :event/contact-email (:contact-email params))
                       (:contact-phone params) (assoc :event/contact-phone (:contact-phone params))
                       (:max-attendees params) (assoc :event/max-attendees (parse-long (:max-attendees params)))
                       (:registration-url params) (assoc :event/registration-url (:registration-url params))
                       (:registration-deadline params) (assoc :event/registration-deadline (:registration-deadline params))
                       (:registration-required params) (assoc :event/registration-required true)
                       (:featured params) (assoc :event/featured true)
                       (:all-day params) (assoc :event/all-day true))]
      (biff/submit-tx ctx [event-data])
      {:status 303
       :headers {"location" "/admin/events"}})
    (catch Exception e
      (log/error e "Failed to create event")
      {:status 400
       :body "Failed to create event"})))

(defn update-event [{:keys [params session] :as ctx}]
  (try
    (let [event-id (parse-uuid (:id params))
          slug (slugify (:title params))
          event-data {:db/op :update
                     :db/doc-type :event
                     :xt/id event-id
                     :event/title (:title params)
                     :event/slug slug
                     :event/description (:description params)
                     :event/start-date (:start-date params)
                     :event/event-type (keyword (:event-type params))
                     :event/updated-at :db/now}
          event-data (cond-> event-data
                       (:end-date params) (assoc :event/end-date (:end-date params))
                       (:contact-person params) (assoc :event/contact-person (:contact-person params))
                       (:contact-email params) (assoc :event/contact-email (:contact-email params))
                       (:contact-phone params) (assoc :event/contact-phone (:contact-phone params))
                       (:max-attendees params) (assoc :event/max-attendees (parse-long (:max-attendees params)))
                       (:registration-url params) (assoc :event/registration-url (:registration-url params))
                       (:registration-deadline params) (assoc :event/registration-deadline (:registration-deadline params))
                       (:registration-required params) (assoc :event/registration-required true)
                       (:featured params) (assoc :event/featured true)
                       (:all-day params) (assoc :event/all-day true))]
      (biff/submit-tx ctx [event-data])
      {:status 303
       :headers {"location" "/admin/events"}})
    (catch Exception e
      (log/error e "Failed to update event")
      {:status 400
       :body "Failed to update event"})))

(defn delete-event [{:keys [path-params] :as ctx}]
  (try
    (let [event-id (parse-uuid (:id path-params))]
      (biff/submit-tx ctx [{:db/op :delete :xt/id event-id}])
      {:status 303
       :headers {"location" "/admin/events"}})
    (catch Exception e
      (log/error e "Failed to delete event")
      {:status 400
       :body "Failed to delete event"})))

(def module
  {:routes ["/admin" {:middleware [mid/wrap-signed-in]}
            ["" {:get admin-page}]
            ["/sermons" {:get sermons-page}]
            ["/sermons/new" {:get new-sermon-page}]
            ["/sermons/create" {:post create-sermon}]
            ["/sermons/:id/edit" {:get edit-sermon-page}]
            ["/sermons/update" {:post update-sermon}]
            ["/sermons/:id/delete" {:post delete-sermon}]
            ["/events" {:get events-page}]
            ["/events/new" {:get new-event-page}]
            ["/events/create" {:post create-event}]
            ["/events/:id/edit" {:get edit-event-page}]
            ["/events/update" {:post update-event}]
            ["/events/:id/delete" {:post delete-event}]]})