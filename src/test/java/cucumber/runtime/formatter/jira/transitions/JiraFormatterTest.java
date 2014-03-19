package cucumber.runtime.formatter.jira.transitions;

import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Tag;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;

import cucumber.runtime.formatter.JiraFormatter;

public class JiraFormatterTest {

	@Test
	public void testPassedScenario() throws URISyntaxException, JsonParseException, JsonMappingException, IOException {
		URL jiraUrl = new URL("http://jira/browse/PTIPSP");
		JiraFormatter formatter = new JiraFormatter(jiraUrl);
				
		ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add( new Tag("@issue_12346", null) );
		Scenario scenario = new Scenario(null, tags, null, null, null, null, null);
		formatter.scenario(scenario);
	}
}
