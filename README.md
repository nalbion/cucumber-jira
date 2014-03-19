cucumber-jira
=============

A cucumber Formatter/Reporter that can transition JIRA issues.

## Usage

In order for cucumber-jira to connect to your Jira server, you must provide the URL along with the formatter class name.
You must also provide the username and password for a Jira account.  These can be embedded within the URL:

```
--format cucumber.runtime.formatter.JiraFormatter:http://user:password@jira/browse/MY_PROJECT
```

...or you can define the `jira.username` and `jira.password` environment variables 
(or include `-Djira.username=MyUserName -Djira.password=SECRET` in the arguments to cucumber)

```
--format cucumber.runtime.formatter.JiraFormatter:http://jira/browse/MY_PROJECT
```

### Downloading Issues From Jira

The cucumber-jira formatter is intended to be used with the [jira-maven-plugin](https://github.com/nalbion/jira-maven-plugin) 
for a complete "round trip" solution.

```
@issue_MYPROJECT-123 @status_Open 
@Component123 @LabelXYZ 
@type_Story @reporter_BugFinder @assignee_BugFixer
Feature: Demo Feature
  @issue_MYPROJECT-124 @status_Testing_in_Progress 
  @Component123 @LabelXYZ  
  @type_Test @reporter_BugFinder @assignee_BugFixer
  Scenario: Demo Scenario
    Given ...
```


### Transition Configuration
You can configure the transitions that should be executed on the issues by providing a `jira-transitions.json` file.

The primary child elements "passed", "failed" and "skipped" map to the result of each scenario.  
There is a "before" element at the same level, which allows Jira transitions to be executed before the scenario is executed.

Within these elements, the "self" element describes transitions that should be executed on the issue 
that is mapped to the scenario (by a tag prefixed with "@issue_" - see [jira-maven-plugin](https://github.com/nalbion/jira-maven-plugin)).
There may also be other "relationship" elements that are named after linked Jira issues - eg "depends_on".

Within the "self" and the relationship elements you may provide a transition configuration element
for each possible state in which the issues may exist.  Within these elements you name:
  - the name of the transition to invoke when the above conditions (result, relationship, current state) are met.
  - "unless" (optional) - the transition should not be invoked if it is in this named state
  - "comment" (optional) - if there is an error message, the error message will be used rather than this value.
		The comment value may include "%s", in which case the error message will be injected 
		into the comment and the comment will be used along with the embedded error message.
  - "fields" (optional) - any other fields that should be updated during the transition
     * "assignee" - null to unassign, or provide a Jira username. 
	 
	 
There is special handling of the "assignee" field - the purpose is to automatically unassign resolved issues, 
and to assign regressions to somebody.  ${reporter}" may be used to assign the issue to the original reporter.
An issue that is already assigned to somebody will not be reassigned - the configuration file would probably 
become too complicated.


An example jira-transitions.json file:
```json
{
	"before": {
		"self": {
			"Not Run": {
				"transition": "Testing in progress"
			},
			"Test Failed": {
				"transition": "Testing in progress"
			},
			"Test Passed": {
				"transition": "Retest"
			}
		}
	},

	"passed": {
		"self": {
			"*": {
				"transition": "Passed",
				"unless": "Test Passed",
				"fields": {
					"assignee": null
				}
			}
		},
		
		"depends_on" : {
		
		}
	},
	
	"failed": {
		"self": {
			"*": {
				"transition": "Failed",
				"unless": "Test Failed",
				"fields": {
					"assignee": "${reporter}"
				}
			}
		},
		
		"depends_on" : {
		
		}
	},
	
	"skipped": {
		"self": {
			"*": {
				"transition": "Back to Not Run",
				"unless": "Not Run"
			}
		},
		
		"depends_on" : {
		
		}
	}
}
```