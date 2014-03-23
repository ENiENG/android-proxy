package com.lechucksoftware.proxy.proxysettings.feedbackutils;

import java.net.URI;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.shouldit.proxy.lib.APL;
import com.shouldit.proxy.lib.APLConstants;
import com.shouldit.proxy.lib.ProxyConfiguration;
import com.shouldit.proxy.lib.utils.ProxyUtils;
import com.shouldit.proxy.lib.log.LogWrapper;

public class ApplicationSubmitService extends IntentService 
{
	public static String TAG = "ApplicationSubmitService";
	
    public ApplicationSubmitService() 
    {
        super("ApplicationSubmitService");
    }
 
    @Override
    protected void onHandleIntent(Intent intent) 
    {
    	PInfo appInfo = (PInfo) intent.getSerializableExtra("appInfo");
        SubmitApplicationFeedback(getApplicationContext(),appInfo);
    }
    
    @Override
    public void onDestroy() 
    {
    	LogWrapper.d(TAG, "ApplicationSubmitService destroying");
    }
    
	/**
	 * @param context
	 */
	public void SubmitApplicationFeedback(Context context, PInfo appInfo) 
	{
		try
        {
			ProxyConfiguration proxyConf = APL.getCurrentHttpProxyConfiguration();
    		URI uri = URI.create("");
			String result = ProxyUtils.getURI(uri, proxyConf.getProxy(), APLConstants.DEFAULT_TIMEOUT);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
	}
}