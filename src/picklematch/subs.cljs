(ns picklematch.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :user
 (fn [db _]
   (:user db)))

(rf/reg-sub
 :games
 (fn [db _]
   (:games db)))

(rf/reg-sub
 :current-session-date
 (fn [db _]
   (:current-session-date db)))