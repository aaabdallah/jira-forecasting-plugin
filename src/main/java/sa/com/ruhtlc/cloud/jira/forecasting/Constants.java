package sa.com.ruhtlc.cloud.jira.forecasting;

public class Constants
{
	public static final String[] VALUES_YES = { "Yes" };

	/*
	 * The names and descriptions of the fields are put here and not in a properties file because these should NOT change unless
	 * the code is appropriately updated. The names of the fields in particular are considered "written in stone" because this is 
	 * the only way to recognize them on start up.
	 */
	public static final String ISSUE_TYPE_FORECAST_NAME = "Forecast";
	public static final String ISSUE_TYPE_FORECAST_DESCRIPTION = "A forecast related to any kind of work. Added by Forecasting plugin. DO NOT EDIT OR DELETE.";

	public static final String ISSUE_TYPE_FORECAST_SUBTASK_NAME = "Forecast SubTask";
	public static final String ISSUE_TYPE_FORECAST_SUBTASK_DESCRIPTION = "A forecast subtask related to any kind of work. Added by Forecasting plugin. DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_STATUS_NAME = "Forecast Status";
	public static final String CUSTOM_FIELD_FORECAST_STATUS_DESCRIPTION = "Status of the forecast. DO NOT EDIT OR DELETE.";
	public static final String[] CUSTOM_FIELD_FORECAST_STATUS_OPTIONS = { "Not Started", "In Progress", "Done" };
	public static final int CUSTOM_FIELD_FORECAST_STATUS_DEFAULT_OPTION_NUMBER = 0; 

	public static final String CUSTOM_FIELD_FORECAST_CATEGORY_NAME = "Forecast Category";
	public static final String CUSTOM_FIELD_FORECAST_CATEGORY_DESCRIPTION = "Category of the forecast. DO NOT EDIT OR DELETE.";
	public static final String[] CUSTOM_FIELD_FORECAST_CATEGORY_OPTIONS = { "First Category", "Second Category", "Third Category" };
	public static final int CUSTOM_FIELD_FORECAST_CATEGORY_DEFAULT_OPTION_NUMBER = 0; 

	public static final String CUSTOM_FIELD_FORECAST_CLOSING_DATE_NAME = "Closing Date";
	public static final String CUSTOM_FIELD_FORECAST_CLOSING_DATE_DESCRIPTION = "Expected closing date (when agreement is reached). DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_BOOKING_DATE_NAME = "Booking Date";
	public static final String CUSTOM_FIELD_FORECAST_BOOKING_DATE_DESCRIPTION = "Expected booking date (when the agreement is formally signed). DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_DELIVERY_DATE_NAME = "Delivery Date";
	public static final String CUSTOM_FIELD_FORECAST_DELIVERY_DATE_DESCRIPTION = "Expected delivery (\"go live\") date. DO NOT EDIT OR DELETE.";
	
	public static final String CUSTOM_FIELD_FORECAST_RECOGNITION_DATE_NAME = "Recognition Date";
	public static final String CUSTOM_FIELD_FORECAST_RECOGNITION_DATE_DESCRIPTION = "Expected recognition date (when revenue begins to accrue). DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_CLOSING_DATE_REVIEWED_NAME = "Closing Date Reviewed";
	public static final String CUSTOM_FIELD_FORECAST_CLOSING_DATE_REVIEWED_DESCRIPTION = "Indicates if the closing date was reviewed. DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_BOOKING_DATE_REVIEWED_NAME = "Booking Date Reviewed";
	public static final String CUSTOM_FIELD_FORECAST_BOOKING_DATE_REVIEWED_DESCRIPTION = "Indicates if the booking date was reviewed. DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_DELIVERY_DATE_REVIEWED_NAME = "Delivery Date Reviewed";
	public static final String CUSTOM_FIELD_FORECAST_DELIVERY_DATE_REVIEWED_DESCRIPTION = "Indicates if the delivery date was reviewed. DO NOT EDIT OR DELETE.";
	
	public static final String CUSTOM_FIELD_FORECAST_RECOGNITION_DATE_REVIEWED_NAME = "Recognition Date Reviewed";
	public static final String CUSTOM_FIELD_FORECAST_RECOGNITION_DATE_REVIEWED_DESCRIPTION = "Indicates if the recognition date was reviewed. DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_TOTAL_VALUE_NAME = "Total Value";
	public static final String CUSTOM_FIELD_FORECAST_TOTAL_VALUE_DESCRIPTION = "Expected total value.";
	
	public static final String CUSTOM_FIELD_FORECAST_INITIAL_PAYMENT_AMOUNT_NAME = "Initial Payment Amount";
	public static final String CUSTOM_FIELD_FORECAST_INITIAL_PAYMENT_AMOUNT_DESCRIPTION = "Expected initial payment (if any). DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_MONTHLY_PAYMENTS_AMOUNT_NAME = "Monthly Payments Amount";
	public static final String CUSTOM_FIELD_FORECAST_MONTHLY_PAYMENTS_AMOUNT_DESCRIPTION = "Expected monthly payment (if any). DO NOT EDIT OR DELETE.";

	public static final String CUSTOM_FIELD_FORECAST_MONTHLY_PAYMENTS_DURATION_NAME = "Monthly Payments Duration";
	public static final String CUSTOM_FIELD_FORECAST_MONTHLY_PAYMENTS_DURATION_DESCRIPTION = "Expected number of monthly payments. DO NOT EDIT OR DELETE.";

	public static final String SCREEN_FORECAST_SCREEN_NAME = "Forecast Screen";
	public static final String SCREEN_FORECAST_SCREEN_DESCRIPTION = "A screen showing a forecast's relevant fields. DO NOT EDIT OR DELETE.";
	public static final String SCREEN_FORECAST_SCREEN_TAB1_NAME = "Field Tab";

	public static final String SCREEN_FORECAST_SCREEN_SCHEME_NAME = "Forecast Screen Scheme";
	public static final String SCREEN_FORECAST_SCREEN_SCHEME_DESCRIPTION = "A screen scheme mapping all operations to forecast screens. DO NOT EDIT OR DELETE.";

	public static final String SCREEN_FORECAST_ISSUE_TYPE_SCREEN_SCHEME_NAME = "Forecast Issue Type Screen Scheme";
	public static final String SCREEN_FORECAST_ISSUE_TYPE_SCREEN_SCHEME_DESCRIPTION = "A mapping between forecast issue types and custom forecast screens. DO NOT EDIT OR DELETE.";
	
	public static final String PROJECT_ROLE_FORECAST_REVIEWERS_NAME = "Forecast Reviewers";
	public static final String PROJECT_ROLE_FORECAST_REVIEWERS_DESCRIPTION = "A project role that represents authorized reviewers of forecasts in a project. DO NOT EDIT OR DELETE.";
}
