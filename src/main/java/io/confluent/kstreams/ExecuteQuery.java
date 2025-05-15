package io.confluent.kstreams;

import com.google.cloud.bigquery.BigQueryException;
import io.confluent.common.utils.TestUtils;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.avro.generated.record.raw_results_value;
import org.apache.flink.avro.generated.record.sql_query_value;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class ExecuteQuery {
    // Get required env variables.
    static final String PROJECT_ID = System.getenv("GOOGLE_CLOUD_PROJECT_ID");
    static final String DATASET = Optional.ofNullable(System.getenv("BIGQUERY_DATASET_ID")).orElse("stocks");
    static final String QUERY_TOPIC =  Optional.ofNullable(System.getenv("QUERY_TOPIC")).orElse("sql_query");
    static final String RESULTS_TOPIC = Optional.ofNullable(System.getenv("RESULTS_TOPIC")).orElse("raw_results");
    static final String BOOTSTRAP_SERVER = System.getenv("BOOTSTRAP_SERVER");
    static final String KAFKA_API_KEY = System.getenv("KAFKA_API_KEY");
    static final String KAFKA_API_SECRET = System.getenv("KAFKA_API_SECRET");
    static final String SCHEMA_REGISTRY_URL = System.getenv("SCHEMA_REGISTRY_URL");
    static final String SCHEMA_REGISTRY_KEY = System.getenv("SCHEMA_REGISTRY_KEY");
    static final String SCHEMA_REGISTRY_SECRET = System.getenv("SCHEMA_REGISTRY_SECRET");

    static BigQueryClient bigquery;
    // Use marker and apply filter in log4j2.xml to display only selected logs to the console
    static Marker display = MarkerFactory.getMarker("DISPLAY");

    public static void main(final String[] args) {
        log.info(display, "Application started");

        // Validate that all required env variables have been set.
        checkEnvVariablesAreSet();

        // Configure the KStreams application.
        final Properties streamsConfiguration = getStreamsConfiguration();

        try {
            log.info(display, "Initializing BigQuery Client");
            bigquery = new BigQueryClient();

            // Define the processing topology of the Streams application.
            final StreamsBuilder builder = new StreamsBuilder();
            executeBQStream(builder);
            final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);

            // Clean local state prior to starting the processing topology.
            streams.cleanUp();
            // Now run the processing topology via `start()` to begin processing its input data.
            streams.start();

            // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        } catch (BigQueryException e) {
            log.error(display, "Application failed due to error: {}", String.valueOf(e));
        }

    }

    private static void checkEnvVariablesAreSet() {
        log.info(display, "Checking environment variables");

        Map<String, String> envVars = new HashMap<>();
        List<String> varsNotSet = new ArrayList<>();

        envVars.put("GOOGLE_CLOUD_PROJECT_ID", PROJECT_ID);
        envVars.put("BIGQUERY_DATASET_ID", DATASET);
        envVars.put("QUERY_TOPIC", QUERY_TOPIC);
        envVars.put("RESULTS_TOPIC", RESULTS_TOPIC);
        envVars.put("BOOTSTRAP_SERVER", BOOTSTRAP_SERVER);
        envVars.put("KAFKA_API_KEY", KAFKA_API_KEY);
        envVars.put("KAFKA_API_SECRET", KAFKA_API_SECRET);
        envVars.put("SCHEMA_REGISTRY_URL", SCHEMA_REGISTRY_URL);
        envVars.put("SCHEMA_REGISTRY_KEY", SCHEMA_REGISTRY_KEY);
        envVars.put("SCHEMA_REGISTRY_SECRET", SCHEMA_REGISTRY_SECRET);

        for (Map.Entry<String, String> var : envVars.entrySet()) {
            if ( var.getValue() == null || var.getValue().trim().isEmpty()) {
                varsNotSet.add(var.getKey());
            }
        }

        if (!varsNotSet.isEmpty()) {
            log.error(display, "Unable to run. Please set the following environment variables {}", varsNotSet);
            System.exit(1);
        }
    }

    /**
     * Configure the Streams application.
     *
     * @return Properties getStreamsConfiguration
     */
    static Properties getStreamsConfiguration() {
        log.info(display, "Configuring KStreams application");

        final Properties streamsConfiguration = new Properties();

        // A unique identifier for the stream processing application.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "google-cloud-lab-sql-automation" + PROJECT_ID);
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "pie_labs|google-cloud-lab-sql-automation|" + PROJECT_ID);
        // A list of host/port pairs used to establish the initial connection to the Kafka cluster.
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);

        // Security settings - SASL/PLAIN authentication with Kafka API Key and Secret
        streamsConfiguration.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        streamsConfiguration.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        final String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                + KAFKA_API_KEY + "\" password=\"" + KAFKA_API_SECRET + "\";";
        streamsConfiguration.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);

        // Confluent Schema Registry Connection
        streamsConfiguration.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        final String basicAuthCredentialsSource = "USER_INFO";
        streamsConfiguration.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, basicAuthCredentialsSource);
        streamsConfiguration.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, SCHEMA_REGISTRY_KEY + ":" + SCHEMA_REGISTRY_SECRET);


        // The default Serializer/Deserializer classes for record keys and values
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.consumerPrefix("auto.offset.reset"), "earliest");

        // The location for the temporary directory where Kafka Streams will store state.
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getAbsolutePath());

        return streamsConfiguration;
    }

    /**
     * Executes the SQL Query in BigQuery
     *
     * @return String the query result
     */
    static String getQueryResults(String query) throws InterruptedException {
        return bigquery.runQuery(query);
    }

    /**
     * Define the processing topology for executing the SQL queries on BigQuery.
     *
     * @param builder StreamsBuilder to use
     */
    static void executeBQStream(final StreamsBuilder builder) {
        // Read the source stream
        builder.<String, sql_query_value>stream(QUERY_TOPIC)
                // Sanitize the output by removing null record values.
                .filter((key, sqlQuery) -> sqlQuery.getSqlQuery() != null)
                // Map the value
                .mapValues(sqlQuery -> {
                    String query = sqlQuery.getSqlQuery().toString();
                    try {
                        // Execute the query in BigQuery to get the result
                        // Map the id, text_query and query_results against the sql_result_value class generated by Avro
                        // Set the query id as the key and the sqlResult object as the value
                        return raw_results_value.newBuilder()
                                .setId(sqlQuery.getId())
                                .setTextQuery(sqlQuery.getTextQuery())
                                .setQueryResults(getQueryResults(query))
                                .build();
                    } catch (InterruptedException e) {
                        log.error(display, "Unable to run query due to error: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                // Write to the result topic
                .to(RESULTS_TOPIC);
    }
}