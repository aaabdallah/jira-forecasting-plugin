package sa.com.ruhtlc.cloud.jira.forecasting.webworks.debugger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class DebuggerAction extends JiraWebActionSupport
{
    private static final Logger log = LoggerFactory.getLogger(DebuggerAction.class);
 
    public String doDefault()
    {
    	log.debug(" DebuggerAction doDefault " );
    	return "success";
    }

    @Override
    public String execute() throws Exception 
    {
    	// doDefault();
        return super.execute(); //returns SUCCESS
    }
}
