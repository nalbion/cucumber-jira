package cucumber.runtime.formatter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import cucumber.runtime.formatter.jira.transitions.JiraTransitionsConfig;
import cucumber.runtime.formatter.jira.transitions.TestResultConfig;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.formatter.model.Tag;

/**
 * <pre>--format jira:http://@jira/browse/MY_PROJECT -Djira.username=user -Djira.password=password</pre>
 * or 
 * <pre>--format jira:http://user:password@jira/browse/MY_PROJECT</pre> 
 * 
 * @author Nicholas Albion
 */
public class JiraFormatter implements Formatter, Reporter {
	private static final Logger log = LoggerFactory.getLogger(JiraFormatter.class);
	private static final String EOL = System.getProperty("line.separator");
	
//	private String jiraProjectKey;
	private JiraRestClient jira;
	private JiraIssue jiraIssue = new JiraIssue();
	private JiraTransitionsConfig transitionsConfig;
    
	private class JiraIssue {
		String key;
//		String assignee;
//		String reporter;
		LinkedList<String> dependantIssues = new LinkedList<String>();
		Result testResult;
		
		void reset() {
			key = null;
			testResult = null;
			dependantIssues.clear();
		}
	}
	
	/**
	 * @param jiraUrl - eg: "http://user:password@jira:80/browse/MY_PROJECT"
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
    public JiraFormatter( URL jiraUrl ) throws URISyntaxException, JsonParseException, JsonMappingException, IOException {
//    	String path = jiraUrl.getPath();			// /browse/MY_PROJECT 
//    	jiraProjectKey = path.substring(8);
    	
    	String username = System.getenv("jira.username");
    	String password = System.getenv("jira.password");
    	String host;
    	int port;
    	
    	if( username == null || password == null ) {
    		String authority = jiraUrl.getAuthority();	// user:password@jira:80
    		int at = authority.lastIndexOf('@');
    		int colon = authority.indexOf(':');
    		if( at <= 0 || colon <= 0 ) {
    			throw new IllegalArgumentException("JIRA username and password must be provided " +
    					"as environment variables 'jira.username', 'jira.password' OR " +
    					"in the URL - eg: http://user:password@jira:80/browse/MY_PROJECT");
    		}
    		
    		username = authority.substring(0, colon);
        	password = authority.substring(colon + 1, at);
        	
        	colon = authority.indexOf(':', at);
        	
        	if( colon > 0 ) {
        		host = authority.substring( at + 1, colon );
        		port = Integer.parseInt( authority.substring( colon + 1 ) );
        	} else {
        		host = authority.substring( at + 1 );
        		port = 80;
        	}
    	} else {
    		host = jiraUrl.getHost();
    		port = jiraUrl.getPort();
    	}
    	jiraUrl = new URL( jiraUrl.getProtocol(), host, port, "/" );
    	
    	System.out.println("JIRA URL: " + jiraUrl + ", user: " + username);
    	URI jiraServerUri = jiraUrl.toURI();
    	
    	final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
    	jira = factory.createWithBasicHttpAuthentication(jiraServerUri, username, password);
    	
    	transitionsConfig = JiraTransitionsConfig.load();
    }

    // ----------------- Reporter methods -----------------
    @Override
    public void before(Match match, Result result) {}
    
    @Override
    public void result(Result result) {
//    	System.out.println("result: " + result.getStatus() + ", jiraIssue.key: " + jiraIssue.key);
    	
    	if( Result.FAILED.equals(result.getStatus()) ) {
    		jiraIssue.testResult = result;
    	} else if( jiraIssue.testResult == null && !Result.PASSED.equals(result.getStatus()) ) {
    		jiraIssue.testResult = result;
    	}
//        if (Result.UNDEFINED.equals(result.getStatus()) || "pending".equals(result.getStatus())) skipped = result;
    	
    }
    

    @Override
    public void after(Match match, Result result) {
//    	if (result.getStatus().equals(Result.FAILED)) {
    	System.out.println("after: " + result.getStatus());
    	if( match != null ) {
    		System.out.println("  match.location: " + match.getLocation());
    		System.out.println("  match.arguments: " + match.getArguments());
    	}
    }

    /** Called when Cucumber finds a method with a matching pattern annotation */
    @Override
    public void match(Match match) {
    	Issue issue = jira.getIssueClient().getIssue( jiraIssue.key ).claim();
    	
    	Map<String, Integer> transitionIds = new HashMap<String, Integer>();
    	for( Transition transition : jira.getIssueClient().getTransitions(issue).claim() ) {
    		transitionIds.put( transition.getName(), transition.getId() );
    	}
System.out.println("before " + jiraIssue.key + ", status: " + issue.getStatus().getName());    	
    	TestResultConfig beforeConfig = transitionsConfig.getTestResultConfig( "before" );
    	log.debug("transitionsConfig for before: " + beforeConfig);
try {    	
    	TransitionInput transitionInput = transitionsConfig.getTransition( beforeConfig, "self", 
    																		issue,
    																		null, //"Running test: " + match.getLocation(), 
    																		transitionIds );
    	
    	if( transitionInput != null ) {
    		System.out.println("Transitioning " + issue.getKey() + " to " + transitionInput.getId());
    		jira.getIssueClient().transition(issue, transitionInput).claim();
    	}
} catch( IllegalStateException e ) {
	throw new IllegalStateException("Jira issue: " + jiraIssue.key + ", current status: " + issue.getStatus().getName(), e);
}   	
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
System.out.println("embedding: " + mimeType);        	
    }

    @Override
    public void write(String text) {
System.out.println("Write: " + text);    	
    }

    // ----------------- Formatter methods -----------------
    /** @param featureURI the URI where the gherkin originated from. Typically a file path. */
    @Override
    public void uri(String featureURI) {
    	System.out.println("featureURI: " + featureURI);
    }

    @Override
    public void feature(Feature feature) {
    }

    @Override
    public void background(Background background) {
    }

    @Override
    public void scenario(Scenario scenario) {
    	transitionIssueOnFinalResult();
    	
    	for( Tag tag : scenario.getTags() ) {
//    		TODO: add a tag from issuelinks { {type.outward}_{outwardIssue.key}
//    		(need to replace spaces with _)
//    		@depends_on_KEY  
//    		{outwardIssue.fields.status.name}  ("Open")
    		String tagName = tag.getName();   		
    		if( tagName.startsWith("@issue_") ) {
    			jiraIssue.key = tagName.substring(7);
    		} else if( tagName.startsWith("@depends_on_") ) {
    			jiraIssue.dependantIssues.add( tagName.substring(12) );
    		}
    	}
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
    }

    @Override
    public void examples(Examples examples) {
    }

    @Override
    public void step(Step step) {
    }

    @Override
    public void eof() {
    }

    @Override
    public void syntaxError(String state, String event, 
    						List<String> legalEvents, String uri, Integer line) {
    }

    @Override
    public void done() {
    	transitionIssueOnFinalResult();
    	
System.out.println("Done");   
    }

    @Override
    public void close() {
    	try {
    		log.debug("closing JIRA connection");
//			Thread.sleep(1000);
			jira.close();
		} catch (Exception e) {}
    }
    
    private void transitionIssueOnFinalResult() {
    	// result(Result result) gets called after each step, but we want to transition at the end of each scenario.
    	// This method therefore gets called from scenario(Scenario scenario) - at the beginning of each scenario
    	// and then in done() to process the last scenario.
    	// Because we are transitioning the previous issue, there's nothing to do the first time this method is called.  
    	if( jiraIssue.key == null ) { return; }
    	
    	String testResult;
    	String testErrorMessage;
    	if( jiraIssue.testResult == null ) {
    		testResult = Result.PASSED;
    		testErrorMessage = null;
    	} else {
    		testResult = jiraIssue.testResult.getStatus();
    		testErrorMessage = formatErrorMessageForJira( jiraIssue.testResult.getErrorMessage() );
    		System.out.println(testErrorMessage);    		
    	}
    	
    	
    	System.out.println( "transitionIssueOnFinalResult: " + jiraIssue.key + ": " + testResult );
    	Issue issue = jira.getIssueClient().getIssue( jiraIssue.key /* didn't work: , expandos */).claim();
    	//for( IssueField field : issue.getFields() ) {
//    		System.out.println( " field " + field.getId() + " (" + field.getName() + "): " + field.getValue() );
    	//}
//    	    	IssueField transitionsField = issue.getField("transitions");
//    	    	if( transitionsField != null ) {
//    	    		System.out.println( "transitions: " + transitionsField.getValue() );
//    	    	}

    	Map<String, Integer> transitionIds = new HashMap<String, Integer>();
    	for( Transition transition : jira.getIssueClient().getTransitions(issue).claim() ) {
    		transitionIds.put( transition.getName(), transition.getId() );
    	}
    	    	
    	TestResultConfig testResultConfig = transitionsConfig.getTestResultConfig( testResult );
    	log.debug("transitionsConfig for '" + testResult + "': " + testResultConfig);    	
		TransitionInput transitionInput = transitionsConfig.getTransition( testResultConfig, "self", issue, testErrorMessage, transitionIds );
		if( transitionInput != null ) {
			System.out.println("Transitioning " + issue.getKey() + " to " + transitionInput.getId() + ", " + transitionInput.getComment());
			jira.getIssueClient().transition(issue, transitionInput).claim();
		}
    	
    	IssueField issueLinksField = issue.getField("issuelinks");
    	if( issueLinksField != null ) {
    		System.out.println("issuelinks: " + issueLinksField.getValue());
    	}
   	    
//    	for( IssueLink issueLink : issue.getIssueLinks() ) {
//    		String relationship = issueLink.getIssueLinkType().getDescription();
//    	   	TransitionInput transitionInput = testResultConfig.getTransition( relationship, issueLink.get, result.getErrorMessage(), transitionIds);
//    	   	jira.getIssueClient().transition(issue, transitionInput).claim();
//    	}
    	
    	jiraIssue.reset();
    }
    
    private String formatErrorMessageForJira( String errorMessage ) {
    	StringBuilder str = new StringBuilder();
    	boolean comparingTables = false;
    	
    	for( String line : errorMessage.split("[\\r\\n]+") ) {
    		if( line.startsWith("at cucumber.") ) {
    			continue;
    		}
    		if( "cucumber.runtime.table.TableDiffException: Tables were not identical:".equals(line) ) {
    			line = "Tables were not identical:";
    			comparingTables = true;
    		} else if( comparingTables ) {
    			if( line.startsWith("    - | ")) {
    				line = "{color:orange}" + line.substring(5) + "{color}";
//    				line = line.substring(5);
//    				continue;
    			} else if( line.startsWith("    + | ")) {
    				//line = "{color:red}" + line.substring(5) + "{red}";
    				line = line.substring(5);
    			} else if( line.startsWith("      | ")) {
    				//line = "{color:green}" + line.substring(5) + "{color}";
    				line = line.substring(5);
    			} else {
    				comparingTables = false;
    			}
    		}
    		    		
    		str.append(line).append(EOL);
    	}
    	
    	return str.toString();
    }
}
