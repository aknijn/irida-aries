package ca.corefacility.bioinformatics.irida.pipeline.results.updater.impl;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.PostProcessingException;
import ca.corefacility.bioinformatics.irida.model.sample.MetadataTemplateField;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.PipelineProvidedMetadataEntry;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisOutputFile;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.AnalysisType;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.BuiltInAnalysisTypes;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.pipeline.results.updater.AnalysisSampleUpdater;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

//ISS
import ca.corefacility.bioinformatics.irida.service.EmailController;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.model.user.Role;
import ca.corefacility.bioinformatics.irida.repositories.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.corefacility.bioinformatics.irida.repositories.joins.project.ProjectSampleJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.joins.project.ProjectUserJoinRepository;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.enums.ProjectRole;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import java.io.FileNotFoundException;

/**
 * {@link AnalysisSampleUpdater} that adds a number of results from a RECOVERY run to the metadata of a {@link Sample}
 */
@Component
public class RECOVERYSampleUpdater implements AnalysisSampleUpdater {
    private static final Logger logger = LoggerFactory.getLogger(RECOVERYSampleUpdater.class);
	private static final String RECOVERY_FILE = "recovery_type";

	private MetadataTemplateService metadataTemplateService;
	private IridaWorkflowsService iridaWorkflowsService;
	private SampleService sampleService;

    //ISS
    private EmailController emailController;
    private UserRepository userRepository;
	private final ProjectSampleJoinRepository psjRepository;
	private final ProjectUserJoinRepository pujRepository;
	private final ProjectService projectService;


	// @formatter:off
	private static Map<String, String> RECOVERY_FIELDS = ImmutableMap.<String,String>builder()
		.put("information_name", "Sample_code")
		.put("qc_status", "QC_status")
		.put("region", "Regione")
		.put("year", "Anno")
		.put("lineage", "Lineage")
		.put("ORF1ab", "ORF1ab")
		.put("S-protein", "S-protein")
		.put("ORF3a", "ORF3a")
		.put("E-protein", "E-protein")
		.put("M-protein", "M-protein")
		.put("ORF6", "ORF6")
		.put("ORF7a", "ORF7a")
		.put("ORF7b", "ORF7b")
		.put("ORF8", "ORF8")
		.put("N-protein", "N-protein")
		.put("ORF10", "ORF10")
		//.put("Intergenic", "Intergenic")
		
		.build();
	// @formatter:on

	@Autowired
	public RECOVERYSampleUpdater(MetadataTemplateService metadataTemplateService, SampleService sampleService,
							  IridaWorkflowsService iridaWorkflowsService, EmailController emailController,
                              ProjectSampleJoinRepository psjRepository, ProjectUserJoinRepository pujRepository,
							  ProjectService projectService) {
		this.metadataTemplateService = metadataTemplateService;
		this.sampleService = sampleService;
		this.iridaWorkflowsService = iridaWorkflowsService;

		this.psjRepository = psjRepository;
		this.pujRepository = pujRepository;
		this.projectService = projectService;
        this.emailController = emailController;
	}

	/**
	 * Add RECOVERY results to the metadata of the given {@link Sample}s
	 *
	 * @param samples  The samples to update.
	 * @param analysis the {@link AnalysisSubmission} to apply to the samples
	 * @throws PostProcessingException if the method cannot read the "recovery_out" output file
	 */
	@Override
	public void update(Collection<Sample> samples, AnalysisSubmission analysis) throws PostProcessingException {
		AnalysisOutputFile recoveryFile = analysis.getAnalysis().getAnalysisOutputFile(RECOVERY_FILE);
		String analysisName = analysis.getName();

		Path filePath = recoveryFile.getFile();

        List<String> recipients = new ArrayList<String>();
		ArrayList<String> clusters = new ArrayList<String>();
		ArrayList<String> sampleCodes = new ArrayList<String>();
		ArrayList<String> lineages = new ArrayList<String>();
		ArrayList<String> sproteins = new ArrayList<String>();
		String jsonString;

		Map<String, MetadataEntry> stringEntries = new HashMap<>();
		try {
			IridaWorkflow iridaWorkflow = iridaWorkflowsService.getIridaWorkflow(analysis.getWorkflowId());
			String workflowVersion = iridaWorkflow.getWorkflowDescription().getVersion();

			//Read the JSON file from RECOVERY output
			@SuppressWarnings("resource")
			String jsonFile = new Scanner(new BufferedReader(new FileReader(filePath.toFile()))).useDelimiter("\\Z").next();
			jsonString = jsonFile;

			// map the results into a Map
			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> recoveryResults = mapper
					.readValue(jsonFile, new TypeReference<List<Map<String, Object>>>() {
					});

			if (recoveryResults.size() > 0) {
				Map<String, Object> result = recoveryResults.get(0);

				//loop through each of the requested fields and save the entries
				Long masterProjectId = 101L;
				RECOVERY_FIELDS.entrySet().forEach(e -> {
					if (result.containsKey(e.getKey()) && result.get(e.getKey()) != null) {
						String value = result.get(e.getKey()).toString();
						PipelineProvidedMetadataEntry metadataEntry = new PipelineProvidedMetadataEntry(value, "text", analysis);

                        //sample_code
                        if (e.getValue().equals("Sample_code")) { 
							stringEntries.put(e.getValue(), metadataEntry);
							sampleCodes.add(value);
						}
                        else {
							if (e.getValue().equals("Lineage")) { 
								stringEntries.put(e.getValue(), metadataEntry);
								lineages.add(value);
							}
							else {
								if (e.getValue().equals("S-protein")) { 
									stringEntries.put(e.getValue(), metadataEntry);
									sproteins.add(value);
								}
								else {
									stringEntries.put(e.getValue(), metadataEntry);
								}
							}
                        }
					}
				});

				// convert string map into metadata fields
				Map<MetadataTemplateField, MetadataEntry> metadataMap = metadataTemplateService.getMetadataMap(stringEntries);

				//save metadata back to sample
				samples.forEach(s -> {
					s.mergeMetadata(metadataMap);
					sampleService.updateFields(s.getId(), ImmutableMap.of("metadata", s.getMetadata()));
				});

				//EMAIL : if cluster send e-mail to all members of projects of coinvolved samples, if not only to members of this sample's project
				// if isAlert send e-mail also to ROLE_MANAGER members
				Boolean isAlert = checkMutations(lineages.get(0), sproteins.get(0));
				samples.forEach(s -> {
					logger.debug("isAlert: " + isAlert.toString());
					List<Join<Project, Sample>> projectsForSample = psjRepository.getProjectForSample(s);					
					for (Join<Project, Sample> projectForSample : projectsForSample) {
						Project project = projectForSample.getSubject();
						if (!project.isMasterProject()) {
							List<Join<Project, User>> projectUsers = pujRepository.getUsersForProject(projectForSample.getSubject());
							for (Join<Project, User> projectUser : projectUsers) {
								if (isAlert) {
									if (!recipients.contains(projectUser.getObject().getEmail()))
										{ recipients.add(projectUser.getObject().getEmail());}										
								} else {										
									if (!recipients.contains(projectUser.getObject().getEmail()) && !projectUser.getObject().getSystemRole().equals(Role.ROLE_MANAGER))
										{ recipients.add(projectUser.getObject().getEmail());}
								}
							}
						}
					}
				});
			} else {
				throw new PostProcessingException("RECOVERY results for file are not correctly formatted");
			}

		} catch (IOException e) {
			throw new PostProcessingException("Error parsing JSON from RECOVERY results", e);
		} catch (IridaWorkflowNotFoundException e) {
			throw new PostProcessingException("Workflow is not found", e);
		}
		if (emailController.isMailConfigured()) {
		   	//emailController.sendEndOfAnalysisEmail(String.join(",", recipients), analysisName, sampleCodes.get(0), "Coronavirus", lineages.get(0), sproteins.get(0), jsonString);
        }
	}

	private Boolean checkMutations(String lineages, String sproteins) {
		// if the variant is VOC then isAlert = true
		Boolean isAlert = false;
		if ( lineages.startsWith("P.1 ") ) { isAlert = true; }
		if ( lineages.startsWith("P.2 ") ) { isAlert = true; }
		if ( lineages.startsWith("B.1.1.7 ") ) { isAlert = true; }
		if ( lineages.startsWith("B.1.351 ") ) { isAlert = true; }
		if ( lineages.startsWith("B.1.525 ") ) { isAlert = true; }
		if ( sproteins.contains("N439K") ) { isAlert = true; }
		return isAlert; 
	}

	@Override
	public AnalysisType getAnalysisType() {
		return BuiltInAnalysisTypes.RECOVERY_TYPING;
	}
}
