(ns compojure.api.json
  "Pimped version of https://github.com/weavejester/lein-ring.
   Might use https://github.com/ngrunwald/ring-middleware-format later."
  (:require [ring.util.response :refer [content-type]]
            [cheshire.core :as cheshire]
            [clojure.walk :as walk]
            [clojure.java.io :as io]))

(defn json-request?
  "Checks from request content-type weather it's JSON."
  [{:keys [content-type] :as request}]
  (and
    content-type
    (not (empty? (re-find #"^application/(vnd.+)?json" content-type)))))

(defn json-request-support
  [handler & [{:keys [keywords?] :or {keywords? true}}]]
  (fn [{:keys [character-encoding content-type body] :as request}]
    (handler
      (if-not (and body (json-request? request))
        request
        (let [json (cheshire/parse-stream (io/reader body :encoding (or character-encoding "utf-8")) keywords?)]
          (cond
            (sequential? json) (-> request
                                 (assoc :body (vec json))
                                 (assoc :body-params (vec json)))
            (map? json) (-> request
                          (assoc :body json)
                          (assoc :body-params json)
                          (assoc :json-params json)
                          (update-in [:params] merge json))
            :else request))))))

(defn json-response-support
  [handler]
  (fn [request]
    (let [{:keys [body] :as response} (handler request)]
      (if (coll? body)
        (-> response
          (content-type "application/json; charset=utf-8")
          (update-in [:body] cheshire/generate-string))
        response))))

(defn json-support
  [handler]
  (-> handler json-request-support json-response-support))