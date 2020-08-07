// methods need to be synchronized
// pull in the current project being looked at
// query the server for that project's field settings
// hide/show depending on the result
// if the project name changes in the dropdown, then reload settings

// CHECK if on http://localhost:2990/secure/admin/ViewCustomFields.jspa then do nothing

function _fp_processFieldSettingsEventHandler(fieldSettings)
{
	AJS.$.each(fieldSettings, function(i, oneSetting)
	{
		if (oneSetting)
		{
			// console.log(oneSetting.key, oneSetting.enabled, oneSetting.customName, oneSetting.allowedValues);
			if (oneSetting.enabled === false)
			{
				AJS.$("#" + oneSetting.jiraCFID).parent().hide();
			}
			else if (oneSetting.enabled === true)
			{
				if (oneSetting.customName)
					AJS.$("label[for='" + oneSetting.jiraCFID + "']").text(oneSetting.customName);
				// if (oneSetting.allowedValues) console.log(oneSetting.allowedValues);
				AJS.$("#" + oneSetting.jiraCFID).parent().show();
			}
		}
	})
}

function _fp_processFieldSettings()
{
	var projectId = AJS.$("#project-field"); // Returns project KEY
	if (projectId)
		projectId = projectId.val();

	if (projectId)
	{
		var lastLparens = 0, lastRparens = 0;
		lastLparens = projectId.lastIndexOf("(");
		lastRparens = projectId.lastIndexOf(")");

		if (lastLparens + 1 < lastRparens)
		{
			projectId = projectId.substring(lastLparens + 1, lastRparens);

			if (projectId)
				AJS.$.getJSON("/rest/forecasting/1.0/projectsettings/key/" + projectId, _fp_processFieldSettingsEventHandler);
		}
		else
			projectId = null;
	}
	else
	{
		projectId = AJS.$("input[name='pid']"); // Returns project ID
		if (projectId.size() > 0)
		{
			projectId = projectId.val();

			if (projectId)
				AJS.$.getJSON("/rest/forecasting/1.0/projectsettings/id/" + projectId, _fp_processFieldSettingsEventHandler);
		}
		else
			projectId = null;
	}
}

_fp_processFieldSettings();
