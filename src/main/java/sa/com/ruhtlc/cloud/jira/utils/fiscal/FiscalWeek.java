package sa.com.ruhtlc.cloud.jira.utils.fiscal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Holds information for a fiscal week. Only applicable for fiscal years from 1999-2062 AD.
 * EVERYTHING IS ONE BASED (unlike java.util.Calendar).
 */
public class FiscalWeek implements Comparable<FiscalWeek>
{
	public static void main(String arg[])
	{
		System.out.println( FiscalWeek.identifyFiscalWeek(new Date()) );
	}

	public static FiscalWeek identifyFiscalWeek(Date date)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		int year = calendar.get(Calendar.YEAR);
		FiscalYear fiscalYear = FiscalYear.getInstance(year);
		if ( ! (fiscalYear.getFirstWeek().getStartingDate().getTime() <= calendar.getTimeInMillis()
			    && fiscalYear.getLastWeek().getEndingDate().getTime() >= calendar.getTimeInMillis()) )
			fiscalYear = FiscalYear.getInstance(year - 1); // the only other possibility

		for (int i=1; i<=fiscalYear.getNumberOfWeeks(); i++)
		{
			if (calendar.getTimeInMillis() <= fiscalYear.getWeek(i).getEndingDate().getTime())
				return fiscalYear.getWeek(i);
		}
		return null;
	}

	/**
	 * The bits are arranged to allow for a natural ordering of fiscal weeks by simple value.
	 * 
	 * Fiscal Year Off By One Flag: 1 bit (0)	0B00000000000000000000000000000001
	 * Unused: 7 bits (1-7)						0B00000000000000000000000011111110
	 * Week of Year: 6 bits (8-13)				0B00000000000000000011111100000000
	 * Quarter: 2 bits (14-15)					0B00000000000000001100000000000000
	 * Day of Month: 5 bits (16-20)				0B00000000000111110000000000000000
	 * Month: 4 bits (21-24)					0B00000001111000000000000000000000
	 * Year - 2000: 6 bits (25-30)				0B01111110000000000000000000000000
	 * Sign (fixed zero): 1 bit (31)			0B10000000000000000000000000000000
	 */
	private final int bits;

	/**
	 * Package visibility to limit its use... it should even be part of FiscalYear as a nested class.
	 * @param fiscalYear 1999-2062
	 * @param year 2000-2063
	 * @param quarter 1-4
	 * @param month 1-12
	 * @param weekOfYear 1-53
	 * @param dayOfMonth 1-31
	 */
	FiscalWeek(int fiscalYear, int year, int fiscalQuarter, int month, int fiscalWeekOfYear, int dayOfMonth)
	{
		int tempo = (fiscalWeekOfYear << 8);
		tempo |= ((fiscalQuarter-1) << 14);
		tempo |= (dayOfMonth << 16);
		tempo |= (month << 21);
		tempo |= ((year-2000) << 25);

		// If the fiscal year is not equal to the year, there is only case: the fiscal
		// year is one year more than the year. This is for the cases where a fiscal year
		// starts in late December of the preceding year. This is a boolean possibility,
		if (fiscalYear != year)
			tempo += 1;

		bits = tempo;
		
		// System.out.println( toString() );
	}

	/**
	 * @return the calendar date of the month (1, 2, ..., 31)
	 */
	public int getDayOfMonth()
	{
		return (bits & 0B00000000000111110000000000000000) >> 16;
	}

	/** A number from 1 to 53, where the first week might actually start in the previous calendar year */
	public int getFiscalWeekOfYear()
	{
		return (bits & 0B00000000000000000011111100000000) >> 8;
	}
	
	/** 
	 * @return A week number assuming the fiscal year follows a 5-4-4 week pattern per quarter. The one exception is if
	 * the last quarter is in a 53 week year, so the pattern would be 5-4-5. Hence this method always returns a number
	 * between 1 and 5 inclusively.
	 */
	public int getFiscalWeekOfMonth()
	{
		int woy = getFiscalWeekOfYear();
		if (woy == 53) return 5;
		
		if ( (woy-1)%13 < 5 )
			return 1 + (woy-1)%13;
		else if ( (woy-1)%13 < 9 )
			return (woy-1)%13 - 4;
		return (woy-1)%13 - 8;
	}

	/** ONE BASED: the calendar month (1-12) */
	public int getMonth()
	{
		return (bits & 0B00000001111000000000000000000000) >> 21;
	}

	/** The fiscal quarter, 1-4, keeping in mind the first quarter may actually begin in the previous calendar year. */
	public int getFiscalQuarter()
	{
		return ((bits & 0B00000000000000001100000000000000) >> 14) + 1;
	}
	
	/** The calendar NOT fiscal year of this week */
	public int getYear()
	{
		return ((bits & 0B01111110000000000000000000000000) >> 25) + 2000;
	}

	/** The fiscal month (1-12) keeping in mind the first fiscal month may actually start in the previous calendar year */
	public int getFiscalMonthOfYear()
	{
		int woy = getFiscalWeekOfYear();
		if (woy == 53) return 12;
		
		if ( (woy-1)%13 < 5 )
			return 1 + ((getFiscalQuarter()-1) * 3);
		else if ( (woy-1)%13 < 9 )
			return 2 + ((getFiscalQuarter()-1) * 3);
		return 3 + ((getFiscalQuarter()-1) * 3);
	}

	/** The fiscal month number with respect to its fiscal quarter, so 1, 2, or 3. */
	public int getFiscalMonthOfQuarter()
	{
		int woy = getFiscalWeekOfYear();
		if (woy == 53) return 3;
		
		if ( (woy-1)%13 < 5 )
			return 1;
		else if ( (woy-1)%13 < 9 )
			return 2;
		return 3;
	}

	/** The fiscal year of the week. Usually it is the same as the calendar year, but since the fiscal year
	 * can start in the previous calendar year, it is sometimes off by one (for the first week of the fiscal
	 * year essentially).
	 */
	public int getFiscalYear()
	{
		if ( (bits & 0B00000000000000000000000000000001) == 0)
			return getYear();
		return getYear() + 1;
	}
	
	public FiscalWeek getNextWeek()
	{
		if (getFiscalWeekOfYear() < FiscalYear.getInstance(getFiscalYear()).getNumberOfWeeks())
			return FiscalYear.getInstance(getFiscalYear()).getWeek(getFiscalWeekOfYear() + 1);
		return FiscalYear.getInstance(getFiscalYear()+1).getFirstWeek();
	}

	public FiscalWeek getPreviousWeek()
	{
		if (getFiscalWeekOfYear() > 1)
			return FiscalYear.getInstance(getFiscalYear()).getWeek(getFiscalWeekOfYear() - 1);
		return FiscalYear.getInstance(getFiscalYear()-1).getLastWeek();
	}

	/**
	 * Returns "year-month-dayOfMonth" (numbers, not padded).
	 */
	public String toString()
	{
		return String.format("FY%d-FQ%d-FMoQ%d-FWoM%d FMoY%d-FWoY%d %d-%d-%d",
			getFiscalYear(), getFiscalQuarter(), getFiscalMonthOfQuarter(), getFiscalWeekOfMonth(), 
			getFiscalMonthOfYear(), getFiscalWeekOfYear(),
			getYear(), getMonth(), getDayOfMonth());
	}
	
	/**
	 * @return Date representation of the week's first day with hours, minutes, and seconds set to zero
	 */
	public Date getStartingDate()
	{
		Calendar calendar = new GregorianCalendar(getYear(), getMonth()-1, getDayOfMonth(), 0, 0, 0);
		return calendar.getTime();
	}

	/**
	 * @return Date representation of the week's last day with hours, minutes, and seconds set to ZERO (repeat: ZERO)
	 */
	public Date getEndingDate()
	{
		Calendar calendar = new GregorianCalendar(getYear(), getMonth()-1, getDayOfMonth(), 0, 0, 0);
		calendar.add(Calendar.DATE, 6);
		return calendar.getTime();
	}
	
	/**
	 * Checks if the passed in date is within this week, IGNORING ALL FIELDS IN THE HOUR OR LESS POSITION.
	 * Therefore it ONLY CHECKS AGAINST YEAR, MONTH, AND DAY OF MONTH.
	 * @param date the date to check
	 * @return
	 */
	public boolean includes(Date date)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		if (getStartingDate().after(date) || getEndingDate().before(date))
			return false;
		
		return true;
	}

	@Override
	public int compareTo(FiscalWeek o)
	{
		if (o == null)
			throw new NullPointerException("Cannot compare to a null value");
		
		if (this.bits < o.bits)
			return -1;
		else if (this.bits > o.bits)
			return 1;
		else
			return 0;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null)
			return false;
		
		FiscalWeek otherWeek = (FiscalWeek) o;
		
		if (this.bits == otherWeek.bits)
			return true;
		
		return false;
	}
	
	public int hashCode()
	{
		return bits;
	}
}
