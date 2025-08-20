(ns admin.com.schema)

(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id                     :user/id]
          [:user/email                :string]
          [:user/joined-at            inst?]
          [:user/foo {:optional true} :string]
          [:user/bar {:optional true} :string]]

   :msg/id :uuid
   :msg [:map {:closed true}
         [:xt/id       :msg/id]
         [:msg/user    :user/id]
         [:msg/text    :string]
         [:msg/sent-at inst?]]

   ;; Church Content Types
   :sermon/id :uuid
   :sermon [:map {:closed true}
            [:xt/id                              :sermon/id]
            [:sermon/user                        :user/id]
            [:sermon/title                       :string]
            [:sermon/slug                        :string]
            [:sermon/speaker                     :string]
            [:sermon/date                        :string]
            [:sermon/scripture-text              :string]
            [:sermon/seo            {:optional true} :map]
            [:sermon/tags           {:optional true} :any]
            [:sermon/notes-pdf      {:optional true} :map]
            [:sermon/featured       {:optional true} :boolean]
            [:sermon/duration       {:optional true} :int]
            [:sermon/video-url      {:optional true} :string]
            [:sermon/topic          {:optional true} [:enum :salvation :faith :hope :love :discipleship :worship :prayer :service :missions :family :stewardship]]
            [:sermon/summary        {:optional true} :string]
            [:sermon/video-embed    {:optional true} :string]
            [:sermon/series-part    {:optional true} :int]
            [:sermon/audio-url      {:optional true} :string]
            [:sermon/transcript     {:optional true} :string]
            [:sermon/series-name    {:optional true} :string]
            [:sermon/created-at                      inst?]
            [:sermon/updated-at                      inst?]]

   :blog-entry/id :uuid
   :blog-entry [:map {:closed true}
                [:xt/id                                    :blog-entry/id]
                [:blog-entry/user                          :user/id]
                [:blog-entry/title                         :string]
                [:blog-entry/slug                          :string]
                [:blog-entry/author                        :string]
                [:blog-entry/content                       :any]
                [:blog-entry/publish-date                  :string]
                [:blog-entry/seo              {:optional true} :map]
                [:blog-entry/tags             {:optional true} :any]
                [:blog-entry/excerpt          {:optional true} :string]
                [:blog-entry/featured         {:optional true} :boolean]
                [:blog-entry/featured-image   {:optional true} :map]
                [:blog-entry/categories       {:optional true} [:enum :news :teaching :testimony :community :missions :youth :worship]]
                [:blog-entry/created-at                        inst?]
                [:blog-entry/updated-at                        inst?]]

   :homepage-feature/id :uuid
   :homepage-feature [:map {:closed true}
                      [:xt/id                                       :homepage-feature/id]
                      [:homepage-feature/user                       :user/id]
                      [:homepage-feature/title                      :string]
                      [:homepage-feature/content                    :string]
                      [:homepage-feature/display-order              :int]
                      [:homepage-feature/image                      :map]
                      [:homepage-feature/expires-at    {:optional true} :string]
                      [:homepage-feature/button-text   {:optional true} :string]
                      [:homepage-feature/button-style  {:optional true} [:enum :primary :secondary :outline]]
                      [:homepage-feature/button-url    {:optional true} :string]
                      [:homepage-feature/active        {:optional true} :boolean]
                      [:homepage-feature/subtitle      {:optional true} :string]
                      [:homepage-feature/created-at                     inst?]
                      [:homepage-feature/updated-at                     inst?]]

   :event/id :uuid
   :event [:map {:closed true}
           [:xt/id                                :event/id]
           [:event/user                           :user/id]
           [:event/title                          :string]
           [:event/slug                           :string]
           [:event/description                    :string]
           [:event/start-date                     :string]
           [:event/event-type                     [:enum :service :meeting :social :outreach :study :conference :retreat :fundraiser]]
           [:event/seo                {:optional true} :map]
           [:event/tags               {:optional true} :any]
           [:event/recurrence-pattern {:optional true} [:enum :weekly :monthly :yearly]]
           [:event/contact-email      {:optional true} [:re #".+@.+\..+"]]
           [:event/featured           {:optional true} :boolean]
           [:event/max-attendees      {:optional true} :int]
           [:event/contact-person     {:optional true} :string]
           [:event/registration-deadline {:optional true} :string]
           [:event/registration-url   {:optional true} :string]
           [:event/recurring          {:optional true} :boolean]
           [:event/featured-image     {:optional true} :map]
           [:event/end-date           {:optional true} :string]
           [:event/gallery            {:optional true} :map]
           [:event/all-day            {:optional true} :boolean]
           [:event/contact-phone      {:optional true} :string]
           [:event/cost               {:optional true} :map]
           [:event/location           {:optional true} :map]
           [:event/registration-required {:optional true} :boolean]
           [:event/created-at                          inst?]
           [:event/updated-at                          inst?]]

   ;; File uploads
   :upload/id :uuid
   :upload [:map {:closed true}
            [:xt/id                    :upload/id]
            [:upload/user              :user/id]
            [:upload/filename          :string]
            [:upload/original-filename :string]
            [:upload/content-type      :string]
            [:upload/size              :int]
            [:upload/url               :string]
            [:upload/uploaded-at       inst?]]})

(def module
  {:schema schema})
