package cucumber.runtime.formatter.jira.transitions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;

import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;

import static org.junit.Assert.*;

public class JiraTransitionsConfigTest {

	@Test
	public void testJiraTransitionsConfig() throws JsonParseException, JsonMappingException, IOException {
		// Given
		Map<String, Integer> transitionIds = new HashMap<String, Integer>();
		transitionIds.put("Testing in Progress", 1);
		JiraTransitionsConfig config = JiraTransitionsConfig.load();
				
		// When
//		HashMap<String, Map<String, TransitionData>> relationshipConfig = config.getTestResultConfig( "before" );
//		TransitionInput transition = config.getTransition(relationshipConfig, "self", "Not Run", null, transitionIds);
		
		// Then
//		assertEquals( 1, transition.getId() );
	}
}
