package ca.corefacility.bioinformatics.irida.repositories.sample;

import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;

import java.util.List;

/**
 * Custom methods for getting {@link Sample}s.  This can be used to create custom, higher speed methods for specific
 * {@link Sample} related tasks.
 */
public interface SampleRepositoryCustom {
	/**
	 * Get the {@link Sample}s for a {@link Project} without extending into related objects.  Note: This method will not
	 * return things like metadata and should only be used for high-performance listing.
	 *
	 * @param project the {@link Project} to get samples for
	 * @return a list of {@link Sample}
	 */
	public List<Sample> getSamplesForProjectShallow(Project project);

    //ISS
	public List<Long> getSampleIdsByCodeInProject(Project project, List<String> sampleCodes);
	public String getClusterIdByCodes(Project project, List<String> sampleCodes);
	public Long getMasterProjectIdByCode(String sampleCode);
	public void setClusterIdByCode(Project project, List<String> sampleCodes, String clusterId);
	public String getNextClusterId(Project project);
	public List<String> getRecipientsByCodes(Project project, List<String> sampleCodes, Boolean isAlert);	
	public List<Sample> getSamplesForClusterShallow(Project project, String sampleCode, String clusterId);
}
