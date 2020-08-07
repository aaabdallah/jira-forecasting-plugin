package sa.com.ruhtlc.cloud.jira.forecasting.reports;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.velocity.htmlsafe.HtmlSafe;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;
import sa.com.ruhtlc.cloud.jira.utils.JC;
import sa.com.ruhtlc.cloud.jira.utils.fiscal.FiscalWeek;

abstract public class AbstractForecastReport extends AbstractReport
{
	private static final Logger log = LoggerFactory.getLogger(AbstractForecastReport.class);

	public enum CompletionStatus {
		DONE // Jira status is in the DONE category 
		{ public String toString() { return "Done"; } },
		ONSCHEDULE // Non-DONE, no other warnings (neither delayed nor postponed)
		{ public String toString() { return "On Schedule"; } },
		DELAYED // Non-DONE, forecast-date-to-use has passed
		{ public String toString() { return "Delayed"; } },
		POSTPONED // Non-DONE, forecast-date-to-use in this period or in the future, but date was moved in violation of rules
		{ public String toString() { return "Postponed"; } },
	};
	
	public class ForecastSummary
	{
		public static final String NONE = "None";

		private Issue forecast;
		private List<ForecastChangeItem> forecastDateToUseChanges = null;
		private Date lastReviewedDate = null; // the last date that was reviewed (NOT the date on which it was reviewed)
		private CompletionStatus completionStatus = CompletionStatus.ONSCHEDULE;
		private String debugMessage = null;
		
		public ForecastSummary(String forecastDateToUse, Issue forecast, boolean collectForecastDateToUseChanges)
		{
			this.forecast = forecast;
			
			if (collectForecastDateToUseChanges)
				collectForecastDateToUseChanges(forecastDateToUse, true);
			determineCompletionStatus(forecastDateToUse);
		}
		
		public Issue getForecast() { return forecast; }
		
		/**
		 * @return total value field if non-null, else (initialPayment + monthlyPayment x durationMonths)
		 */
		public Double getTotalAmount() 
		{
			Double totalValue = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastTotalValueCF());
			
			if (totalValue != null)
				return totalValue;
			
			Double initialPayment = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastInitialPaymentAmountCF());
			Double monthlyPayment = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsAmountCF());
			Double durationMonths = (Double) forecast.getCustomFieldValue(Configurator.getInstance().getForecastMonthlyPaymentsDurationCF());
			
			if (initialPayment == null)
				initialPayment = 0.0;

			if (monthlyPayment == null || durationMonths == null)
				return initialPayment;
			else
				return initialPayment + monthlyPayment * durationMonths;
		}

		public String getCategory() 
		{
			try
			{
				if (forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF()) != null)
					return ((LazyLoadedOption) forecast.getCustomFieldValue(Configurator.getInstance().getForecastCategoryCF())).getValue();
				return NONE;
			}
			catch (Exception e)
			{
				log.debug("getCategory Exception", e);
				return NONE;
			}
		}
		
		public String getDebugMessage() { return debugMessage; }
		public CompletionStatus getCompletionStatus() { return completionStatus; }
		public Date getClosingDate() { return ((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastClosingDateCF())) ; }
		public Date getBookingDate() { return ((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastBookingDateCF())) ; }
		public Date getDeliveryDate() { return ((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastDeliveryDateCF())) ; }
		public Date getRecognitionDate() { return ((Date) forecast.getCustomFieldValue(Configurator.getInstance().getForecastRecognitionDateCF())) ; }
		public Date getLastReviewedDate( String forecastDateToUse ) 
		{ 
			if ( lastReviewedDate != null ) return lastReviewedDate;

			// Some forecasts may not have any changes, so the lastReviewedDate will not be set
			// In that case, return the current value of the date (it is the original value, and we consider it reviewed)
			return getForecastDateToUse(forecastDateToUse);
		}
	
		public String getForecastStatus() 
		{
			try
			{
				if ( forecast.getCustomFieldValue(Configurator.getInstance().getForecastStatusCF()) != null)
					return ((LazyLoadedOption) forecast.getCustomFieldValue(Configurator.getInstance().getForecastStatusCF())).getValue();
				return NONE;
			} 
			catch (Exception e)
			{
				log.debug("getForecastStatus Exception", e);
				return NONE;
			}
		}
		
		public String getStatus() 
		{
			try
			{
				if ( forecast.getStatus() != null)
					return forecast.getStatus().getName();
				return NONE;
			} 
			catch (Exception e)
			{
				log.debug("getStatus Exception", e);
				return NONE;
			}
		}

		@HtmlSafe // to inform Velocity templates not to escape the embedded HTML; see https://developer.atlassian.com/jiradev/jira-platform/jira-architecture/jira-templates-and-jsps/html-escaping-for-velocity-templates
		public String getFormattedDescription()
		{
			try
			{
				FieldLayout fieldLayout = JC.fieldLayoutManager.getFieldLayout(this.forecast);
				FieldLayoutItem fieldLayoutItem = fieldLayout.getFieldLayoutItem(IssueFieldConstants.DESCRIPTION);
				
				return JC.renderManager.getRenderedContent(fieldLayoutItem, forecast);
			} 
			catch (Exception e)
			{
				log.debug("getFormattedDescription Exception", e);
				return NONE;
			}
		}
		
		public Date getForecastDateToUse( String forecastDateToUse )
		{
			if (forecastDateToUse.equalsIgnoreCase("Closing Date")) return getClosingDate();
			else if (forecastDateToUse.equalsIgnoreCase("Booking Date")) return getBookingDate();
			else if (forecastDateToUse.equalsIgnoreCase("Recognition Date")) return getRecognitionDate();
			else return getDeliveryDate();
		}
		
		public List<ForecastChangeItem> getForecastDateToUseChanges()
		{
			return forecastDateToUseChanges;
		}

		private void determineCompletionStatus(String forecastDateToUse)
		{
			try
			{
				if ( forecast.getStatus() != null && forecast.getStatus().getStatusCategory() != null)
				{
					if ( forecast.getStatus().getStatusCategory().getKey().equals(StatusCategory.COMPLETE) )
					{
						completionStatus = CompletionStatus.DONE;
						return;
					}
				}
				
				// If the original date has already passed, then mark as delayed
				// Note this code uses fiscal weeks: another sign that refactoring is necessary since
				// it should be code further down that knows about fiscal weeks.
				FiscalWeek currentFiscalWeek = FiscalWeek.identifyFiscalWeek(new Date());
				FiscalWeek lastReviewedDateFiscalWeek = FiscalWeek.identifyFiscalWeek(getLastReviewedDate(forecastDateToUse));
				FiscalWeek lastUnreviewedDateFiscalWeek = FiscalWeek.identifyFiscalWeek(getForecastDateToUse(forecastDateToUse));
				if (currentFiscalWeek.compareTo(lastReviewedDateFiscalWeek) > 0)
				{
					// even if the last reviewed date is in the past, if the proposed new date is 
					// now or in the future, then mark as postponed
					if (lastUnreviewedDateFiscalWeek.compareTo(currentFiscalWeek) >= 0)
						completionStatus = CompletionStatus.POSTPONED;
					else // reviewed date is in the past, proposed (unreviewed) date is also in the past.
						completionStatus = CompletionStatus.DELAYED;
					return;
				}
	
				// Now we need to find out whether this forecast should be flagged as
				// violating postponement rules or not. This is if any changes to the
				// forecast have been collected (this is optional; see constructor).
				if (forecastDateToUseChanges != null && !forecastDateToUseChanges.isEmpty())
				{
					ForecastChangeItem mostRecentChange = forecastDateToUseChanges.get(0);
					
					// if the most recent change is a review, then of course all is well,
					// unless the forecast is late, then it would have been marked as DELAYED
					// (see above in the same method)
					if (mostRecentChange.getAction().equals(ForecastChangeItem.REVIEW))
					{
						// flag nothing
					}
					else // the most recent change was a change in the date value
					{
						SimpleDateFormat yyyyMMddFormatter = new SimpleDateFormat("yyyy-MM-dd");
						// Check if that last change was a violation based on the date the change was made (but see below),
						// the last reviewed date (which may be its original date if it has never been reviewed),
						// and the value the date was changed to.
						if (!getDateChangeEvaluation(
								mostRecentChange.getDatePerformed(), 
								lastReviewedDate, 
								yyyyMMddFormatter.parse((String)mostRecentChange.getNewValue())).equals(ForecastChangeItem.CHANGE))
							completionStatus = CompletionStatus.POSTPONED;
					}
				}
			}
			catch (Exception e)
			{
				log.debug("determineCompletionStatus Exception", e);
			}
		}

		public void collectForecastDateToUseChanges( String forecastDateToUse, boolean reverse )
		{
			try
			{
				List<ChangeHistory> changeHistoryList = JC.changeHistoryManager.getChangeHistories(forecast);
				forecastDateToUseChanges = new ArrayList<ForecastChangeItem>();
				SimpleDateFormat yyyyMMddFormatter = new SimpleDateFormat("yyyy-MM-dd");
				
				// mostRecentDatePickedAsTarget = the most recent date set for the forecast (NOT the date it was changed)
				// Since lastReviewedDate should be null here, initialize this to the current value.
				// Later (see below) it will be overwritten potentially by real changes
				Date mostRecentDatePickedAsTarget = null;
				
				if (changeHistoryList != null && changeHistoryList.size() > 0)
				{
					// The list is sorted from oldest to newest
					
					// Go through all the changes, and filter out the changes we are interested in:
					// 1. Changes to the date in use (booking, closing, delivery, or recognition)
					// 2. Reviews of said date
					// We want to come out of this loop with all relevant changes stored, and also
					// know the last date that was reviewed (or the original date if it was never
					// reviewed).
					for (ChangeHistory changeHistory : changeHistoryList)
					{
						if (changeHistory.getChangeItemBeans() != null)
						{
							for (ChangeItemBean change : changeHistory.getChangeItemBeans())
							{
								log.debug("Change on " + change.getCreated());
								// we are interested in changes to the date in use
								if (change.getField().equals(forecastDateToUse))
								{
									// Keep a pointer to the last date that was changed to, in case a review happened after it
									// (see below), then we will use that date as the new baseline (it was reviewed in other words)
									mostRecentDatePickedAsTarget = yyyyMMddFormatter.parse(change.getTo());

									log.debug("For issue " + forecast.getKey() + " ---> 1000: " + mostRecentDatePickedAsTarget);

									// Case: first change, so there is no reviewed date yet, so simply pick the original value.
									// This is an important assumption: the first time the forecast is created, before it is reviewed
									// we consider this initial date as the baseline.
									if (lastReviewedDate == null)
									{
										if (change.getFrom() == null) // no value to begin with
											lastReviewedDate = yyyyMMddFormatter.parse(change.getTo());
										else
											lastReviewedDate = yyyyMMddFormatter.parse(change.getFrom());
									}
									
									// store this as an important change to show the user: the date was changed.
									// As part of the record, we also evaluate whether the change broke any rules (e.g.
									// out of quarter, out of week, etc.). We consider this with respect to the date
									// the change was made and the last reviewed date. Keep in mind the date the change
									// was made determines how flexible one can be with that forecast (i.e. we cannot
									// move it out of that particular quarter and so on).
									forecastDateToUseChanges.add(
										new ForecastChangeItem(
											change.getCreated(), 
											getDateChangeEvaluation(change.getCreated(), lastReviewedDate, yyyyMMddFormatter.parse(change.getTo())), 
											changeHistory.getAuthorDisplayName(), change.getFrom(), change.getTo()));
								}
								// we are also interested in changes to the review checkbox for the date
								else if (change.getField().equals(forecastDateToUse + " Reviewed") && change.getTo() != null && !change.getTo().trim().isEmpty()
									&& JC.projectRoleManager.isUserInProjectRole(changeHistory.getAuthorObject(), Configurator.getInstance().getForecastReviewersRole(), forecast.getProjectObject()))
								{
									// store this as an important change to show the user: the date was reviewed
									forecastDateToUseChanges.add(new ForecastChangeItem(change.getCreated(), ForecastChangeItem.REVIEW, changeHistory.getAuthorDisplayName(), "false", "true"));
									// note this review date: it will be critical to "forgive" violations. 
									// Note that merely reviewing the date means it is no longer marked as a problem
									lastReviewedDate = mostRecentDatePickedAsTarget;
									
									log.debug("For issue " + forecast.getKey() + " ---> 2000: " + lastReviewedDate);
								}
							}
						}
					}
				}
				
				if (forecastDateToUseChanges.isEmpty())
				{
					forecastDateToUseChanges = null;
					
					// Since there have been no changes, we set the last reviewed date to be the original date.
					lastReviewedDate = getForecastDateToUse(forecastDateToUse);
				}
				else
				{
					// Reverse the list so that it is from newest to oldest now.
					Collections.reverse(forecastDateToUseChanges);
				}

				log.debug("At end of calculateForecastDateToUseChanges");
			}
			catch (Exception e)
			{
				log.warn("Unable to calculate forecast's date changes for forecast "
					+ (forecast != null ? forecast.getKey() : "<no forecast key found>"), e);
				
				//StringWriter sw = new StringWriter();
				//PrintWriter pw = new PrintWriter(sw);
				//e.printStackTrace(pw);
				//debugMessage = debugMessage + " " + sw.toString(); 
			}
		}

		/**
		 * Placement error: this method already uses fiscal weeks inside it: it should be pushed down into a lower class.
		 */
		public String getDateChangeEvaluation( Date whenTheChangeOccurredDate, Date originalDate, Date newDate )
		{
			// We start by checking the original date: has it passed already? If so, we are late. That forecast
			// should be evaluated against the date it was supposed to be done, and NOT against the date when
			// the change occurred (maybe the change occurred afterwards, so late forecasts would get a free pass,
			// and that is bad). In other words, if a forecast has slipped, any change to it should be evaluated
			// against the date it was supposed to be done.
			if (daysBetweenDates(new Date(), originalDate) == 0)
				whenTheChangeOccurredDate = originalDate;

			FiscalWeek whenTheChangeOccurredFiscalWeek = FiscalWeek.identifyFiscalWeek(whenTheChangeOccurredDate);
			FiscalWeek originalFiscalWeek = FiscalWeek.identifyFiscalWeek(originalDate);
			FiscalWeek newFiscalWeek = FiscalWeek.identifyFiscalWeek(newDate);
			
			if (whenTheChangeOccurredFiscalWeek.getFiscalYear() == originalFiscalWeek.getFiscalYear())
			{
				// if the change moved the date out of the then-current year, flag it
				if (originalFiscalWeek.getFiscalYear() < newFiscalWeek.getFiscalYear())
					return ForecastChangeItem.YEAR;

				if (whenTheChangeOccurredFiscalWeek.getFiscalQuarter() == originalFiscalWeek.getFiscalQuarter())
				{
					// if the change moved the date out of the then-current quarter, flag it
					if (originalFiscalWeek.getFiscalQuarter() < newFiscalWeek.getFiscalQuarter())
						return ForecastChangeItem.QUARTER;

					if (whenTheChangeOccurredFiscalWeek.getFiscalMonthOfYear() == originalFiscalWeek.getFiscalMonthOfYear())
					{
						// if the change moved the date out of the then-current month, flag it
						if (originalFiscalWeek.getFiscalMonthOfYear() < newFiscalWeek.getFiscalMonthOfYear())
							return ForecastChangeItem.MONTH;

						if (whenTheChangeOccurredFiscalWeek.getFiscalWeekOfYear() == originalFiscalWeek.getFiscalWeekOfYear())
						{
							// if the change moved the date out of the then-current week, flag it
							if (originalFiscalWeek.getFiscalWeekOfYear() < newFiscalWeek.getFiscalWeekOfYear())
								return ForecastChangeItem.WEEK;
						}
					}
				}
			}
			return ForecastChangeItem.CHANGE; // meaning no violation detected
		}

		public List<ChangeItemBean> getForecastDateToUseChanges2( String forecastDateToUse, boolean reverse )
		{
			CustomField forecastDateToUseCF = null;
			if (forecastDateToUse.equalsIgnoreCase("Closing Date"))
			{
				forecastDateToUseCF = Configurator.getInstance().getForecastClosingDateCF();
			}
			else if (forecastDateToUse.equalsIgnoreCase("Booking Date"))
			{
				forecastDateToUseCF = Configurator.getInstance().getForecastBookingDateCF();
			}
			else if (forecastDateToUse.equalsIgnoreCase("Recognition Date"))
			{
				forecastDateToUseCF = Configurator.getInstance().getForecastRecognitionDateCF();
			}
			else
			{
				forecastDateToUseCF = Configurator.getInstance().getForecastDeliveryDateCF();
			}
			
			List<ChangeHistory> changeHistoryList = JC.changeHistoryManager.getChangeHistories(forecast);
			if (changeHistoryList != null && changeHistoryList.size() > 0)
			{
				if (reverse) Collections.reverse(changeHistoryList);

				StringBuilder buff = new StringBuilder();
				int i = 1;
				for (ChangeHistory changeHistory : changeHistoryList)
				{
					buff.setLength(0);
					buff.append("\n\t" + "Change history #" + i++);
					buff.append("\n\t" + "Issue ID: " + forecast.getId());
					buff.append("\n\t" + "Issue Summary: " + forecast.getSummary());
					buff.append("\n\t" + "Change history timestamp: " + changeHistory.getTimePerformed());
					buff.append("\n\t" + "Change history author: " + changeHistory.getAuthorDisplayName() );
					//buff.append("\n\t" + "Change history comment: " + changeHistory.getComment() );
					if (changeHistory.getChangeItemBeans() != null)
					{
						for (ChangeItemBean change : changeHistory.getChangeItemBeans())
						{
							buff.append("\n\t\t" + ">>> Change <<<");
							buff.append("\n\t\t\t" + "Field: " + change.getField());
							buff.append("\n\t\t\t" + "From: " + change.getFrom());
							buff.append("\n\t\t\t" + "To: " + change.getTo());
							buff.append("\n\t\t\t" + "Created: " + change.getCreated());
						}
					}
					log.debug(buff.toString());
				}
			}
			
			

			List<ChangeItemBean> changes = JC.changeHistoryManager.getChangeItemsForField(forecast, forecastDateToUseCF.getFieldName());

			if (changes != null && changes.size() > 0)
			{
				if (reverse) Collections.reverse(changes);
				return changes;
			}

			return null;
		}

		public int getLastUpdatedInDaysAgo()
		{
			return daysBetweenDates(forecast.getUpdated(), new Date(System.currentTimeMillis())) - 1;
		}

		public String getLastUpdatedBy()
		{
			String lastUpdatedBy = null;
			List<ChangeHistory> changeHistoryList = JC.changeHistoryManager.getChangeHistories(forecast);

			if (changeHistoryList != null && changeHistoryList.size() > 0)
			{
				// The list is sorted from oldest to newest
				ChangeHistory lastChange = changeHistoryList.get( changeHistoryList.size() - 1 );
				
				lastUpdatedBy = lastChange.getAuthorDisplayName();
			}
			
			if (lastUpdatedBy == null)
				lastUpdatedBy = forecast.getCreator().getDisplayName();

			return lastUpdatedBy;
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
		
		public List<IssueLink> getOutwardLinks()
		{
			return JC.issueLinkManager.getOutwardLinks(forecast.getId());
		}

		public List<IssueLink> getInwardLinks()
		{
			return JC.issueLinkManager.getInwardLinks(forecast.getId());
		}
	}

	public class ForecastChangeItem
	{
		// changes that cause violations
		public static final String YEAR = "year";
		public static final String QUARTER = "quarter";
		public static final String MONTH = "month";
		public static final String WEEK = "week";
		// non-violating change
		public static final String CHANGE = "change";
		// review
		public static final String REVIEW = "review";

		private Date datePerformed;
		private String action;
		private String userDisplayName;
		private Object oldValue, newValue;
		
		public ForecastChangeItem(Date datePerformed, String action, String userDisplayName, Object oldValue, Object newValue)
		{
			this.datePerformed = datePerformed;
			this.action = action;
			this.userDisplayName = userDisplayName;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
		public Date getDatePerformed()
		{
			return datePerformed;
		}
		public String getAction()
		{
			return action;
		}
		public String getUserDisplayName()
		{
			return userDisplayName;
		}
		public Object getOldValue()
		{
			return oldValue;
		}
		public Object getNewValue()
		{
			return newValue;
		}
	}

	/**
	 * Here, we just initialize the instance variables this class is responsible for. The "init"
	 * method supplied by AbstractReport is called at odd (and frequent) times, so it is not a 
	 * good idea to initialize variables there.
	 */
	public void validate(ProjectActionSupport action, @SuppressWarnings("rawtypes") Map params)
	{
		super.validate(action, params);
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	private Date validateDateParam(ProjectActionSupport action, DateTimeFormatter formatter, String paramName, Map params)
	{
		Date date = null;
		try { date = formatter.parse((String) params.get(paramName)); } 
		catch (Exception e) { action.addError(paramName, JC.jiraAuthenticationContext.getI18nHelper().getText("scscjf.report.error.invaliddate")); }
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
