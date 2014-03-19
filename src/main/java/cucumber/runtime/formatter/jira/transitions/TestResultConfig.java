package cucumber.runtime.formatter.jira.transitions;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnySetter;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;

/**
 * {@link JiraTransitionsConfig} has a <code>TestResultConfig</code> instance for 
 * "before" and each possible test result: "passed", "failed", "skipped" and "undefined"    
 */
public class TestResultConfig {
	/**
	 * Keys are "self" and tokenised outward issuelink description - eg: "depends_on".
	 * Keys of the inner Map<String,TransitionData> map to the name of the current status of the issue (or "*" for all)
	 */
	private HashMap<String, Map<String, TransitionData>> relationshipConfigs = new HashMap<String, Map<String,TransitionData>>();
	
	@Override
	public String toString() {
		return relationshipConfigs.toString();
	}
	
	@JsonAnySetter
    public void setRelationshipConfig(String relationship, Map<String, TransitionData> value) {
    	relationshipConfigs.put(relationship, value);
    }
	
	public void setRelationshipConfig(String relationship, String fromState, TransitionData transition) {
		Map<String, TransitionData> relationshipConfig = relationshipConfigs.get(relationship);
		if( relationshipConfig == null ) {
			relationshipConfig = new HashMap<String, TransitionData>();
			relationshipConfigs.put(relationship, relationshipConfig);
		}
		
    	relationshipConfig.put(fromState, transition);
    }
	
    public Map<String, TransitionData> getRelationshipConfig(String relationship) {
        return relationshipConfigs.get(relationship);
    }
    
	/**
	 * @param status - current status
	 * @param message
	 * @param transitionIds
	 * @return null if no transition is required from <code>currentStatus</code> due to {@link #unless}
	 */
	public TransitionInput getTransition( String relationship, String status, Issue issue,
										String message, Map<String, Integer> transitionIds ) {
		Map<String, TransitionData> statusConfig = relationshipConfigs.get(relationship);
		
		if( statusConfig == null ) { return null; }
		
		TransitionData transition = statusConfig.get( status );
		if( transition == null ) {
			transition = statusConfig.get( "*" );
			if( transition == null ) {
				return null;
			}
		}
		
		return transition.getJiraTransition( status, issue, message, transitionIds );
	}
}
