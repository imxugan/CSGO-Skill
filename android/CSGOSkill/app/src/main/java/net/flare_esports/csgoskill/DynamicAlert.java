/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build.*;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.view.View;

/*
 * This is simply a non-exhaustive "wrapper" class to make life slightly easier
 * and to hopefully standardize how we use Alerts in the future.
 *
 * To use selection lists, you must get() the DynamicAlert and then add your
 * stuff. You can then safely remake the very same DynamicAlert by using
 * newAlert(Builder), all without making a new DynamicAlert.
 *
 * By default (except when initialized with a builder), all DynamicAlerts have
 * an OK button which simply dismisses the dialogue, and cannot be cancelled.
 */

class DynamicAlert {

    private AlertDialog.Builder self;
    private Context c;

    public static final char ACTION_DISMISS = 0;
    public static final char ACTION_RECREATE = 1;
    public static final char ACTION_CANCEL = 2;
    public static final char ACTION_NONE = 3;

    // Set to your own custom default theme
    public static final int THEME_DEFAULT = R.style.Flare_Dialog_AlertDialog;

    // Change based on what default functionality you desire. Can safely
    // include default message text.
    private void defaultSetup() {
        setButton();
        noCancel();
    }

    /* CONSTRUCTORS */
    public DynamicAlert(Context context) {
        self = new AlertDialog.Builder(context);
        c = context;
        defaultSetup();
    }

    public DynamicAlert(Context context, String message) {
        self = new AlertDialog.Builder(context);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    public DynamicAlert(Context context, @StringRes int message) {
        self = new AlertDialog.Builder(context);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    public DynamicAlert(Context context, String message, @StyleRes int theme) {
        self = new AlertDialog.Builder(context, theme);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    public DynamicAlert(Context context, @StringRes int message, @StyleRes int theme) {
        self = new AlertDialog.Builder(context, theme);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    public DynamicAlert(AlertDialog.Builder builder) { self = builder; c = builder.getContext(); }
    /* END CONSTRUCTORS */


    // Get function
    public AlertDialog.Builder get() { return self; }

    // Show function
    public AlertDialog show() {
        // Sneaky always-show-regardless-of-who-called-and-where function B-)
        boolean isUiThread = VERSION.SDK_INT >= VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();
        if (isUiThread)
            return self.show();
        else
            new Handler(Looper.getMainLooper()).post(new Runnable() { @Override public void run() { self.show(); }});
        return self.create();
    }

    // Create function
    public AlertDialog create() { return self.create(); }


    /* REBUILDERS */
    private DynamicAlert new_alert(Context context, @StyleRes int theme, String message) {
        self = new AlertDialog.Builder(context, theme);
        defaultSetup();
        if (message != null) self.setMessage(message);
        c = context;
        return this;
    }

    private DynamicAlert new_alert(Context context, String message) {
        self = new AlertDialog.Builder(context);
        defaultSetup();
        if (message != null) self.setMessage(message);
        c = context;
        return this;
    }

    public DynamicAlert newAlert(Context context) { return new_alert(context, null); }

    public DynamicAlert newAlert(Context context, @StyleRes int theme) { return new_alert(context, theme, null); }

    public DynamicAlert newAlert(Context context, @StyleRes int theme, String message) { return new_alert(context, theme, message); }

    public DynamicAlert newAlert(Context context, String message) { return new_alert(context, message); }

    public DynamicAlert newAlert() { return new_alert(c, null); }

    public DynamicAlert newAlert(AlertDialog.Builder builder) { self = builder; c = builder.getContext(); return this; }

    public DynamicAlert newAlert(@StyleRes int theme) { return new_alert(c, theme, null); }

    public DynamicAlert newAlert(@StyleRes int theme, String message) { return new_alert(c, theme, message); }

    public DynamicAlert newAlert(String message) { return new_alert(c, message); }
    /* END REBUILDERS */


    /* SET MESSAGES */
    public DynamicAlert setMessage(String message) {
        self.setMessage(message);
        return this;
    }

    public DynamicAlert setMessage(@StringRes int message) {
        self.setMessage(message);
        return this;
    }
    /* END MESSAGES */


    /* SET TITLE */
    public DynamicAlert setTitle(String title) {
        self.setTitle(title);
        return this;
    }

    public DynamicAlert setTitle(@StringRes int title) {
        self.setTitle(title);
        return this;
    }

    public DynamicAlert setTitle(View view) {
        self.setCustomTitle(view);
        return this;
    }
    /* END TITLE */


    /* SET VIEW */
    public DynamicAlert setView(@LayoutRes int layout) {
        self.setView(layout);
        return this;
    }

    public DynamicAlert setView(View view) {
        self.setView(view);
        return this;
    }
    /* END VIEW */


    /* SET ICON */
    public DynamicAlert setIcon(@DrawableRes int icon) { self.setIcon(icon); return this; }

    public DynamicAlert setIcon(Drawable icon) { self.setIcon(icon); return this; }

    public DynamicAlert setIconT(@AttrRes int icon) { self.setIconAttribute(icon); return this; }
    /* END ICON */

    /* CANCELABLE */
    public DynamicAlert noCancel() { self.setCancelable(false); return this; }

    /**
     * You might as well be using a Toast message if your alert can be canceled.
     */
    public DynamicAlert allowCancel() {
        self.setCancelable(true); // Are you sure about that?
        return this;
    }
    /* END CANCEL */

    // Set Buttons
    private DynamicAlert positive(final char action, String text, final Runnable runnable) {
        self.setPositiveButton(text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (runnable != null) new Thread(runnable).start();
                switch (action) {
                    case ACTION_DISMISS: dialogInterface.dismiss(); break;
                    case ACTION_CANCEL: dialogInterface.cancel(); break;
                }
            }
        });
        return this;
    }

    private DynamicAlert negative(final char action, String text, final Runnable runnable) {
        self.setNegativeButton(text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (runnable != null) new Thread(runnable).start();
                switch (action) {
                    case ACTION_DISMISS: dialogInterface.dismiss(); break;
                    case ACTION_CANCEL: dialogInterface.cancel(); break;
                }
            }
        });
        return this;
    }

    private DynamicAlert neutral(final char action, String text, final Runnable runnable) {
        self.setNeutralButton(text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (runnable != null) new Thread(runnable).start();
                switch (action) {
                    case ACTION_DISMISS: dialogInterface.dismiss(); break;
                    case ACTION_CANCEL: dialogInterface.cancel(); break;
                }
            }
        });
        return this;
    }

    private DynamicAlert positive(String text, DialogInterface.OnClickListener listener) {
        self.setPositiveButton(text, listener);
        return this;
    }

    private DynamicAlert negative(String text, DialogInterface.OnClickListener listener) {
        self.setNegativeButton(text, listener);
        return this;
    }

    private DynamicAlert neutral(String text, DialogInterface.OnClickListener listener) {
        self.setNeutralButton(text, listener);
        return this;
    }


    /* SET POSITIVE */
    public DynamicAlert setPositive() { return positive(ACTION_DISMISS, c.getString(android.R.string.yes), null); }


    public DynamicAlert setPositive(String text)         { return positive(ACTION_DISMISS, text, null); }

    public DynamicAlert setPositive(@StringRes int text) { return positive(ACTION_DISMISS, c.getString(text), null); }

    public DynamicAlert setPositive(char action)         { return positive(action, c.getString(android.R.string.yes), null); }

    public DynamicAlert setPositive(Runnable runnable)   { return positive(ACTION_NONE, c.getString(android.R.string.yes), runnable); }


    public DynamicAlert setPositive(String text, char action)               { return positive(action, text, null); }

    public DynamicAlert setPositive(@StringRes int text, char action)       { return positive(action, c.getString(text), null); }

    public DynamicAlert setPositive(String text, Runnable runnable)         { return positive(ACTION_NONE, text, runnable); }

    public DynamicAlert setPositive(@StringRes int text, Runnable runnable) { return positive(ACTION_NONE, c.getString(text), runnable); }

    public DynamicAlert setPositive(char action, Runnable runnable)         { return positive(action, c.getString(android.R.string.yes), runnable); }


    public DynamicAlert setPositive(String text, char action, Runnable runnable)         { return positive(action, text, runnable); }

    public DynamicAlert setPositive(@StringRes int text, char action, Runnable runnable) { return positive(action, c.getString(text), runnable); }


    public DynamicAlert setPositive(DialogInterface.OnClickListener listener)                      { return positive(c.getString(android.R.string.yes), listener); }

    public DynamicAlert setPositive(String text, DialogInterface.OnClickListener listener)         { return positive(text, listener); }

    public DynamicAlert setPositive(@StringRes int text, DialogInterface.OnClickListener listener) { return positive(c.getString(text), listener); }
    /* END POSITIVE */


    /* SET NEGATIVE */
    public DynamicAlert setNegative() { return negative(ACTION_DISMISS, c.getString(android.R.string.no), null); }


    public DynamicAlert setNegative(String text)         { return negative(ACTION_DISMISS, text, null); }

    public DynamicAlert setNegative(@StringRes int text) { return negative(ACTION_DISMISS, c.getString(text), null); }

    public DynamicAlert setNegative(char action)         { return negative(action, c.getString(android.R.string.no), null); }

    public DynamicAlert setNegative(Runnable runnable)   { return negative(ACTION_NONE, c.getString(android.R.string.no), runnable); }


    public DynamicAlert setNegative(String text, char action)               { return negative(action, text, null); }

    public DynamicAlert setNegative(@StringRes int text, char action)       { return negative(action, c.getString(text), null); }

    public DynamicAlert setNegative(String text, Runnable runnable)         { return negative(ACTION_NONE, text, runnable); }

    public DynamicAlert setNegative(@StringRes int text, Runnable runnable) { return negative(ACTION_NONE, c.getString(text), runnable); }

    public DynamicAlert setNegative(char action, Runnable runnable)         { return negative(action, c.getString(android.R.string.no), runnable); }


    public DynamicAlert setNegative(String text, char action, Runnable runnable)         { return negative(action, text, runnable); }

    public DynamicAlert setNegative(@StringRes int text, char action, Runnable runnable) { return negative(action, c.getString(text), runnable); }


    public DynamicAlert setNegative(DialogInterface.OnClickListener listener)                      { return negative(c.getString(android.R.string.no), listener); }

    public DynamicAlert setNegative(String text, DialogInterface.OnClickListener listener)         { return negative(text, listener); }

    public DynamicAlert setNegative(@StringRes int text, DialogInterface.OnClickListener listener) { return negative(c.getString(text), listener); }
    /* END NEGATIVE */


    /* SET NEUTRAL */
    public DynamicAlert setButton() { return neutral(ACTION_DISMISS, c.getString(android.R.string.ok), null); }


    public DynamicAlert setButton(String text)         { return neutral(ACTION_DISMISS, text, null); }

    public DynamicAlert setButton(@StringRes int text) { return neutral(ACTION_DISMISS, c.getString(text), null); }

    public DynamicAlert setButton(char action)         { return neutral(action, c.getString(android.R.string.ok), null); }

    public DynamicAlert setButton(Runnable runnable)   { return neutral(ACTION_NONE, c.getString(android.R.string.ok), runnable); }


    public DynamicAlert setButton(String text, char action)               { return neutral(action, text, null); }

    public DynamicAlert setButton(@StringRes int text, char action)       { return neutral(action, c.getString(text), null); }

    public DynamicAlert setButton(String text, Runnable runnable)         { return neutral(ACTION_NONE, text, runnable); }

    public DynamicAlert setButton(@StringRes int text, Runnable runnable) { return neutral(ACTION_NONE, c.getString(text), runnable); }

    public DynamicAlert setButton(char action, Runnable runnable)         { return neutral(action, c.getString(android.R.string.ok), runnable); }


    public DynamicAlert setButton(String text, char action, Runnable runnable)         { return neutral(action, text, runnable); }

    public DynamicAlert setButton(@StringRes int text, char action, Runnable runnable) { return neutral(action, c.getString(text), runnable); }


    public DynamicAlert setButton(DialogInterface.OnClickListener listener)                      { return neutral(c.getString(android.R.string.no), listener); }

    public DynamicAlert setButton(String text, DialogInterface.OnClickListener listener)         { return neutral(text, listener); }

    public DynamicAlert setButton(@StringRes int text, DialogInterface.OnClickListener listener) { return neutral(c.getString(text), listener); }
    /* END NEUTRAL */


    /* SET LISTENER */
    public DynamicAlert setCancelAction(final Runnable runnable) {
        self.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                new Thread(runnable).start();
            }
        });
        return this;
    }

    public DynamicAlert setCancelAction(DialogInterface.OnCancelListener listener) {
        self.setOnCancelListener(listener);
        return this;
    }

    public DynamicAlert setCancelAction(final int action) {
        // Recreating a dismissed alert is totally not cool bro
        Log.i("Friendly Reminder", "Recreating an Alert Dialogue is NOT recommended!");
        self.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (action == ACTION_RECREATE) show();
            }
        });
        return this;
    }

    public DynamicAlert setDismissAction(final int action) {
        // Recreating a dismissed alert is totally not cool bro
        Log.i("Friendly Reminder", "Recreating an Alert Dialogue is NOT recommended!");
        self.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (action == ACTION_RECREATE) show();
            }
        });
        return this;
    }

    public DynamicAlert setDismissAction(final Runnable runnable) {
        self.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                new Thread(runnable).start();
            }
        });
        return this;
    }

    public DynamicAlert setDismissAction(DialogInterface.OnDismissListener listener) {
        self.setOnDismissListener(listener);
        return this;
    }
    /* END LISTENER */

}
