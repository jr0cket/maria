(ns maria.pages.live-layout
  (:require [re-view.core :as v :refer [defview]]
            [maria.views.floating.tooltip :as tooltip]
            [re-db.d :as d]
            [maria.commands.which-key :as which-key]
            [maria.eval :as e]
            [maria.repl-specials]
            [maria.views.cards :as repl-ui]
            [cljs.core.match :refer-macros [match]]
            [maria.views.floating.float-ui :as hint]
            [maria.views.bottom-bar :as dock]
            [maria.pages.docs :as docs]
            [maria.persistence.local :as local]
            [maria.views.top-bar :as toolbar]
            [maria.views.icons :as icons]
            [maria.curriculum :as curriculum]
            [commands.exec :as exec]
            [maria.commands.doc :as doc]))

(defonce _
         (e/on-load #(d/transact! [[:db/add :repl/state :eval-log [{:id    (d/unique-id)
                                                                    :value (repl-ui/plain [:span.gray "Ready."])}]]])))

(defn last-n [n v]
  (subvec v (max 0 (- (count v) n))))

(defn doc-list-section [docs title]
  [:div
   [:.sans-serif.mh3.mv2 title]
   [:.bg-white.ma3
    (docs/doc-list docs)]])

(defview home
  [this]
  (let [username (d/get :auth-public :username)]
    [:div
     (toolbar/doc-toolbar {})

     (-> doc/modules
         (doc-list-section "Learning Modules"))

     (some-> (doc/locals-dir :local/recents)
             (doc-list-section "Recent"))

     (some-> (seq (doc/user-gists username))
             (doc-list-section "My gists"))]))

(defview layout
  [{:keys []}]
  [:.cursor-text.bg-light-gray.overflow-hidden.w-100.relative.sans-serif
   {:on-click #(when (= (.-target %) (.-currentTarget %))
                 (exec/exec-command-name :navigate/focus-end))
    :style    {:min-height     "100%"
               :padding-bottom 40}}
   (hint/display-hint)
   [:.relative.border-box.flex.flex-column.w-100
    (when-let [segments (d/get :router/location :segments)]

      (match segments
             ["home"] (home)
             ["gist" id filename] (docs/file-edit {:id       id
                                                   :filename filename})
             ["gists" username] (docs/gists-list username)

             ["local"] (docs/doc-list (doc/user-gists "local"))
             ["local" id] (docs/file-edit {:id     id
                                           :local? true})

             ))]

   (which-key/show-commands)
   (dock/BottomBar {})
   (tooltip/Tooltip)])