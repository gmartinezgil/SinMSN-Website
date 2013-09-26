package mx.com.sinmsn.web.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.PackageResource;
import org.apache.wicket.protocol.http.WebRequestCycle;

/**
 * 
 * @author jerry
 *
 */
public final class WebUtil {
    //images
	public static final Resource LOGO_IMAGE = PackageResource.get(WebUtil.class, "logo.PNG").setCacheable(true);
	
    public static final Resource ONLINE_CONTACT_IMAGE = PackageResource.get(WebUtil.class, "status_online.png").setCacheable(true);
    public static final Resource BUSY_CONTACT_IMAGE = PackageResource.get(WebUtil.class, "status_busy.png").setCacheable(true);
    public static final Resource AWAY_CONTACT_IMAGE = PackageResource.get(WebUtil.class, "status_away.png").setCacheable(true);
    public static final Resource OFFLINE_CONTACT_IMAGE = PackageResource.get(WebUtil.class, "status_offline.png").setCacheable(true);
    
    //the log...
    private static final Log log = LogFactory.getLog(WebUtil.class);

    /**
     * 
     * @return
     */
    public static String getCurrentDate()  {
    	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    	return sdf.format(new Date());
    }
    
    /**
     * 
     * @return
     */
    public static String getUserRemoteAddress() {
        String userRemoteAddress = ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest().getRemoteAddr();
        log.info("remote address - "+userRemoteAddress);
        if (userRemoteAddress == null || userRemoteAddress.trim().length() == 0) {
        	//behind a proxy or firewall... 
            userRemoteAddress = ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest().getHeader("X-Forwarded-For");
            log.info("remote address (behind proxy) - "+userRemoteAddress);
        }
        if (userRemoteAddress.equals("127.0.0.1") || userRemoteAddress.startsWith("192.168.")) { //local host...only for testing pourposes...
        	//userRemoteAddress = "77.193.101.135";//clisson, france 
        	//userRemoteAddress = "201.89.145.85";//gramado,brazil
        	//userRemoteAddress = "128.100.57.19";//toronto canada
        	//userRemoteAddress = "74.6.28.231";//usa, sunnyvale
        	//userRemoteAddress = "86.136.176.201";//birmingham, england
        	//userRemoteAddress = "61.135.168.39";//beijing, china
        	//userRemoteAddress = "213.144.15.38";//karlsrune, germany
        	//userRemoteAddress = "190.189.89.173";//buenos aires, argentina
            userRemoteAddress = "189.146.182.29"; //near HOME, local ISP address provider...for testing..only...//mexico, mexico
        }
        return userRemoteAddress;
    }
    
}//END OF FILE