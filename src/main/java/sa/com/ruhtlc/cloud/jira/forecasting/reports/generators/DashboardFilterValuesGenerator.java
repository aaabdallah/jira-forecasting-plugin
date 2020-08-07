package sa.com.ruhtlc.cloud.jira.forecasting.reports.generators;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.issue.fields.CustomField;

import sa.com.ruhtlc.cloud.jira.utils.JC;

/**
 * This does not return all custom fields; it is customized for the forecast analytics dashboard report,
 * so some fields are not shown since the dashboard enables them by default.
 */
public class DashboardFilterValuesGenerator implements ValuesGenerator<String>
{
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, String> getValues(Map map)
	{
		Map<String, String> values = new LinkedHashMap<String, String>();
		List<CustomField> customFields = JC.customFieldManager.getCustomFieldObjects();
		
		for (CustomField cf : customFields)
		{
			String fieldName = cf.getFieldName();
			
			if (!fieldName.equals("Rank")
				&& !fieldName.equals("Flagged"))
				values.put(cf.getId(), fieldName);
		}
	
		return values;
	}
}
