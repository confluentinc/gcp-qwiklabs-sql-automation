# GCP Qwiklabs Sql Automation Kafka Streams App

This repository provide a KStreams application for use with Google Qwiklabs Workshop: **AI-Powered SQL Automation & Real-Time Data Streaming with Confluent and Google Cloud.**

The purpose of the application is to stream SQL queries from a Kafka topic, execute the queries on a BigQuery dataset, and then return the raw results to a Kafka topic.

## Running the Application

1. Clone the Github Repo

```bash
git clone https://github.com/confluentinc/gcp-qwiklabs-sql-automation.git
```

```bash
cd gcp-qwiklabs-sql-automation
```

2. Set the following environment variables for Google and Confluent Cloud:

```bash
export GCP_PROJECT_ID="<YOUR_GCP_PROJECT_ID>"
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
java -jar target/gcp-qwiklabs-sql-automation-0.0.1-SNAPSHOT.jar
```

This repository is part of the Confluent organization on GitHub.
It is public and open to contributions from the community.

Please see the LICENSE file for contribution terms.
Please see the CHANGELOG.md for details of recent updates.
