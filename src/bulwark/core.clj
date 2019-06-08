(ns bulwark.core
  (:require [com.netflix.hystrix.core :as hystrix])
  (:import (com.netflix.hystrix HystrixCommand HystrixCommand$Setter HystrixCommandProperties
                                HystrixThreadPoolProperties HystrixExecutable)
           (org.slf4j MDC)))

(defn- capture-logging-context [f]
  (let [context (MDC/getCopyOfContextMap)]
    (fn [& args]
      (try
        (when context
          (MDC/setContextMap context))
        (apply f args)
        (finally
          (MDC/clear))))))

(defn- init-fn [{:keys [thread-count
                        breaker-sleep-window-ms
                        breaker-error-threshold-percentage
                        execution-timeout-ms
                        request-volume-threshold]}]
  (fn [_ ^HystrixCommand$Setter setter]
    (-> setter
        (.andCommandPropertiesDefaults
         (-> (HystrixCommandProperties/Setter)
             (.withCircuitBreakerSleepWindowInMilliseconds (or breaker-sleep-window-ms 5000))
             (.withCircuitBreakerErrorThresholdPercentage (or breaker-error-threshold-percentage 50))
             (.withExecutionTimeoutInMilliseconds (or execution-timeout-ms 1000))
             (.withCircuitBreakerRequestVolumeThreshold (or request-volume-threshold 20))))
        (.andThreadPoolPropertiesDefaults
         (.withCoreSize (HystrixThreadPoolProperties/Setter) thread-count)))))

(defn- fallback-wrapper [fallback-fn]
  (capture-logging-context
   (fn []
     (let [^HystrixCommand command hystrix/*command*]
       (fallback-fn {:response-timed-out?        (.isResponseTimedOut command)
                     :failed-execution-exception (.getFailedExecutionException command)
                     :circuit-breaker-open?      (.isCircuitBreakerOpen command)
                     :semaphore-rejected?        (.isResponseSemaphoreRejected command)
                     :thread-pool-rejected?      (.isResponseThreadPoolRejected command)})))))

(defn- hystrix-conf [{:keys [group-key thread-pool-key command-key run-fn fallback-fn]
                      :as config}]
  (cond-> {:type            :command
           :group-key       (hystrix/group-key group-key)
           :command-key     (hystrix/command-key command-key)
           :thread-pool-key (hystrix/thread-pool-key (or thread-pool-key group-key))
           :init-fn         (init-fn (select-keys config [:thread-count
                                                          :breaker-sleep-window-ms
                                                          :breaker-error-threshold-percentage
                                                          :execution-timeout-ms
                                                          :request-volume-threshold]))
           :run-fn          (capture-logging-context run-fn)}
    (some? fallback-fn) (assoc :fallback-fn (fallback-wrapper fallback-fn))))

(defn execute-with-hystrix [key-map]
  (let [definition (hystrix-conf key-map)]
    (.execute ^HystrixExecutable (hystrix/instantiate* definition))))

(defmacro with-hystrix
  "Executes the given forms within a Hystrix command."
  [key-map & body]
  `(execute-with-hystrix (assoc ~key-map :run-fn (fn [] ~@body))))
