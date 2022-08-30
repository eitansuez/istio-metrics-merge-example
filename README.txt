2022.08.30

This application is part of an exercise to comprehend how metrics merging works in Istio.
  See:  https://istio.io/latest/docs/ops/integrations/prometheus/

Description of metrics merging, as I understand it:

    The istio-agent process in the sidecar exposes the scrape endpoint to prometheus.
    This scrape endpoint is implemented by merging (or aggregating) the metrics from two sources:
    - the application's scrape endpoint
    - the envoy sidecar's scrape endpoint, which collects its own networking-related metrics.

Related: writeup by Rob Salmond, which is a little obfuscated, but worth a read:
  https://superorbital.io/journal/istio-metrics-merging/

  His blog entry references the metrics merging design doc:
    https://docs.google.com/document/d/1TTeN4MFmh4aUYYciR4oDBTtJsxl5-T5Tu3m3mGEdSo8/view

  The design doc mentions the construction of an "internal" environment variable ISTIO_PROMETHEUS_ANNOTATIONS derived from the information in the annotations, that tells the agent the url of the app/workload's scrape endpoint.

The convention appears to be as follows:
  On the app workload pod manifest, add the annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "<whatever port to use>"
    prometheus.io/path: "<whatever path to use>"

  And the istio agent will use that information to locate the application's scrape endpoint, and take care to aggregate the metrics together with envoy's collected metrics/scrape endpoint.

To test this:
1. create a hello-world style spring boot app with the micrometer prometheus dependency and check that you can hit that endpoint (hence this project)
2. construct a k8s deployment manifest for this app, and deploy the app to an istio-enabled k8s cluster.
3. deploy the prometheus add-on, or make sure that it's deployed
4. check the metrics in prometheus for the presence of the app workload metrics.
5. Locate and check the value of the ISTIO_PROMETHEUS_ANNOTATIONS environment variable inside the sidecar container.
