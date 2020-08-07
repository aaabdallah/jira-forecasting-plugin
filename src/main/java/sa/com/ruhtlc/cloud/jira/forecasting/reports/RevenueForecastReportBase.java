package sa.com.ruhtlc.cloud.jira.forecasting.reports;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.web.action.ProjectActionSupport;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.utils.JC;

abstract public class RevenueForecastReportBase extends AbstractReport
{
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(RevenueForecastReportBase.class);
	
	public class ForecastContribution
	{
		private Issue forecast;
		private int amount;
		private int accumulatedAmount;
		
		public ForecastContribution(Issue forecast, int amount)
		{
			this.forecast = forecast;
			this.amount = amount;
			
			Integer existingTotal = forecastAmountTotalsForReport.get(forecast);
			this.accumulatedAmount = existingTotal != null ? (existingTotal + amount) : amount;
			
			forecastAmountTotalsForReport.put(forecast, this.accumulatedAmount);
		}
		
		public Issue getForecast() { return forecast; }
		public int getAmount() { return amount; } // forecast single contribution amount
		public int getAccumulatedAmount() { return accumulatedAmount; } // forecast accumulated amount
		public Double getTotalAmount() { return ((Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsAmountCF()) * (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF()) ); }
		public Double getDuration() { return (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF()); }
		public Date getStartDate() { return ((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF())) ; }
		public Date getEndDate()
		{
			Calendar utilityCalendar = toCalendarWithZeroTime( getStartDate() );
			utilityCalendar.add(Calendar.MONTH, getDuration().intValue());
			utilityCalendar.add(Calendar.DATE, -1);
			return utilityCalendar.getTime();
		}
		public int getLastUpdatedInDaysAgo()
		{
			return daysBetweenDates(forecast.getUpdated(), new Date(System.currentTimeMillis())) - 1;
		}
		public String getReporter()
		{
			if (forecast != null && forecast.getReporter() != null)
			{
				if (forecast.getReporter().getDisplayName() != null)
					return forecast.getReporter().getDisplayName();
				else
					return forecast.getReporter().toString();
			}
			return "Not specified";
		}
		public String getAssignee()
		{
			if (forecast != null && forecast.getAssignee() != null)
			{
				if (forecast.getAssignee().getDisplayName() != null)
					return forecast.getAssignee().getDisplayName();
				else
					return forecast.getAssignee().toString();
			}
			return "Not specified";
		}
	}
	
	protected Long selectedProjectId = null;
	protected int lastUpdatedWarningThreshold = 0;
	protected TreeSet<String> forecastDomains = null;
	protected LinkedHashMap<Issue, Integer> forecastAmountTotalsForReport = null;
	protected int totalForecastContributionsWithinReport = 0;

	/**
	 * Here, we just initialize the instance variables this class is responsible for. The "init"
	 * method supplied by AbstractReport is called at odd (and frequent) times, so it is not a 
	 * good idea to initialize variables there.
	 */
	public void validate(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
	{
		super.validate(action, params);

		forecastDomains = new TreeSet<String>();
		forecastAmountTotalsForReport = new LinkedHashMap<Issue, Integer>();
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	private Date validateDateParam(ProjectActionSupport action, DateTimeFormatter formatter, String paramName, Map params)
	{
		Date date = null;
		try { date = formatter.parse((String) params.get(paramName)); } 
		catch (Exception e) { action.addError(paramName, JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.revenueforecast.error.invaliddate")); }
		return date;
	}
	
	/**
	 * Note if start == end, answer is 1. This method IGNORES the time fields.
	 * @param start inclusive
	 * @param end inclusive
	 * @return number of days or parts thereof (hence 0.25 days will be 1). If start is after end, then 0.
	 */
	public int daysBetweenDates(Date start, Date end)
	{
		if (start.after(end)) return 0;

		Calendar startCalendar = toCalendarWithZeroTime(start);
		Calendar endCalendar = toCalendarWithZeroTime(end);

		return 1 + (int) ((endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis()) / (24 * 60 * 60 * 1000L));
	}
	
	public Calendar toCalendarWithZeroTime(Date d)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(d);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
}
