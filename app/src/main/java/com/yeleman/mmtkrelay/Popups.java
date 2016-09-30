package com.yeleman.mmtkrelay;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.SystemUpdatePolicy;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.Callable;

import static java.lang.String.format;


public class Popups {

    public static class displayQuitOrRetry extends Popups {
        public displayQuitOrRetry(final MainActivity activity,
                                  String title, String message,
                                  final Callable callback) {
            super();

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.setTitle(title);
            alertBuilder.setMessage(message);
            alertBuilder.setIcon(R.mipmap.ic_launcher);
            alertBuilder.setPositiveButton(R.string.quit_button,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            activity.quit();
                        }
                    });
            alertBuilder.setNegativeButton(R.string.retry_button,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                callback.call();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
            // Remember, create doesn't show the dialog
            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.show();
        }
    }

    public static class displayErrorPopup extends Popups {

        public displayErrorPopup(Activity activity, String message) {
            super();
            AlertDialog.Builder helpBuilder = new AlertDialog.Builder(activity);
            helpBuilder.setTitle("Erreur !");
            helpBuilder.setMessage("Impossible d'envoyer le rapport :\n\n" + message + "\n\nVous devez corriger et re-envoyer.");
            helpBuilder.setIcon(R.mipmap.ic_launcher);
            helpBuilder.setPositiveButton("Fermer et corriger",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing but close the dialog
                        }
                    });

            // Remember, create doesn't show the dialog
            AlertDialog helpDialog = helpBuilder.create();
            helpDialog.show();
        }
    }

    public static AlertDialog.Builder getDialogBuilder(Activity activity,
                                                       String title,
                                                       String message,
                                                       boolean cancelable) {
        AlertDialog.Builder smsDialogBuilder = new AlertDialog.Builder(activity);
        smsDialogBuilder.setCancelable(cancelable);
        smsDialogBuilder.setTitle(title);
        smsDialogBuilder.setMessage(message);
        smsDialogBuilder.setIcon(R.mipmap.ic_launcher);
        return smsDialogBuilder;
    }

    public static AlertDialog getStandardDialog(final Activity activity,
                                                String title,
                                                String message,
                                                boolean cancelable,
                                                final boolean shouldFinishActivity) {
        AlertDialog.Builder dialogBuilder = getDialogBuilder(activity, title, message, cancelable);
        dialogBuilder.setPositiveButton(
                activity.getString(R.string.standard_dialog_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // close the dialog (auto)
                        if (shouldFinishActivity) {
                            activity.finish();
                        }
                    }
                });
        return dialogBuilder.create();
    }

//    public static void updatePopupForStatus(AlertDialog dialog, int status) {
//        int textColor = Constants.getColorForStatus(status);
//        if (textColor != -1) {
//            // title
//            int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
//            TextView tv = (TextView) dialog.findViewById(textViewId);
//            tv.setTextColor(textColor);
//            // divider
//            int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
//            View divider = dialog.findViewById(dividerId);
//            divider.setBackgroundColor(textColor);
//        }
//    }

    public static DialogInterface.OnClickListener getBlankClickListener() {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        };
    }

    public static ProgressDialog getStandardProgressDialog(Activity activity,
                                                           String title,
                                                           String message,
                                                           boolean cancelable) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setIcon(R.mipmap.ic_launcher);
        progressDialog.setCancelable(cancelable);
        return progressDialog;
    }

//    public static void startIntentIfOnline(final Activity activity, final Intent intent) {
//        if (isOnline(activity)) {
//            activity.startActivity(intent);
//            return;
//        }
//
//        AlertDialog.Builder dialogBuilder = getDialogBuilder(
//                activity, activity.getString(R.string.required_connexion_title),
//                activity.getString(R.string.required_connexion_body), true);
//        dialogBuilder.setNegativeButton(activity.getString(R.string.required_connexion_cancel), getBlankClickListener());
//        dialogBuilder.setPositiveButton(activity.getString(R.string.required_connexion_retry), new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                startIntentIfOnline(activity, intent);
//            }
//        });
//        AlertDialog dialog = dialogBuilder.create();
//        dialog.show();
//    }

    public static boolean isOnline(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

}
