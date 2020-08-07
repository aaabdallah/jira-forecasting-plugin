package sa.com.ruhtlc.cloud.jira.forecasting.reports.generators;

import java.util.LinkedHashMap;
import java.util.Map;

import com.atlassian.configurable.ValuesGenerator;

import sa.com.ruhtlc.cloud.jira.forecasting.Configurator;

public class YearValuesGenerator implements ValuesGenerator<String>
{
	private static final Map<String, String> values;
	static 
	{
		values = new LinkedHashMap<String, String>();
		
		int start = Integer.parseInt( Configurator.getInstance().getProperties().getProperty("scscjf.report.parameter.startyear.value", "2015") );
		int end = Integer.parseInt( Configurator.getInstance().getProperties().getProperty("scscjf.report.parameter.endyear.value", "2050") );
		for (int i=start; i<=end; i++) 
			values.put(Integer.toString(i), Integer.toString(i));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, String> getValues(Map map)
	{
		return values;
	}
}
