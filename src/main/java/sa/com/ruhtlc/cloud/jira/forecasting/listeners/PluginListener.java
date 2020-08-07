package sa.com.ruhtlc.cloud.jira.forecasting.listeners;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.JiraEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.lifecycle.LifecycleAware;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.utils.Debugger;
import sa.com.ruhtlc.cloud.jira.utils.JC;

// Exporting the component as an OSGI service is the only way I could make this
// work in JIRA 7.1 as a LifecycleAware object. The tutorials do not mention this, 
// and it does not work without this.
@ExportAsService
@Component
public class PluginListener implements InitializingBean, DisposableBean, LifecycleAware
{
	private static final Logger log = LoggerFactory.getLogger(PluginListener.class);

	private List<Object> eventListeners = new ArrayList<Object>();
	
	public PluginListener()
	{
		log.debug("Forecasting Plugin: DEFAULT CONSTRUCTOR");
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		// Called after Spring has initialized the bean.
		// Do not put eventPublisher registration here: it will be too
		// early. This bean is created by Spring before JIRA is fully
		// set up.
	}

	@Override
	public void destroy() throws Exception
	{
		// Called when Spring destroys the bean.
	}

	@Override
	public void onStart()
	{
		log.info("Forecasting Plugin: Attempting to start.");
		
		try
		{
			Configurator.getInstance().enable();

			// registerEventListener( new Debugger() );
			// registerEventListener( new ForecastIssueListener() );
			// registerEventListener( this );
			
			log.info("Forecasting Plugin: Successful start.");
		}
		catch (Exception e)
		{
			log.error("Forecasting Plugin: Unexpected error while starting. Please see stack trace", e);
		}
	}

	public void onStop()
	{
		log.info("Forecasting Plugin: Attempting to stop.");

		try
		{
			unregisterAllEventListeners();

			Configurator.getInstance().disable();

			log.info("Forecasting Plugin: Successful stop.");
		}
		catch (Exception e)
		{
			log.error("Forecasting Plugin: Unexpected error while stopping. Please see stack trace", e);
		}
	}

	public void registerEventListener(Object listener)
	{
		JC.eventPublisher.register(listener);
		eventListeners.add(listener);
	}
	
	public void unregisterAllEventListeners()
	{
		for (Object listener : eventListeners)
			JC.eventPublisher.unregister( listener );
	}
	/*@EventListener
	public void processIssueEvent(IssueEvent issueEvent)
	{
		Long eventTypeId = issueEvent.getEventTypeId();
		Issue issue = issueEvent.getIssue();
		// if it's an event we're interested in, log it
		if (eventTypeId.equals(EventType.ISSUE_CREATED_ID))
		{
			log.debug("\n\n\t#+#+# Issue {} has been created at {}.\n", issue.getKey(), issue.getCreated());
		} 
		else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID))
		{
			log.debug("\n\n\t#+#+# Issue {} has been resolved at {}.\n", issue.getKey(), issue.getResolutionDate());
		} 
		else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID))
		{
			log.debug("\n\n\t#+#+# Issue {} has been closed at {}.\n", issue.getKey(), issue.getUpdated());
		}
		else
		{
			log.debug("\n\n\t#+#+# Unhandled event type\n");
		}
	}*/

	// Does not work. Use onStart instead
	/*@EventListener
	public void processJiraStartedEvent(JiraStartedEvent jiraStartedEvent) 
	{
		log.debug("\n\n\t#+#+# PluginListener: processJiraStartedEvent\n");
		log.debug("\n\n\t#+#+# PluginListener: eventPublisher is " + eventPublisher + " \n");
		if (eventPublisher == null)
			eventPublisher = ComponentAccessor.getComponent(EventPublisher.class);
		log.debug("\n\n\t#+#+# PluginListener: eventPublisher is " + eventPublisher + " \n");
		eventPublisher.register(this);
	}*/
	
	/*@EventListener
	public void processLoginEvent(LoginEvent loginEvent)
	{
		log.debug("\n\n\t#+#+# PluginListener: processLoginEvent (" + loginEvent.getUser().getUsername() + ") \n");
	}*/
	
	/*@EventListener public void logJiraEvent(JiraEvent event)
	{
		log.debug("Jira Event Caught: " + event.getClass());
	}*/
	/*@EventListener public void logJiraEvent(PluginEnabledEvent event)
	{
		log.debug("Jira PluginEnabledEvent Caught: " + event.getClass());
		log.debug("Jira PluginEnabledEvent Plugin Name: " + event.getPlugin().getName());
	}*/
}
