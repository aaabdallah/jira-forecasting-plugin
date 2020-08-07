package sa.com.ruhtlc.cloud.jira.forecasting.webworks.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.web.action.JiraWebActionSupport;

import sa.com.ruhtlc.cloud.jira.utils.JC;
import webwork.action.ServletActionContext;

public class UpdateCustomFieldOptionsAction extends JiraWebActionSupport
{
	private static final Logger log = LoggerFactory.getLogger(UpdateCustomFieldOptionsAction.class);
	private static final String ERROR_MESSAGE_MISSING_VALUE = "Missing value.";
	private static final String ERROR_MESSAGE_NO_SUCH_FIELD = "No such custom field found";
	private static final String ERROR_MESSAGE_NO_SUCH_FIELD_CONFIGURATION_CONTEXT = "No such field configuration context found";
	//ARA private static final String UC_LRE = "\u202A"; // Left to right embedding (http://dotancohen.com/howto/rtl_right_to_left.html)
	//ARA private static final String UC_RLE = "\u202B"; // Right to left embedding
	
	private String customFieldName, fieldConfigurationSchemeContextName, optionsAsString, defaultOption;
	private String successMessage;

	@Override
	public String execute() throws Exception
	{
		return super.execute(); // returns SUCCESS
	}

	@Override
	public String doDefault() throws Exception
	{
		try
		{
			logParameters();

			successMessage = null;
			processFormSubmission( getFormSubmission() );
		}
		catch (Exception e)
		{
			log.warn("Unexpected exception during administration updating of custom field options.", e);
			addErrorMessage("Unexpected error. Please consult the system administrator.");
		}
		return "input";
	}

	private String getFormSubmission()
	{
		try
		{
			HttpServletRequest request = ServletActionContext.getRequest();
			String formSubmitted = request.getParameter("formSubmitted");

			if (formSubmitted != null && !formSubmitted.trim().isEmpty())
				return formSubmitted.trim().toLowerCase();
		}
		catch (Exception e)
		{
		}
		return null;
	}

	private void processFormSubmission(String command)
	{
		if (command == null)
			return;

		command = command.trim();
		if (!command.equals("load") && !command.equals("save"))
		{
			addErrorMessage("Unrecognized command");
			return;
		}

		HttpServletRequest request = ServletActionContext.getRequest();
		customFieldName = request.getParameter("customFieldName");
		fieldConfigurationSchemeContextName = request.getParameter("fieldConfigurationSchemeContextName");
		optionsAsString = request.getParameter("optionsAsString");
		defaultOption = request.getParameter("defaultOption");
		
		if (customFieldName == null || customFieldName.trim().isEmpty())
			addError("customFieldName", ERROR_MESSAGE_MISSING_VALUE);
		if (fieldConfigurationSchemeContextName == null || fieldConfigurationSchemeContextName.trim().isEmpty())
			addError("fieldConfigurationSchemeContextName", ERROR_MESSAGE_MISSING_VALUE);
		
		customFieldName = customFieldName.trim();
		fieldConfigurationSchemeContextName = fieldConfigurationSchemeContextName.trim();
		
		if (command.equals("load"))
		{
			optionsAsString = null; // pressing "Load" should clear out anything in the options (to be loaded below if found)
			defaultOption = null;
		}

		if (getHasErrors() || getHasErrorMessages())
			return;

		// validate form parameters
		
		// search for that custom field
		// NOTE: Custom fields can have shared names. We do NOT check for that here: it is bad practice to have
		// non-unique names anyway, so to save development time, we will only pull the first one.
		CustomField customField = JC.customFieldManager.getCustomFieldObjectByName(customFieldName);
		if (customField == null)
		{
			addError("customFieldName", ERROR_MESSAGE_NO_SUCH_FIELD);
			return;
		}
		
		// search for the field configuration context
		FieldConfig fieldConfig = null;
		for (FieldConfigScheme fieldConfigScheme : customField.getConfigurationSchemes())
		{
			if (fieldConfigScheme.getName().equals(fieldConfigurationSchemeContextName))
			{
				fieldConfig = fieldConfigScheme.getOneAndOnlyConfig();
				break;
			}
		}
		if (fieldConfig == null)
		{
			addError("fieldConfigurationSchemeContextName", ERROR_MESSAGE_NO_SUCH_FIELD_CONFIGURATION_CONTEXT);
			return;
		}

		if (command.equals("load"))
		{
			optionsAsString = loadOptionsAsStringForFieldConfig(fieldConfig);

			Object defaultOptionObject = customField.getCustomFieldType().getDefaultValue(fieldConfig);
			//ARA if (defaultOptionObject != null)
			//ARA	defaultOption = defaultOptionObject.toString().replaceAll(UC_LRE + "|" + UC_RLE, "");
		}
		else if (command.equals("save"))
		{
			boolean validDefaultOption = false;
			String values[] = convertOptionsAsStringToArray(optionsAsString);
			
			if (defaultOption != null)
			{
				defaultOption = defaultOption.trim();

				if (defaultOption.isEmpty()) // an empty default means ignore it (ignore bizarre cases!)
				{
					defaultOption = null; // set it to null to make case-handling simpler
					validDefaultOption = true;
				}
				else // non-null, non-empty
				{
					for (String value : values)
					{
						if (value.equals(defaultOption))
						{
							validDefaultOption = true;
							break;
						}
					}
				}
			}
			else // a null default means ignore it
			{
				validDefaultOption = true;
			}

			// check if we have a valid default first
			if (validDefaultOption)
			{
				try
				{
					// BEGIN: BAD CODE
					// Remove the old options no matter what.
					// JC.optionsManager.removeCustomFieldConfigOptions(fieldConfig);
					// customField.getCustomFieldType().setDefaultValue(fieldConfig, null);
					// END: BAD CODE
	
					// Update the new options
					updateOptionsForFieldConfig(customField, fieldConfig, values);
					
					successMessage = "Options successfully saved.";
				}
				catch (Exception e)
				{
					log.warn("Unable to store options.", e);
					addErrorMessage("An error occurred while storing the new options. "
						+ "The old options may have been WIPED. Any issues using those options "
						+ "have had the corresponding custom field reset to nothing. "
						+ "Please reinput the old options.");
				}

				// Load the new options to be sure the user sees what has been saved
				optionsAsString = loadOptionsAsStringForFieldConfig(fieldConfig);
			}
			else // invalid default option
				addErrorMessage("Invalid default option specified. Nothing saved or changed.");
		}
	}

	private String loadOptionsAsStringForFieldConfig(FieldConfig fieldConfig)
	{
		Options options = JC.optionsManager.getOptions(fieldConfig);
		StringBuffer allOptionsAsString = new StringBuffer();
		for (Option option : options)
		{
			if (allOptionsAsString.length() != 0)
				allOptionsAsString.append('\n');
			//ARA allOptionsAsString.append(option.getValue().replaceAll(UC_LRE + "|" + UC_RLE, ""));
			allOptionsAsString.append(option.getValue());
		}
		return allOptionsAsString.toString();
	}

	/**
	 * Strings with Arabic characters in them will have Unicode RLE...LRE embedding 
	 * characters attached to the beginning and end. This method also handles storing
	 * the default value.
	 */
	private void updateOptionsForFieldConfig(CustomField customField, FieldConfig fieldConfig, String[] values)
	{
		log.debug("START updateOptionsForFieldConfig");
		// Lesson learned: do NOT delete options then recreate them. Issues that are using that option
		// get their field cleared completely. For this reason:
		// 1. Remove options that are not referenced in the new values (the parameter passed in).
		//    If the user has screwed up, it is their problem.
		// 2. Go thru the list of new values, and add them in
		
		if (values != null && values.length > 0)
		{
			Options oldOptions = JC.optionsManager.getOptions(fieldConfig);
			// remove old options not contained in the new values
			oldOptions = removeUnwantedOptions(oldOptions, values);

			String defaultOptionWithPossibleUnicodeEmbeddings = defaultOption; 
			//ARA	defaultOption != null && hasArabic(defaultOption) ? UC_RLE + defaultOption + UC_LRE : defaultOption;

			Map<Integer, Option> newOptionPositionsMap = new HashMap<Integer, Option>();
			int sequence = 1;
			for (String value : values)
			{
				Option newOptionToAddOrUpdate = null;

				boolean alreadyExists = false;
				// check if the value already exists
				for (Option oldOption : oldOptions)
				{
					if (oldOption.getValue().equals(value))
					{
						newOptionToAddOrUpdate = oldOption;

						// Setting the sequence directly doesn't work
						// oldOption.setSequence(sequence);
						newOptionPositionsMap.put(sequence-1, newOptionToAddOrUpdate);

						alreadyExists = true;
						break;
					}
				}
				
				//ARA if (hasArabic(value))
				//ARA	value = UC_RLE + value + UC_LRE;

				if (!alreadyExists) // never found it; it's new
				{
					// note that createOption assumes sequence is ONE-based.
					newOptionToAddOrUpdate = JC.optionsManager.createOption(fieldConfig, null, (long) sequence, value);
					newOptionPositionsMap.put(sequence-1, newOptionToAddOrUpdate);
				}

				if (defaultOptionWithPossibleUnicodeEmbeddings != null 
					&& value.equals(defaultOptionWithPossibleUnicodeEmbeddings))
					customField.getCustomFieldType().setDefaultValue(fieldConfig, newOptionToAddOrUpdate);
				
				sequence++;
			}
			
			if (!newOptionPositionsMap.isEmpty())
			{
				log.debug(newOptionPositionsMap.toString());
				oldOptions.moveOptionToPosition(newOptionPositionsMap);
			}
			JC.optionsManager.updateOptions(oldOptions);
		}
		else
		{
			// no new values; remove everything
			JC.optionsManager.removeCustomFieldConfigOptions(fieldConfig);
			customField.getCustomFieldType().setDefaultValue(fieldConfig, null);
		}
		log.debug("END updateOptionsForFieldConfig");
	}
	
	private Options removeUnwantedOptions(Options oldOptions, String[] newValues)
	{
		log.debug("START removeUnwantedOptions");
		Iterator<Option> iterator = oldOptions.iterator();
		ArrayList<Option> unwantedOptions = new ArrayList<Option>();

		// loop thru all options
		outer: while (iterator.hasNext())
		{
			Option option = iterator.next();
			String optionValue = option.getValue();
			
			boolean found = false;
			for (String value : newValues)
			{
				// if the new options contain the old option, go to the next old option
				if (value.equals(optionValue))
				{
					found = true;
					continue outer;
				}
			}
			
			if (!found) // didn't find the old option in new options, so delete it 
				unwantedOptions.add(option);
		}
		
		for (Option unwantedOption : unwantedOptions)
			oldOptions.removeOption(unwantedOption);
		
		log.debug("END removeUnwantedOptions");
		return oldOptions;
	}

	/** Convert a single passed in string (should be an HTTP form input from a text input)
	 * to an array of strings ready to insert into JIRA. The original string will be interpreted 
	 * as "One value per line". Null or empty possible values are ignored.
	 */
	private String[] convertOptionsAsStringToArray(String allOptionsAsString)
	{
		if (allOptionsAsString == null) return null;
		allOptionsAsString = allOptionsAsString.trim();
		if (allOptionsAsString.isEmpty()) return null;

		ArrayList<String> allowedValuesList = new ArrayList<String>();

		String[] possibleAllowedValues = allOptionsAsString.split("\r\n|\n");
		if (possibleAllowedValues != null && possibleAllowedValues.length > 0)
		{
			for (String allowedValue : possibleAllowedValues)
			{
				if (allowedValue == null) continue;
				allowedValue = allowedValue.trim();
				if (allowedValue.isEmpty()) continue;
				
				allowedValuesList.add(allowedValue);
			}
		}

		if (allowedValuesList.size() > 0)
			return allowedValuesList.toArray(new String[allowedValuesList.size()]);
		
		return null;
	}
	
	private boolean hasArabic(String string)
	{
		if (string == null) return false;
		
		for (char c : string.toCharArray())
		{
			if (c >= '\u0600' && c <= '\u06FF')
				return true;
		}
		
		return false;
	}

	public String getCustomFieldName()
	{
		return customFieldName;
	}

	public String getFieldConfigurationSchemeContextName()
	{
		return fieldConfigurationSchemeContextName;
	}

	public String getOptionsAsString()
	{
		return optionsAsString;
	}

	public String getDefaultOption()
	{
		return defaultOption;
	}
	
	public String getSuccessMessage()
	{
		return successMessage;
	}

	public void logParameters()
	{
		if (log.isDebugEnabled())
		{
			try
			{
				HttpServletRequest request = ServletActionContext.getRequest();
				Map<String, String[]> parameters = request.getParameterMap();
				if (parameters != null && !parameters.isEmpty())
				{
					StringBuilder buff = new StringBuilder();
					buff.append("\n\tParameters");
					for (String parameterName : parameters.keySet())
						buff.append("\n\tName: [").append(parameterName).append("] --- Value: ").append(Arrays.toString(parameters.get(parameterName)));
					log.debug(buff.toString());
				}
				else
					log.debug("\n\tNO PARAMETERS FOUND.");
			}
			catch (Exception e)
			{
				log.error("Unable to log parameters", e);
			}
		}
	}
}
