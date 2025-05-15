# Google Cloud SQL Automation Kafka Streams App

This repository provide a KStreams application for use with the Google Cloud Lab: **SQL Automation and Real-Time Data Streaming with Confluent and Vertex AI**.

The purpose of the application is to stream SQL queries from a Kafka topic, execute the queries on a BigQuery dataset, and then return the raw results to a Kafka topic.

## Requirements

Participants must have successfully completed Tasks 1-5 of the lab to run this application.

For this task, access to the Confluent Schema Registry is required. Please create a Schema Registry API Key and Secret before proceeding.

## Running the Application

1. Clone the Github Repo

```bash
git clone https://github.com/confluentinc/google-cloud-lab-sql-automation.git
```

```bash
cd google-cloud-lab-sql-automation
```

2. Set the following environment variables for Google Cloud and Confluent Cloud:

```bash
export GOOGLE_CLOUD_PROJECT_ID="<YOUR_GOOGLE_CLOUD_PROJECT_ID>"
export KAFKA_API_KEY="<YOUR_KAFKA_API_KEY>"
export KAFKA_API_SECRET="<YOUR_KAFKA_API_SECRET>"
export BOOTSTRAP_SERVER="<YOUR_BOOTSTRAP_SERVER>"
export SCHEMA_REGISTRY_URL="<YOUR_SCHEMA_REGISTRY_URL>"
export SCHEMA_REGISTRY_KEY="<YOUR_SCHEMA_REGISTRY_API_KEY>"
export SCHEMA_REGISTRY_SECRET="<YOUR_SCHEMA_REGISTRY_API_SECRET>"
```

3. Authenticate with gcloud

```bash
gcloud auth application-default login
```

3. Run the following command to create the JAR file

```bash
./mvnw clean install
```

4. Run the JAR file

```bash
java -jar target/google-cloud-lab-sql-automation-0.1.0.jar
```

This repository is part of the Confluent organization on GitHub.
It is public and open to contributions from the community.

Please see the LICENSE file for contribution terms.
Please see the CHANGELOG.md for details of recent updates.
