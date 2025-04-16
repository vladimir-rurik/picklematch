(ns picklematch.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :page
 (fn [db _]
   (:page db)))

(rf/reg-sub
 :user
 (fn [db _]
   (:user db)))

(rf/reg-sub
 :games
 (fn [db _]
   (:games db)))

(rf/reg-sub
 :players
 (fn [db _]
   (:players db)))

(rf/reg-sub
 :current-session-date
 (fn [db _]
   (:current-session-date db)))
