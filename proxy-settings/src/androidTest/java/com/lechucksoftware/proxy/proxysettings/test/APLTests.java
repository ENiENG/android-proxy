package com.lechucksoftware.proxy.proxysettings.test;

import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.db.ProxyEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;

import be.shouldit.proxy.lib.APL;
import be.shouldit.proxy.lib.APLNetworkId;
import be.shouldit.proxy.lib.WiFiAPConfig;
import be.shouldit.proxy.lib.reflection.android.ProxySetting;
import timber.log.Timber;

/**
 * Created by mpagliar on 22/08/2014.
 */
public class APLTests extends InstrumentationTestCase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        APL.setup(App.getInstance());
    }

    @SmallTest
    public void testChangeWifiSettings() throws Exception
    {
        Map<APLNetworkId, WiFiAPConfig> networksMap = APL.getWifiAPConfigurations();

        for (APLNetworkId networkId : networksMap.keySet())
        {
            Timber.d("Testing update proxy settings on network: %s", networkId.toString());
            WiFiAPConfig network = networksMap.get(networkId);
            updateWifiNetwork(network);
        }
    }

    private void updateWifiNetwork(WiFiAPConfig network) throws Exception
    {
        Timber.d("Got network to update: %s", network.toShortString());

        ProxySetting proxySetting = network.getProxySetting();
        String host = network.getProxyHost();
        Integer port = network.getProxyPort();
        String exclusion = network.getProxyExclusionList();
        Uri pac = network.getPacFileUri();

        ProxyEntity pe = TestUtils.createRandomProxy();

        Timber.d("Created random proxy: %s", pe.toString());

        network.setProxySetting(ProxySetting.STATIC);
        network.setProxyHost(pe.getHost());
        network.setProxyPort(pe.getPort());
        network.setPacUriFile(Uri.EMPTY);

        Timber.d("Write updated network to device: %s", network.toShortString());

        network.writeConfigurationToDevice();

        Thread.sleep(2000);

        WifiConfiguration updatedConfig = APL.getConfiguredNetwork(network.getNetworkId());
        WiFiAPConfig updatedNetwork = APL.getWiFiAPConfiguration(updatedConfig);

        Timber.d("Check network configuration has been written to device: %s", updatedNetwork.toShortString());

        assertTrue(updatedNetwork.isSameConfiguration(network));

        network.setProxySetting(proxySetting);
        network.setProxyHost(host);
        network.setProxyPort(port);
        network.setPacUriFile(pac);

        network.writeConfigurationToDevice();

        Timber.d("Restoring network configuration to start properties: %s", network.toShortString());

        Thread.sleep(2000);

        assertFalse(updatedNetwork.isSameConfiguration(network));
    }


    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void clearLog()
    {
        try
        {
            Process process = new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        }
        catch (IOException e)
        {
            Timber.e(e, "Exception clearing logcat");
        }
    }

    public boolean logContainsString(String msg)
    {
        return readLog().contains(msg);
    }

    public String readLog()
    {
        String result = "";

        try
        {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                log.append(line);
            }

            result = log.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}