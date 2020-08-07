package sa.com.ruhtlc.cloud.jira.forecasting.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;

/**
 * Currently does nothing other than listen (if used). Left in for future customization inshaa-Allaah.
 */
public class ForecastIssueListener
{
	private static final Logger log = LoggerFactory.getLogger(ForecastIssueListener.class);

	@EventListener
	public void processIssueEvent(IssueEvent issueEvent)
	{
		try
		{
			Long eventTypeId = issueEvent.getEventTypeId();
			if (eventTypeId.equals(EventType.ISSUE_CREATED_ID))
			{
				Issue issue = issueEvent.getIssue();
	
				if (issue.getIssueType().equals(Configurator.getInstance().getForecastIssueType())
					|| issue.getIssueType().equals(Configurator.getInstance().getForecastSubtaskIssueType()))
				{
					
				}
			}
		}
		catch (Exception e)
		{
			log.error("Unexpected error while processing IssueEvent in ForecastIssueListener", e);
		}
	}
}
