# Bulwark - Usage 

`bulwark` exposes a macro called `with-hystrix`, that takes a map of Hystrix configuration and executes the body 
in a Hystrix command. A similar macro `with-hystrix-async` is available that executes the body asynchronously and returns a derefable future.


```clojure

;; This returns "success".
(bulwark/with-hystrix {;; These three keys identify the Hystrix command 
                       ;; The group key is used to group commands together,
                       ;; for reporting/monitoring. We suggest having a
                       ;; single group key for all commands in your app.
                       :group-key                          "your-app"
                     
                       ;; The command key uniquely identifies the Hystrix command.                        
                       :command-key                        "foo-command"
                     
                       ;; The thread pool key identifies the thread pool
                       ;; to be used to execute your Hystrix command.
                       ;; If two commands have the same thread pool key, 
                       ;; they will share the same thread pool.
                       ;; We suggest using different thread pools for each command.
                       :thread-pool-key                    "foo-thread-pool"
                     
                       :thread-count                       2
                       :breaker-sleep-window-ms            500
                       :breaker-request-volume-threshold   200
                       :breaker-error-threshold-percentage 20
                       :execution-timeout-ms               100}
  "success")

;; Optionally, you can provide a fallback.
(let [fallback
      ;; These are the keys the fallback function is passed.
      (fn [{:keys [response-timed-out?                      
                   failed-execution-exception               ; If an exception was thrown, it is available here
                   circuit-breaker-open?                    
                   semaphore-rejected?                      ; See https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.semaphore.maxConcurrentRequests
                   thread-pool-rejected?                    
                   ]}]
        "failure")]

  ;; Runs the fallback (because of a timeout) and evaluates to its return value, i.e. "failure"
  (bulwark/with-hystrix {:group-key                          "your-app"
                         :command-key                        "foo-command"
                         :thread-pool-key                    "foo-thread-pool"
                         :thread-count                       2
                         :breaker-sleep-window-ms            500
                         :breaker-request-volume-threshold   200
                         :breaker-error-threshold-percentage 20
                         :execution-timeout-ms               100
                         :fallback-fn                        fallback}
                        (Thread/sleep 500)
    "success"))

;; You can run a command asynchronously. This returns a future.
;; The fallback-fn is optional.
;; Deref the future using @ or the deref function.
;; This returns "success".
(let [fallback (constantly "failure")]
  @(bulwark/with-hystrix-async {;; These three keys identify the Hystrix command 
                                ;; The group key is used to group commands together,
                                ;; for reporting/monitoring. We suggest having a
                                ;; single group key for all commands in your app.
                                :group-key                          "your-app"
                                
                                ;; The command key uniquely identifies the Hystrix command.                        
                                :command-key                        "foo-command"
                                
                                ;; The thread pool key identifies the thread pool
                                ;; to be used to execute your Hystrix command.
                                ;; If two commands have the same thread pool key, 
                                ;; they will share the same thread pool.
                                ;; We suggest using different thread pools for each command.
                                :thread-pool-key                    "foo-thread-pool"
                                
                                :thread-count                       2
                                :breaker-sleep-window-ms            500
                                :breaker-request-volume-threshold   200
                                :breaker-error-threshold-percentage 20
                                :execution-timeout-ms               100
                                :fallback-fn                        fallback}
     "success"))
```
