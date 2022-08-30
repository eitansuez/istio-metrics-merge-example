package com.eitan.testmetrics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestMetricsApplication

fun main(args: Array<String>) {
	runApplication<TestMetricsApplication>(*args)
}
