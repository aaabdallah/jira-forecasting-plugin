package sa.com.ruhtlc.cloud.jira.utils.fiscal;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class FiscalYear
{
	private static HashMap<Integer, FiscalYear> cache = new HashMap<Integer, FiscalYear>();
	
	public static FiscalYear getInstance(int year)
	{
		// avoid multiple simultaneous requests (rare) to create the same object for nothing
		synchronized( cache )
		{
			FiscalYear instance = cache.get(year);
			if (instance == null)
				return new FiscalYear(year);
			return instance;
		}
	}

	private int fiscalYear;
	private final FiscalWeek[] weeks = new FiscalWeek[53];

	/**
	 * @param fiscalYear 1999-2062
	 */
	private FiscalYear(int fiscalYear)
	{
		this.fiscalYear = fiscalYear;
		Calendar calendar = new GregorianCalendar(fiscalYear, 0, 1);
		
		while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
			calendar.add(Calendar.DAY_OF_YEAR, -1);
		
		for (int weekOfYear=1; weekOfYear<=52; weekOfYear++)
		{
			weeks[weekOfYear-1] = new FiscalWeek(
				fiscalYear, 
				calendar.get(Calendar.YEAR), 
				(((weekOfYear - 1) / 13) + 1),
				(calendar.get(Calendar.MONTH) + 1),
				weekOfYear,
				calendar.get(Calendar.DAY_OF_MONTH));
			
			// roll over in preparation for next week
			calendar.add(Calendar.DAY_OF_MONTH, 7);
		}
		
		// if there are extra days left more than or equal to SEVEN days, use the
		// last slot (i.e. the fiscal year will have 53 weeks)
		if (calendar.get(Calendar.DATE) <= 25) // checking if earlier than or equal to December 25
			weeks[52] = new FiscalWeek(fiscalYear, calendar.get(Calendar.YEAR), 4, (calendar.get(Calendar.MONTH) + 1), 53, calendar.get(Calendar.DAY_OF_MONTH));
		
		cache.put(fiscalYear, this);
	}
	
	public int getFiscalYear()
	{
		return fiscalYear;
	}
	
	public FiscalWeek[] getWeeks()
	{
		return weeks;
	}

	/**
	 * ONE BASED (1 to 53).
	 * 
	 * @param i ONE BASED (1 to 53)
	 * @return the week without any checking. Hence if week 53 is requested when there is none, it will return null.
	 */
	public FiscalWeek getWeek(int i)
	{
		return weeks[i-1];
	}
	
	/**
	 * ONE BASED (1 to 53).
	 * 
	 * @param i ONE BASED (1 to 53)
	 * @return the week with checking: if the requested week is 53 but there is none, it will return week 52
	 */
	public FiscalWeek getWeekAdjustingForLast(int i)
	{
		if (i==53 && weeks[52] == null)
			return weeks[51];

		return weeks[i-1];
	}

	public FiscalWeek getFirstWeek()
	{
		return weeks[0];
	}
	
	public FiscalWeek getLastWeek()
	{
		return weeks[52] == null ? weeks[51] : weeks[52];
	}
	
	/** Either 52 or 53 */
	public int getNumberOfWeeks()
	{
		return weeks[52] == null ? 52 : 53;
	}
}