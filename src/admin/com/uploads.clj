(ns admin.com.uploads
  (:require [com.biffweb :as biff :refer [q]]
            [admin.com.middleware :as mid]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [xtdb.api :as xt])
  (:import [java.io File]
           [java.nio.file Files Paths StandardCopyOption]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"content-type" "application/json"}
   :body (cheshire/generate-string data)})

(defn error-response [message & [status]]
  {:status (or status 400)
   :headers {"content-type" "application/json"}
   :body (cheshire/generate-string {:error message})})

(defn allowed-file-type? [content-type]
  (re-matches #"image/(jpeg|jpg|png|gif|webp)|application/pdf|audio/(mpeg|mp3|wav)|video/(mp4|webm)" content-type))

(defn generate-filename [original-filename]
  (let [timestamp (System/currentTimeMillis)
        uuid (str (random-uuid))
        extension (when original-filename
                    (last (str/split original-filename #"\.")))]
    (str timestamp "-" uuid (when extension (str "." extension)))))

(defn save-file [file upload-dir]
  (let [temp-file (:tempfile file)
        filename (generate-filename (:filename file))
        upload-path (str upload-dir "/" filename)
        target-file (io/file upload-path)]
    (.mkdirs (.getParentFile target-file))
    (Files/copy (.toPath temp-file) 
                (.toPath target-file)
                (into-array StandardCopyOption [StandardCopyOption/REPLACE_EXISTING]))
    {:filename filename
     :original-filename (:filename file)
     :content-type (:content-type file)
     :size (:size file)
     :url (str "/uploads/" filename)}))

(defn upload-file [{:keys [session multipart-params] :as ctx}]
  (let [file (get multipart-params "file")
        upload-dir "resources/public/uploads"]
    (cond
      (nil? file)
      (error-response "No file provided")
      
      (not (allowed-file-type? (:content-type file)))
      (error-response "File type not allowed")
      
      (> (:size file) (* 10 1024 1024)) ; 10MB limit
      (error-response "File too large (max 10MB)")
      
      :else
      (try
        (let [file-info (save-file file upload-dir)
              file-id (random-uuid)]
          ;; Store file metadata in database
          (biff/submit-tx ctx
            [{:db/doc-type :upload
              :xt/id file-id
              :upload/user (:uid session)
              :upload/filename (:filename file-info)
              :upload/original-filename (:original-filename file-info)
              :upload/content-type (:content-type file-info)
              :upload/size (:size file-info)
              :upload/url (:url file-info)
              :upload/uploaded-at :db/now}])
          (json-response (assoc file-info :id file-id) 201))
        (catch Exception e
          (error-response (str "Upload failed: " (.getMessage e)) 500))))))

(defn list-uploads [{:keys [biff/db session params] :as ctx}]
  (let [limit (min (or (some-> (:limit params) Integer/parseInt) 50) 100)
        offset (or (some-> (:offset params) Integer/parseInt) 0)
        user-id (:uid session)]
    (let [uploads (q db
                     '{:find (pull ?e [*])
                       :in [?user-id ?limit ?offset]
                       :where [[?e :db/doc-type :upload]
                               [?e :upload/user ?user-id]]
                       :limit ?limit
                       :offset ?offset}
                     user-id limit offset)]
      (json-response {:items uploads
                      :limit limit
                      :offset offset
                      :count (count uploads)}))))

(defn delete-upload [{:keys [biff/db session path-params] :as ctx}]
  (let [upload-id (parse-uuid (:upload-id path-params))
        upload (xt/entity db upload-id)]
    (cond
      (nil? upload)
      (error-response "Upload not found" 404)
      
      (not= (:upload/user upload) (:uid session))
      (error-response "Unauthorized" 403)
      
      :else
      (try
        ;; Delete file from filesystem
        (let [file-path (str "resources/public" (:upload/url upload))]
          (when (.exists (io/file file-path))
            (.delete (io/file file-path))))
        
        ;; Delete from database
        (biff/submit-tx ctx [{:xt/id upload-id :db/op :delete}])
        (json-response {:message "Upload deleted successfully"})
        
        (catch Exception e
          (error-response (str "Delete failed: " (.getMessage e)) 500))))))

;; Module definition
(def module
  {:routes ["/admin" {:middleware [mid/wrap-signed-in]}
            ["/api/uploads"
             ["" {:get list-uploads
                  :post upload-file}]
             ["/:upload-id" {:delete delete-upload}]]]})