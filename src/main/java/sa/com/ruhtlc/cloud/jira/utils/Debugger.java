package sa.com.ruhtlc.cloud.jira.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.JiraEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.forecasting.Constants;

public class Debugger
{
	public Debugger()
	{
	}

	/**
	 * Useful for listing all the possible fields within JIRA; this should
	 * include any custom fields you have created.
	 */
	public static void logAllNavigableFields(Logger log)
	{
		try
		{
			for (NavigableField field : JC.fieldManager.getNavigableFields())
				log.debug("Field name: " + field.getName() + ", ID: " + field.getId());
		} catch (Exception e)
		{
			log.debug("Error during logAllNavigableFields", e);
		}
	}
	
	public static void logFieldConfigurationSchemes(Logger log, CustomField customField)
	{
		StringBuilder buff = new StringBuilder();
		List<FieldConfigScheme> schemes = JC.fieldConfigSchemeManager.getConfigSchemesForField(customField);
		
		buff.append("\n\t").append(customField.getName());
		for (FieldConfigScheme scheme : schemes)
		{
			
			buff.append("\n\t").append("Field config scheme: ").append(scheme.getName());
			buff.append("\n\t\t").append("Global? ").append(scheme.isGlobal() ? "Yes" : "No");
			buff.append("\n\t\t").append("All Projects? ").append(scheme.isAllProjects() ? "Yes" : "No");
			buff.append("\n\t\t").append("All Issue Types? ").append(scheme.isAllIssueTypes() ? "Yes" : "No");

			buff.append("\n\t\t").append("Contexts:");
			List<JiraContextNode> contexts = scheme.getContexts();
			for (JiraContextNode context : contexts)
			{
				buff.append("\n\t\t\t").append("Jira Context Node object: ").append(context);
				buff.append("\n\t\t\t").append("In global issue context? ").append(context.isInContext( IssueContext.GLOBAL ) );
				buff.append("\n\t\t\t").append("Context Issue Id: ").append(context.getIssueTypeId()).append(" , Project Id: ").append(context.getProjectId());
			}
			
			// map of issue type ID (NOT NAME as Atlassian's documentation says!) to field configuration
			Map<String, FieldConfig> fieldConfigMap = scheme.getConfigs();
			for (String issueTypeId : fieldConfigMap.keySet())
			{
				FieldConfig fieldConfig = fieldConfigMap.get( issueTypeId );
				buff.append("\n\t\t").append("Issue type ID: ").append(issueTypeId).append(" , Field Configuration: ").append(fieldConfig.getName());
				buff.append("\n\t\t").append("Custom field name: ").append(fieldConfig.getCustomField().getName());
				Options options = JC.optionsManager.getOptions(fieldConfig);
				buff.append("\n\t\t").append("Options: ").append(Arrays.toString(options.toArray()));
			}
		}
		log.debug(buff.toString());
	}

	public static void logForecastFields(Logger log, Issue forecast)
	{
		try
		{
			StringBuilder builder = new StringBuilder(256);
			builder.append("\n\tForecast Issue ID: [Long] ").append(forecast.getId());
			builder.append("\n\tForecast Issue Key: [String] ").append(forecast.getKey());
			builder.append("\n\tForecast Summary: [String] ").append(forecast.getSummary());
			builder.append("\n\tForecast Assignee: [String] ").append(forecast.getAssignee() != null ? forecast.getAssignee().getDisplayName() : "null");

			builder.append("\n\tForecast Category: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF()))).append("] ")
			.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF()));
			builder.append("\n\tForecast Status: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastStatusCF()))).append("] ")
			.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastStatusCF()));

			builder.append("\n\tForecast Closing Date: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastClosingDateCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastClosingDateCF()));
			builder.append("\n\tForecast Booking Date: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastBookingDateCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastBookingDateCF()));
			builder.append("\n\tForecast Delivery Date: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF()));
			builder.append("\n\tForecast Recognition Date: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastRecognitionDateCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastRecognitionDateCF()));

			builder.append("\n\tForecast Initial Payment Amount: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastInitialPaymentAmountCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastInitialPaymentAmountCF()));
			builder.append("\n\tForecast Monthly Payments Amount: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsAmountCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsAmountCF()));
			builder.append("\n\tForecast Monthly Payments Duration: [").append(getClassName(forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF()))).append("] ")
				.append(forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF()));
			log.debug(builder.toString());
		} 
		catch (Exception e)
		{
			log.debug("Error during logForecastFields", e);
		}
	}

	public static void logStackTrace(Logger log)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StringBuilder builder = new StringBuilder();
		for (StackTraceElement e : stackTrace)
			builder.append(e.toString()).append('\n');
		
		log.debug(builder.toString());
	}

	private static String getClassName(Object obj)
	{
		return obj == null ? "NULL" : obj.getClass().getName();
	}

	@EventListener
	public void logJiraEvent(Logger log, JiraEvent jiraEvent)
	{
		log.debug("LOGGING JIRA EVENT: " + jiraEvent.getClass().getCanonicalName());
	}
}
