package sa.com.ruhtlc.cloud.jira.forecasting.reports.generators;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.configurable.ValuesGenerator;

public class FiscalWeekValuesGenerator implements ValuesGenerator<String>
{
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(FiscalWeekValuesGenerator.class);

	private static final Map<String, String> values;
	static 
	{
		values = new LinkedHashMap<String, String>();
		StringBuilder weekLabel = new StringBuilder(25);
		for (int i=1; i<=52; i++)
		{
			if (i <= 9)
				weekLabel.append(' ');
			weekLabel.append(' ').append(i);
			weekLabel.append(' ').append(' ').append('(').append(' ').append('Q');
			weekLabel.append((i-1)/13 + 1);
			weekLabel.append(' ').append('-').append(' ').append('M');
			if ((i-1)%13 < 5)
			{
				weekLabel.append('1');
				weekLabel.append(' ').append('-').append(' ').append('W');
				weekLabel.append((i-1)%13 + 1).append(' ').append(')').append(' ');
			}
			else if ((i-1)%13 < 9)
			{
				weekLabel.append('2');
				weekLabel.append(' ').append('-').append(' ').append('W');
				weekLabel.append((i-1)%13 - 4).append(' ').append(')').append(' ');
			}
			else
			{
				weekLabel.append('3');
				weekLabel.append(' ').append('-').append(' ').append('W');
				weekLabel.append((i-1)%13 - 8).append(' ').append(')').append(' ');
			}
			
			values.put(Integer.toString(i), weekLabel.toString());
			weekLabel.setLength(0);
		}
		values.put("53", " 53  ( Q4 - M3 - W5 ) " );
	}

	// 1 :: Q1, M1, W1
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, String> getValues(Map map)
	{
		return values;
	}
}
