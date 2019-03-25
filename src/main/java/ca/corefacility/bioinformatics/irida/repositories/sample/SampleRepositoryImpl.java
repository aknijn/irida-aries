package ca.corefacility.bioinformatics.irida.repositories.sample;

import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

/**
 * Impl of custom methods for {@link SampleRepository}.  This class can be used for speed improvements for sample
 * listing methods.
 */
public class SampleRepositoryImpl implements SampleRepositoryCustom {
	private final DataSource dataSource;

	@Autowired
	public SampleRepositoryImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Sample> getSamplesForProjectShallow(Project project) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		//query to read samples for a project
		String queryString = "select s.id, s.createdDate, s.modifiedDate, s.description, s.sampleName, s.collectedBy, s.geographicLocationName, s.isolate, s.isolationSource, s.latitude, s.longitude, s.organism, s.strain, s.collectionDate, null as remote_status FROM sample s INNER JOIN project_sample p ON p.sample_id=s.id WHERE p.project_id=:project";

		parameters.addValue("project", project.getId());

		List<Sample> results = tmpl.query(queryString, parameters, new RowMapper<Sample>() {

			@Override
			public Sample mapRow(ResultSet rs, int rowNum) throws SQLException {
				Sample s = new Sample();

				s.setId(rs.getLong("s.id"));
				s.setCreatedDate(rs.getTimestamp("s.createdDate"));
				s.setModifiedDate(rs.getTimestamp("s.modifiedDate"));
				s.setDescription(rs.getString("s.description"));
				s.setSampleName(rs.getString("s.sampleName"));
				s.setCollectedBy(rs.getString("s.collectedBy"));
				s.setGeographicLocationName(rs.getString("s.geographicLocationName"));
				s.setIsolate(rs.getString("s.isolate"));
				s.setIsolationSource(rs.getString("s.isolationSource"));
				s.setLatitude(rs.getString("s.latitude"));
				s.setLongitude(rs.getString("s.longitude"));
				s.setOrganism(rs.getString("s.organism"));
				s.setStrain(rs.getString("s.strain"));
				s.setCollectionDate(rs.getDate("s.collectionDate"));

				return s;
			}
		});

		return results;
	}

	/**ISS
	 * {@inheritDoc}
	 */
 	public List<Long> getSampleIdsByCodeInProject(Project project, List<String> sampleCodes) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		//query to read samples for a project
		String queryString = "select s.id FROM sample s INNER JOIN project_sample p ON p.sample_id=s.id INNER JOIN sample_metadata_entry AS sm ON s.id=sm.sample_id INNER JOIN metadata_field AS mf ON sm.metadata_KEY =mf.id INNER JOIN metadata_entry AS me ON sm.metadata_id =me.id WHERE p.project_id=:project AND mf.label='Sample_Code' AND me.value in (:sampleCodes)";

		parameters.addValue("project", project.getId());
		parameters.addValue("sampleCodes", sampleCodes);

		List<Long> results = tmpl.query(queryString, parameters, new RowMapper<Long>() {

			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				//Sample s = new Sample();
				//s.setId(rs.getLong("s.id"));
				//Sample s = getSampleBySampleId(project, id);
                Long id = rs.getLong("s.id");
				return id;
			}
		});

		return results;
	}
}
