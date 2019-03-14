package ca.corefacility.bioinformatics.irida.processing.impl;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.SampleSequencingObjectJoin;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFilePair;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequencingObject;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.BuiltInAnalysisTypes;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission.Builder;
import ca.corefacility.bioinformatics.irida.processing.FileProcessor;
import ca.corefacility.bioinformatics.irida.processing.FileProcessorException;
import ca.corefacility.bioinformatics.irida.repositories.analysis.submission.AnalysisSubmissionRepository;
import ca.corefacility.bioinformatics.irida.repositories.joins.project.ProjectSampleJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.joins.sample.SampleSequencingObjectJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.sequencefile.SequencingObjectRepository;
import ca.corefacility.bioinformatics.irida.repositories.user.UserRepository;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;

import ca.corefacility.bioinformatics.irida.model.workflow.submission.ProjectAnalysisSubmissionJoin;
import ca.corefacility.bioinformatics.irida.repositories.analysis.submission.ProjectAnalysisSubmissionJoinRepository;
import ca.corefacility.bioinformatics.irida.service.ProjectService;

import com.google.common.collect.Sets;

/**
 * File processor which launches a PHANTASTIC typing pipeline on uploaded sequences.
 * It will check with a sequence's associated project for whether or not it
 * should type with PHANTASTIC.
 */
@Component
public class PhantasticTypingFileProcessor implements FileProcessor {
	private static final Logger logger = LoggerFactory.getLogger(PhantasticTypingFileProcessor.class);

	private final SequencingObjectRepository objectRepository;
	private final SampleSequencingObjectJoinRepository ssoRepository;
	private final ProjectSampleJoinRepository psjRepository;
	private final AnalysisSubmissionRepository submissionRepository;
	private final UserRepository userRepository;
	private final IridaWorkflowsService workflowsService;

	private final ProjectAnalysisSubmissionJoinRepository pasRepository;
	private final ProjectService projectService;

	@Autowired
	public PhantasticTypingFileProcessor(SequencingObjectRepository objectRepository,
			AnalysisSubmissionRepository submissionRepository, IridaWorkflowsService workflowsService,
			UserRepository userRepository, SampleSequencingObjectJoinRepository ssoRepository,
			ProjectSampleJoinRepository psjRepository, ProjectAnalysisSubmissionJoinRepository pasRepository, ProjectService projectService) {
		this.objectRepository = objectRepository;
		this.submissionRepository = submissionRepository;
		this.workflowsService = workflowsService;
		this.userRepository = userRepository;
		this.ssoRepository = ssoRepository;
		this.psjRepository = psjRepository;

		this.pasRepository = pasRepository;
		this.projectService = projectService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(SequencingObject sequencingObject) {
		logger.debug("Setting up PHANTASTIC typing for sequence " + sequencingObject.getId());

		User admin = userRepository.loadUserByUsername("admin");

		Project.AutomatedPHANTASTICSetting automatedPHANTASTICSetting = shouldTypeWithPHANTASTIC(sequencingObject);

		// Ensure it's a paired upload. Single end can't currently be
		// assembled/typed.
		if (sequencingObject instanceof SequenceFilePair) {
			IridaWorkflow defaultWorkflowByType;

			// get the workflow
			try {
				defaultWorkflowByType = workflowsService.getDefaultWorkflowByType(BuiltInAnalysisTypes.PHANTASTIC_TYPING);
			} catch (IridaWorkflowNotFoundException e) {
				throw new FileProcessorException("Cannot find assembly workflow", e);
			}

			UUID pipelineUUID = defaultWorkflowByType.getWorkflowIdentifier();

			// build an AnalysisSubmission
			Builder builder = new AnalysisSubmission.Builder(pipelineUUID);

			if(automatedPHANTASTICSetting.equals(Project.AutomatedPHANTASTICSetting.AUTO_METADATA)){
				builder.updateSamples(true);
			}
			else if(automatedPHANTASTICSetting.equals(Project.AutomatedPHANTASTICSetting.AUTO)){
				builder.updateSamples(false);
			}

            Sample sampleForSequencingObject = ssoRepository.getSampleForSequencingObject(sequencingObject).getSubject();
            String token = "tW9jiEusOSGvsE91qso1";
            String species = "Escherichia coli";
            String genomeSize = "5.0";
            String trueConfigFile = "escherichia_coli.config";
            // H + idSample se progetto locale, altrimenti letto da metadati Sample_code
            String sample_code = "H_" + sampleForSequencingObject.getId().toString();
            Long masterProjectId = 48L;
            //List<Join<Project, Sample>> projectsForSample = psjRepository.getProjectForSample(sampleForSequencingObject);
		    //for (Join<Project, Sample> projectForSample : projectsForSample) {
            //    species = projectForSample.getSubject().getOrganism();
            //}
            species = sampleForSequencingObject.getOrganism();
            String region = "-";
            if(sampleForSequencingObject.getGeographicLocationName() != null) {
                region = sampleForSequencingObject.getGeographicLocationName();
            }
            String year = "-";
            if(sampleForSequencingObject.getCollectionDate() != null) {
                year = Integer.toString(sampleForSequencingObject.getCollectionDate().getYear()+1900);
            }
			if(species.equals("Shiga toxin-producing Escherichia coli")){
                species = "Escherichia coli";
			}
			if(species.equals("Listeria monocytogenes")){
                masterProjectId = 49L;
				genomeSize = "3.3";
                trueConfigFile = "listeria_monocytogenes.config";
			}

            builder.inputParameter("phanta_token", token);
            builder.inputParameter("phanta_species", species);
            builder.inputParameter("phanta_genomeSize", genomeSize);
            builder.inputParameter("phanta_trueConfigFile", trueConfigFile);
            builder.inputParameter("phantt-ec_token", token);
            builder.inputParameter("phantt-ec_sample_code", sample_code);
            builder.inputParameter("phantt-ec_species", species);
            builder.inputParameter("phantt-ec_region", region);
            builder.inputParameter("phantt-ec_year", year);
            builder.inputParameter("phantt-lm_token", token);
            builder.inputParameter("phantt-lm_sample_code", sample_code);
            builder.inputParameter("phantt-lm_species", species);
            builder.inputParameter("phantt-lm_region", region);
            builder.inputParameter("phantt-lm_year", year);
            builder.inputParameter("phantc-ec_token", token);
            builder.inputParameter("phantc-ec_sample_code", sample_code);
            builder.inputParameter("phantc-ec_species", species);
            builder.inputParameter("phantc-lm_token", token);
            builder.inputParameter("phantc-lm_sample_code", sample_code);
            builder.inputParameter("phantc-lm_species", species);

			AnalysisSubmission submission = builder.inputFiles(Sets.newHashSet((SequenceFilePair) sequencingObject))
					.priority(AnalysisSubmission.Priority.LOW)
					.name("Automated PHANTASTIC Typing " + sequencingObject.toString()).build();
			submission.setSubmitter(admin);

			submission = submissionRepository.save(submission);

            // Share samples with the master project
            Project masterProject = projectService.read(masterProjectId);
            projectService.addSampleToProject(masterProject, sampleForSequencingObject, false);
            // Share analysis results with the required projects
            List<Join<Project, Sample>> projectsForSample = psjRepository.getProjectForSample(sampleForSequencingObject);
			for (Join<Project, Sample> projectForSample : projectsForSample) {
				pasRepository.save(new ProjectAnalysisSubmissionJoin(projectForSample.getSubject(), submission));
			}

			// Associate the submission with the seqobject
			sequencingObject.setPhantasticTyping(submission);

			objectRepository.save(sequencingObject);

			logger.debug("Automated PHANTASTIC typing submission created for sequencing object " + sequencingObject.getId());
		} else {
			logger.warn("Could not run PHANTASTIC typing for sequencing object " + sequencingObject.getId()
					+ " because it's not paired end");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean modifiesFile() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public boolean shouldProcessFile(Long sequencingObjectId) {
		SequencingObject sequencingObject = objectRepository.findOne(sequencingObjectId);

		return !shouldTypeWithPHANTASTIC(sequencingObject).equals(Project.AutomatedPHANTASTICSetting.OFF);
	}

	/**
	 * Check whether any {@link Project} associated with the
	 * {@link SequencingObject} is set to type with PHANTASTIC.
	 *
	 * @param object {@link SequencingObject} to check to type with PHANTASTIC.
	 * @return true if it should type with PHANTASTIC, false otherwise
	 */
	private Project.AutomatedPHANTASTICSetting shouldTypeWithPHANTASTIC(SequencingObject object) {
		Project.AutomatedPHANTASTICSetting type = Project.AutomatedPHANTASTICSetting.OFF;

		SampleSequencingObjectJoin sampleForSequencingObject = ssoRepository.getSampleForSequencingObject(object);

		/*
		 * This is something that should only ever happen in tests, but added
		 * check with a warning
		 */
		if (sampleForSequencingObject != null) {
			List<Join<Project, Sample>> projectForSample = psjRepository
					.getProjectForSample(sampleForSequencingObject.getSubject());

			Set<Project.AutomatedPHANTASTICSetting> phantasticOptions = projectForSample.stream()
					.map(j -> j.getSubject().getPhantasticTypingUploads()).collect(Collectors.toSet());

			if (phantasticOptions.contains(Project.AutomatedPHANTASTICSetting.AUTO_METADATA)) {
				return Project.AutomatedPHANTASTICSetting.AUTO_METADATA;
			} else if (phantasticOptions.contains(Project.AutomatedPHANTASTICSetting.AUTO))
				return Project.AutomatedPHANTASTICSetting.AUTO;
		} else {
			logger.warn("Cannot find sample for sequencing object.  Not typing with PHANTASTIC");
		}

		return type;
	}

}
