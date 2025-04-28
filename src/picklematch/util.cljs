(ns picklematch.util)

(defn format-date-obj-to-iso-str
  "Formats a JavaScript Date object into a YYYY-MM-DD string based on local time."
  [date-obj]
  (when date-obj
    (let [year (.getFullYear date-obj)
          month-raw (inc (.getMonth date-obj)) ; Month is 0-indexed
          day-raw (.getDate date-obj)
          month (if (< month-raw 10) (str "0" month-raw) (str month-raw))
          day (if (< day-raw 10) (str "0" day-raw) (str day-raw))]
      (str year "-" month "-" day))))
