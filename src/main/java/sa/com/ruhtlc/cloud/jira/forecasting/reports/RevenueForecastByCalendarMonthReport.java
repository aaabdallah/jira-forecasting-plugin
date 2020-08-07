package sa.com.ruhtlc.cloud.jira.forecasting.reports;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
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

public class RevenueForecastByCalendarMonthReport extends RevenueForecastReportBase
{
	private static final Logger log = LoggerFactory.getLogger(RevenueForecastByCalendarMonthReport.class);
		
	private Date reportStartingDate = null, reportEndingDate = null;
	private LinkedHashMap<Date, List<ForecastContribution>> forecastContributions = null;

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
			int startYear = Integer.parseInt((String) params.get("startYear"));
			int startMonth = Integer.parseInt((String) params.get("startMonth"));
			int endYear = Integer.parseInt((String) params.get("endYear"));
			int endMonth = Integer.parseInt((String) params.get("endMonth"));
			lastUpdatedWarningThreshold = Integer.parseInt((String) params.get("lastUpdatedWarningThreshold"));
			
			if (lastUpdatedWarningThreshold < 0) lastUpdatedWarningThreshold = 0;

			selectedProjectId = ParameterUtils.getLongParam(params, "selectedProjectId");
			if (selectedProjectId == null)
				action.addError("projectId", JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.revenueforecast.error.invalidproject"));

			Calendar calendar = new GregorianCalendar(startYear, startMonth-1, 1); // has zero for all time fields
			reportStartingDate = calendar.getTime();
			calendar.set(endYear, endMonth-1, 1);
			calendar.add(Calendar.MONTH, 1);
			calendar.add(Calendar.DATE, -1);
			reportEndingDate = calendar.getTime();

			/*DateTimeFormatter dateTimeFormatter = JC.dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
			startDate = validateDateParam(action, dateTimeFormatter, "startDate", params);
			reportEndDate = validateDateParam(action, dateTimeFormatter, "reportEndDate", params);*/
			
			if ( reportStartingDate.after(reportEndingDate) )
				action.addErrorMessage(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.revenueforecast.error.prematureenddate"));

			log.debug("Report validated. Spans from date " + reportStartingDate.toString() + " to date " + reportEndingDate.toString());
		}
		catch (Exception e)
		{
			log.warn(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.revenueforecast.error.unexpected.validation"), e);
			action.addErrorMessage(JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.revenueforecast.error.unexpected.validation"));
		}
	}

	/**
	 * Set up a map of empty buckets, one bucket per report week.
	 * @param startYear
	 * @param startMonth
	 * @param endYear
	 * @param endMonth
	 */
	private void initializeForecastContributions()
	{
		forecastContributions = new LinkedHashMap<Date, List<ForecastContribution>>();
		
		int failsafeCounter = Integer.parseInt( Configurator.getInstance().getProperties().getProperty("scscjf.report.revenueforecast.maximum.weeks", "500") );

		Calendar currentCalendar = new GregorianCalendar(), endingCalendar = new GregorianCalendar();
		currentCalendar.setTime(reportStartingDate);
		endingCalendar.setTime(reportEndingDate);
		
		while (!currentCalendar.after(endingCalendar) && failsafeCounter-- > 0)
		{
			forecastContributions.put(currentCalendar.getTime(), new ArrayList<ForecastContribution>());
			currentCalendar.add(Calendar.MONTH, 1);
		}
	}

	/**
	 * Note this also has a side effect of adding contribution to the total contribution amount for the report.
	 * @return true iff the forecast contributed a nonzero amount
	 */
	private boolean calculateForecastContribution(Issue forecast)
	{
		Calendar utilityCalendar = toCalendarWithZeroTime((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF()));
		Date forecastStartDate = utilityCalendar.getTime();

		// if forecast starts after report ends, then ignore.
		if (forecastStartDate.after(reportEndingDate)) return false;

		Double forecastDuration = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF());
		if (forecastDuration <= 0) return false;
		utilityCalendar.add(Calendar.MONTH, forecastDuration.intValue());
		utilityCalendar.add(Calendar.DATE, -1);
		Date forecastEndDate = utilityCalendar.getTime();

		// if forecast ends before report starts, then ignore.
		if (forecastEndDate.before(reportStartingDate)) return false;

		// We have established that the forecast period overlaps (partially or completely) 
		// the entire report period if we get here.

		// Now cycle through the report's months and determine if the forecast makes any
		// contribution to each month.
		
		// First some calculations related to the forecast amount (distributed across the forecast duration)
		Double forecastAmountPerCalendarMonth = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsAmountCF());
		if (forecastAmountPerCalendarMonth <= 0) return false;
		// Double forecastAmountPerDay = (forecastAmountPerCalendarMonth * forecastDuration) / daysBetweenDates(forecastStartDate, forecastEndDate);

		Set<Date> reportMonths = forecastContributions.keySet();
		for (Date oneReportMonthStartingDate : reportMonths)
		{
			utilityCalendar.setTime(oneReportMonthStartingDate);
			utilityCalendar.add(Calendar.MONTH, 1);
			utilityCalendar.add(Calendar.DATE, -1);
			Date oneReportMonthEndingDate = utilityCalendar.getTime();

			// There are five cases to consider, depending on the overlap:
			// Forecast Period:  >        FFFFFFF        <
			// Month Period:     > M1M  M2M M3M M4M  M5M <
			
			int contributionAmount = 0;
			
			// Cases 1 & 5: if report month in question does not overlap at all with the forecast period, then ignore it
			if (oneReportMonthEndingDate.before(forecastStartDate) || oneReportMonthStartingDate.after(forecastEndDate))
				continue;
			// Case 2: else if report month in question overlaps the forecast period start partially
			else if (oneReportMonthStartingDate.before(forecastStartDate))
			{
				// This way gives inconsistent results: not every month has the same amount of days. 
				// contributionAmount = (int) (Math.round(forecastAmountPerDay * daysBetweenDates(forecastStartDate, oneReportMonthEndingDate)) );

				// So we will simply say: determine a percentage for the first month, then give the remainder to the last month 
				utilityCalendar.setTime(forecastStartDate);
				contributionAmount = (int) (Math.round(forecastAmountPerCalendarMonth * calculateMonthCoverageFraction(utilityCalendar)));
			}
			// Case 4: else if report month in question overlaps the forecast period end partially
			else if (oneReportMonthEndingDate.after(forecastEndDate))
			{
				// This way gives inconsistent results: not every month has the same amount of days. 
				// contributionAmount = (int) (Math.round(forecastAmountPerDay * daysBetweenDates(oneReportMonthStartingDate, forecastEndDate)) );

				// So we will simply say: determine a percentage for the first month, then give the remainder to the last month 
				utilityCalendar.setTime(forecastStartDate);
				contributionAmount = (int) (Math.round(forecastAmountPerCalendarMonth * (1-calculateMonthCoverageFraction(utilityCalendar)) ));
			}
			// Case 3: the report month is completely within the forecast period
			else
				contributionAmount = forecastAmountPerCalendarMonth.intValue();

			forecastContributions.get(oneReportMonthStartingDate).add( new ForecastContribution(forecast, contributionAmount) );
			totalForecastContributionsWithinReport += contributionAmount;
		}
		
		// To simplify the display of forecasts within the same domain in the same column
		// forecastDomains.add( ((LazyLoadedOption) forecast.getCustomFieldValue(Configurator.getInstance().getForecastDomainCF())).getValue() );

		return true;
	}

	/**
	 * @return the fraction (0+ to 1) of the month from the passed in date to the end of that date's month.
	 * Rounds up strictly, so Jan 31 would give 1/31.
	 */
	protected double calculateMonthCoverageFraction(Calendar cal)
	{
		if (cal.get(Calendar.DATE) == 1) return 1.0;
		
		double dayOfMonth = cal.get(Calendar.DATE);
		
		cal.add(Calendar.MONTH, 1);
		cal.set(Calendar.DATE, 1);
		cal.add(Calendar.DATE, -1);
		
		double lastDayOfMonth = cal.get(Calendar.DATE);
		
		return (lastDayOfMonth - dayOfMonth + 1) / lastDayOfMonth; 
	}

	/**
	 * This method calculates the percentage overlap between an interval (specified by start and end dates) and
	 * a third date to the end of the same interval. Granularity is limited to the DAY, hence even if the passed in
	 * dates have important hours, minutes, or seconds, they are IGNORED. This means if the end of an interval
	 * is Dec 31, 2015 11:00:00 PM and the date in question is Dec 31, 2015 11:30:00 PM, there IS an overlap of
	 * 1 day. In this case, depending on the start date, the result will be a nonzero fraction.
	 * IMPORTANT: the interval start date and interval end date are INCLUSIVE. This means if the interval start
	 * date is equal to the interval end date (again IGNORING TIME), it describes an interval of ONE DAY.
	 * 
	 * @param subIntervalStartDate the subinterval start date. the subinterval end date is the same as the interval end date
	 * @param intervalStartDate interval start date
	 * @param intervalEndDate interval end date
	 * @return a fraction between 0 and 1.
	private double calculateIntervalOverlapInDaysFraction(Date subIntervalStartDate, Date intervalStartDate, Date intervalEndDate)
	{
		Calendar subIntervalStartCalendar = toCalendarWithZeroTime(subIntervalStartDate);
		Calendar intervalStartCalendar = toCalendarWithZeroTime(intervalStartDate);
		Calendar intervalEndCalendar = toCalendarWithZeroTime(intervalEndDate);

		if (subIntervalStartCalendar.getTimeInMillis() <= intervalStartCalendar.getTimeInMillis()) return 1.0;
		if (intervalEndCalendar.getTimeInMillis() < subIntervalStartCalendar.getTimeInMillis()) return 0.0;

		int intervalLengthDays = 1 +
			(int) ((intervalEndCalendar.getTimeInMillis() - intervalStartCalendar.getTimeInMillis()) / (24 * 60 * 60 * 1000L));
		int subIntervalStartToIntervalEndDays = 1 +
			(int) ((intervalEndCalendar.getTimeInMillis() - subIntervalStartCalendar.getTimeInMillis()) / (24 * 60 * 60 * 1000L));

		return ((double) subIntervalStartToIntervalEndDays) / intervalLengthDays;
	}
	 */
	
	public String generateReportHtml(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map map) throws Exception
	{		
		if (!action.getErrorMessages().isEmpty() || !action.getErrors().isEmpty())
			return null;

		JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
		Query query = queryBuilder.where()
			.project(selectedProjectId)
			.and().issueType(Configurator.getInstance().getForecastIssueType().getId(), Configurator.getInstance().getForecastSubtaskIssueType().getId())
			.buildQuery();
		// long issueCount = JC.searchService.searchCount(JC.jiraAuthenticationContext.getLoggedInUser(), query);
		
		SearchResults searchResults = JC.searchService.search(JC.jiraAuthenticationContext.getLoggedInUser(), query, PagerFilter.getUnlimitedFilter());
		int forecastCount = 0;
		
		if (searchResults != null && searchResults.getTotal() > 0)
		{
			if (log.isDebugEnabled()) log.debug("Found " + searchResults.getTotal() + " forecasts matching query conditions.");

			initializeForecastContributions();

			if (log.isDebugEnabled()) log.debug("Total number of report months: " + forecastContributions.size());

			List<Issue> forecasts = searchResults.getIssues();
			for (Issue forecast : forecasts)
			{
				if (log.isDebugEnabled()) Debugger.logForecastFields(log, forecast);
				if (calculateForecastContribution(forecast))
					forecastCount++;
			}
		}
		else
		{
			log.debug("No forecasts found matching query conditions");
		}
		
		Map<String, Object> velocityParams = new HashMap<String, Object>();

		// DateTimeFormatter dateTimeFormatter = JC.dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER);
		velocityParams.put("reportStartingDate", reportStartingDate);
		velocityParams.put("reportEndingDate", reportEndingDate);
		velocityParams.put("forecastContributions", forecastContributions);
		velocityParams.put("forecastDomains", forecastDomains);
		velocityParams.put("totalForecastContributionsWithinReport", totalForecastContributionsWithinReport);
		
		velocityParams.put("selectedProjectId", selectedProjectId);
		velocityParams.put("selectedProjectName", JC.projectManager.getProjectObj(selectedProjectId).getName());
		velocityParams.put("lastUpdatedWarningThreshold", lastUpdatedWarningThreshold);
		
		velocityParams.put("forecastCount", forecastCount);
		
		velocityParams.put("yearMonthDayFormatter", new SimpleDateFormat("MMM dd, yyyy"));
		velocityParams.put("yearMonthFormatter", new SimpleDateFormat("yyyy.MM"));
		velocityParams.put("decimalFormatter", new DecimalFormat("#,###"));

		log.debug("Dispatching to report view template.");
		
		return descriptor.getHtml("view", velocityParams);
	}
}
