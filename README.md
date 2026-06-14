# Java Monitoring Project with Docker, Prometheus, Grafana and Jenkins CI/CD

## Project Overview

This project demonstrates a complete monitoring and CI/CD implementation using:

* Java Spring Boot Application
* Docker Containerization
* Prometheus Monitoring
* Grafana Dashboarding
* Jenkins CI/CD Pipeline
* GitHub Source Control
* WSL Ubuntu Development Environment

---

# Environment Details

## Host Environment

* Windows 11
* WSL2 Ubuntu
* Docker Desktop
* VS Code
* GitHub Repository

## Technology Stack

| Component   | Version          |
| ----------- | ---------------- |
| Java        | 17               |
| Spring Boot | 4.x              |
| Maven       | Latest           |
| Docker      | Docker Desktop   |
| Prometheus  | Latest           |
| Grafana     | Latest           |
| Jenkins     | Docker Container |
| GitHub      | Source Control   |

---

# Phase 1: Spring Boot Application Setup

## Objective

Create a simple Java application exposing:

* Home Endpoint
* Users Endpoint
* Orders Endpoint
* Health Endpoint
* Prometheus Metrics Endpoint

---

## Controller Implementation

File:

src/main/java/com/monitoring/controller/AppController.java

```java
@RestController
public class AppController {

    @Autowired
    private MeterRegistry meterRegistry;

    @GetMapping("/")
    public String home() {
        return "Java Monitoring App";
    }

    @GetMapping("/users")
    public String users() {
        return "Users Endpoint";
    }

    @GetMapping("/orders")
    public String orders() {

        meterRegistry.counter("orders_count").increment();

        return "Order Created";
    }
}
```

---

# Phase 2: Prometheus Integration

## Dependencies Added

pom.xml

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## Actuator Configuration

application.properties

```properties
management.endpoints.web.exposure.include=*
management.prometheus.metrics.export.enabled=true
management.endpoint.health.show-details=always
```

---

# Issue #1

## Problem

/orders endpoint returned:

```json
{
  "status":404
}
```

## Root Cause

Controller package:

```java
package com.monitoring.controller;
```

Application package:

```java
package com.monitoring.java_monitoring;
```

Spring Boot scanned only:

```text
com.monitoring.java_monitoring
```

and ignored:

```text
com.monitoring.controller
```

---

## Resolution

Updated:

```java
@SpringBootApplication(
    scanBasePackages = "com.monitoring"
)
```

---

# Phase 3: Dockerization

## Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
```

---

## Build Docker Image

```bash
mvn clean package

docker build -t java-monitoring .
```

---

## Run Container

```bash
docker run -d \
-p 8080:8080 \
--name java-app \
java-monitoring
```

---

# Issue #2

## Problem

```text
Conflict.
The container name "/java-app" is already in use
```

## Root Cause

Old container existed.

---

## Resolution

```bash
docker stop java-app

docker rm java-app
```

Then recreate:

```bash
docker run -d \
-p 8080:8080 \
--name java-app \
java-monitoring
```

---

# Phase 4: Custom Metrics

## Orders Counter

Metric added:

```java
meterRegistry.counter("orders_count")
             .increment();
```

---

## Verification

```bash
curl http://localhost:8080/orders
```

Check metrics:

```bash
curl http://localhost:8080/actuator/prometheus \
| grep orders_count
```

Output:

```text
orders_count_total 7.0
```

---

# Issue #3

## Problem

Prometheus metric not visible.

## Root Cause

Old Docker image still running.

Application changes were not rebuilt.

---

## Resolution

```bash
mvn clean package

docker build -t java-monitoring .

docker stop java-app

docker rm java-app

docker run -d \
-p 8080:8080 \
--name java-app \
java-monitoring
```

---

# Phase 5: Prometheus Setup

## Run Prometheus

```bash
docker run -d \
-p 9090:9090 \
--name prometheus \
-v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
prom/prometheus
```

---

## Validation

Prometheus UI:

```text
http://localhost:9090
```

Query:

```promql
orders_count_total
```

---

# Phase 6: Grafana Setup

## Run Grafana

```bash
docker run -d \
-p 3000:3000 \
--name grafana \
grafana/grafana
```

---

## Login

```text
admin
admin
```

---

## Dashboard Panels

### Orders Processed

```promql
orders_count_total
```

### JVM Memory

```promql
jvm_memory_used_bytes
```

### Request Count

```promql
http_server_requests_seconds_count
```

---

# Phase 7: Jenkins Setup

## Approach

Installed Jenkins using Docker.

Image:

```text
custom-jenkins
```

---

# Issue #4

## Problem

Jenkins apt repository failed.

```text
NO_PUBKEY 7198F4B714ABFC68
```

---

## Resolution

Abandoned native installation.

Used Dockerized Jenkins instead.

---

## Jenkins Container

```bash
docker run -d \
--name jenkins \
-p 8081:8080 \
-p 50000:50000 \
-v jenkins_home:/var/jenkins_home \
custom-jenkins
```

---

# Issue #5

## Problem

Docker unavailable inside Jenkins.

```text
docker: command not found
```

---

## Resolution

Created custom Jenkins image with:

* Git
* Maven
* Docker CLI

Installed.

Verification:

```bash
mvn -version

docker version
```

Successful.

---

# Issue #6

## Problem

Docker Build Stage Failed

```text
permission denied while trying to connect
to Docker daemon socket
```

---

## Root Cause

Docker socket mounted but Jenkins user lacked permission.

Socket:

```text
root:1001
```

Jenkins user:

```text
gid=1000
```

---

## Resolution

Recreated Jenkins container:

```bash
docker stop jenkins

docker rm jenkins
```

Started:

```bash
docker run -d \
--name jenkins \
--user root \
-p 8081:8080 \
-p 50000:50000 \
-v jenkins_home:/var/jenkins_home \
-v /var/run/docker.sock:/var/run/docker.sock \
custom-jenkins
```

Verification:

```bash
docker version
```

Worked successfully.

---

# Issue #7

## Problem

Pipeline failed before execution.

```text
fatal: not in a git directory
```

---

## Root Cause

Git repository ownership mismatch.

Git detected:

```text
dubious ownership
```

---

## Resolution

Inside Jenkins:

```bash
git config --global \
--add safe.directory '*'
```

---

# Issue #8

## Problem

Pipeline SCM loading failed.

Checkout stage:

```groovy
checkout scm
```

conflicted with:

```text
Pipeline Script from SCM
```

---

## Resolution

Removed explicit checkout stage.

Jenkins SCM handled checkout automatically.

---

# Final Jenkinsfile

```groovy
pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t java-monitoring:latest .'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                docker stop java-app || true
                docker rm java-app || true

                docker run -d \
                -p 8080:8080 \
                --name java-app \
                java-monitoring:latest
                '''
            }
        }
    }
}
```

---

# Final Validation

## Jenkins

Pipeline Status:

```text
SUCCESS
```

---

## Docker

```bash
docker ps
```

Container:

```text
java-app
```

running successfully.

---

## Application

```bash
curl http://localhost:8080/
```

Output:

```text
Java Monitoring App
```

---

## Metrics

```bash
curl http://localhost:8080/actuator/prometheus \
| grep orders_count
```

Output:

```text
orders_count_total 7.0
```

---

# Project Outcome

Successfully implemented:

* Spring Boot Application
* Docker Containerization
* Custom Metrics
* Prometheus Monitoring
* Grafana Dashboard
* Jenkins CI/CD Pipeline
* GitHub Integration
* Automated Build & Deployment

The application can now be rebuilt and deployed automatically through Jenkins whenever code changes are pushed to GitHub.

