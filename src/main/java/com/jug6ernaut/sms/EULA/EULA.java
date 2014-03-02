package com.jug6ernaut.sms.EULA;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import com.jug6ernaut.sms.R;

/**
 * Created by williamwebb on 3/1/14.
 */
public class EULA {

    private String EULA_PREFIX = "eula_";
    private Activity mActivity;
    private PackageInfo versionInfo;
    final String eulaKey;
    final SharedPreferences prefs;

    public EULA(Activity context) {
        mActivity = context;
        versionInfo = getPackageInfo();

        eulaKey = EULA_PREFIX + versionInfo.versionCode;
        prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pi = null;
        try {
            pi = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pi;
    }

    public void show(boolean force,boolean showCancel){
        show(force,showCancel,null);
    }

    public void show(boolean force, boolean showCancel, final OnOkListener okListener) {
        if (!isAccepted() || force) {

            // Show the Eula
            String title = mActivity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            String message = mActivity.getString(R.string.updates) + "\n\n" + mActivity.getString(R.string.eula);

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(eulaKey, true);
                            editor.commit();
                            dialogInterface.dismiss();
                            if(okListener != null)
                                okListener.onOk();
                        }
                    }).setCancelable(false);
            if(showCancel)
                builder.setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the activity as they have declined the EULA
                            mActivity.finish();
                        }

                    });
            builder.create().show();
        }
    }

    public boolean isAccepted(){
        return prefs.getBoolean(eulaKey, false);
    }

    public static interface OnOkListener{
        public void onOk();
    }
}
