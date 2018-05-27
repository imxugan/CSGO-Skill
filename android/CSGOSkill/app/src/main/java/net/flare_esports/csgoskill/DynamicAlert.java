/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

// Intentionally left as Java

package net.flare_esports.csgoskill;

import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
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
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

/**
 * This is simply a non-exhaustive "wrapper" class to make life slightly easier
 * and to hopefully standardize how we use Alerts in the future.
 *
 * To use selection lists, you must get() the DynamicAlert and then add your
 * stuff. You can then safely remake the very same DynamicAlert by using
 * newAlert(Builder), all without making a new DynamicAlert.
 *
 * By default (except when initialized with a builder), all DynamicAlerts have
 * a GOT IT button, and cannot be cancelled.
 *
 * <b>Everything is chainable!</b>
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class DynamicAlert {

    private AlertDialog.Builder self;
    private Context c;

    /**
     * Special Actions for specifying what a button should do when pressed.
     */
    public enum Action {
        /** For dismissing the dialog, default in many cases */
        DISMISS,

        /** For annoying users by constantly recreating the same dialog */
        RECREATE,

        /** For canceling the dialog, like {@code DISMISS} except it calls the {@code onCancelListener}*/
        CANCEL,

        /** For doing absolutely nothing */
        NONE
    }

    /** The default theme used for all dialogs */
    private static final int THEME_DEFAULT = R.style.Theme_Flare_AlertDialog;

    /**
     * The default setup for all DynamicAlerts. If you are copying this class, just change the
     * stuff in here to whatever you want to propagate to all your dialogs. Can safely include
     * any default message or title text, as they'll always be overridden if set.
     */
    private void defaultSetup() {
        setPositive(R.string.got_it);
        setCancelable(false);
    }

    /**
     * Creates the most basic DynamicAlert
     *
     * @param context context that this alert will be shown in
     */
    public DynamicAlert(Context context) {
        self = new AlertDialog.Builder(context, THEME_DEFAULT);
        c = context;
        defaultSetup();
    }

    /**
     * Creates a DynamicAlert with some text inside
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     */
    public DynamicAlert(Context context, String message) {
        self = new AlertDialog.Builder(context, THEME_DEFAULT);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    /**
     * Creates a DynamicAlert with some text inside
     *
     * @param context context that this alert will be shown in
     * @param message string resource for the text inside
     */
    public DynamicAlert(Context context, @StringRes int message) {
        self = new AlertDialog.Builder(context, THEME_DEFAULT);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    /**
     * Creates a DynamicAlert with the specified theme and some text inside. Now you're customizing!
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     * @param theme the style for this alert
     */
    public DynamicAlert(Context context, String message, @StyleRes int theme) {
        self = new AlertDialog.Builder(context, theme);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    /**
     * Creates a DynamicAlert with the {@code theme} and some text inside. Now you're customizing!
     *
     * @param context context that this alert will be shown in
     * @param message string resource for the text inside
     * @param theme the style for this alert
     */
    public DynamicAlert(Context context, @StringRes int message, @StyleRes int theme) {
        self = new AlertDialog.Builder(context, theme);
        c = context;
        defaultSetup();
        self.setMessage(message);
    }

    /**
     * Remakes a DynamicAlert with the {@code builder}. Useful for when you need to work with the
     * {@link AlertDialog.Builder} for stuff.
     *
     * @param builder the builder to create a DynamicAlert from
     */
    public DynamicAlert(@NotNull AlertDialog.Builder builder) { self = builder; c = builder.getContext(); }
    /* END CONSTRUCTORS */


    /**
     * Useful for when you need to work with the {@link AlertDialog.Builder} for stuff
     *
     * @return the builder contained inside this DynamicAlert
     */
    public AlertDialog.Builder get() { return self; }

    /**
     * Tries to show the dialog regardless of the calling thread, this doesn't always work!
     *
     * @return the {@link AlertDialog} created
     */
    public AlertDialog show() {
        // Sneaky always-show-regardless-of-who-called-and-where function B-)
        boolean isUiThread = VERSION.SDK_INT >= VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();
        if (isUiThread)
            return self.show();
        else
            new Handler(Looper.getMainLooper()).post(self::show);
        return self.create();
    }

    /**
     * Creates this DynamicAlert
     *
     * @return the created {@link AlertDialog}
     */
    public AlertDialog create() { return self.create(); }


    /**
     * Converts this DynamicAlert into a new one, resetting it entirely.
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     * @param theme the style for this alert
     * @return the new DynamicAlert
     */
    private DynamicAlert new_alert(Context context, String message, @StyleRes int theme) {
        self = new AlertDialog.Builder(context, theme);
        defaultSetup();
        if (message != null) self.setMessage(message);
        c = context;
        return this;
    }

    /**
     * Converts this DynamicAlert into a new one, resetting it entirely.
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     * @return the new DynamicAlert
     */
    private DynamicAlert new_alert(Context context, String message) {
        return new_alert(context, message, THEME_DEFAULT);
    }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param context context that this alert will be shown in
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(Context context) { return new_alert(context, null); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(Context context, String message) { return new_alert(context, message); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param context context that this alert will be shown in
     * @param theme the style for this alert
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(Context context, @StyleRes int theme) { return new_alert(context, null, theme); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param context context that this alert will be shown in
     * @param message the text inside
     * @param theme the style for this alert
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(Context context, String message, @StyleRes int theme) { return new_alert(context, message, theme); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert() { return new_alert(c, null); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param message the text inside
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(String message) { return new_alert(c, message); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param theme the style for this alert
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(@StyleRes int theme) { return new_alert(c, null, theme); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param message the text inside
     * @param theme the style for this alert
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(String message, @StyleRes int theme) { return new_alert(c, message, theme); }

    /**
     * Resets this DynamicAlert, useful for creating multiple alerts with the same DynamicAlert object
     *
     * @param builder the builder to create a DynamicAlert from
     * @return the new DynamicAlert
     */
    public DynamicAlert newAlert(@NotNull AlertDialog.Builder builder) { self = builder; c = builder.getContext(); return this; }


    /* SET MESSAGES */

    /**
     * Sets the message for this DynamicAlert
     *
     * @param message the text inside
     * @return this DynamicAlert
     */
    public DynamicAlert setMessage(String message) {
        self.setMessage(message);
        return this;
    }

    /**
     * Sets the message for this DynamicAlert
     *
     * @param message string resource for the text inside
     * @return this DynamicAlert
     */
    public DynamicAlert setMessage(@StringRes int message) {
        self.setMessage(message);
        return this;
    }

    /**
     * Sets an HTML message for this DynamicAlert. Note: this calls {@link #setView(View)}!
     *
     * @param message raw HTML string
     * @return this DynamicAlert
     */
    public DynamicAlert setHTML(String message) {
        TextView text = new TextView(c);
        text.setText(Html.fromHtml(message));

        // Get the primary text color for the supplied theme
        TypedValue a = new TypedValue();
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        Resources.Theme theme = self.getContext().getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, a, true);
        text.setTextColor(a.data);

        // Get the text size for the supplied theme
        theme.resolveAttribute(android.R.attr.textSize, a, true);

        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimension(metrics));
        /* The normal padding for the TextView used by default in alert_dialog.xml comes out to
         * Top:     7dp
         * Bottom: 17dp
         * Start:  19dp
         * End:    15dp
         *
         * This TextView is called "android.R.id.message". If you want to see it, just type that
         * inside any class with Android Studio, right-click the "message" section, and select
         * "Go To -> Declaration" and find the file called "alert_dialog.xml"
         *
         * The custom view, which is used if a view is set with Builder.setView(), has this padding
         * Top:    5dp
         * Bottom: 5dp
         * Start:  0dp
         * End:    0dp
         *
         * This FrameLayout is called "android.R.id.custom". Follow the above instructions to find it
         *
         * That is why this method creates all the extra views with padding and stuff.
         * Remember that, by default, the TextView is actually inside of a ScrollView!
         */

        ScrollView scroll = new ScrollView(c);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        // After testing, I determined these values perfectly mimic the padding, despite it not matching mathematically
        scroll.setPadding((int)Constants.dpToPx(19), (int)Constants.dpToPx(7), (int)Constants.dpToPx(15), 0);
        int five = (int) Constants.dpToPx(5);
        text.setPadding(five, five, five, five);
        scroll.addView(text, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        self.setView(scroll);
        return this;
    }

    /* END MESSAGES */


    /* SET TITLE */

    /**
     * Adds a title to this DynamicAlert, use {@code null} to remove the title
     *
     * @param title text on top
     * @return this DynamicAlert
     */
    public DynamicAlert setTitle(String title) {
        self.setTitle(title);
        return this;
    }

    /**
     * Adds a title to this DynamicAlert, use {@code null} to remove the title
     *
     * @param title string resource for the text on top
     * @return this DynamicAlert
     */
    public DynamicAlert setTitle(@StringRes int title) {
        self.setTitle(title);
        return this;
    }

    /**
     * Adds a view to the top of this DynamicAlert, for custom title bars
     *
     * @param view custom title bar
     * @return this DynamicAlert
     */
    public DynamicAlert setTitle(View view) {
        self.setCustomTitle(view);
        return this;
    }

    /* END TITLE */


    /* SET VIEW */

    /**
     * Sets a custom view for this DynamicAlert, for ultra-customized alerts. Be careful! This removes
     * any message text as it overwrites the content view of the alert.
     *
     * @param layout custom alert layout
     * @return this DynamicAlert, should be followed by {@link DynamicAlert#show()}
     */
    public DynamicAlert setView(@LayoutRes int layout) {
        self.setView(layout);
        return this;
    }

    /**
     * Sets a custom view for this DynamicAlert, for ultra-customized alerts. Be careful! This removes
     * any message text as it overwrites the content view of the alert.
     *
     * @param view custom alert
     * @return this DynamicAlert, should be followed by {@link DynamicAlert#show()}
     */
    public DynamicAlert setView(View view) {
        self.setView(view);
        return this;
    }

    /* END VIEW */


    /* SET ICON */

    /**
     * Adds a custom icon next to the title
     *
     * @param icon title icon
     * @return this DynamicAlert
     */
    public DynamicAlert setIcon(@DrawableRes int icon) { self.setIcon(icon); return this; }

    /**
     * Adds a custom icon next to the title
     *
     * @param icon title icon
     * @return this DynamicAlert
     */
    public DynamicAlert setIcon(Drawable icon) { self.setIcon(icon); return this; }

    /**
     * Sets the icon attributes
     *
     * @param attr icon attributes
     * @return this DynamicAlert
     */
    public DynamicAlert setIconT(@AttrRes int attr) { self.setIconAttribute(attr); return this; }

    /* END ICON */

    /* CANCELABLE */

    /**
     * Sets whether the DynamicAlert can be canceled, that is, dismissed with no actions taken.
     * Enabling this is most likely a mistake.
     *
     * @param canCancel whether or not the DynamicAlert can be canceled
     * @return this DynamicAlert
     */
    public DynamicAlert setCancelable(boolean canCancel) { self.setCancelable(canCancel); return this; }

    /* END CANCEL */

    /* SET BUTTONS */

    /**
     * Private stuff
     *
     * @param action what happens when this button is pressed
     * @param text the text for this button
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    private DynamicAlert positive(Action action, String text, final Runnable runnable) {
        self.setPositiveButton(text, (dialogInterface, i) -> {
            if (runnable != null) new Thread(runnable).start();
            switch (action) {
                case DISMISS: dialogInterface.dismiss(); break;
                case CANCEL: dialogInterface.cancel(); break;
            }
        });
        return this;
    }

    /**
     * Private stuff
     *
     * @param action what happens when this button is pressed
     * @param text the text for this button
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    private DynamicAlert negative(Action action, String text, final Runnable runnable) {
        self.setNegativeButton(text, (dialogInterface, i) -> {
            if (runnable != null) new Thread(runnable).start();
            switch (action) {
                case DISMISS: dialogInterface.dismiss(); break;
                case CANCEL: dialogInterface.cancel(); break;
            }
        });
        return this;
    }

    /**
     * Private stuff
     *
     * @param action what happens when this button is pressed
     * @param text the text for this button
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    private DynamicAlert neutral(Action action, String text, final Runnable runnable) {
        self.setNeutralButton(text, (dialogInterface, i) -> {
            if (runnable != null) new Thread(runnable).start();
            switch (action) {
                case DISMISS: dialogInterface.dismiss(); break;
                case CANCEL: dialogInterface.cancel(); break;
            }
        });
        return this;
    }

    /**
     * Private stuff
     *
     * @param text the text for this button
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    private DynamicAlert positive(String text, DialogInterface.OnClickListener listener) {
        self.setPositiveButton(text, listener);
        return this;
    }

    /**
     * Private stuff
     *
     * @param text the text for this button
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    private DynamicAlert negative(String text, DialogInterface.OnClickListener listener) {
        self.setNegativeButton(text, listener);
        return this;
    }

    /**
     * Private stuff
     *
     * @param text the text for this button
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    private DynamicAlert neutral(String text, DialogInterface.OnClickListener listener) {
        self.setNeutralButton(text, listener);
        return this;
    }


    /* SET POSITIVE */

    /**
     * Adds a default positive "OK" button
     *
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive() { return positive(Action.DISMISS, c.getString(android.R.string.ok), null); }


    /**
     * Adds a positive button
     *
     * @param text button text
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(String text)         { return positive(Action.DISMISS, text, null); }

    /**
     * Adds a positive button
     *
     * @param text string resource for button text
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(@StringRes int text) { return positive(Action.DISMISS, c.getString(text), null); }

    /**
     * Adds a default positive "OK" button
     *
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(Action action)         { return positive(action, c.getString(android.R.string.ok), null); }

    /**
     * Adds a default positive "OK" button
     *
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(Runnable runnable)   { return positive(Action.NONE, c.getString(android.R.string.ok), runnable); }


    /**
     * Adds a positive button
     *
     * @param text button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(String text, Action action)               { return positive(action, text, null); }

    /**
     * Adds a positive button
     *
     * @param text string resource for button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(@StringRes int text, Action action)       { return positive(action, c.getString(text), null); }

    /**
     * Adds a positive button
     *
     * @param text button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(String text, Runnable runnable)         { return positive(Action.NONE, text, runnable); }

    /**
     * Adds a positive button
     *
     * @param text string resource for button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(@StringRes int text, Runnable runnable) { return positive(Action.NONE, c.getString(text), runnable); }

    /**
     * Adds a positive button
     *
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(Action action, Runnable runnable)         { return positive(action, c.getString(android.R.string.ok), runnable); }


    /**
     * Adds a positive button
     *
     * @param text button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(String text, Action action, Runnable runnable)         { return positive(action, text, runnable); }

    /**
     * Adds a positive button
     *
     * @param text string resource for button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(@StringRes int text, Action action, Runnable runnable) { return positive(action, c.getString(text), runnable); }


    /**
     * Adds a default positive "OK" button
     *
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(DialogInterface.OnClickListener listener)                      { return positive(c.getString(android.R.string.ok), listener); }

    /**
     * Adds a positive button
     *
     * @param text button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(String text, DialogInterface.OnClickListener listener)         { return positive(text, listener); }

    /**
     * Adds a positive button
     *
     * @param text string resource for button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setPositive(@StringRes int text, DialogInterface.OnClickListener listener) { return positive(c.getString(text), listener); }

    /* END POSITIVE */


    /* SET NEGATIVE */

    /**
     * Adds a default negative "NO" button
     *
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative() { return negative(Action.DISMISS, c.getString(android.R.string.no), null); }


    /**
     * Adds a negative button
     *
     * @param text button text
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(String text)         { return negative(Action.DISMISS, text, null); }

    /**
     * Adds a negative button
     *
     * @param text string resource for button text
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(@StringRes int text) { return negative(Action.DISMISS, c.getString(text), null); }

    /**
     * Adds a default negative "NO" button
     *
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(Action action)         { return negative(action, c.getString(android.R.string.no), null); }

    /**
     * Adds a default negative "NO" button
     *
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(Runnable runnable)   { return negative(Action.NONE, c.getString(android.R.string.no), runnable); }


    /**
     * Adds a negative button
     *
     * @param text button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(String text, Action action)               { return negative(action, text, null); }

    /**
     * Adds a negative button
     *
     * @param text string resource for button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(@StringRes int text, Action action)       { return negative(action, c.getString(text), null); }

    /**
     * Adds a negative button
     *
     * @param text button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(String text, Runnable runnable)         { return negative(Action.NONE, text, runnable); }

    /**
     * Adds a negative button
     *
     * @param text string resource for button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(@StringRes int text, Runnable runnable) { return negative(Action.NONE, c.getString(text), runnable); }

    /**
     * Adds a negative button
     *
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(Action action, Runnable runnable)         { return negative(action, c.getString(android.R.string.no), runnable); }


    /**
     * Adds a negative button
     *
     * @param text button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(String text, Action action, Runnable runnable)         { return negative(action, text, runnable); }

    /**
     * Adds a negative button
     *
     * @param text string resource for button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(@StringRes int text, Action action, Runnable runnable) { return negative(action, c.getString(text), runnable); }


    /**
     * Adds a default negative "NO" button
     *
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(DialogInterface.OnClickListener listener)                      { return negative(c.getString(android.R.string.no), listener); }

    /**
     * Adds a negative button
     *
     * @param text button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(String text, DialogInterface.OnClickListener listener)         { return negative(text, listener); }

    /**
     * Adds a negative button
     *
     * @param text string resource for button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setNegative(@StringRes int text, DialogInterface.OnClickListener listener) { return negative(c.getString(text), listener); }

    /* END NEGATIVE */


    /* SET NEUTRAL */

    /**
     * Adds a default neutral "OK" button
     *
     * @return this DynamicAlert
     */
    public DynamicAlert setButton() { return neutral(Action.DISMISS, c.getString(android.R.string.ok), null); }


    /**
     * Adds a neutral button
     *
     * @param text button text
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(String text)         { return neutral(Action.DISMISS, text, null); }

    /**
     * Adds a neutral button
     *
     * @param text string resource for button text
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(@StringRes int text) { return neutral(Action.DISMISS, c.getString(text), null); }

    /**
     * Adds a default neutral "OK" button
     *
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(Action action)         { return neutral(action, c.getString(android.R.string.ok), null); }

    /**
     * Adds a default neutral "OK" button
     *
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(Runnable runnable)   { return neutral(Action.NONE, c.getString(android.R.string.ok), runnable); }


    /**
     * Adds a neutral button
     *
     * @param text button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(String text, Action action)               { return neutral(action, text, null); }

    /**
     * Adds a neutral button
     *
     * @param text string resource for button text
     * @param action button action
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(@StringRes int text, Action action)       { return neutral(action, c.getString(text), null); }

    /**
     * Adds a neutral button
     *
     * @param text button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(String text, Runnable runnable)         { return neutral(Action.NONE, text, runnable); }

    /**
     * Adds a neutral button
     *
     * @param text string resource for button text
     * @param runnable function to run when this button is pressed
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(@StringRes int text, Runnable runnable) { return neutral(Action.NONE, c.getString(text), runnable); }

    /**
     * Adds a neutral button
     *
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(Action action, Runnable runnable)         { return neutral(action, c.getString(android.R.string.ok), runnable); }


    /**
     * Adds a neutral button
     *
     * @param text button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(String text, Action action, Runnable runnable)         { return neutral(action, text, runnable); }

    /**
     * Adds a neutral button
     *
     * @param text string resource for button text
     * @param action button action
     * @param runnable function to run when this button is pressed, regardless of {@code action}
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(@StringRes int text, Action action, Runnable runnable) { return neutral(action, c.getString(text), runnable); }


    /**
     * Adds a default neutral "OK" button
     *
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(DialogInterface.OnClickListener listener)                      { return neutral(c.getString(android.R.string.ok), listener); }

    /**
     * Adds a neutral button
     *
     * @param text button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(String text, DialogInterface.OnClickListener listener)         { return neutral(text, listener); }

    /**
     * Adds a neutral button
     *
     * @param text string resource for button text
     * @param listener {@link DialogInterface.OnClickListener} for this button
     * @return this DynamicAlert
     */
    public DynamicAlert setButton(@StringRes int text, DialogInterface.OnClickListener listener) { return neutral(c.getString(text), listener); }

    /* END NEUTRAL */


    /* SET LISTENER */

    /**
     * Function for the {@link AlertDialog.Builder#setOnCancelListener(DialogInterface.OnCancelListener)}
     *
     * @param runnable function
     * @return this DynamicAlert
     */
    public DynamicAlert setCancelAction(final Runnable runnable) {
        self.setOnCancelListener(dialog -> runnable.run());
        return this;
    }

    /**
     * Function for the {@link AlertDialog.Builder#setOnDismissListener(DialogInterface.OnDismissListener)}
     *
     * @param runnable function
     * @return this DynamicAlert
     */
    public DynamicAlert setDismissAction(final Runnable runnable) {
        self.setOnDismissListener(dialog -> runnable.run());
        return this;
    }

    /* END LISTENER */

}
