package com.lechucksoftware.proxy.proxysettings.services;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.constants.Intents;
import com.lechucksoftware.proxy.proxysettings.db.WiFiAPEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiAPConfig;
import be.shouldit.proxy.lib.constants.APLReflectionConstants;
import be.shouldit.proxy.lib.utils.ProxyUtils;
import timber.log.Timber;

/**
 * Created by Marco on 09/03/14.
 */
public class WifiSyncService extends EnhancedIntentService
{
    public static final String CALLER_INTENT = "CallerIntent";
    public static String TAG = WifiSyncService.class.getSimpleName();
    private boolean isHandling = false;
    private static WifiSyncService instance;

    public WifiSyncService()
    {
        super("WifiSyncService", android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE);
    }

    public static WifiSyncService getInstance()
    {
        return instance;
    }

    public boolean isHandlingIntent()
    {
        return isHandling;
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        instance = this;
        isHandling = true;

        App.getTraceUtils().startTrace(TAG, "syncAP", Log.INFO);

        List<APLNetworkId> configsToCheck = getConfigsToCheck(intent);
        App.getTraceUtils().partialTrace(TAG, "syncAP", Log.INFO);

        syncProxyConfigurations(configsToCheck);
        App.getTraceUtils().stopTrace(TAG, "syncAP", Log.INFO);

        isHandling = false;
    }

    private List<APLNetworkId> getConfigsToCheck(Intent intent)
    {
        List<APLNetworkId> networkIds = new ArrayList<APLNetworkId>();

        if (intent != null && intent.hasExtra(WifiSyncService.CALLER_INTENT))
        {
            Intent caller = (Intent) intent.getExtras().get(WifiSyncService.CALLER_INTENT);

            if (caller != null)
            {
//                if (caller.getAction().equals(Intents.WIFI_AP_UPDATED))
//                {
//                    if (caller.hasExtra(Intents.UPDATED_WIFI))
//                    {
//                        APLNetworkId wifiId = (APLNetworkId) caller.getExtras().get(Intents.UPDATED_WIFI);
//                        networkIds.add(wifiId);
//                    }
//                }
//                else
                if (caller.getAction().equals(APLReflectionConstants.CONFIGURED_NETWORKS_CHANGED_ACTION))
                {
                    if (caller.hasExtra(APLReflectionConstants.EXTRA_WIFI_CONFIGURATION))
                    {
                        WifiConfiguration wifiConf = (WifiConfiguration) caller.getExtras().get(APLReflectionConstants.EXTRA_WIFI_CONFIGURATION);
                        if (wifiConf != null)
                        {
                            APLNetworkId wifiId = new APLNetworkId(ProxyUtils.cleanUpSSID(wifiConf.SSID), ProxyUtils.getSecurity(wifiConf));
                            networkIds.add(wifiId);
                        }
                    }

//                    if (caller.hasExtra(APLReflectionConstants.EXTRA_MULTIPLE_NETWORKS_CHANGED))
//                    {
//                        App.getTraceUtils().e(TAG,"EXTRA_MULTIPLE_NETWORKS_CHANGED not handled");
//                        App.getTraceUtils().logIntent(TAG, caller, Log.ERROR, true);
//                    }
                }
            }
        }

        return networkIds;
    }

    private void syncProxyConfigurations(List<APLNetworkId> configurations)
    {
        Map<APLNetworkId, WifiConfiguration> configuredNetworks = APL.getConfiguredNetworks();
        Timber.i("Configured %d Wi-Fi on the device", configuredNetworks.size());

        if (configurations.isEmpty())
        {
            Timber.i("No configurations specificed, must sync all of them!");
            configurations.addAll(configuredNetworks.keySet());
        }

        Timber.i(String.format("Analyzing %d Wi-Fi AP configurations", configurations.size()));

        for (APLNetworkId aplNetworkId : configurations)
        {
            try
            {
                App.getTraceUtils().partialTrace(TAG, "syncAP", "Handling network: " + aplNetworkId.toString(), Log.INFO);

                if (configuredNetworks.containsKey(aplNetworkId))
                {
                    WifiConfiguration wifiConfiguration = configuredNetworks.get(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Get WifiConfiguration", Log.INFO);

                    WiFiAPConfig wiFiAPConfig = APL.getWiFiAPConfiguration(wifiConfiguration);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Get WiFiAPConfig", Log.INFO);

                    WiFiAPEntity wiFiAPEntity = App.getDBManager().upsertWifiAP(wiFiAPConfig);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "Upsert WiFiAPEntity", Log.INFO);

                    App.getWifiNetworksManager().updateWifiConfig(wiFiAPConfig);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "updateWifiConfig: " + wiFiAPEntity.toString(), Log.INFO);
                }
                else
                {
                    App.getDBManager().deleteWifiAP(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "deleteWifiAP: " + aplNetworkId.toString(), Log.INFO);

                    App.getWifiNetworksManager().removeWifiConfig(aplNetworkId);
                    App.getTraceUtils().partialTrace(TAG, "syncAP", "removeWifiConfig: " + aplNetworkId.toString(), Log.INFO);
                }
            }
            catch (Exception e)
            {
                Timber.e(e,"Exception during ProxySyncService");
            }
        }

        Timber.i(TAG, "Sending broadcast intent " + Intents.PROXY_REFRESH_UI);
        Intent intent = new Intent(Intents.PROXY_REFRESH_UI);
        getApplicationContext().sendBroadcast(intent);
    }
}