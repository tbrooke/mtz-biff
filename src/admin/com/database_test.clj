(ns admin.com.database-test
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [admin.com.ui :as ui]
            [rum.core :as rum]
            [xtdb.api :as xt]
            [clojure.tools.logging :as log]))

(defn test-db-connection [{:keys [biff/db] :as ctx}]
  "Test database connectivity and return status"
  (try
    (let [test-result (q db '{:find [count]
                             :where [[_ :xt/id _]]
                             :limit 1})]
      {:status :success
       :message "Database connection successful"
       :details {:query-result (count test-result)
                :db-type (type db)}})
    (catch Exception e
      {:status :error
       :message "Database connection failed"
       :details {:error (.getMessage e)}})))

(defn create-test-data [{:keys [session] :as ctx}]
  "Create sample test data to verify database operations"
  (try
    (let [test-user-id (random-uuid)
          test-sermon-id (random-uuid)
          test-event-id (random-uuid)]
      (biff/submit-tx ctx
        [;; Create test user
         {:db/doc-type :user
          :xt/id test-user-id
          :user/email "test@example.com"
          :user/joined-at :db/now
          :user/foo "test-foo-value"}
         
         ;; Create test sermon
         {:db/doc-type :sermon
          :xt/id test-sermon-id
          :sermon/user test-user-id
          :sermon/title "Test Sermon - Database Connection"
          :sermon/slug "test-sermon-db-connection"
          :sermon/speaker "Test Pastor"
          :sermon/date "2024-01-01"
          :sermon/scripture-text "John 3:16"
          :sermon/topic :faith
          :sermon/summary "This is a test sermon to verify database connectivity"
          :sermon/created-at :db/now
          :sermon/updated-at :db/now}
         
         ;; Create test event
         {:db/doc-type :event
          :xt/id test-event-id
          :event/user test-user-id
          :event/title "Test Event - Database Connection"
          :event/slug "test-event-db-connection"
          :event/description "This is a test event to verify database operations"
          :event/start-date "2024-02-01"
          :event/event-type :service
          :event/created-at :db/now
          :event/updated-at :db/now}])
      
      {:status :success
       :message "Test data created successfully"
       :details {:test-user-id test-user-id
                :test-sermon-id test-sermon-id
                :test-event-id test-event-id}})
    (catch Exception e
      {:status :error
       :message "Failed to create test data"
       :details {:error (.getMessage e)}})))

(defn query-test-data [{:keys [biff/db]}]
  "Query and display test data to verify database operations"
  (try
    (let [users (q db '{:find (pull user [*])
                       :where [[user :user/email]]
                       :limit 5})
          sermons (q db '{:find (pull sermon [*])
                         :where [[sermon :sermon/title]]
                         :limit 5})
          events (q db '{:find (pull event [*])
                        :where [[event :event/title]]
                        :limit 5})]
      {:status :success
       :message "Data queried successfully"
       :details {:users (count users)
                :sermons (count sermons) 
                :events (count events)
                :sample-data {:users (take 2 users)
                             :sermons (take 2 sermons)
                             :events (take 2 events)}}})
    (catch Exception e
      {:status :error
       :message "Failed to query test data"
       :details {:error (.getMessage e)}})))

(defn database-test-page [{:keys [session] :as ctx}]
  (let [connection-test (test-db-connection ctx)
        data-query (query-test-data ctx)]
    (ui/page
     {:base/title "Database Connection Test"}
     [:div.max-w-4xl.mx-auto.p-6
      [:h1.text-3xl.font-bold.mb-6 "Database Connection Test"]
      
      ;; Connection Status
      [:div.mb-8.p-4.border.rounded-lg
       {:class (if (= (:status connection-test) :success)
                 "bg-green-50 border-green-300"
                 "bg-red-50 border-red-300")}
       [:h2.text-xl.font-semibold.mb-2 "Connection Status"]
       [:p.text-lg {:class (if (= (:status connection-test) :success)
                             "text-green-800"
                             "text-red-800")}
        (:message connection-test)]
       [:details.mt-2
        [:summary.cursor-pointer "Details"]
        [:pre.mt-2.text-sm.bg-gray-100.p-2.rounded
         (pr-str (:details connection-test))]]]
      
      ;; Test Data Operations
      [:div.mb-8
       [:h2.text-xl.font-semibold.mb-4 "Database Operations Test"]
       [:div.flex.gap-4.mb-4
        (biff/form
         {:action "/database-test/create-data"
          :class "inline"}
         [:button.btn.bg-blue-500.hover:bg-blue-700.text-white.px-4.py-2.rounded
          {:type "submit"}
          "Create Test Data"])
        
        (biff/form
         {:action "/database-test/clear-data"
          :class "inline"}
         [:button.btn.bg-red-500.hover:bg-red-700.text-white.px-4.py-2.rounded
          {:type "submit"}
          "Clear Test Data"])]
       
       ;; Data Query Results
       [:div.p-4.border.rounded-lg.bg-gray-50
        [:h3.text-lg.font-semibold.mb-2 "Current Data in Database"]
        [:p.mb-2 {:class (if (= (:status data-query) :success)
                           "text-green-700"
                           "text-red-700")}
         (:message data-query)]
        (when (= (:status data-query) :success)
          (let [details (:details data-query)]
            [:div
             [:p.mb-2 (str "Users: " (:users details))]
             [:p.mb-2 (str "Sermons: " (:sermons details))]
             [:p.mb-2 (str "Events: " (:events details))]
             [:details.mt-2
              [:summary.cursor-pointer "Sample Data"]
              [:pre.mt-2.text-sm.bg-white.p-2.rounded.overflow-auto
               (with-out-str (clojure.pprint/pprint (:sample-data details)))]]]))]]])))

(defn create-test-data-handler [ctx]
  (let [result (create-test-data ctx)]
    (log/info "Create test data result:" result)
    {:status 303
     :headers {"location" "/database-test"}}))

(defn clear-test-data [{:keys [biff/db] :as ctx}]
  "Clear test data from database"
  (try
    ;; Query for test entities
    (let [test-entities (q db '{:find [id]
                               :where [[id :user/email "test@example.com"]]})
          test-sermons (q db '{:find [id]
                              :where [[id :sermon/title "Test Sermon - Database Connection"]]})
          test-events (q db '{:find [id]
                             :where [[id :event/title "Test Event - Database Connection"]]})]
      
      ;; Create retract transactions
      (when (seq (concat test-entities test-sermons test-events))
        (biff/submit-tx ctx
          (concat
           (map (fn [[id]] {:db/op :delete :xt/id id}) test-entities)
           (map (fn [[id]] {:db/op :delete :xt/id id}) test-sermons)
           (map (fn [[id]] {:db/op :delete :xt/id id}) test-events))))
      
      {:status :success
       :message "Test data cleared successfully"
       :details {:cleared-entities (+ (count test-entities)
                                     (count test-sermons)
                                     (count test-events))}})
    (catch Exception e
      {:status :error
       :message "Failed to clear test data"
       :details {:error (.getMessage e)}})))

(defn clear-test-data-handler [ctx]
  (let [result (clear-test-data ctx)]
    (log/info "Clear test data result:" result)
    {:status 303
     :headers {"location" "/database-test"}}))

(def module
  {:routes ["/database-test" {:middleware [mid/wrap-signed-in]}
            ["" {:get database-test-page}]
            ["/create-data" {:post create-test-data-handler}]
            ["/clear-data" {:post clear-test-data-handler}]]})