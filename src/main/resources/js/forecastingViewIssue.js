function _fp_customizeViewIssuePage()
{
	AJS.$("#opsbar-opsbar-transitions").hide();
	AJS.$("#status-val").parent().parent().hide();
	// AJS.$("#opsbar-operations_more_drop .issueaction-log-work").parent().hide();
	AJS.$("#opsbar-operations_more_drop .issueaction-issue-to-subtask").parent().hide();
	AJS.$("#opsbar-operations_more_drop .issueaction-edit-labels").parent().hide();
	AJS.$("#hipchat-viewissue-panel").hide();
}

AJS.toInit( 
	function()
	{
		// This is the appropriate event for the browse project issues page
		// JIRA.bind(JIRA.Events.ISSUE_REFRESHED, _fp_customizeViewIssuePage);
		// This is the appropriate event for the view issue page
		// AJS.$( document ).bind("ready", _fp_customizeViewIssuePage);
	}
);
