(ns maria.persistence.firebase
  (:require [re-view.core :as v :refer [defview]]
            [maria.persistence.tokens :as tokens]
            [re-db.d :as d]
            [goog.object :as gobj]
            [maria.persistence.local :as local]
            [maria.persistence.github :as github]))

(def firebase-auth (.auth js/firebase))

(.onAuthStateChanged firebase-auth (fn [user]
                                     (d/transact! (if-let [{:keys [displayName uid providerData]} (some-> user (.toJSON) (js->clj :keywordize-keys true))]
                                                    (let [github-id (get-in providerData [0 :uid])]
                                                      (github/get-username github-id (fn [{:keys [value error]}]
                                                                                       (if value (d/transact! [{:db/id     :auth-public
                                                                                                                :username  value
                                                                                                                :maria-url (str "/gists/" value)}])
                                                                                                 (.error js/console "Unable to retrieve GitHub username" error))))
                                                      [{:db/id        :auth-public
                                                        :display-name displayName
                                                        :id           github-id
                                                        :signed-in?   true}
                                                       (merge {:db/id         :auth-secret
                                                               :uid           uid
                                                               :provider-data providerData})])
                                                    [[:db/retract-entity :auth-public]
                                                     [:db/retract-entity :auth-secret]]))))


(def providers {:github (doto (js/firebase.auth.GithubAuthProvider.)
                          (.addScope "gist"))})

(defn sign-in [provider]
  (->
    (.signInWithPopup firebase-auth (get providers provider))
    (.then (fn [result]
             (tokens/put-token provider (-> result
                                            (gobj/get "credential")
                                            (gobj/get "accessToken")))))))

(defn sign-out []
  (local/local-put "auth/accessToken" nil)
  (.signOut firebase-auth))