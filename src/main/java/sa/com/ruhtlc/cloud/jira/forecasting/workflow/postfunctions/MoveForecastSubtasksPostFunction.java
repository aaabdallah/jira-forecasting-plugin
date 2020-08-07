package sa.com.ruhtlc.cloud.jira.forecasting.workflow.postfunctions;

import java.util.Collection;
import java.util.Map;

import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.spi.SimpleStep;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.utils.JC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the post-function class that gets executed at the end of the
 * transition. Any parameters that were saved in your factory class will be
 * available in the transientVars Map.
 */
public class MoveForecastSubtasksPostFunction extends AbstractJiraFunctionProvider
{
	private static final Logger log = LoggerFactory.getLogger(MoveForecastSubtasksPostFunction.class);
	public static final String FIELD_MESSAGE = "messageField";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException
	{
		// prettyPrint(transientVars);
			
		MutableIssue issue = getIssue(transientVars);
		
		if (issue.isSubTask())
			if ( issue.getIssueTypeId().equals( Configurator.getInstance().getForecastSubtaskIssueType().getId() ))
				if (transientVars.get("movingForecastSubtaskAsPartOfLargerMove") == null)
					throw new WorkflowException("Unable to move forecast subtask by itself: please move the parent issue.");
		
		String userKey = (String) transientVars.get("userKey");
		// in the original OpenSymphony code, transitions were known as actions
		Integer currentActionId = (Integer) transientVars.get("actionId");

		Collection<Issue> subtasks = issue.getSubTaskObjects();
		
		for (Issue subtask : subtasks)
		{
			// Move forecast subtasks ONLY, and NOT other subtask types. This is to remain true to JIRA's
			// original design, though there are those (including me) who disagree with it.
			if (subtask.getIssueTypeId().equals( Configurator.getInstance().getForecastSubtaskIssueType().getId()) )
			{
				TransitionValidationResult transitionValidationResult = 
					JC.issueService.validateTransition(JC.userManager.getUserByKey(userKey), subtask.getId(), currentActionId, 
						JC.issueService.newIssueInputParameters());
				
				if (transitionValidationResult.isValid())
				{
					// this works but is undocumented. it was just a good guess... it's logical enough that it should continue to work
					// Essentially we need to add a flag to the transition's transient variables when "execute" (this very method)
					// is executed for the subtasks. We do this by using the getAdditionInputs which seems like it should be named
					// getTransientVars... Because the results ARE sent to the execute() call for the forecast subtask.
					transitionValidationResult.getAdditionInputs().put("movingForecastSubtaskAsPartOfLargerMove", new Boolean(true));

					IssueResult transitionResult = JC.issueService.transition(JC.userManager.getUserByKey(userKey), transitionValidationResult);
					if (!transitionResult.isValid())
					{
						ErrorCollection errorCollection = transitionResult.getErrorCollection();
						log.debug( "" + errorCollection.getErrorMessages() );
						log.debug( "" + errorCollection.getErrors() );
						log.debug( "" + errorCollection.getReasons() );
						throw new WorkflowException("Unable to transition forecast subtask.");
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "unused", "rawtypes" })
	private void prettyPrint(Map map)
	{
		StringBuilder builder = new StringBuilder(256);
		for (Object key : map.keySet())
		{
			builder.append("Key [").append( key != null ? key.getClass().getName() : "null").append("] ").append(key == null ? "null" : key.toString());
			Object value = map.get(key);
			builder.append(" ==>> Value [").append( value != null ? value.getClass().getName() : "null").append("] ").append(value == null ? "null" : value.toString());
			builder.append("\n");
		}
		log.debug(builder.toString());
	}
}