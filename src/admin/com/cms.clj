(ns admin.com.cms
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [xtdb.api :as xt]))

;; Content type configurations
(def content-types
  {:sermon {:doc-type :sermon
            :required-fields [:sermon/title :sermon/slug :sermon/speaker :sermon/date :sermon/scripture-text]
            :api-path "/api/sermons"}
   :blog-entry {:doc-type :blog-entry
                :required-fields [:blog-entry/title :blog-entry/slug :blog-entry/author :blog-entry/content :blog-entry/publish-date]
                :api-path "/api/blog-entries"}
   :homepage-feature {:doc-type :homepage-feature
                      :required-fields [:homepage-feature/title :homepage-feature/content :homepage-feature/display-order :homepage-feature/image]
                      :api-path "/api/homepage-features"}
   :event {:doc-type :event
           :required-fields [:event/title :event/slug :event/description :event/start-date :event/event-type]
           :api-path "/api/events"}})

;; Helper functions
(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"content-type" "application/json"}
   :body (cheshire/generate-string data)})

(defn error-response [message & [status]]
  {:status (or status 400)
   :headers {"content-type" "application/json"}
   :body (cheshire/generate-string {:error message})})

(defn generate-slug [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-")))

(defn prepare-content-for-db [content-type params user-id]
  (let [id (random-uuid)
        now (java.util.Date.)
        doc-type (:doc-type (content-type content-types))]
    (-> params
        (assoc :db/doc-type doc-type
               :xt/id id
               (keyword (name doc-type) "user") user-id
               (keyword (name doc-type) "created-at") now
               (keyword (name doc-type) "updated-at") now)
        (update (keyword (name doc-type) "slug") #(or % (generate-slug (:title params)))))))

(defn update-content-for-db [content-type params]
  (let [doc-type (:doc-type (content-type content-types))
        now (java.util.Date.)]
    (-> params
        (assoc :db/op :update
               (keyword (name doc-type) "updated-at") now))))

;; Generic CRUD operations
(defn create-content [{:keys [session params path-params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        config (content-type content-types)]
    (if config
      (let [content-doc (prepare-content-for-db content-type params (:uid session))]
        (biff/submit-tx ctx [content-doc])
        (json-response {:id (:xt/id content-doc) :message "Content created successfully"} 201))
      (error-response "Invalid content type" 404))))

(defn get-content [{:keys [biff/db path-params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        content-id (parse-uuid (:content-id path-params))
        config (content-type content-types)]
    (if config
      (if-let [content (xt/entity db content-id)]
        (json-response content)
        (error-response "Content not found" 404))
      (error-response "Invalid content type" 404))))

(defn update-content [{:keys [biff/db session params path-params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        content-id (parse-uuid (:content-id path-params))
        config (content-type content-types)]
    (if config
      (if-let [existing (xt/entity db content-id)]
        (let [content-doc (-> params
                              (assoc :xt/id content-id)
                              (update-content-for-db content-type))]
          (biff/submit-tx ctx [content-doc])
          (json-response {:message "Content updated successfully"}))
        (error-response "Content not found" 404))
      (error-response "Invalid content type" 404))))

(defn delete-content [{:keys [biff/db path-params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        content-id (parse-uuid (:content-id path-params))
        config (content-type content-types)]
    (if config
      (if (xt/entity db content-id)
        (do
          (biff/submit-tx ctx [{:xt/id content-id :db/op :delete}])
          (json-response {:message "Content deleted successfully"}))
        (error-response "Content not found" 404))
      (error-response "Invalid content type" 404))))

(defn list-content [{:keys [biff/db path-params params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        config (content-type content-types)
        doc-type (:doc-type config)
        limit (min (or (some-> (:limit params) Integer/parseInt) 50) 100)
        offset (or (some-> (:offset params) Integer/parseInt) 0)]
    (if config
      (let [query-result (q db
                            '{:find (pull ?e [*])
                              :in [?doc-type ?limit ?offset]
                              :where [[?e :db/doc-type ?doc-type]]
                              :limit ?limit
                              :offset ?offset}
                            doc-type limit offset)]
        (json-response {:items query-result
                        :limit limit
                        :offset offset
                        :count (count query-result)}))
      (error-response "Invalid content type" 404))))

;; Public API routes (for frontend consumption)
(defn public-list-content [{:keys [biff/db path-params params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        config (content-type content-types)
        doc-type (:doc-type config)
        limit (min (or (some-> (:limit params) Integer/parseInt) 50) 100)
        offset (or (some-> (:offset params) Integer/parseInt) 0)
        featured-only (= "true" (:featured params))]
    (if config
      (let [base-query '{:find (pull ?e [*])
                         :in [?doc-type]
                         :where [[?e :db/doc-type ?doc-type]]}
            query (if featured-only
                    (case content-type
                      :sermon '{:find (pull ?e [*])
                                :in [?doc-type]
                                :where [[?e :db/doc-type ?doc-type]
                                        [?e :sermon/featured true]]}
                      :blog-entry '{:find (pull ?e [*])
                                    :in [?doc-type]
                                    :where [[?e :db/doc-type ?doc-type]
                                            [?e :blog-entry/featured true]]}
                      :event '{:find (pull ?e [*])
                               :in [?doc-type]
                               :where [[?e :db/doc-type ?doc-type]
                                       [?e :event/featured true]]}
                      base-query)
                    base-query)
            results (q db query doc-type)]
        (json-response {:items (take limit (drop offset results))
                        :limit limit
                        :offset offset
                        :total (count results)}))
      (error-response "Invalid content type" 404))))

(defn public-get-content [{:keys [biff/db path-params] :as ctx}]
  (let [content-type (keyword (:content-type path-params))
        slug (:slug path-params)
        config (content-type content-types)
        doc-type (:doc-type config)]
    (if config
      (let [slug-field (keyword (name doc-type) "slug")
            query-result (q db
                            '{:find (pull ?e [*])
                              :in [?doc-type ?slug ?slug-field]
                              :where [[?e :db/doc-type ?doc-type]
                                      [?e ?slug-field ?slug]]}
                            doc-type slug slug-field)]
        (if-let [content (first query-result)]
          (json-response content)
          (error-response "Content not found" 404)))
      (error-response "Invalid content type" 404))))

;; Module definition
(def module
  {:routes ["/admin" {:middleware [mid/wrap-signed-in]}
            ["/api/content/:content-type"
             ["" {:get list-content
                  :post create-content}]
             ["/:content-id"
              ["" {:get get-content
                   :put update-content
                   :delete delete-content}]]]]
   :api-routes [["/api/public/:content-type"
                 ["" {:get public-list-content}]
                 ["/by-slug/:slug" {:get public-get-content}]]]})