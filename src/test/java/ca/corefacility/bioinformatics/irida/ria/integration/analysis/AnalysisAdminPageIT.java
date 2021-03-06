package ca.corefacility.bioinformatics.irida.ria.integration.analysis;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ca.corefacility.bioinformatics.irida.ria.integration.AbstractIridaUIITChromeDriver;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.LoginPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.analysis.AnalysesUserPage;

import com.github.springtestdbunit.annotation.DatabaseSetup;

/**
 * <p>
 * Integration test to ensure that the Project Details Page.
 * </p>
 *
 */
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/ria/web/analysis/AnalysisAdminView.xml")
public class AnalysisAdminPageIT extends AbstractIridaUIITChromeDriver {

	@Before
	public void setUpTest() {
		LoginPage.loginAsManager(driver());
	}

	@Test
	public void testPageSetup() {
		AnalysesUserPage userPage = AnalysesUserPage.initializePage(driver());
		assertEquals("Admin has not personal analysis", 8, userPage.getNumberOfAnalyses());

		AnalysesUserPage adminPage = AnalysesUserPage.initializeAdminPage(driver());
		assertEquals("Should be 8 analyses displayed on the page", 9, adminPage.getNumberOfAnalyses());
	}

	@Test
	public void testAdvancedFilters() {
		AnalysesUserPage page = AnalysesUserPage.initializeAdminPage(driver());
		assertEquals("Should be 9 analyses displayed on the page", 9, page.getNumberOfAnalyses());

		page.filterByState("Queued");
		assertEquals("Should be 1 analysis in the state of 'NEW/Queued'", 1, page.getNumberOfAnalyses());

		page.filterByState("Completed");
		assertEquals("Should be 2 analysis in the state of 'COMPLETED'", 2, page.getNumberOfAnalyses());

		page.filterByState("Prepared");
		assertEquals("Should be 0 analysis in the state of 'PREPARED'", 0, page.getNumberOfAnalyses());

		// Clear
		page.clearFilter();
		assertEquals("Should be 9 analyses displayed on the page", 9, page.getNumberOfAnalyses());

		// Clear
		page.clearFilter();
	}
}
