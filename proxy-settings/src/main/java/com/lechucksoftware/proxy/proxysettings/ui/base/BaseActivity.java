package com.lechucksoftware.proxy.proxysettings.ui.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lechucksoftware.proxy.proxysettings.App;
import com.lechucksoftware.proxy.proxysettings.BuildConfig;
import com.lechucksoftware.proxy.proxysettings.R;
import com.lechucksoftware.proxy.proxysettings.constants.Constants;
import com.lechucksoftware.proxy.proxysettings.constants.Intents;
import com.lechucksoftware.proxy.proxysettings.services.ViewServer;
import com.lechucksoftware.proxy.proxysettings.utils.UIUtils;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.enums.SnackbarType;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by marco on 07/11/13.
 */
public class BaseActivity extends ActionBarActivity
{
    private static boolean active = false;
    Snackbar snackbar = null;
    private UIHandler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onNewIntent");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onDestroy");
        ViewServer.get(this).removeWindow(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (BuildConfig.DEBUG)
        {
            // ONLY on DEBUG
            ViewServer.get(this).setFocusedWindow(this);
        }

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onResume");

        IntentFilter ifilt = new IntentFilter();
        ifilt.addAction(Intents.SERVICE_COMUNICATION);

        uiHandler = new UIHandler(this);

        try
        {
            registerReceiver(broadcastReceiver, ifilt);
        }
        catch (IllegalArgumentException e)
        {
            Timber.e(e, "Exception resuming BaseActivity");
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onPause");

        uiHandler.dismissSnackbar();
        uiHandler = null;

        try
        {
            // Stop the registered status receivers
            unregisterReceiver(broadcastReceiver);
        }
        catch (IllegalArgumentException e)
        {
            Timber.e(e, "Exception pausing BaseWifiActivity");
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onStart");
        active = true;
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Timber.tag(this.getClass().getSimpleName());
        Timber.d("onStop");
        active = false;
    }

    public void onDialogResult(int requestCode, int resultCode, Bundle arguments)
    {
        // Intentionally left blank
    }

    private class UIHandler extends Handler
    {
        private static final int SNACKBAR_CREATION = 1;
        private static final String CREATE_SNACKBAR = "CREATE_SNACKBAR";
        private static final String DISMISS_SNACKBAR = "DISMISS_SNACKBAR";
        WeakReference<BaseActivity> mActivity;

        public UIHandler(BaseActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message)
        {
            Bundle b = message.getData();
//            Timber.d("handleMessage: " + b.toString());

            if (b.containsKey(CREATE_SNACKBAR))
            {
                startSnackbarHandler(b.getInt(CREATE_SNACKBAR));
            }

            if (b.containsKey(DISMISS_SNACKBAR))
            {
                dismissSnackbarHandler();
            }
        }

        private void dismissSnackbarHandler()
        {
            BaseActivity activity = mActivity.get();
            if (activity != null)
            {
                if (snackbar != null)
                {
                    Date creationTime = (Date) snackbar.getTag();
                    Date currentTime = new Date();

                    long diffFromLast = currentTime.getTime() - creationTime.getTime();

                    if (diffFromLast < 1000)
                    {
                        dismissSnackbar();
                    }
                    else
                    {
                        snackbar.dismiss();
                        snackbar = null;
                    }
                }
            }
        }

        private void startSnackbarHandler(int savingOperations)
        {
            BaseActivity activity = mActivity.get();
            if (activity != null)
            {
                if (snackbar == null)
                {
                    snackbar = Snackbar.with(activity)
                            .type(SnackbarType.SINGLE_LINE)
                            .text(getResources().getQuantityString(R.plurals.saving_wifi_networks, savingOperations, savingOperations))
                            .swipeToDismiss(false)
                            .color(Color.GRAY)
                            .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE);
                    snackbar.setTag(new Date());
                    snackbar.show(activity);
                }
                else
                {
                    snackbar.text(getResources().getQuantityString(R.plurals.saving_wifi_networks, savingOperations, savingOperations));
                }
            }
        }

        public void dismissSnackbar()
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putString(DISMISS_SNACKBAR, "");
            message.setData(b);
            sendMessageDelayed(message, 500);
        }

        public void startSnackbar(int savingOperations)
        {
            Message message = this.obtainMessage();
            Bundle b = new Bundle();
            b.putInt(CREATE_SNACKBAR, savingOperations);
            message.setData(b);
            sendMessageDelayed(message, 0);
        }
    }

    public void refreshUI()
    {
        int savingOperations = App.getWifiNetworksManager().savingOperationsCount();
        if (savingOperations > 0)
        {
            uiHandler.startSnackbar(savingOperations);
        }
        else
        {
            uiHandler.dismissSnackbar();
        }

        try
        {
            List<Fragment> fragments = getSupportFragmentManager().getFragments(); //findFragmentById(R.id.fragment_container);
            for (Fragment f : fragments)
            {
                if (f instanceof IBaseFragment)
                {
                    IBaseFragment ibf = (IBaseFragment) f;
                    ibf.refreshUI();
                }
            }
        }

        catch (
                Exception e
                )

        {
            Timber.e(e, "Exception during IBaseFragment refresh from %s", this.getClass().getSimpleName());
        }

    }

    private void handleIntent(Intent intent)
    {
        String action = intent.getAction();

        App.getTraceUtils().logIntent(this.getClass().getSimpleName(), intent, Log.DEBUG, true);

        if (action.equals(Intents.SERVICE_COMUNICATION))
        {
            final String title;
            final String message;
            final String closeActivty;

            if (intent.hasExtra(Constants.SERVICE_COMUNICATION_TITLE))
            {
                title = intent.getStringExtra(Constants.SERVICE_COMUNICATION_TITLE);
            }
            else
            {
                title = "";
            }

            if (intent.hasExtra(Constants.SERVICE_COMUNICATION_MESSAGE))
            {
                message = intent.getStringExtra(Constants.SERVICE_COMUNICATION_MESSAGE);
            }
            else
            {
                message = "";
            }

            if (intent.hasExtra(Constants.SERVICE_COMUNICATION_CLOSE_ACTIVITY))
            {
                closeActivty = intent.getStringExtra(Constants.SERVICE_COMUNICATION_CLOSE_ACTIVITY);
            }
            else
            {
                closeActivty = "";
            }

            UIUtils.showDialog(BaseActivity.this, message, title, new MaterialDialog.ButtonCallback()
            {

                @Override
                public void onPositive(MaterialDialog dialog)
                {

                    if (!TextUtils.isEmpty(closeActivty)
                            && closeActivty.equals(this.getClass().getSimpleName()))
                    {
                        finish();
                    }
                }

            });
        }
        else
        {
            Timber.e("Received intent not handled: " + intent.getAction());
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            handleIntent(intent);
        }
    };
}
