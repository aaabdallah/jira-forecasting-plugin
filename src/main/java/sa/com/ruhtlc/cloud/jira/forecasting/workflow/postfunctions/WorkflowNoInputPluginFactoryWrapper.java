package sa.com.ruhtlc.cloud.jira.forecasting.workflow.postfunctions;

import com.atlassian.jira.plugin.workflow.WorkflowNoInputPluginFactory;

/**
 * Strange error in JIRA: if the parent class is used directly, it complains about a
 * NoClassFoundError during startup. This was the only solution found.
 */
public class WorkflowNoInputPluginFactoryWrapper extends WorkflowNoInputPluginFactory
{

}
