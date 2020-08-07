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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.ReportModuleDescriptor;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.forecasting.reports.AbstractForecastReport.ForecastSummary;
import sa.com.ruhtlc.cloud.jira.utils.Debugger;
import sa.com.ruhtlc.cloud.jira.utils.JC;
import sa.com.ruhtlc.cloud.jira.utils.fiscal.FiscalWeek;
import sa.com.ruhtlc.cloud.jira.utils.fiscal.FiscalYear;

public class ScheduleForecastByFiscalWeekReport extends AbstractForecastReportByFiscalWeek
{
	private static final Logger log = LoggerFactory.getLogger(ScheduleForecastByFiscalWeekReport.class);
		
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

	}
	
	public String generateReportHtml(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map map) throws Exception
	{
		Map<String, Object> velocityParams = new HashMap<String, Object>();

		try
		{
			log.debug("Entering generateReportHtml");
			
			if (!action.getErrorMessages().isEmpty() || !action.getErrors().isEmpty())
				return null;
	
			findForecastsAndAssignToFiscalWeeks(true);
			
			// DateTimeFormatter dateTimeFormatter = JC.dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
			velocityParams.put("reportStartingFiscalWeek", reportStartingFiscalWeek);
			velocityParams.put("reportEndingFiscalWeek", reportEndingFiscalWeek);
			velocityParams.put("reportCurrentWeek", reportCurrentWeek);
			velocityParams.put("reportCurrentFiscalYear", reportCurrentWeek.getFiscalYear());
			velocityParams.put("reportCurrentFiscalWeekOfYear", reportCurrentWeek.getFiscalWeekOfYear());
			velocityParams.put("reportCurrentFiscalMonthOfYear", reportCurrentWeek.getFiscalMonthOfYear());
			velocityParams.put("reportCurrentFiscalQuarter", reportCurrentWeek.getFiscalQuarter());
			
			velocityParams.put("forecastDateToUse", forecastDateToUse);
			velocityParams.put("forecastSummaries", forecastSummaries);
			velocityParams.put("forecastDomains", forecastDomains);
			
			velocityParams.put("jsonForecastSummariesWithHtml", forecastSummariesToJSON());

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
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy");
		boolean atLeastOneSummary = false;
		StringBuilder buff = new StringBuilder(1024);
		buff.append('[');
		for (Entry<FiscalWeek, List<ForecastSummary>> bucket : forecastSummaries.entrySet())
		{
			FiscalWeek fiscalWeek = bucket.getKey();
			List<ForecastSummary> forecastSummariesForWeek = bucket.getValue();
			

			// Add the default entry that just holds the position of the fiscal week (even if there are no forecasts then)
			if (atLeastOneSummary) buff.append(',');
			buff.append('\n');
			buff.append('{');
			buff.append("\"ID\":\"").append("none").append('"');
			buff.append(',').append("\"Fiscal Week\":\"").append(dateFormatter.format(fiscalWeek.getStartingDate())).append('"');
			buff.append(',').append("\"Fiscal Year\":").append(fiscalWeek.getFiscalYear());
			buff.append(',').append("\"Fiscal Quarter\":").append(fiscalWeek.getFiscalQuarter());
			buff.append(',').append("\"Fiscal Month of Year\":").append(fiscalWeek.getFiscalMonthOfYear());
			buff.append(',').append("\"Fiscal Month of Quarter\":").append(fiscalWeek.getFiscalMonthOfQuarter());
			buff.append(',').append("\"Fiscal Week of Month\":").append(fiscalWeek.getFiscalWeekOfMonth());
			buff.append(',').append("\"Fiscal Week of Year\":").append(fiscalWeek.getFiscalWeekOfYear());
			buff.append('}');

			atLeastOneSummary = true;
			
			if (forecastSummariesForWeek != null && !forecastSummariesForWeek.isEmpty())
			{
				for (ForecastSummary forecastSummary : forecastSummariesForWeek)
				{
					if (atLeastOneSummary) buff.append(',');
					buff.append('\n');
					buff.append('{');
					buff.append("\"ID\":\"").append(forecastSummary.getForecast().getKey()).append('"');
					buff.append(',').append("\"Summary\":\"").append(forecastSummary.getForecast().getSummary().replace("\"", "'")).append('"');
					buff.append(',').append("\"Last Updated\":").append(forecastSummary.getLastUpdatedInDaysAgo());
					buff.append(',').append("\"Last Updated By\":\"").append(forecastSummary.getLastUpdatedBy()).append('"');
					buff.append(',').append("\"Forecast Category\":\"").append(forecastSummary.getCategory()).append('"');
					buff.append(',').append("\"Assignee\":\"").append(forecastSummary.getAssignee()).append('"');
					buff.append(',').append("\"Status\":\"").append(forecastSummary.getStatus()).append('"');
					buff.append(',').append("\"Completion Status\":\"").append(forecastSummary.getCompletionStatus().toString()).append('"');
					buff.append(',').append("\"Current " + forecastDateToUse + "\":\"").append(dateFormatter.format(forecastSummary.getForecastDateToUse(forecastDateToUse))).append('"');
					buff.append(',').append("\"Last Reviewed " + forecastDateToUse + "\":\"").append(dateFormatter.format(forecastSummary.getLastReviewedDate(forecastDateToUse))).append('"');
					buff.append(',').append("\"Fiscal Week\":\"").append(dateFormatter.format(fiscalWeek.getStartingDate())).append('"');
					buff.append(',').append("\"Fiscal Year\":").append(fiscalWeek.getFiscalYear());
					buff.append(',').append("\"Fiscal Quarter\":").append(fiscalWeek.getFiscalQuarter());
					buff.append(',').append("\"Fiscal Month of Year\":").append(fiscalWeek.getFiscalMonthOfYear());
					buff.append(',').append("\"Fiscal Month of Quarter\":").append(fiscalWeek.getFiscalMonthOfQuarter());
					buff.append(',').append("\"Fiscal Week of Month\":").append(fiscalWeek.getFiscalWeekOfMonth());
					buff.append(',').append("\"Fiscal Week of Year\":").append(fiscalWeek.getFiscalWeekOfYear());
					buff.append(',').append("\"Total Value\":").append(forecastSummary.getTotalAmount().intValue());
					buff.append(',').append("\"Description\":\"").append(forecastSummary.getFormattedDescription().replace("\"", "'").replace("\n", "")).append('"');
					if (forecastSummary.getForecast().getParentObject() != null)
						buff.append(',').append("\"Parent\":\"").append(forecastSummary.getForecast().getParentObject().getKey()).append('"');
					else
						buff.append(',').append("\"Parent\":\"").append('"');
					
					List<IssueLink> issueLinks = forecastSummary.getOutwardLinks();
					if (issueLinks != null && !issueLinks.isEmpty())
					{
						buff.append(',').append("\"Outward Links\":");
						buff.append('[');
						boolean firstLink = true;
						for (IssueLink issueLink : issueLinks)
						{
							if (firstLink)
								firstLink = false;
							else
								buff.append(',');
							buff.append('{');
							buff.append("\"ID\":\"").append(issueLink.getDestinationObject().getKey()).append('"');
							buff.append(',').append("\"Summary\":\"").append(issueLink.getDestinationObject().getSummary().replace("\"", "'")).append('"');
							buff.append('}');
						}
						buff.append(']');
					}
					issueLinks = forecastSummary.getInwardLinks();
					if (issueLinks != null && !issueLinks.isEmpty())
					{
						buff.append(',').append("\"Inward Links\":");
						buff.append('[');
						boolean firstLink = true;
						for (IssueLink issueLink : issueLinks)
						{
							if (firstLink)
								firstLink = false;
							else
								buff.append(',');
							buff.append('{');
							buff.append("\"ID\":\"").append(issueLink.getSourceObject().getKey()).append('"');
							buff.append(',').append("\"Summary\":\"").append(issueLink.getSourceObject().getSummary().replace("\"", "'")).append('"');
							buff.append('}');
						}
						buff.append(']');
					}
					
					//buff.append(',').append("").append("");
					///browse/$forecastSummary.getForecast().getKey()
					buff.append('}');
				}
			}
		}
		buff.append(']');
		return buff.toString();
	}
}
