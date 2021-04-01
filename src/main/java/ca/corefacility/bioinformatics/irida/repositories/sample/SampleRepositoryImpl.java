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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Impl of custom methods for {@link SampleRepository}.  This class can be used for speed improvements for sample
 * listing methods.
 */
public class SampleRepositoryImpl implements SampleRepositoryCustom {
    private static final Logger logger = LoggerFactory.getLogger(SampleRepositoryImpl.class);
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
		String queryString = "select s.id FROM sample s INNER JOIN project_sample p ON p.sample_id=s.id INNER JOIN sample_metadata_entry AS sm ON s.id=sm.sample_id " +
			"INNER JOIN metadata_field AS mf ON sm.metadata_KEY =mf.id INNER JOIN metadata_entry AS me ON sm.metadata_id =me.id " +
			"WHERE p.project_id=:project AND mf.label='Sample_Code' AND me.value IN (:sampleCodes)";
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

	/**ISS
	 * {@inheritDoc}
	 */
 	public String getClusterIdByCodes(Project project, List<String> sampleCodes) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		String queryString = "SELECT mes.value FROM sample_metadata_entry AS smw INNER JOIN sample_metadata_entry AS sms ON smw.sample_id=sms.sample_id " + 
			"INNER JOIN metadata_field AS mfw ON smw.metadata_KEY=mfw.id INNER JOIN metadata_entry AS mew ON smw.metadata_id=mew.id " +
			"INNER JOIN metadata_field AS mfs ON sms.metadata_KEY=mfs.id INNER JOIN metadata_entry AS mes ON sms.metadata_id=mes.id " +
			"WHERE mfw.label='Sample_Code' AND mew.value IN (:sampleCodes) AND mfs.label='Cluster_Id' ORDER BY ABS(length(mes.value) - length(replace(mes.value, '_', ''))-1.4) LIMIT 1";
		parameters.addValue("sampleCodes", sampleCodes);
		String result = tmpl.queryForObject(queryString, parameters, String.class);
		return result;
	}

 	public Long getMasterProjectIdByCode(String sampleCode) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		String queryString = "SELECT s.organism FROM sample_metadata_entry AS smw INNER JOIN sample AS s ON smw.sample_id=s.id " +
			"INNER JOIN metadata_field AS mfw ON smw.metadata_KEY=mfw.id INNER JOIN metadata_entry AS mew ON smw.metadata_id=mew.id " +
			"WHERE mfw.label='Sample_Code' AND mew.value =:sampleCode";
		parameters.addValue("sampleCode", sampleCode);
		String result = tmpl.queryForObject(queryString, parameters, String.class);
		Long projectId = 48L;
		if(result.equals("Listeria monocytogenes")){
			projectId = 49L;
		}
		if(result.equals("Coronavirus")){
			projectId = 101L;
		}
		return projectId;
	}

 	public void setClusterIdByCode(Project project, List<String> sampleCodes, String clusterId) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		String queryString = "UPDATE metadata_entry AS mes INNER JOIN sample_metadata_entry AS sms ON mes.id=sms.metadata_id " +
			"INNER JOIN sample_metadata_entry AS smw ON sms.sample_id=smw.sample_id INNER JOIN metadata_field AS mfw ON smw.metadata_KEY=mfw.id " +
			"INNER JOIN metadata_entry AS mew ON smw.metadata_id=mew.id INNER JOIN metadata_field AS mfs ON sms.metadata_KEY=mfs.id " +
			"SET mes.value = :clusterId WHERE mfw.label='Sample_Code' AND mew.value IN (:sampleCodes) AND mfs.label='Cluster_Id'";
		parameters.addValue("sampleCodes", sampleCodes);
		parameters.addValue("clusterId", clusterId);

		int status = tmpl.update(queryString, parameters);
        if(status != 0){
            logger.debug("Updating cluster_id metadata for samples " + sampleCodes);
        } else {
            logger.debug("No sample found with sampleCodes " + sampleCodes);
        }
	}

 	public String getNextClusterId(Project project) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		String queryString = "SELECT MAX(CAST(SUBSTRING(mew.value,9) AS int))+1 FROM sample_metadata_entry AS smw " + 
			"INNER JOIN metadata_field AS mfw ON smw.metadata_KEY=mfw.id INNER JOIN metadata_entry AS mew ON smw.metadata_id=mew.id WHERE mfw.label='Cluster_Id'";
		String result = tmpl.queryForObject(queryString, parameters, String.class);
		return "Cluster_" + result;
	}

	public List<String> getRecipientsByCodes(Project project, List<String> sampleCodes, Boolean isAlert) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		String queryString;

		//query to read samples for a project
		if (isAlert) {
			queryString = "select DISTINCT u.email FROM project_sample ps INNER JOIN project_user pu ON pu.project_id=ps.project_id " +
				"INNER JOIN user u ON u.id=pu.user_id INNER JOIN sample_metadata_entry AS sm ON ps.sample_id=sm.sample_id " +
				"INNER JOIN metadata_field AS mf ON sm.metadata_KEY =mf.id INNER JOIN metadata_entry AS me ON sm.metadata_id =me.id " +
				"WHERE mf.label='Sample_Code' AND me.value IN (:sampleCodes)";
		} else {
			queryString = "select DISTINCT u.email FROM project_sample ps INNER JOIN project_user pu ON pu.project_id=ps.project_id " +
				"INNER JOIN user u ON u.id=pu.user_id INNER JOIN sample_metadata_entry AS sm ON ps.sample_id=sm.sample_id " +
				"INNER JOIN metadata_field AS mf ON sm.metadata_KEY =mf.id INNER JOIN metadata_entry AS me ON sm.metadata_id =me.id " +
				"WHERE mf.label='Sample_Code' AND u.system_role <>'ROLE_MANAGER' AND me.value IN (:sampleCodes)";
		}
		parameters.addValue("sampleCodes", sampleCodes);

		List<String> results = tmpl.query(queryString, parameters, new RowMapper<String>() {

			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String recipient = rs.getString("u.email");
				return recipient;
			}
		});

		return results;		
	}

	public List<Sample> getSamplesForClusterShallow(Project project, String sampleCode, String clusterId) {
		NamedParameterJdbcTemplate tmpl = new NamedParameterJdbcTemplate(dataSource);
		MapSqlParameterSource parameters = new MapSqlParameterSource();

		//query to read samples for a cluster
		String queryString;
		if (clusterId.contains("_ext")) {
			queryString = "select s.id, s.createdDate, s.modifiedDate, s.description, s.sampleName, s.collectedBy, s.geographicLocationName, s.isolate, s.isolationSource, s.latitude, s.longitude, s.organism, s.strain, s.collectionDate, null as remote_status " +
				"FROM sample_metadata_entry sme " +
				"INNER JOIN metadata_field mdf ON mdf.id = sme.metadata_KEY " +
				"INNER JOIN metadata_entry mde ON mde.id = sme.metadata_id " +
				"INNER JOIN sample s ON s.id = sme.sample_id " +                  
				"WHERE mde.value = :clusterId AND mdf.label = 'Cluster_Id'";
			parameters.addValue("clusterId", clusterId.substring(0, clusterId.length() - 4));
		} else if (clusterId.contains("Cluster_")) {
			queryString = "select s.id, s.createdDate, s.modifiedDate, s.description, s.sampleName, s.collectedBy, s.geographicLocationName, s.isolate, s.isolationSource, s.latitude, s.longitude, s.organism, s.strain, s.collectionDate, null as remote_status " +
				"FROM sample_metadata_entry sme " +
				"INNER JOIN metadata_field mdf ON mdf.id = sme.metadata_KEY " +
				"INNER JOIN metadata_entry mde ON mde.id = sme.metadata_id " +
				"INNER JOIN sample s ON s.id = sme.sample_id " +                  
				"WHERE mde.value = :clusterId AND mdf.label = 'Cluster_Id'";
			parameters.addValue("clusterId", clusterId);
		} else {
			queryString = "select s.id, s.createdDate, s.modifiedDate, s.description, s.sampleName, s.collectedBy, s.geographicLocationName, s.isolate, s.isolationSource, s.latitude, s.longitude, s.organism, s.strain, s.collectionDate, null as remote_status " +
				"FROM sample_metadata_entry sme " +
				"INNER JOIN metadata_field mdf ON mdf.id = sme.metadata_KEY " +
				"INNER JOIN metadata_entry mde ON mde.id = sme.metadata_id " +
				"INNER JOIN sample s ON s.id = sme.sample_id " +                  
				"WHERE mdf.label = 'Sample_Code' AND mde.value = :sampleCode";
			parameters.addValue("sampleCode", sampleCode);
		}

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

}
