package sa.com.ruhtlc.cloud.jira.forecasting.reports;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.ReportModuleDescriptor;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.utils.Debugger;
import sa.com.ruhtlc.cloud.jira.utils.JC;
import sa.com.ruhtlc.cloud.jira.utils.fiscal.FiscalWeek;
import sa.com.ruhtlc.cloud.jira.utils.fiscal.FiscalYear;

public class ForecastAnalyticsDashboardReport extends AbstractForecastReportByFiscalWeek
{
	private static final Logger log = LoggerFactory.getLogger(ForecastAnalyticsDashboardReport.class);

	protected String[] extraFieldFilters = null;

	/**
	 * Note that this is called frequently and in unexpected places (e.g. AFTER
	 * a report is run). Be careful what you put in "init".
	 */
	public void init(ReportModuleDescriptor reportModuleDescriptor)
	{
		super.init(reportModuleDescriptor);
	}

	public void validate(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
	{
		super.validate(action, params);
		
		try
		{
			extraFieldFilters = ParameterUtils.getStringArrayParam(params, "extraFieldFilters");
			if (extraFieldFilters != null)
				log.debug(Arrays.toString( extraFieldFilters ));
		}
		catch (Exception e)
		{
			log.warn(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.unexpected.validation"), e);
			action.addErrorMessage(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.unexpected.validation"));
			log.debug("Leaving validation with errors");
		}
	}

	public String generateReportHtml(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map map) throws Exception
	{
		Map<String, Object> velocityParams = new HashMap<String, Object>();

		try
		{
			log.debug("Entering generateReportHtml");
			
			if (!action.getErrorMessages().isEmpty() || !action.getErrors().isEmpty())
				return null;
	
			findForecastsAndAssignToFiscalWeeks(false);
			
			// DateTimeFormatter dateTimeFormatter = JC.dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
			velocityParams.put("reportStartingFiscalWeek", reportStartingFiscalWeek);
			velocityParams.put("reportEndingFiscalWeek", reportEndingFiscalWeek);
			velocityParams.put("reportCurrentWeek", reportCurrentWeek);
			
			velocityParams.put("forecastDateToUse", forecastDateToUse);
			velocityParams.put("forecastSummaries", forecastSummaries);
			velocityParams.put("jsonForecastSummariesWithHtml", forecastSummariesToJSON());
			velocityParams.put("extraFieldFilterNamesWithHtml", extraFieldFilterNamesToJSON());
			velocityParams.put("forecastDomains", forecastDomains);
			
			velocityParams.put("forecastsId", forecastsId);
			velocityParams.put("selectedProjectId", selectedProjectId);
			velocityParams.put("forecastsSourceName", forecastsSourceName);
			velocityParams.put("forecastsSourceURL", forecastsSourceURL);
			
			velocityParams.put("lastUpdatedWarningThreshold", lastUpdatedWarningThreshold);
			
			velocityParams.put("validForecastsCount", validForecastsCount);
			velocityParams.put("forecastsMissingDateToUseCount", forecastsMissingDateToUseCount);
			
			velocityParams.put("MMMddyyyyFormatter", new SimpleDateFormat("MMM dd, yyyy"));
			velocityParams.put("yyyyMMddFormatter", new SimpleDateFormat("yyyy-MM-dd"));
			velocityParams.put("ddMMyyFormatter", new SimpleDateFormat("dd-MM-yy"));
			velocityParams.put("yearMonthFormatter", new SimpleDateFormat("yyyy.MM"));
			velocityParams.put("decimalFormatter", new DecimalFormat("#,###"));
			
			velocityParams.put("ForecastChangeItem", ForecastChangeItem.class);
	
			log.debug("Dispatching to report view template.");
			
			return descriptor.getHtml("view", velocityParams);
		}
		catch (Exception e)
		{
			if (JC.jiraAuthenticationContext.isLoggedInUser())
			{
				velocityParams.put("title", "Unexpected error!");
				velocityParams.put("body", "An unexpected error has occurred during report generation. Please copy the system error details below, and send them to a system administrator.");
				velocityParams.put("exception", e);
			}
			else
			{
				velocityParams.put("title", "Error: Not Logged In.");
				velocityParams.put("body", "Please login to use this report.");
			}
			return descriptor.getHtml("error", velocityParams);
		}
	}

	private String forecastSummariesToJSON()
	{
		if (forecastSummaries == null || forecastSummaries.isEmpty())
			return "[]";
		
		boolean atLeastOneSummary = false;
		StringBuilder buff = new StringBuilder(1024);
		buff.append('[');
		for (Entry<FiscalWeek, List<ForecastSummary>> bucket : forecastSummaries.entrySet())
		{
			FiscalWeek fiscalWeek = bucket.getKey();
			List<ForecastSummary> forecastSummariesForWeek = bucket.getValue();
			
			if (forecastSummariesForWeek != null && !forecastSummariesForWeek.isEmpty())
			{
				for (ForecastSummary forecastSummary : forecastSummariesForWeek)
				{
					if (atLeastOneSummary) buff.append(',');
					buff.append('\n');
					buff.append('{');
					buff.append("\"ID\":\"").append(forecastSummary.getForecast().getKey()).append('"');
					buff.append(',').append("\"Fiscal Year\":").append(fiscalWeek.getFiscalYear());
					buff.append(',').append("\"Fiscal Quarter\":").append(fiscalWeek.getFiscalQuarter());
					buff.append(',').append("\"Fiscal Month of Year\":").append(fiscalWeek.getFiscalMonthOfYear());
					buff.append(',').append("\"Fiscal Month of Quarter\":").append(fiscalWeek.getFiscalMonthOfQuarter());
					buff.append(',').append("\"Fiscal Week of Month\":").append(fiscalWeek.getFiscalWeekOfMonth());
					buff.append(',').append("\"Fiscal Week of Year\":").append(fiscalWeek.getFiscalWeekOfYear());
					buff.append(',').append("\"Total Value\":").append(forecastSummary.getTotalAmount().intValue());
					buff.append(',').append("\"Assignee\":\"").append(forecastSummary.getAssignee()).append('"');
					buff.append(',').append("\"Status\":\"").append(forecastSummary.getStatus()).append('"');
					buff.append(',').append("\"Summary\":\"").append(forecastSummary.getForecast().getSummary()).append('"');
					
					if (extraFieldFilters != null && extraFieldFilters.length > 0)
					{
						for (String extraFieldFilter : extraFieldFilters)
						{
							CustomField customField = JC.customFieldManager.getCustomFieldObject(extraFieldFilter);
							Object customFieldValue = forecastSummary.getForecast().getCustomFieldValue( customField );
							
							buff.append(',').append("\"").append(customField.getFieldName()).append("\":");
							
							if (customFieldValue == null) 
								buff.append("\"null\"");
							else if (customFieldValue instanceof LazyLoadedOption)
								buff.append("\"" + ((LazyLoadedOption) customFieldValue).getValue() + "\"");
							else if (customFieldValue instanceof Number)
								buff.append(customFieldValue.toString());
							else
								buff.append("\"" + customFieldValue.toString() + "\"");
						}
					}
				
					//buff.append(',').append("").append("");
					///browse/$forecastSummary.getForecast().getKey()
					buff.append('}');
					atLeastOneSummary = true;
				}
			}
		}
		buff.append(']');
		return buff.toString();
	}

	private String extraFieldFilterNamesToJSON()
	{
		if (forecastSummaries == null || forecastSummaries.isEmpty())
			return "[]";
		if (extraFieldFilters != null && extraFieldFilters.length > 0)
		{
			StringBuilder buff = new StringBuilder();
			buff.append('[');
			
			boolean atLeastOneField = false;
			for (String extraFieldFilter : extraFieldFilters)
			{
				if (atLeastOneField) buff.append(',');
				CustomField customField = JC.customFieldManager.getCustomFieldObject(extraFieldFilter);
				buff.append('"').append(customField.getFieldName()).append('"');
				atLeastOneField = true;
			}
			buff.append(']');
			return buff.toString();
		}
		else
			return "[]";
	}
}
