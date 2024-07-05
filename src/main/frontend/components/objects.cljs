(ns frontend.components.objects
  "Tagged objects"
  (:require [frontend.components.views :as views]
            [frontend.db :as db]
            [frontend.db-mixins :as db-mixins]
            [frontend.db.async :as db-async]
            [frontend.db.model :as db-model]
            [frontend.handler.editor :as editor-handler]
            [frontend.mixins :as mixins]
            [frontend.state :as state]
            [logseq.outliner.property :as outliner-property]
            [promesa.core :as p]
            [rum.core :as rum]))

(defn- get-all-objects
  [class]
  (->> (:block/_tags class)
       (map (fn [row] (assoc row :id (:db/id row))))))

(defn- add-new-object!
  [table class]
  (p/let [block (editor-handler/api-insert-new-block! ""
                                                      {:page (:block/uuid class)
                                                       :properties {:block/tags (:db/id class)}
                                                       :edit-block? false})
          set-data! (get-in table [:data-fns :set-data!])
          _ (set-data! (get-all-objects (db/entity (:db/id class))))]
    (editor-handler/edit-block! (db/entity [:block/uuid (:block/uuid block)]) 0 :unknown-container)))

(rum/defc objects-inner < rum/static
  [config class view-entity object-ids]
  (let [[data set-data!] (rum/use-state [])
        properties (outliner-property/get-class-properties class)
        columns (views/build-columns config properties)]
    (rum/use-effect!
     (fn []
       (p/let [_result (db-async/<get-tag-objects (state/get-current-repo) (:db/id view-entity))]
         (set-data! (get-all-objects (db/entity (:db/id view-entity))))))
     [object-ids])
    (views/view view-entity {:data data
                             :set-data! set-data!
                             :columns columns
                             :add-new-object! #(add-new-object! view-entity class)})))

(rum/defcs objects < rum/reactive db-mixins/query mixins/container-id
  [state class]
  [:div.ml-2
   (let [config {:container-id (:container-id state)}
         object-ids (db-model/get-class-objects (state/get-current-repo) (:db/id class))
         ;; TODO: create the default table view for this class
         view-entity class]
     (objects-inner config class view-entity object-ids))])
