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
 :players
 (fn [db _]
   (:players db)))

(rf/reg-sub
 :selected-date
 (fn [db _]
   (:selected-date db)))

(rf/reg-sub
 :loading?
 (fn [db _]
   (:loading? db)))
