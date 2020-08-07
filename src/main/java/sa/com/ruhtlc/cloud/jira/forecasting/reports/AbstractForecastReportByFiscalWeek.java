package sa.com.ruhtlc.cloud.jira.forecasting.reports;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
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

abstract public class AbstractForecastReportByFiscalWeek extends AbstractForecastReport
{
	private static final Logger log = LoggerFactory.getLogger(AbstractForecastReportByFiscalWeek.class);
	
	protected String forecastsId = null, selectedProjectId = null; 
	protected String forecastsSourceName = null, forecastsSourceURL = null;
	protected String forecastDateToUse = null;
	protected int lastUpdatedWarningThreshold = 0;
	protected TreeSet<String> forecastDomains = null;
	protected int validForecastsCount = 0;
	protected int forecastsMissingDateToUseCount = 0;
	protected FiscalWeek reportStartingFiscalWeek = null, reportEndingFiscalWeek = null, reportCurrentWeek = null;
	protected LinkedHashMap<FiscalWeek, List<ForecastSummary>> forecastSummaries = null;

	public void validate(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
	{
		super.validate(action, params);

		try
		{
			forecastDomains = new TreeSet<String>();

			int startFiscalYear = Integer.parseInt((String) params.get("startYear"));
			int startFiscalWeek = Integer.parseInt((String) params.get("startWeek"));
			int endFiscalYear = Integer.parseInt((String) params.get("endYear"));
			int endFiscalWeek = Integer.parseInt((String) params.get("endWeek"));
			forecastDateToUse = (String) params.get("forecastDateToUse");
			lastUpdatedWarningThreshold = Integer.parseInt((String) params.get("lastUpdatedWarningThreshold"));
			
			if (endFiscalYear == 0) endFiscalYear = startFiscalYear; // same year
			if (lastUpdatedWarningThreshold < 0) lastUpdatedWarningThreshold = 0;

			// the built-in JIRA "filterprojectpicker" sends back TWO variables: the selected project ID which
			// by default contains the project in which you are browsing for the report, and the forecastId (the
			// actual field) which is ONLY FILLED IN if you actively choose something (project or filter).
			forecastsId = ParameterUtils.getStringParam(params, "forecastsId");
			selectedProjectId = ParameterUtils.getStringParam(params, "selectedProjectId");
			if ((forecastsId == null || forecastsId.isEmpty()) && 
				(selectedProjectId == null || selectedProjectId.isEmpty()))
			{
				action.addError("forecastsId", JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.invalidforecasts"));
			}

			reportStartingFiscalWeek = FiscalYear.getInstance((short) startFiscalYear).getWeekAdjustingForLast(startFiscalWeek);
			reportEndingFiscalWeek = FiscalYear.getInstance((short) endFiscalYear).getWeekAdjustingForLast(endFiscalWeek);

			/*DateTimeFormatter dateTimeFormatter = JC.dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
			startDate = validateDateParam(action, dateTimeFormatter, "startDate", params);
			reportEndDate = validateDateParam(action, dateTimeFormatter, "reportEndDate", params);*/
			
			if ( reportStartingFiscalWeek.compareTo(reportEndingFiscalWeek) > 0)
			{
				action.addErrorMessage(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.prematureenddate"));
			}

			if (!action.getErrorMessages().isEmpty() || !action.getErrors().isEmpty())
				log.debug("Leaving validation with errors");
			else
				log.debug("Report validated at this class level. Spans from week " + reportStartingFiscalWeek.toString() + " to week " + reportEndingFiscalWeek.toString());
		}
		catch (Exception e)
		{
			log.warn(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.unexpected.validation"), e);
			action.addErrorMessage(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.unexpected.validation"));
			log.debug("Leaving validation with errors");
		}
	}

	/**
	 * Set up a map of empty buckets, one bucket per report week.
	 * @param startYear
	 * @param startMonth
	 * @param endYear
	 * @param endMonth
	 */
	protected void initializeForecastSummaries()
	{
		forecastSummaries = new LinkedHashMap<FiscalWeek, List<ForecastSummary>>();

		FiscalWeek weekIterator = reportStartingFiscalWeek;
		
		int failsafeCounter = Integer.parseInt( Configurator.getInstance().getProperties().getProperty("scscjf.report.scheduleforecast.maximum.weeks", "500") );

		while (weekIterator.compareTo(reportEndingFiscalWeek) <= 0 && failsafeCounter-- > 0)
		{
			forecastSummaries.put(weekIterator, new ArrayList<ForecastSummary>());
			weekIterator = weekIterator.getNextWeek();
		}
		reportCurrentWeek = FiscalWeek.identifyFiscalWeek(new Date());
	}

	protected void assignForecastToFiscalWeek(Issue forecast, boolean collectForecastDateToUseChanges)
	{
		Calendar utilityCalendar = null;
		CustomField forecastDateToUseCF = null;
		Date forecastDate = null;
		try
		{
			if (forecastDateToUse.equalsIgnoreCase("Closing Date"))
			{
				utilityCalendar = toCalendarWithZeroTime((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastClosingDateCF()));
				forecastDateToUseCF = Configurator.getInstance().getForecastClosingDateCF();
			}
			else if (forecastDateToUse.equalsIgnoreCase("Booking Date"))
			{
				utilityCalendar = toCalendarWithZeroTime((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastBookingDateCF()));
				forecastDateToUseCF = Configurator.getInstance().getForecastBookingDateCF();
			}
			else if (forecastDateToUse.equalsIgnoreCase("Recognition Date"))
			{
				utilityCalendar = toCalendarWithZeroTime((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastRecognitionDateCF()));
				forecastDateToUseCF = Configurator.getInstance().getForecastRecognitionDateCF();
			}
			else
			{
				utilityCalendar = toCalendarWithZeroTime((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF()));
				forecastDateToUseCF = Configurator.getInstance().getForecastDeliveryDateCF();
			}

			// Issues that do not have any of the four custom fields are filtered away.
			if (utilityCalendar == null)
			{
				log.debug("Forecast missing date of interest, hence ignoring");
				forecastsMissingDateToUseCount++;
				return;
			}

			forecastDate = utilityCalendar.getTime();

			// if forecast starts after report ends, then ignore.
			if (forecastDate.after(reportEndingFiscalWeek.getEndingDate())) return;
	
			// if forecast ends before report starts, then ignore.
			if (forecastDate.before(reportStartingFiscalWeek.getStartingDate())) return;
	
			// We have established that the forecast falls within the report period if we get here.
	
			// Now cycle through the report's weeks and determine where the forecast lies.
			Set<FiscalWeek> reportWeeks = forecastSummaries.keySet();
			for (FiscalWeek oneReportWeek : reportWeeks)
			{
				Date oneReportWeekStartingDate = oneReportWeek.getStartingDate();
				Date oneReportWeekEndingDate = oneReportWeek.getEndingDate();
	
				if (oneReportWeekStartingDate.equals(forecastDate)
					|| oneReportWeekEndingDate.equals(forecastDate)
					|| (oneReportWeekStartingDate.before(forecastDate) && oneReportWeekEndingDate.after(forecastDate)) )
				{
					forecastSummaries.get(oneReportWeek).add( new ForecastSummary(forecastDateToUse, forecast, collectForecastDateToUseChanges) );
					validForecastsCount++;
					break;
				}
			}
	
			// To simplify the display of forecasts within the same domain in the same column
			// Technically this could be done in Javascript but why not just do it here now...
			if (forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF()) != null)
				forecastDomains.add( ((LazyLoadedOption) forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF())).getValue() );
			else
				forecastDomains.add( ForecastSummary.NONE );

		}
		catch (Exception e)
		{
			log.debug("Exception while evaluating possible forecast, hence ignoring.", e);
			forecastsMissingDateToUseCount++;
		}
	}
	
	protected void findForecastsAndAssignToFiscalWeeks(boolean collectForecastDateToUseChanges) throws SearchException
	{
		JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
		Query query = null;
		
		if (forecastsId != null && !forecastsId.isEmpty())
		{
			if (forecastsId.startsWith("project-"))
			{
				query = queryBuilder.where()
					.project(forecastsId.substring(8))
					//.and().issueType(Configurator.getInstance().getForecastIssueType().getId(), Configurator.getInstance().getForecastSubtaskIssueType().getId())
					.buildQuery();
				
				forecastsSourceName = JC.projectManager.getProjectObj(Long.valueOf(forecastsId.substring(8))).getName() + " (Project)";
				forecastsSourceURL = "/browse/" + JC.projectManager.getProjectObj(Long.valueOf(forecastsId.substring(8))).getKey();
			}
			else // forecastsId.startsWith("filter-")
			{
				query = queryBuilder.where()
					.savedFilter(forecastsId.substring(7))
					//.and().issueType(Configurator.getInstance().getForecastIssueType().getId(), Configurator.getInstance().getForecastSubtaskIssueType().getId())
					.buildQuery();

				forecastsSourceName = 
					JC.searchRequestService.getFilter(new JiraServiceContextImpl(JC.jiraAuthenticationContext.getLoggedInUser()), Long.valueOf(forecastsId.substring(7))).getName()
					 + " (Filter)";
				forecastsSourceURL = "/secure/IssueNavigator.jspa?requestId=" + forecastsId.substring(7);
			}
		}
		else
		{
			query = queryBuilder.where()
				.project(selectedProjectId)
				//.and().issueType(Configurator.getInstance().getForecastIssueType().getId(), Configurator.getInstance().getForecastSubtaskIssueType().getId())
				.buildQuery();

			forecastsSourceName = JC.projectManager.getProjectObj(Long.valueOf(selectedProjectId)).getName() + " (Project)";
			forecastsSourceURL = "/browse/" + JC.projectManager.getProjectObj(Long.valueOf(selectedProjectId)).getKey();
		}
		// long issueCount = JC.searchService.searchCount(JC.jiraAuthenticationContext.getLoggedInUser(), query);
		
		SearchResults searchResults = JC.searchService.search(JC.jiraAuthenticationContext.getLoggedInUser(), query, PagerFilter.getUnlimitedFilter());
		validForecastsCount = 0;
		forecastsMissingDateToUseCount = 0;
		
		if (searchResults != null && searchResults.getTotal() > 0)
		{
			if (log.isDebugEnabled()) log.debug("Found " + searchResults.getTotal() + " forecasts matching query conditions.");

			initializeForecastSummaries();

			if (log.isDebugEnabled()) log.debug("Total number of report weeks: " + forecastSummaries.size());

			List<Issue> forecasts = searchResults.getIssues();
			log.debug("Size " + forecasts.size());
			int i = 1;
			for (Issue forecast : forecasts)
			{
				if (log.isDebugEnabled())
				{
					log.debug("Forecast #" + i++);
					Debugger.logForecastFields(log, forecast);
					
					List<CustomField> forecastCustomFields = JC.customFieldManager.getCustomFieldObjects(forecast);
					StringBuilder buff = new StringBuilder();
					for (CustomField fcf : forecastCustomFields)
						buff.append('\t').append(fcf.getFieldName()).append(" --> ").append(forecast.getCustomFieldValue(fcf)).append('\n');
					log.debug(buff.toString());
				}
				assignForecastToFiscalWeek(forecast, collectForecastDateToUseChanges);
			}
		}
		else
		{
			log.debug("No forecasts found matching query conditions");
		}		
	}
}
