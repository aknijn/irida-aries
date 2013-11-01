package ca.corefacility.bioinformatics.irida.pipeline.workflow.impl;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.corefacility.bioinformatics.irida.pipeline.workflow.WorkflowSubmissionException;

public class WorkflowSubmitterGalaxyTest
{
	@Mock private WorkflowRESTAPIGalaxy workflowAPIGalaxy;
	
	private WorkflowSubmitterGalaxy workflowSubmitter;
	private ExecutableWorkflowGalaxy goodWorkflow;
	private String goodWorkflowString;
	private ExecutableWorkflowGalaxy badWorkflow;
	private String badWorkflowString;
	
	@Before
	public void setup() throws FileNotFoundException, URISyntaxException
	{
		URL goodWorkflowURL = this.getClass().getResource("GalaxyWorkflowGood.ga");
		URL badWorkflowURL = this.getClass().getResource("GalaxyWorkflowBad.ga");
		Scanner goodWorkflowScanner = new Scanner(new File(goodWorkflowURL.toURI()));
		Scanner badWorkflowScanner = new Scanner(new File(badWorkflowURL.toURI()));
		
		MockitoAnnotations.initMocks(this);
		
		goodWorkflowString = goodWorkflowScanner.useDelimiter("\\Z").next();
		goodWorkflowScanner.close();
		goodWorkflow = new ExecutableWorkflowGalaxy(goodWorkflowString);
		
		badWorkflowString = badWorkflowScanner.useDelimiter("\\Z").next();
		badWorkflowScanner.close();
		badWorkflow = new ExecutableWorkflowGalaxy(badWorkflowString);
						
		workflowSubmitter = new WorkflowSubmitterGalaxy(workflowAPIGalaxy);
	}
	
	@Test
	public void testSubmitWorkflowGood() throws WorkflowSubmissionException
	{
		when(workflowAPIGalaxy.importWorkflow(goodWorkflow)).thenReturn("id");
		
		assertTrue(workflowSubmitter.submitWorkflow(new WorkflowImpl(goodWorkflowString)));
		verify(workflowAPIGalaxy).importWorkflow(goodWorkflow);
	}
	
	@Test(expected=WorkflowSubmissionException.class)
	public void testSubmitWorkflowBad() throws WorkflowSubmissionException
	{	
		when(workflowAPIGalaxy.importWorkflow(badWorkflow)).thenThrow(new WorkflowSubmissionException("null"));
		
		assertFalse(workflowSubmitter.submitWorkflow(new WorkflowImpl(badWorkflowString)));
		verify(workflowAPIGalaxy).importWorkflow(badWorkflow);
	}
}
