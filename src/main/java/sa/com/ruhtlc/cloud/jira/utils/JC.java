package sa.com.ruhtlc.cloud.jira.utils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.IssueLinkService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.managedconfiguration.ManagedConfigurationItemService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeManager;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

/**
 * Helper class to load all JIRA Components from ComponentAccessor once and keep references to them.
 * This is safe per:<br/>
 * https://answers.atlassian.com/questions/37543426/safe-to-keep-static-references-to-injected-dependencies
 * 
 * The answer from the Atlassian developer includes a finishing sentence of "Use dependency injection anyway. It's good for you."
 * This is not supported in current circumstances:
 * <li/>JIRA 7.0.5 injection does <b>NOT</b> work per the example they are giving at https://developer.atlassian.com/jiradev/jira-applications/jira-service-desk/automation-rule-components
 * <li/>the Atlassian Spring Scanner responsible for their dependency injection via annotations will change drastically from JIRA 7.1 to 7.2
 * <li/>this "old school" way of doing things is MUCH simpler
 * 
 * In general, dependency injection is nice... but not the Atlassian current mess.
 * 
 * @author Ahmed
 *
 */
public class JC
{
	//**** WARNING ****
	//Do NOT use this approach for Active Objects: it does not work (lots of hours lost on that one).
	//Instead, you MUST use dependency injection (internal race/timing issues).
	//public static final ActiveObjects activeObjects = ComponentAccessor.getOSGiComponentInstanceOfType(com.atlassian.activeobjects.external.ActiveObjects.class);
	//**** WARNING ****

	public static final ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
	public static final ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
	public static final CommentManager commentManager = ComponentAccessor.getCommentManager();
	public static final ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
	public static final CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();	
	public static final DateTimeFormatter dateTimeFormatter = ComponentAccessor.getComponent(DateTimeFormatterFactory.class).formatter();
	public static final EventPublisher eventPublisher = ComponentAccessor.getComponent(EventPublisher.class);
	public static final FieldConfigSchemeManager fieldConfigSchemeManager = ComponentAccessor.getFieldConfigSchemeManager();
	public static final FieldLayoutManager fieldLayoutManager = ComponentAccessor.getFieldLayoutManager();
	public static final FieldManager fieldManager = ComponentAccessor.getFieldManager();
	public static final FieldScreenManager fieldScreenManager = ComponentAccessor.getFieldScreenManager();
	public static final FieldScreenSchemeManager fieldScreenSchemeManager = ComponentAccessor.getComponent(FieldScreenSchemeManager.class);
	public static final IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
	public static final IssueManager issueManager = ComponentAccessor.getIssueManager();
	public static final IssueService issueService = ComponentAccessor.getIssueService();
	public static final IssueTypeManager issueTypeManager = ComponentAccessor.getComponent(IssueTypeManager.class);
	public static final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = ComponentAccessor.getIssueTypeScreenSchemeManager();
	public static final JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
	public static final ManagedConfigurationItemService managedConfigurationItemService = ComponentAccessor.getComponent(ManagedConfigurationItemService.class);
	public static final OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
	public static final ProjectManager projectManager = ComponentAccessor.getProjectManager();
	public static final RendererManager renderManager = ComponentAccessor.getRendererManager();
	public static final ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager.class);
	public static final SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
	public static final SearchRequestService searchRequestService = ComponentAccessor.getComponent(SearchRequestService.class);
	public static final UserManager userManager = ComponentAccessor.getUserManager();
	public static final WorkflowManager workflowManager = ComponentAccessor.getWorkflowManager();
}
