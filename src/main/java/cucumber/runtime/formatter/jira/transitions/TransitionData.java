package cucumber.runtime.formatter.jira.transitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;

public class TransitionData {
//	private int id;
	/** The name of the transition to apply */
	@JsonProperty
	private String transition;
	/** Don't perform the transition if the issue's current state is equal to this value */ 
	@JsonProperty
	private String unless;
	/** Optional fields to be sent to Jira when performing the transition */
	@JsonProperty
	private HashMap<String, String> fields;
	/**
	 * If this field contains "%s" the error message will be injected into the comment
	 */
	private String comment;
	
	/** To be used by the JSON parser only */
	public TransitionData() {}
	
	public TransitionData( String transitionName, String unless ) {
		this.transition = transitionName;
		this.unless = unless;
	}
	
	public TransitionData( String transitionName, String unless, HashMap<String, String> fields, String comment ) {
		this.transition = transitionName;
		this.unless = unless;
		this.fields = fields;
		this.comment = comment;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(transition);
		
		if( unless != null ) {
			str.append(" (unless " + unless + ")");
		}
		
		if( fields != null ) {
			str.append(", fields: " + fields);
		}
		
		if( comment != null ) {
			str.append(", comment: " + comment);
		}
		
		return str.toString();
	}
	
	/**
	 * @param currentStatus
	 * @param jiraIssue
	 * @param message
	 * @param transitionIds - a Map of transition name -> ID obtained from the JIRA API.
	 * 						(the transition name is stored in the jira-transitions.json config file
	 * 						but the JIRA API requires an integer ID)
	 * @return null if no transition is required from <code>currentStatus</code> due to {@link #unless}
	 */
	public TransitionInput getJiraTransition( String currentStatus, Issue jiraIssue, String message, Map<String, Integer> transitionIds ) {
		if( currentStatus.equals(unless) ) {
			// No transition required
			return null;
		}
		if( unless != null && unless.indexOf(',') > 0 ) {
			if( unless.matches("\\b" + currentStatus + "\\b" ) ) {
				return null;
			}
		}
		
		if( this.comment != null && message != null && this.comment.indexOf("%s") >= 0 ) {
			message = String.format( this.comment, message );
		} else if( message == null ) {
			message = this.comment;
		}
		
		Integer id = transitionIds.get(transition);
		if( id == null ) {
			throw new IllegalStateException("No transition ID found for " + transition + " in " + transitionIds);
		}
		
		if( fields == null ) {
			if( message == null ) {
				return new TransitionInput(id);
			} else {
				return new TransitionInput(id, Comment.valueOf(message));
			}
		}
		
		ArrayList<FieldInput> fieldInputs = new ArrayList<FieldInput>(fields.size());
		for( String fieldId : fields.keySet() ) {
			String value = fields.get(fieldId);
			
			if( "assignee".equals(fieldId) ) {
				if( value != null ) {
					if( jiraIssue.getAssignee() != null ) {
						// We want to be able to automatically unassign resolved issues
						// and assign regressed issues to the reporter.
						// Don't reassign if the issue has already been assigned to somebody
						continue;
					}
					if( "${reporter}".equals(value) ) {
						value = jiraIssue.getReporter().getName();
					}
				}
			}
			fieldInputs.add( new FieldInput( fieldId, value ) );
		}
		
		if( message == null ) {
			return new TransitionInput(id, fieldInputs);
		} else {
			return new TransitionInput(id, fieldInputs, Comment.valueOf(message));
		}
	}
}
