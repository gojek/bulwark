(ns bulwark.core-test
  (:require [clojure.test :refer :all]
            [bulwark.core :as bulwark]))

(deftest with-hystrix-works
  (testing "Successful call"
    (let [fallback (fn [_]
                     "failure")
          result (bulwark/with-hystrix {:group-key                          "foo"
                                        :command-key                        "foo"
                                        :thread-pool-key                    "foo"
                                        :thread-count                       2
                                        :breaker-sleep-window-ms            500
                                        :breaker-error-threshold-percentage 20
                                        :execution-timeout-ms               100
                                        :fallback-fn                        fallback}
                                       "success")]
      (is (= result "success"))))

  (testing "Fallback when exception is thrown"
    (let [failed-exception (promise)
          fallback (fn [{:keys [failed-execution-exception]}]
                     (deliver failed-exception failed-execution-exception)
                     "failure")
          result (bulwark/with-hystrix {:group-key                          "bar"
                                        :command-key                        "bar"
                                        :thread-pool-key                    "bar"
                                        :thread-count                       2
                                        :breaker-sleep-window-ms            500
                                        :breaker-error-threshold-percentage 20
                                        :execution-timeout-ms               100
                                        :fallback-fn                        fallback}
                                       (throw (ex-info "foo" {:error "foo"}))
                                       "success")]
      (is (= result "failure"))
      (is (= (ex-data @failed-exception)
             {:error "foo"}))))

  (testing "Fallback when body times out"
    (let [timed-out? (promise)
          fallback (fn [{:keys [response-timed-out?]}]
                     (deliver timed-out? response-timed-out?)
                     "failure")
          result (bulwark/with-hystrix {:group-key                          "goo"
                                        :command-key                        "goo"
                                        :thread-pool-key                    "goo"
                                        :thread-count                       2
                                        :breaker-sleep-window-ms            500
                                        :breaker-error-threshold-percentage 20
                                        :execution-timeout-ms               100
                                        :fallback-fn                        fallback}
                                       (Thread/sleep 200)
                                       "success")]
      (is (= result "failure"))
      (is @timed-out?))))

(deftest with-hystrix-queue-works
  (testing "Successful call"
    (let [fallback (fn [_]
                     "failure")
          result (bulwark/with-hystrix-queue {:group-key                          "foo"
                                              :command-key                        "foo"
                                              :thread-pool-key                    "foo"
                                              :thread-count                       2
                                              :breaker-sleep-window-ms            500
                                              :breaker-error-threshold-percentage 20
                                              :execution-timeout-ms               100
                                              :fallback-fn                        fallback}
                                             {:status "success" :response "success"})]
      (is (= (:status @result) "success"))
      (is (= (:response @result) "success"))))

  (testing "Fallback when exception is thrown"
    (let [fallback (fn [{:keys [failed-execution-exception]}]
                     {:status "failure" :response failed-execution-exception}
                     )
          result (bulwark/with-hystrix-queue {:group-key                          "bar"
                                              :command-key                        "bar"
                                              :thread-pool-key                    "bar"
                                              :thread-count                       2
                                              :breaker-sleep-window-ms            500
                                              :breaker-error-threshold-percentage 20
                                              :execution-timeout-ms               100
                                              :fallback-fn                        fallback}
                                             (throw (ex-info "foo" {:error "foo"}))
                                             "success")]
      (is (= (:status @result) "failure"))
      (is (= (ex-data (:response @result))
             {:error "foo"}))))

  (testing "Fallback when body times out"
    (let [fallback (fn [{:keys [response-timed-out?]}]
                     {:status "failure" :response response-timed-out?})
          result (bulwark/with-hystrix-queue {:group-key                          "goo"
                                              :command-key                        "goo"
                                              :thread-pool-key                    "goo"
                                              :thread-count                       2
                                              :breaker-sleep-window-ms            500
                                              :breaker-error-threshold-percentage 20
                                              :execution-timeout-ms               100
                                              :fallback-fn                        fallback}
                                             (Thread/sleep 200)
                                             "success")]
      (is (= (:status @result) "failure"))
      (is (:response @result)))))
