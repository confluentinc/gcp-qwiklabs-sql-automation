package io.confluent.kstreams;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.confluent.kstreams.ExecuteQuery.DATASET;
import static io.confluent.kstreams.ExecuteQuery.PROJECT_ID;
import static io.confluent.kstreams.ExecuteQuery.display;

@Slf4j
public class BigQueryClient {

    final BigQuery bigquery;
    public BigQueryClient() {
        // Initialize client that will be used to send requests.
        bigquery = BigQueryOptions.getDefaultInstance().getService();
    }

    public String runQuery(String query) throws InterruptedException {
        log.info(display, "Preparing to execute query in BigQuery");
        // Sanitize query to ensure valid SQL syntax.
        String cleanQuery = sanitizeQuery(query);
        log.info(display, "Sanitized query to run: {}", cleanQuery);
        // Create the query job configuration
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(cleanQuery).setDefaultDataset(DATASET).build();
        // Execute the query
        Job queryJob = runQueryJob(queryConfig);
        log.info(display, "Query execution completed");
        // Get the results.
        TableResult result = queryJob.getQueryResults();
        log.info(display, "Getting results");
        // Log the results for each row
        result.iterateAll().forEach(values -> {
            List<String> rowResults = new ArrayList<>();
            values.forEach(value-> rowResults.add(value.getStringValue()));
            log.info(display, "Result: {}", rowResults);
        });

        // Format the results.
        return formatResults(result, cleanQuery);
    }

    public String sanitizeQuery(String query) {
        return query.replace("```sql", "").replace("```", "").trim();
    }

    public Job runQueryJob (QueryJobConfiguration queryConfig) {
        // Create a unique job with job ID
        UUID uuid = UUID.randomUUID();
        String jobName = "qwiklabs_execute_sql_query_" + uuid;
        JobId jobId = JobId.newBuilder().setProject(PROJECT_ID).setJob(jobName).build();
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        log.info(display, "Built query job with ID: {}", jobId.toString());

        // Wait for the query to complete.
        try {
            queryJob = queryJob.waitFor();
            log.info(display, "Running query");
        } catch (InterruptedException e){
            log.error(display, "Query failed to complete due to error: {}", String.valueOf(e));
        }

        // Check for errors.
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getExecutionErrors() != null && !queryJob.getStatus().getExecutionErrors().isEmpty()) {
            List<String> errorMessages = getErrorMessage(queryJob);
            throw new RuntimeException(String.valueOf(errorMessages));
        }
        return queryJob;
    }

    private static List<String> getErrorMessage(Job queryJob) {
        List<String> errorMessages = new java.util.ArrayList<>();
        List<BigQueryError> errors = queryJob.getStatus().getExecutionErrors();
        for (BigQueryError e : errors) {
            errorMessages.add(e.getMessage());
        }
        return errorMessages;
    }

    private static String formatResults(TableResult results, String cleanQuery) {
        // Format the SQL query and raw TableResult as a JSON string
        JSONObject queryResults = new JSONObject();
        queryResults.put("sqlQuery", cleanQuery);
        queryResults.put("rawResults", results);
        String formattedResults = queryResults.toString();
        log.info(display, "Returning results");
        return formattedResults;
    }

}