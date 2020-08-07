package sa.com.ruhtlc.cloud.jira.utils.fiscal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The fiscal year - as defined here - is a 5-4-4 week-based year. Every quarter
 * is composed of those 13 weeks. Every week starts on Sunday. This adds up to
 * 364 days in the fiscal year. Over time, the fiscal year will get further and
 * further "out of sync" with the calendar year.For this reason, when the fiscal
 * year end has moved at least one week earlier than the end of the calendar
 * year, we make that fiscal year absorb that week also. Hence, a fiscal year is
 * usually 52 weeks (5-4-4 * 4) ... but sometimes it goes up to 53 weeks. This
 * will move the fiscal year to be closer in sync with the calendar year, until
 * a few years later, then we repeat the same fix.
 * 
 * Note that this means the fiscal year always ends on the same day OR earlier
 * than the calendar year by no more than 6 days. This rule is important for
 * determining the start of arbitrary fiscal years.
 * 
 * @author Ahmed
 *
 */
public class FiscalCalendarHelper
{
	private static final Logger log = LoggerFactory.getLogger(FiscalCalendarHelper.class);

	private static int startingFiscalYear = 2016;
	private static int startingYear = 2015;
	private static int startingMonth = 12;
	private static int startingDate = 27;
	private static int[] quarterWeekPattern = new int[] { 5, 4, 4 };

	private static Calendar calendar = new GregorianCalendar(startingYear, startingMonth - 1, startingDate, 0, 0, 0);

	/*
	 * public static Date[] getWeeks(int year) { Calendar calendar = new
	 * GregorianCalendar(startingYear, startingMonth-1, startingDate, 0, 0, 0);
	 * for (int i=0; i<3; i++) { System.out.println(calendar.toString());
	 * calendar.add(Calendar.DATE, 7); } }
	 */
	public static void main(String[] args)
	{
		// Determine the weeks for a given year
		// Given a particular date, determine the year and week it is in
		Calendar calendar = new GregorianCalendar(startingYear, startingMonth - 1, startingDate, 0, 0, 0);
		int totalCycles = 1;
		int fiscalYearCntr = startingFiscalYear;
		outer: while (totalCycles == 3)
		{
			System.out.print("\n=================================================================");
			int week = 1;
			for (int monthCntr = 0; monthCntr < 12; monthCntr++)
			{
				if (monthCntr % 3 == 0)
					System.out.printf("\n%d - Q%d\n", fiscalYearCntr, (monthCntr / 3 + 1));

				for (int mWeekCntr = 0; mWeekCntr < quarterWeekPattern[monthCntr % 3]; mWeekCntr++)
				{
					if (mWeekCntr > 0)
						System.out.print("-- ");

					System.out.printf("%d: Q%d-M%d-W%d %d %d %d ", week++, monthCntr / 3 + 1, monthCntr + 1, mWeekCntr + 1, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));

					calendar.add(Calendar.DATE, 7);
				}

				if (monthCntr == 11 && calendar.get(Calendar.DATE) <= 25) // room
																			// for
																			// one
																			// more
																			// week
				{
					System.out.print("-- ");

					System.out.printf("%d: Q%d-M%d-W5 %d %d %d ", week++, monthCntr / 3 + 1, monthCntr + 1, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));

					calendar.add(Calendar.DATE, 7);
				}

				System.out.println();
			}
			fiscalYearCntr++;
			totalCycles += week;
			if (totalCycles >= 400)
				break;
		}
		/*
		for (short i = 2016; i < 2035; i++)
		{
			FiscalYear fy2016 = FiscalYear.getInstance( i );
			System.out.println( fy2016.getFiscalYear() + ": " 
				+ fy2016.getNumberOfWeeks() + (fy2016.getNumberOfWeeks() == 53 ? "*" : "") 
				+ " -- " + fy2016.getWeek(1).toString() );
		}
		*/
		/*
		FiscalWeek week = FiscalYear.getInstance(2016).getWeek(1);
		System.out.println(week);
		week = week.getNextWeek();
		System.out.println(week);
		
		System.out.println( FiscalWeek.identifyFiscalWeek( new Date(117, 0, 8) ) );*/
		Pattern allowedIdentifiers = Pattern.compile("\\w|\\w[\\w ]*\\w");
		System.out.println( allowedIdentifiers.matcher("abc").matches() );
		System.out.println( allowedIdentifiers.matcher("a c").matches() );
		System.out.println( allowedIdentifiers.matcher("a").matches() );
		System.out.println( allowedIdentifiers.matcher(" a").matches() );
	}
}
