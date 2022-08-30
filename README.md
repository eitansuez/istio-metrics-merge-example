2022.08.30

This application is part of an exercise to comprehend how metrics works in Istio.

Specifically this exercise is designed to understand metrics merging.
See:  https://istio.io/latest/docs/ops/integrations/prometheus/

Description of metrics merging, as I understand it:

The istio-agent process in the sidecar exposes the scrape endpoint to prometheus.
This scrape endpoint is handled by merging or aggregating both the application's scrape endpoint and the envoy sidecar's scrape endpoint, which collects its own networking-related metrics.

Also, see this writeup by Rob Salmond, which is a little obfuscated, but worth a read:
  https://superorbital.io/journal/istio-metrics-merging/

  The blog entry references the metrics merging design doc:
    https://docs.google.com/document/d/1TTeN4MFmh4aUYYciR4oDBTtJsxl5-T5Tu3m3mGEdSo8/view

The convention appears to be as follows:
  on your app workload, add the annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "<whatever port to use>"
    prometheus.io/path: "<whatever path to use>"

The istio agent will take care of aggregating your workload scrape endpoint metrics together with envoy.

How to test this:
1. create a hello-world style spring boot app with the micrometer prometheus dependency and check that you can hit that endpoint
2. construct a k8s deployment manifest for this app, and deploy the app to an istio-enabled k8s cluster.
3. deploy the prometheus add-on, or make sure that it's deployed
4. check the metrics in prometheus for the presence of the app workload metrics.

Also, the design doc mentions the construction of an environment variable ISTIO_PROMETHEUS_ANNOTATIONS derived from the information in the annotations, that tells the agent the url of the app/workload's scrape endpoint.
See if can find this variable defined in the sidecar container.
