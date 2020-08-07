package sa.com.ruhtlc.cloud.jira.forecasting.reports.generators;

import java.util.LinkedHashMap;
import java.util.Map;

import com.atlassian.configurable.ValuesGenerator;

public class MonthValuesGenerator implements ValuesGenerator<String>
{
	private static final Map<String, String> values;
	static 
	{
		values = new LinkedHashMap<String, String>();
		values.put("1", " 1 - January " );
		values.put("2", " 2 - February " );
		values.put("3", " 3 - March " );
		values.put("4", " 4 - April " );
		values.put("5", " 5 - May " );
		values.put("6", " 6 - June " );
		values.put("7", " 7 - July " );
		values.put("8", " 8 - August " );
		values.put("9", " 9 - September " );
		values.put("10", " 10 - October " );
		values.put("11", " 11 - November " );
		values.put("12", " 12 - December " );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, String> getValues(Map map)
	{
		return values;
	}

}
