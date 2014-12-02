package ca.corefacility.bioinformatics.irida.service.upload.galaxy;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.upload.UploadSample;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyAccountEmail;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyProjectName;
import ca.corefacility.bioinformatics.irida.pipeline.upload.UploadWorker;
import ca.corefacility.bioinformatics.irida.pipeline.upload.Uploader;
import ca.corefacility.bioinformatics.irida.service.upload.UploadService;

import com.google.common.collect.Lists;

/**
 * Defines an upload service to upload genomic sequence data into a Galaxy
 * instance.
 * 
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 *
 */
@Service
public class GalaxyUploadService implements
		UploadService<GalaxyProjectName, GalaxyAccountEmail> {
	private static final Logger logger = LoggerFactory
			.getLogger(GalaxyUploadService.class);

	private final Executor uploadExecutor;

	private UploadSampleConversionServiceGalaxy uploadSampleConversionService;
	private Uploader<GalaxyProjectName, GalaxyAccountEmail> galaxyUploader;

	/**
	 * Builds a new GalaxyUploadService with the given information.
	 * 
	 * @param uploadExecutor
	 *            An {@link Executor} used for executing uploads in different
	 *            threads.
	 * 
	 * @param galaxyUploader
	 *            The GalaxyUploader to use to connect to an instance of Galaxy.
	 * @param uploadSampleConversionService
	 *            The service for constructing objects to upload to Galaxy.
	 */
	@Autowired
	public GalaxyUploadService(Executor uploadExecutor,
			Uploader<GalaxyProjectName, GalaxyAccountEmail> galaxyUploader,
			UploadSampleConversionServiceGalaxy uploadSampleConversionService) {
		this.uploadExecutor = uploadExecutor;
		this.galaxyUploader = galaxyUploader;
		this.uploadSampleConversionService = uploadSampleConversionService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public URL getUrl() {
		return galaxyUploader.getUrl();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UploadWorker performUploadAllSamples(long projectId,
			GalaxyProjectName projectName, GalaxyAccountEmail accountName)
			throws ConstraintViolationException {

		checkNotNull(projectName, "galaxyLibraryName is null");
		checkNotNull(accountName, "galaxyUserEmail is null");

		logger.debug("Uploading all samples for project id=" + projectId
				+ ", galaxy email=" + accountName + ", library name="
				+ projectName + " to Galaxy " + getUrl());

		Set<UploadSample> galaxySamples = uploadSampleConversionService
				.getUploadSamplesForProject(projectId);

		return buildAndRunUploadWorker(galaxySamples, projectName, accountName);
	}

	private UploadWorker buildAndRunUploadWorker(
			Set<UploadSample> galaxySamples,
			GalaxyProjectName galaxyLibraryName,
			GalaxyAccountEmail galaxyUserEmail)
			throws ConstraintViolationException {

		UploadWorker uploadWorker = galaxyUploader.uploadSamples(
				Lists.newArrayList(galaxySamples), galaxyLibraryName,
				galaxyUserEmail);

		uploadExecutor.execute(uploadWorker);

		return uploadWorker;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UploadWorker performUploadSelectedSamples(
			Set<Sample> selectedSamples, GalaxyProjectName projectName,
			GalaxyAccountEmail accountName) throws ConstraintViolationException {

		checkNotNull(selectedSamples, "selectedSamples is null");
		checkNotNull(projectName, "galaxyLibraryName is null");
		checkNotNull(accountName, "galaxyUserEmail is null");

		Set<UploadSample> galaxySamples = uploadSampleConversionService
				.convertToUploadSamples(selectedSamples);

		return buildAndRunUploadWorker(galaxySamples, projectName, accountName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UploadWorker performUploadSelectedSequenceFiles(
			Set<Long> selectedSequenceFileIds, GalaxyProjectName projectName,
			GalaxyAccountEmail accountName) throws ConstraintViolationException {

		checkNotNull(selectedSequenceFileIds, "selectedSequenceFileIds is null");
		checkNotNull(projectName, "projectName is null");
		checkNotNull(accountName, "accountName is null");

		Set<UploadSample> galaxySamples = uploadSampleConversionService
				.convertSequenceFilesByIdToUploadSamples(selectedSequenceFileIds);

		return buildAndRunUploadWorker(galaxySamples, projectName, accountName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UploadWorker performUploadSelectedSequenceFiles(
			Set<Long> selectedSequenceFileIds, String projectName,
			String accountName) throws ConstraintViolationException {

		checkNotNull(selectedSequenceFileIds, "selectedSequenceFileIds is null");
		checkNotNull(projectName, "projectName is null");
		checkNotNull(accountName, "accountName is null");

		return performUploadSelectedSequenceFiles(selectedSequenceFileIds,
				new GalaxyProjectName(projectName), new GalaxyAccountEmail(
						accountName));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConfigured() {
		return galaxyUploader.isDataLocationAttached();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return galaxyUploader.isDataLocationConnected();
	}
}