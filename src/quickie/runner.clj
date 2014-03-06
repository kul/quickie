(ns quickie.runner
  (:require [clojure.test :as test]
            [clansi.core :as clansi]
            [clojure.string :as string]))

(defn out-str-result [f]
  (let [string-writer (new java.io.StringWriter)]
     (binding [test/*test-out* string-writer]
       (let [result (f)]
         {:output (-> (str string-writer)
                      (string/split #"\n")
                      butlast)
          :result result}))))

(def matchers
  [#"^quickie\.runner"
   #"^quickie\.autotest"
   #"^user\$eval"
   #"^clojure\.lang"
   #"^clojure\.test"
   #"^clojure\.core"
   #"^clojure\.main"
   #"^java\.lang"
   #"^java\.util\.concurrent\.ThreadPoolExecutor\$Worker"])

(defn needed-line [line]
  (let [line-trimmed (string/trim line)
        needed       (not-any? #(re-seq % line-trimmed) matchers)]
    {:line   line
     :needed needed}))

(def group-size 5)
(def prefix-size (long (/ group-size 2)))

(defn group-lines [lines]
  (let [matched-lines     (map needed-line lines)
        prefix            (repeat prefix-size nil)
        partionable-lines (concat prefix matched-lines prefix)]
    (partition group-size 1 partionable-lines)))

(defn filter-lines [lines]
  (let [groups       (group-lines lines)
        needed-line? (fn [group] (some :needed group))
        needed-lines (filter needed-line? groups)]
    (->> (map #(nth % prefix-size) needed-lines)
         (map :line))))

(defn print-pass [result]
  (-> (:output result)
      last
      println)
  (println (clansi/style "   All Tests Passing!   " :black :bg-green)))

(defn print-fail [result]
  (let [{:keys [error fail]} (:result result)
        lines               (:output result)]
    (->> lines
         filter-lines
         (string/join "\n")
         println)
    (prn "\007")
    (println (clansi/style (str "   " error " errors and " fail " failures   ") :black :bg-red))))

(defn print-result [result]
  (let [{:keys [error fail]} (:result result)]
    (if (= 0 (+ error fail))
      (print-pass result)
      (print-fail result))))

(defn run [project]
  (try
    (let [matcher (:test-matcher project #"test")
          result  (out-str-result #(test/run-all-tests matcher))]
      (print-result result))
    (catch Exception e 
      (do
        (println (.getMessage e))
        (.printStackTrace e)))))

