2022.08.30

# Hello boot / test metrics

This application is part of an exercise to comprehend how [metrics merging](https://istio.io/latest/docs/ops/integrations/prometheus/) works in Istio.

## Description of metrics merging (as I understand it)

The istio-agent process in the sidecar exposes the scrape endpoint to prometheus.

This scrape endpoint is implemented by merging (or aggregating) the metrics from two sources:

- the application's scrape endpoint
- the envoy sidecar's scrape endpoint, which collects its own networking-related metrics.

### Related

[Writeup by Rob Salmond](https://superorbital.io/journal/istio-metrics-merging/), which is a little obfuscated, but worth a read.

His blog entry references the [metrics merging design doc](https://docs.google.com/document/d/1TTeN4MFmh4aUYYciR4oDBTtJsxl5-T5Tu3m3mGEdSo8/view).

The design doc mentions the construction of an "internal" environment variable ISTIO_PROMETHEUS_ANNOTATIONS derived from the information in the annotations, that tells the agent the url of the app/workload's scrape endpoint.

## How to communicate the url of the app scrape endpoint

The convention appears to be as follows:

On the app workload pod manifest, add the annotations:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/actuator/prometheus/"
```

The istio-agent will use that information to locate the application's scrape endpoint, and take care to aggregate the metrics together with envoy's collected metrics/scrape endpoint.

## Building the app

1. Produce a local image named `test-metrics:<version>` (the version is specified in the gradle build file), tag it and push it to a local registry (assumption: using a local k8s cluster configured with a local registry exposed to k8s on port 5000 and exposed on localhost on `<port>`, see [how to do this with k3d](https://k3d.io/v5.4.6/usage/registries/#create-a-dedicated-registry-together-with-your-cluster)).

    ```
    gradle bootBuildImage --imageName=localhost:<port>/test-metrics:<version> --publishImage
    ```

## To test this

1. Create a hello-world style spring boot app with the micrometer prometheus dependency and check that you can hit that endpoint (this project).

2. Deploy the Istio Prometheus add-on.

    ```shell
    k apply -f ${ISTIO_DIR}/samples/addons/prometheus.yaml
    ```

3. Construct a k8s deployment manifest for this app (see manifests/hello-boot.yaml), and deploy the app to an istio-enabled k8s cluster.

    ```shell
    k apply -f manifests/hello-boot.yaml
    ```

4. Verify that the workload metrics endpoint indeed functions.

    ```shell
    k exec -it helloboot-v1-<tab> -c istio-proxy -- curl localhost:8080/actuator/prometheus
    ```

5. Check envoy's prometheus scrape endpoint.

    ```shell
    k exec -it helloboot-v1-<tab> -c istio-proxy -- curl localhost:15090/stats/prometheus
    ```

6. Locate and check the value of the ISTIO_PROMETHEUS_ANNOTATIONS environment variable inside the sidecar container.

    ```shell
    k exec -it helloboot-v1-<tab> -c istio-proxy -- /bin/sh
    echo $ISTIO_PROMETHEUS_ANNOTATIONS
    ```

    The output should look like this:

    ```json
    {"scrape":"true","path":"/actuator/prometheus","port":"8080"}
    ```

8. Verify that the aggregated endpoint response includes both envoy and app metrics

    ```shell
    k exec -it helloboot-v1-585487d5f7-9d27j -c istio-proxy -- curl localhost:15020/stats/prometheus
    ```

9. Give it a couple of minutes, and check the prometheus dashboard for the presence of app workload metrics, e.g. `http_server_requests_seconds_count` or `jvm_memory_max_bytes` (or any custom metrics you choose to expose in your app via micrometer).

    ```shell
    istioctl dashboard prometheus
    ```

