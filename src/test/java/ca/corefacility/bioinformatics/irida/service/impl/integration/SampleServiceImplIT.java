package ca.corefacility.bioinformatics.irida.service.impl.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.model.Project;
import ca.corefacility.bioinformatics.irida.model.Role;
import ca.corefacility.bioinformatics.irida.model.Sample;
import ca.corefacility.bioinformatics.irida.model.User;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import ca.corefacility.bioinformatics.irida.service.SampleService;
import ca.corefacility.bioinformatics.irida.service.SequenceFileService;
import ca.corefacility.bioinformatics.irida.utils.test.IridaIntegrationTest;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.google.common.collect.ImmutableList;

/**
 * Integration tests for the sample service.
 * 
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 * 
 */
public class SampleServiceImplIT extends IridaIntegrationTest {

	@Autowired
	private SampleService sampleService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private SequenceFileService sequenceFileService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Before
	public void setUp() {
		User u = new User();
		u.setUsername("fbristow");
		u.setPassword(passwordEncoder.encode("Password1"));
		u.setSystemRole(Role.ROLE_ADMIN);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(u, "Password1",
				ImmutableList.of(Role.ROLE_ADMIN));
		auth.setDetails(u);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Straightforward merging of samples all belonging to the same project.
	 */
	@Test
	@DatabaseSetup("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	@DatabaseTearDown("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	public void testMergeSamples() {
		Sample mergeInto = sampleService.read(1l);
		Project p = projectService.read(1l);

		Sample merged = sampleService.mergeSamples(p, mergeInto, sampleService.read(2l), sampleService.read(3l));

		assertEquals("Merged sample should be same as mergeInto.", mergeInto, merged);

		// merged samples should be deleted
		assertSampleNotFound(2l);
		assertSampleNotFound(3l);

		// the merged sample should have 3 sequence files
		assertEquals("Merged sample should have 3 sequence files", 3,
				sequenceFileService.getSequenceFilesForSample(merged).size());
	}

	/**
	 * Sample merging should be rejected when samples are attempted to be joined
	 * where they do not share the same project.
	 */
	@Test(expected = IllegalArgumentException.class)
	@DatabaseSetup("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	@DatabaseTearDown("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	public void testMergeSampleReject() {
		Sample mergeInto = sampleService.read(1l);
		Project p = projectService.read(1l);
		sampleService.mergeSamples(p, mergeInto, sampleService.read(4l));
	}

	@Test
	@DatabaseSetup("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT_duplicateSampleIds.xml")
	@DatabaseTearDown("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT_duplicateSampleIds.xml")
	public void testGetSampleByExternalIdDuplicates() {
		Project p = projectService.read(1l);
		Sample s = sampleService.getSampleByExternalSampleId(p, "external");
		assertEquals("Should have retrieved sample with ID 1L.", Long.valueOf(1l), s.getId());
	}

	@Test(expected = EntityNotFoundException.class)
	@DatabaseSetup("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	@DatabaseTearDown("/ca/corefacility/bioinformatics/irida/service/impl/SampleServiceImplIT.xml")
	public void testgetSampleByExternalNotFound() {
		Project p = projectService.read(1l);
		sampleService.getSampleByExternalSampleId(p, "garbage");
	}

	private void assertSampleNotFound(Long id) {
		try {
			sampleService.read(id);
			fail("Merged sample with id [" + id + "] should be deleted.");
		} catch (EntityNotFoundException e) {
		} catch (Exception e) {
			fail("Failed for unknown reason; ");
		}
	}
}
