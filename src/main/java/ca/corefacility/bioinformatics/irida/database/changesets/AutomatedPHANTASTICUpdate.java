package ca.corefacility.bioinformatics.irida.database.changesets;

import ca.corefacility.bioinformatics.irida.config.data.IridaApiJdbcDataSourceConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Class to translate automated PHANTASTIC analyses into metadata entries for associated samples.
 */
public class AutomatedPHANTASTICUpdate implements CustomSqlChange {
	private static final Logger logger = LoggerFactory.getLogger(AutomatedPHANTASTICUpdate.class);

	// @formatter:off
	private static Map<String, String> PHANTASTIC_FIELDS = ImmutableMap.of(
		"information_name", "Sample_code",
		"qc_status", "QC_status",
		"qc_messages", "QC_messages"
	);
	// @formatter:on

	private DataSource dataSource;
	private Path outputFileDirectory;

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
		logger.info("The resource accessor is of type [" + resourceAccessor.getClass() + "]");
		final ApplicationContext applicationContext;
		if (resourceAccessor instanceof IridaApiJdbcDataSourceConfig.ApplicationContextAwareSpringLiquibase.ApplicationContextSpringResourceOpener) {
			applicationContext = ((IridaApiJdbcDataSourceConfig.ApplicationContextAwareSpringLiquibase.ApplicationContextSpringResourceOpener) resourceAccessor)
					.getApplicationContext();
		} else {
			applicationContext = null;
		}

		if (applicationContext != null) {
			logger.info("We're running inside of a spring instance, getting the existing application context.");
			this.dataSource = applicationContext.getBean(DataSource.class);
			this.outputFileDirectory = applicationContext.getBean("outputFileBaseDirectory", Path.class);

		} else {
			logger.error(
					"This changeset *must* be run from a servlet container as it requires access to Spring's application context.");
			throw new IllegalStateException(
					"This changeset *must* be run from a servlet container as it requires access to Spring's application context.");
		}
	}

	@Override
	public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
		logger.info("Reading existing automated PHANTASTIC results files to database.  This could take a while...");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		Map<String, Long> metadataHeaderIds = new HashMap<>();

		int errorCount = 0;

		// create the metadata headers
		PHANTASTIC_FIELDS.entrySet().forEach(e -> {
			GeneratedKeyHolder holder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					PreparedStatement statement = con
							.prepareStatement("INSERT INTO metadata_field (label, type) VALUES (?, 'text')",
									Statement.RETURN_GENERATED_KEYS);
					statement.setString(1, e.getValue());
					return statement;
				}
			}, holder);

			//save the metadata header ids
			metadataHeaderIds.put(e.getKey(), holder.getKey().longValue());
		});

		//get all the automated phantastic results
		List<PHANTASTICFileResult> phantasticFileResults = jdbcTemplate
				.query("select a.id, o.sample_id, of.file_path from sequencing_object s INNER JOIN analysis_submission a ON s.phantastic_typing=a.id INNER JOIN sample_sequencingobject o ON o.sequencingobject_id=s.id INNER JOIN analysis_output_file_map f ON f.analysis_id=a.analysis_id INNER JOIN analysis_output_file of ON f.analysisOutputFilesMap_id=of.id WHERE f.analysis_output_file_key='phantastic_out'",
						new RowMapper<PHANTASTICFileResult>() {
							@Override
							public PHANTASTICFileResult mapRow(ResultSet rs, int rowNum) throws SQLException {
								PHANTASTICFileResult phantasticFileResult = new PHANTASTICFileResult();
								phantasticFileResult.submissionId = rs.getLong(1);
								phantasticFileResult.sampleId = rs.getLong(2);
								phantasticFileResult.filePath = Paths.get(rs.getString(3));
								return phantasticFileResult;
							}
						});

		//for each phantastic result get the metadata
		for (PHANTASTICFileResult phantasticFileResult : phantasticFileResults) {
			Path filePath = outputFileDirectory.resolve(phantasticFileResult.filePath);

			if (!filePath.toFile().exists()) {
				logger.error("PHANTASTIC file " + filePath + " does not exist!");
				errorCount++;
			} else {

				try {
					//Read the JSON file from PHANTASTIC output
					@SuppressWarnings("resouce")
					String jsonFile = new Scanner(new BufferedReader(new FileReader(filePath.toFile())))
							.useDelimiter("\\Z").next();

					// map the results into a Map
					ObjectMapper mapper = new ObjectMapper();
					List<Map<String, Object>> phantasticResults = mapper
							.readValue(jsonFile, new TypeReference<List<Map<String, Object>>>() {
							});

					if (phantasticResults.size() > 0) {
						Map<String, Object> result = phantasticResults.get(0);

						//loop through each of the requested fields and save the entries
						PHANTASTIC_FIELDS.entrySet().forEach(e -> {
							if (result.containsKey(e.getKey()) && result.get(e.getKey()) != null) {
								String value = result.get(e.getKey()).toString();

								// insert to metadata_entry
								GeneratedKeyHolder holder = new GeneratedKeyHolder();
								jdbcTemplate.update(new PreparedStatementCreator() {
									@Override
									public PreparedStatement createPreparedStatement(Connection con)
											throws SQLException {
										PreparedStatement statement = con.prepareStatement(
												"INSERT INTO metadata_entry (type, value) VALUES ('text', ?)",
												Statement.RETURN_GENERATED_KEYS);
										statement.setString(1, value);
										return statement;
									}
								}, holder);

								//save the new entry id
								long entryId = holder.getKey().longValue();

								//insert the pipeline_metadata_entry
								jdbcTemplate
										.update("INSERT INTO pipeline_metadata_entry (id, submission_id) VALUES (?,?)",
												entryId, phantasticFileResult.submissionId);

								//remove existing entries for this metadata key and sample
								jdbcTemplate
										.update("DELETE FROM sample_metadata_entry WHERE sample_id=? AND metadata_KEY=?",
												phantasticFileResult.sampleId, metadataHeaderIds.get(e.getKey()));

								//associate with the sample
								jdbcTemplate
										.update("INSERT INTO sample_metadata_entry (sample_id, metadata_id, metadata_KEY) VALUES (?, ?,?)",
												phantasticFileResult.sampleId, entryId, metadataHeaderIds.get(e.getKey()));
							}
						});

					} else {
						logger.error("PHANTASTIC results for file are not correctly formatted: " + filePath);
					}

				} catch (IOException e) {
					logger.error("Error parsing JSON from PHANTASTIC results", e);
				}
			}

		}

		if (errorCount > 0) {
			logger.error("IRIDA could not read " + errorCount
					+ " automated PHANTASTIC result files to update sample metadata.  If these results are essential, check your file paths, restore a database backup, and retry the upgrade.");
		}

		return new SqlStatement[0];
	}

	@Override
	public String getConfirmationMessage() {
		return "Automated PHANTASTIC metadata updated.";
	}

	@Override
	public void setUp() throws SetupException {
		logger.info("Updating metadata for old automated PHANTASTIC runs");
	}

	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}

	/**
	 * Private class to store results of a query for submission, sample, and file path relationships
	 */
	private class PHANTASTICFileResult {
		Long submissionId;
		Long sampleId;
		Path filePath;
	}
}
