(ns tasks
  (:require [com.biffweb.tasks :as tasks]
            [nrepl.server :as nrepl]
            [cider.nrepl :refer [cider-middleware]]))

(defn hello
  "Says 'Hello'"
  []
  (println "Hello"))

(defn start-nrepl
  "Start nREPL server with CIDER middleware on port 7888"
  []
  (let [server (nrepl/start-server 
                 :port 7888
                 :handler (apply nrepl/default-handler cider-middleware))]
    (println "nREPL server started on port 7888 with CIDER middleware")
    (spit ".nrepl-port" "7888")
    server))

;; Tasks should be vars (#'hello instead of hello) so that `clj -M:dev help` can
;; print their docstrings.
(def custom-tasks
  {"hello" #'hello
   "nrepl" #'start-nrepl})

(def tasks (merge tasks/tasks custom-tasks))
