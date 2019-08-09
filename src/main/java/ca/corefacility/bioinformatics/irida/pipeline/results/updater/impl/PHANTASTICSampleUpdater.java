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
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

//ISS
import ca.corefacility.bioinformatics.irida.service.EmailController;
import ca.corefacility.bioinformatics.irida.model.user.User;
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
 * {@link AnalysisSampleUpdater} that adds a number of results from a PHANTASTIC run to the metadata of a {@link Sample}
 */
@Component
public class PHANTASTICSampleUpdater implements AnalysisSampleUpdater {
    private static final Logger logger = LoggerFactory.getLogger(PHANTASTICSampleUpdater.class);
	private static final String PHANTASTIC_FILE = "phantastic_out";
	private static final String PHANTASTIC_DM = "phantastic_dm";

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
	private static Map<String, String> PHANTASTIC_LM_FIELDS = ImmutableMap.<String,String>builder()
		.put("information_name", "Sample_code")
		.put("qc_status", "QC_status")
		.put("region", "Regione")
		.put("year", "Anno")
		.put("mlst_ST", "MLST_ST")
		.put("serotype_serogroup", "Serogroup")
		.put("serotype_amplicons", "Amplicons")
		.build();
	// @formatter:on
	// @formatter:off
	private static Map<String, String> PHANTASTIC_EC_FIELDS = ImmutableMap.<String,String>builder()
		.put("information_name", "Sample_code")
		.put("qc_status", "QC_status")
		.put("region", "Regione")
		.put("year", "Anno")
		.put("mlst_ST", "MLST_ST")
		.put("serotype_o", "Antigen_O")
		.put("serotype_h", "Antigen_H")
		.put("virulotype_eae", "eae")
		.put("virulotype_ehxa", "ehxa")
		.put("virulotype_stx1", "stx1")
		.put("virulotype_stx2", "stx2")
		.put("shigatoxin_subtype", "stx_subtype")
		.build();
	// @formatter:on

	@Autowired
	public PHANTASTICSampleUpdater(MetadataTemplateService metadataTemplateService, SampleService sampleService,
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
	 * Add PHANTASTIC results to the metadata of the given {@link Sample}s
	 *
	 * @param samples  The samples to update.
	 * @param analysis the {@link AnalysisSubmission} to apply to the samples
	 * @throws PostProcessingException if the method cannot read the "phantastic_out" output file
	 */
	@Override
	public void update(Collection<Sample> samples, AnalysisSubmission analysis) throws PostProcessingException {
		AnalysisOutputFile phantasticFile = analysis.getAnalysis().getAnalysisOutputFile(PHANTASTIC_FILE);
		String analysisName = analysis.getName();
        ArrayList<String> sampleSpecies = new ArrayList<String>();

		Path filePath = phantasticFile.getFile();

        ArrayList<String> recipients = new ArrayList<String>();
        ArrayList<String> clusters = new ArrayList<String>();
		ArrayList<String> sampleCodes = new ArrayList<String>();
		String clusterId;

		Map<String, MetadataEntry> stringEntries = new HashMap<>();
		try {
			IridaWorkflow iridaWorkflow = iridaWorkflowsService.getIridaWorkflow(analysis.getWorkflowId());
			String workflowVersion = iridaWorkflow.getWorkflowDescription().getVersion();

			//Read the JSON file from PHANTASTIC output
			@SuppressWarnings("resource")
			String jsonFile = new Scanner(new BufferedReader(new FileReader(filePath.toFile()))).useDelimiter("\\Z")
					.next();

			// map the results into a Map
			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> phantasticResults = mapper
					.readValue(jsonFile, new TypeReference<List<Map<String, Object>>>() {
					});

			if (phantasticResults.size() > 0) {
				Map<String, Object> result = phantasticResults.get(0);

				//loop through each of the requested fields and save the entries
                Map<String, String> PHANTASTIC_FIELDS = PHANTASTIC_EC_FIELDS;
				Integer clusterCriterium = 10;
				Long masterProjectId = 48L;
                if (result.containsKey("serotype_serogroup")) {
                    PHANTASTIC_FIELDS = PHANTASTIC_LM_FIELDS;
					clusterCriterium = 7;
					masterProjectId = 49L;
                }
				PHANTASTIC_FIELDS.entrySet().forEach(e -> {
					if (result.containsKey(e.getKey()) && result.get(e.getKey()) != null) {
						String value = result.get(e.getKey()).toString();
						PipelineProvidedMetadataEntry metadataEntry = new PipelineProvidedMetadataEntry(value, "text", analysis);

                        //sample_code
                        if (e.getValue().equals("Sample_code")) { 
							stringEntries.put(e.getValue(), metadataEntry);
							sampleCodes.add(value);
						}
                        else {
							stringEntries.put(e.getValue(), metadataEntry);
                        }
					}
				});

				clusters = getCluster(sampleCodes.get(0), clusterCriterium, masterProjectId, analysis);
				clusterId = clusters.get(0);
				clusters.remove(0);
				PipelineProvidedMetadataEntry metadataEntry = new PipelineProvidedMetadataEntry(clusterId, "text", analysis);
				stringEntries.put("Cluster_Id", metadataEntry);

				// convert string map into metadata fields
				Map<MetadataTemplateField, MetadataEntry> metadataMap = metadataTemplateService.getMetadataMap(stringEntries);

				//save metadata back to sample
				samples.forEach(s -> {
					s.mergeMetadata(metadataMap);
					sampleService.updateFields(s.getId(), ImmutableMap.of("metadata", s.getMetadata()));
                    //EMAIL
					sampleSpecies.add(s.getOrganism());
                    List<Join<Project, Sample>> projectsForSample = psjRepository.getProjectForSample(s);
        		    for (Join<Project, Sample> projectForSample : projectsForSample) {
                        List<Join<Project, User>> projectUsers = pujRepository.getUsersForProjectByRole(projectForSample.getSubject(), ProjectRole.PROJECT_OWNER);
            		    for (Join<Project, User> projectUser : projectUsers) {
							if (!recipients.contains(projectUser.getObject().getEmail()))
                                { recipients.add(projectUser.getObject().getEmail());}
                        }
                    }
				});
			} else {
				throw new PostProcessingException("PHANTASTIC results for file are not correctly formatted");
			}

		} catch (IOException e) {
			throw new PostProcessingException("Error parsing JSON from PHANTASTIC results", e);
		} catch (IridaWorkflowNotFoundException e) {
			throw new PostProcessingException("Workflow is not found", e);
		}
		if (emailController.isMailConfigured()) {
		   	emailController.sendEndOfAnalysisEmail(String.join(",", recipients), analysisName, sampleCodes.get(0), sampleSpecies.get(0), clusterId, String.join(", ", clusters));
        }
	}

	private ArrayList<String> getCluster(String sampleCode, Integer clusterCriterium, Long masterProjectId, AnalysisSubmission analysis ) throws FileNotFoundException {
		//function that checks the distance matrix and searches for clusters
		//an ArrayList clusters is returned with the clusterId in the first position and the cluster nodes in all the following
		//if no cluster is found clusterId equals "-" and the next six positions in the ArrayList contain the names and the distances of the nearest three samples
		AnalysisOutputFile phantasticDM = analysis.getAnalysis().getAnalysisOutputFile(PHANTASTIC_DM);
		Path dmPath = phantasticDM.getFile();
		ArrayList<String> clusterNodes = new ArrayList<String>();
		ArrayList<String> clusterExtendedNodes = new ArrayList<String>();
		ArrayList<String> clusters = new ArrayList<String>();
		Project masterProject = projectService.read(masterProjectId);
		//check for clusters
		Scanner dm_input = new Scanner(new BufferedReader(new FileReader(dmPath.toFile())));
		String firstLine = dm_input.nextLine();
		Integer i = 0;
		List<String> dm_header = new ArrayList<>(Arrays.asList(firstLine.split("\t")));

		Integer dist1 = 9999;
		Integer dist2 = 9999;
		Integer dist3 = 9999;
		String sample1 = "ERROR";
		String sample2 = "ERROR";
		String sample3 = "ERROR";
		while(dm_input.hasNextLine())
		{
			Scanner dm_colReader = new Scanner(dm_input.nextLine());
			String firstCol = dm_colReader.next();
			//search for the line of the current sample
			if (sampleCode.equals(firstCol)) {
				while(dm_colReader.hasNextInt())
				{
					i++;
					int colNextInt = dm_colReader.nextInt();
					//store the names and distances of the three nearest samples
					if (colNextInt < dist1 && !sampleCode.equals(dm_header.get(i))) {
						sample3 = sample2;
						sample2 = sample1;
						sample1 = dm_header.get(i);
						dist3 = dist2;
						dist2 = dist1;
						dist1 = colNextInt;
					}
					//apply the criterium for a cluster
					if (colNextInt <= clusterCriterium && !sampleCode.equals(dm_header.get(i))) { clusterNodes.add(dm_header.get(i)); }
					else { if (colNextInt <= 15 && !sampleCode.equals(dm_header.get(i))) { clusterExtendedNodes.add(dm_header.get(i)); } }
				}
			}
		}

		Integer clusterNodesSize = clusterNodes.size();
		Integer clusterExtendedNodesSize = clusterExtendedNodes.size();
		logger.debug("clusterNodes.size: " + clusterNodesSize.toString());
		logger.debug("clusterExtendedNodes.size: " + clusterExtendedNodesSize.toString());
		if (clusterNodes.size() == 0) {
			if (clusterExtendedNodes.size() == 0) {
				//no cluster
				clusters.add("-");
				clusters.add(sample1);
				clusters.add(dist1.toString());
				clusters.add(sample2);
				clusters.add(dist2.toString());
				clusters.add(sample3);
				clusters.add(dist3.toString());
				logger.debug("no cluster");
			} else {
				//sample is extended of an existing cluster (or extended of extended => no cluster)
				String clusterId = sampleService.getClusterIdByCodes(masterProject, clusterExtendedNodes);
				logger.debug("sample is extended of an existing cluster: " + clusterId);
				if (clusterId.contains("_ext")) { clusterId = "-"; } else { clusterId = clusterId + "_ext"; }
				clusters.add(clusterId);
				clusters.add(sample1);
				clusters.add(dist1.toString());
				clusters.add(sample2);
				clusters.add(dist2.toString());
				clusters.add(sample3);
				clusters.add(dist3.toString());			}
		} else {
			String clusterId = sampleService.getClusterIdByCodes(masterProject, clusterNodes);
			if (clusterId.equals("-")) {
				//new cluster
				String newClusterId = sampleService.getNextClusterId(masterProject);
				clusters.add(newClusterId);
				sampleService.setClusterIdByCode(masterProject, clusterNodes, newClusterId);
				logger.debug("new cluster: " + newClusterId);
			} else {
				if (clusterId.contains("_ext")) {
					//other samples are at most extended of an existing cluster => new cluster
					String newClusterId = sampleService.getNextClusterId(masterProject);
					clusters.add(newClusterId);
					logger.debug("other samples are at most extended of an existing cluster => new cluster: " + newClusterId);
				} else {
					//other samples are part of an existing cluster
					clusters.add(clusterId);
					sampleService.setClusterIdByCode(masterProject, clusterNodes, clusterId);
					logger.debug("other samples are part of an existing cluster: " + clusterId);
				}
			}
		}
		clusters.addAll(clusterNodes);
		return clusters;
	}

	@Override
	public AnalysisType getAnalysisType() {
		return BuiltInAnalysisTypes.PHANTASTIC_TYPING;
	}
}
