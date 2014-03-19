package cucumber.runtime.formatter.jira.transitions;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;

public class JiraTransitionsConfig {
//	private HashMap<String, Object> config;

	/** Transitions to be applied before the test is executed */
	@JsonProperty
	public TestResultConfig before;
	
	/** Transitions to be applied when a test passes */
	@JsonProperty
	public TestResultConfig passed;
	
	/** Transitions to be applied when a test fails */
	@JsonProperty
	public TestResultConfig failed;
	
	/** Transitions to be applied when a test is skipped */
	@JsonProperty
	public TestResultConfig skipped;
	
	/** Transitions to be applied when a test is undefined */
	@JsonProperty
	public TestResultConfig undefined;
	
	
	private JiraTransitionsConfig() {}
	
	public static JiraTransitionsConfig load() throws JsonParseException, JsonMappingException, IOException {
		return load( JiraTransitionsConfig.class.getResourceAsStream("/jira-transitions.json") );
	}
	
	public static JiraTransitionsConfig load( InputStream in ) throws JsonParseException, JsonMappingException, IOException {
		JiraTransitionsConfig config;
//		 config = new ObjectMapper().readValue(in, HashMap.class);
		if( in == null ) {
//			throw new NullPointerException("null input stream for jira-transitions.json - file probably does not exist");
			config = new JiraTransitionsConfig();
		} else {
			config = new ObjectMapper().readValue( in, JiraTransitionsConfig.class );
		} 
		
		// Default configuration
		if( config.before == null ) {
			config.before = new TestResultConfig();
			config.before.setRelationshipConfig("self", "Not Run", new TransitionData("Testing in progress", null));
			config.before.setRelationshipConfig("self", "Test Passed", new TransitionData("Retest", null));
			config.before.setRelationshipConfig("self", "Test Failed", new TransitionData("Testing in progress", null));
		}
		
		if( config.passed == null ) {
			config.passed = new TestResultConfig();
			HashMap<String, String> fields = new HashMap<String, String>(1);
			fields.put("assignee", null);
			config.passed.setRelationshipConfig("self", "*", new TransitionData("Passed", "Test Passed", fields, null));
		}
		
		if( config.failed == null ) {
			config.failed = new TestResultConfig();
			HashMap<String, String> fields = new HashMap<String, String>(1);
			fields.put("assignee", "${reporter}");
			config.failed.setRelationshipConfig("self", "*", new TransitionData("Failed", "Test Failed"));
		}
		
		if( config.skipped == null ) {
			config.skipped = new TestResultConfig();
			config.skipped.setRelationshipConfig("self", "*", new TransitionData("Back to Not Run", "Not Run"));
		}
		
		return config;
	}
	
//	/**
//	 * @param status - current status
//	 * @param message
//	 * @param transitionIds
//	 * @return null if no transition is required from <code>currentStatus</code> due to {@link #unless}
//	 */
//	public TransitionInput getTransition( String testResult,
//										String relationship, String status, 
//										String message, Map<String, Integer> transitionIds ) {
//		HashMap<String, Map<String, Transition>> relationshipConfig = getTestResultConfig( testResult ); 
//		Map<String, Transition> statusConfig = relationshipConfig.get( relationship );
//		
//		if( statusConfig == null ) { return null; }
//		
//		Transition transition = statusConfig.get( status );
//		if( transition == null ) {
//			transition = statusConfig.get( "*" );
//			if( transition == null ) {
//				return null;
//			}
//		}
//		
//		return transition.getJiraTransition( status, message, transitionIds );
//	}

	public TestResultConfig getTestResultConfig( String testResult ) {
		if( "before".equals(testResult) ) {
			return before;
		} else if( "passed".equals(testResult) ) {
			return passed;
		} else if( "failed".equals(testResult) ) {
			return failed;
		} else if( "skipped".equals(testResult) ) {
			return skipped;
		} else {
			return undefined;
		}
	}
	
	/**
	 * @param relationshipConfig
	 * @param relationship
	 * @param issue - we use the current status to determine the transition,
	 * 				and may use the reporter to reassign a regressed issue
	 * @param message
	 * @param transitionIds
	 * @return null if no transition is required
	 */
	public TransitionInput getTransition( TestResultConfig relationshipConfig,
											String relationship, Issue jiraIssue, 
											String message, Map<String, Integer> transitionIds ) {
		if( relationshipConfig == null ) {
			return null;
		}
		Map<String, TransitionData> statusConfig = relationshipConfig.getRelationshipConfig( relationship );

		if( statusConfig == null ) { return null; }

		String issueCurrentStatus = jiraIssue.getStatus().getName();
		TransitionData transition = statusConfig.get( issueCurrentStatus );
		if( transition == null ) {
			transition = statusConfig.get( "*" );
			if( transition == null ) {
				return null;
			}
		}

		return transition.getJiraTransition( issueCurrentStatus, jiraIssue, message, transitionIds );
	}
	
//	/**
//	 * @param status - current status
//	 * @param message
//	 * @param transitionIds
//	 * @return null if no transition is required from <code>currentStatus</code> due to {@link #unless}
//	 */
//	public TransitionInput getTransition( String testResult, String relationship, String status, 
//										String message, Map<String, Integer> transitionIds ) {
//		return getTestResultConfig(testResult).getTransition(relationship, status, message, transitionIds);
//	}
}
