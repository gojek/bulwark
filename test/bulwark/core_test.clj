(ns bulwark.core-test
  (:require [clojure.test :refer :all]
            [bulwark.core :as bulwark]
            [clojure.stacktrace :as stacktrace])
  (:import (com.netflix.hystrix.exception HystrixRuntimeException)
           (org.slf4j MDC)))

(defmacro catch-and-return
  [& body]
  `(try
     ~@body
     (catch Exception e#
       e#)))

(deftest with-hystrix-works
  (testing "Successful call"
    (let [fallback (fn [_]
                     "failure")
          result   (bulwark/with-hystrix {:group-key                          "foo"
                                          :command-key                        "foo"
                                          :thread-pool-key                    "foo"
                                          :thread-count                       2
                                          :breaker-sleep-window-ms            500
                                          :breaker-request-volume-threshold   20
                                          :breaker-error-threshold-percentage 20
                                          :execution-timeout-ms               100
                                          :fallback-fn                        fallback}
                     "success")]
      (is (= "success" result))))

  (testing "Successful call without a fallback"
    (let [result (bulwark/with-hystrix {:group-key                          "foo"
                                        :command-key                        "foo"
                                        :thread-pool-key                    "foo"
                                        :thread-count                       2
                                        :breaker-sleep-window-ms            500
                                        :breaker-request-volume-threshold   20
                                        :breaker-error-threshold-percentage 20
                                        :execution-timeout-ms               100}
                   "success")]
      (is (= "success" result))))

  (testing "Fallback when exception is thrown"
    (let [fallback (fn [{:keys [failed-execution-exception]}]
                     {:status    "failure"
                      :exception failed-execution-exception})
          result   (bulwark/with-hystrix {:group-key                          "bar"
                                          :command-key                        "bar"
                                          :thread-pool-key                    "bar"
                                          :thread-count                       2
                                          :breaker-sleep-window-ms            500
                                          :breaker-request-volume-threshold   20
                                          :breaker-error-threshold-percentage 20
                                          :execution-timeout-ms               100
                                          :fallback-fn                        fallback}
                     (throw (ex-info "foo" {:error "foo"}))
                     "success")]
      (is (= "failure" (:status result)))
      (is (= {:error "foo"}
             (-> result
                 :exception
                 ex-data)))))

  (testing "When an exception is thrown without a fallback"
    (let [exception (catch-and-return
                     (bulwark/with-hystrix {:group-key                          "bar"
                                            :command-key                        "bar"
                                            :thread-pool-key                    "bar"
                                            :thread-count                       2
                                            :breaker-sleep-window-ms            500
                                            :breaker-request-volume-threshold   20
                                            :breaker-error-threshold-percentage 20
                                            :execution-timeout-ms               100}
                       (throw (ex-info "foo" {:error "foo"}))
                       "success"))]
      (is (= {:error "foo"}
             (-> exception
                 stacktrace/root-cause
                 ex-data)))))

  (testing "Fallback when body times out"
    (let [fallback (fn [{:keys [response-timed-out?]}]
                     {:status              "failure"
                      :response-timed-out? response-timed-out?})
          result   (bulwark/with-hystrix {:group-key                          "goo"
                                          :command-key                        "goo"
                                          :thread-pool-key                    "goo"
                                          :thread-count                       2
                                          :breaker-sleep-window-ms            500
                                          :breaker-request-volume-threshold   20
                                          :breaker-error-threshold-percentage 20
                                          :execution-timeout-ms               100
                                          :fallback-fn                        fallback}
                     (Thread/sleep 200)
                     "success")]
      (is (= {:status              "failure"
              :response-timed-out? true}
             result))))

  (testing "When body times out without a fallback"
    (is (thrown-with-msg? HystrixRuntimeException #"goo timed-out.*"
                          (bulwark/with-hystrix {:group-key                          "goo"
                                                 :command-key                        "goo"
                                                 :thread-pool-key                    "goo"
                                                 :thread-count                       2
                                                 :breaker-sleep-window-ms            500
                                                 :breaker-request-volume-threshold   20
                                                 :breaker-error-threshold-percentage 20
                                                 :execution-timeout-ms               100}
                            (Thread/sleep 200)
                            "success")))))

(deftest with-hystrix-queue-works
  (testing "Successful call"
    (let [fallback (fn [_]
                     "failure")
          result   (bulwark/with-hystrix-async {:group-key                          "foo"
                                                :command-key                        "foo"
                                                :thread-pool-key                    "foo"
                                                :thread-count                       2
                                                :breaker-sleep-window-ms            500
                                                :breaker-request-volume-threshold   20
                                                :breaker-error-threshold-percentage 20
                                                :execution-timeout-ms               100
                                                :fallback-fn                        fallback}
                     {:status   "success"
                      :response "success"})]
      (is (= @result {:status   "success"
                      :response "success"}))))

  (testing "Successful call without a fallback"
    (let [result (bulwark/with-hystrix-async {:group-key                          "foo"
                                              :command-key                        "foo"
                                              :thread-pool-key                    "foo"
                                              :thread-count                       2
                                              :breaker-sleep-window-ms            500
                                              :breaker-request-volume-threshold   20
                                              :breaker-error-threshold-percentage 20
                                              :execution-timeout-ms               100}
                   {:status "success" :response "success"})]
      (is (= @result {:status   "success"
                      :response "success"}))))

  (testing "Fallback when exception is thrown"
    (let [fallback (fn [{:keys [failed-execution-exception]}]
                     {:status "failure" :response failed-execution-exception})
          result   (bulwark/with-hystrix-async {:group-key                          "bar"
                                                :command-key                        "bar"
                                                :thread-pool-key                    "bar"
                                                :thread-count                       2
                                                :breaker-sleep-window-ms            500
                                                :breaker-request-volume-threshold   20
                                                :breaker-error-threshold-percentage 20
                                                :execution-timeout-ms               100
                                                :fallback-fn                        fallback}
                     (throw (ex-info "foo" {:error "foo"}))
                     "success")]
      (is (= "failure"
             (:status @result)))
      (is (= {:error "foo"}
             (ex-data (:response @result))))))

  (testing "When an exception is thrown without a fallback"
    (let [exception (try @(bulwark/with-hystrix-async {:group-key                          "bar"
                                                       :command-key                        "bar"
                                                       :thread-pool-key                    "bar"
                                                       :thread-count                       2
                                                       :breaker-sleep-window-ms            500
                                                       :breaker-request-volume-threshold   20
                                                       :breaker-error-threshold-percentage 20
                                                       :execution-timeout-ms               100}
                            (throw (ex-info "foo" {:error "foo"}))
                            "success")
                         (catch Exception e
                           e))]
      (is (= {:error "foo"}
             (-> exception
                 stacktrace/root-cause
                 ex-data)))))

  (testing "Fallback when body times out"
    (let [fallback (fn [{:keys [response-timed-out?]}]
                     {:status              "failure"
                      :response-timed-out? response-timed-out?})
          result (bulwark/with-hystrix-async {:group-key                          "goo"
                                              :command-key                        "goo"
                                              :thread-pool-key                    "goo"
                                              :thread-count                       2
                                              :breaker-sleep-window-ms            500
                                              :breaker-request-volume-threshold   20
                                              :breaker-error-threshold-percentage 20
                                              :execution-timeout-ms               100
                                              :fallback-fn                        fallback}
                                             (Thread/sleep 200)
                                             "success")]
      (is (= {:status              "failure"
              :response-timed-out? true}
             @result))))

  (testing "When body times out without a fallback"
    (let [exception (catch-and-return
                      @(bulwark/with-hystrix-async {:group-key                          "goo"
                                                    :command-key                        "goo"
                                                    :thread-pool-key                    "goo"
                                                    :thread-count                       2
                                                    :breaker-sleep-window-ms            500
                                                    :breaker-request-volume-threshold   20
                                                    :breaker-error-threshold-percentage 20
                                                    :execution-timeout-ms               100}
                                                   (Thread/sleep 200)
                                                   "success"))]
      (is (instance? HystrixRuntimeException (.getCause exception)))
      (is (re-matches #"goo timed-out.*" (-> exception
                                             (.getCause)
                                             (.getMessage)))))))

(defn with-mdc [f]
  (MDC/setContextMap {"a" "1"})
  (is (= "1" (MDC/get "a")))
  (try (f) (finally (MDC/clear))))

(use-fixtures :once with-mdc)

(deftest test-fallback-function
  (testing "should not clear MDC if running on same thread"
    (let [main-thread (.getName (Thread/currentThread))
          fallback-fn (bulwark/capture-logging-context
                        (fn [] (let [fallback-thread (.getName (Thread/currentThread))]
                                 (is (= main-thread fallback-thread)))))]
      (fallback-fn))
    (is (= "1" (MDC/get "a"))))

  (testing "should clear MDC if fallback is running on different thread"
    (let [main-thread (.getName (Thread/currentThread))
          fallback-fn (bulwark/capture-logging-context
                        (fn [] (let [fallback-thread (.getName (Thread/currentThread))]
                                 (is (not= main-thread fallback-thread)))))
          agnt (agent {})]
      (send-off agnt (fn [state]
                       (fallback-fn)
                       (is (nil? (MDC/get "a")))
                       (assoc state :done true)))
      (await agnt)))
  (is (= "1" (MDC/get "a"))))
