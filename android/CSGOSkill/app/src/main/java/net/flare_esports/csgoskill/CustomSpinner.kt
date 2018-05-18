package net.flare_esports.csgoskill

import android.content.Context
import android.util.AttributeSet
import android.support.v7.widget.AppCompatSpinner
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

/**
 * A custom spinner that has a few more features built into it, compared to the regular spinner class.
 */
class CustomSpinner : AppCompatSpinner {

    private val TAG = "CustomSpinner"
    private var mListener: OnSpinnerEventsListener? = null
    private var mOpenInitiated = false


    /** A public variable Unit to set a listener for when an item is selected */
    var itemSelectedListener: ((parent: AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit)? = null

    /** A public variable Unit to set a listener for when nothing is selected */
    var nothingSelectedListener: ((parent: AdapterView<*>?) -> Unit)? = null


    init {
        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                this@CustomSpinner.nothingSelected(parent)
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                this@CustomSpinner.itemSelected(parent, view, position, id)
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, mode: Int) : super(context, attrs, defStyleAttr, mode)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, mode: Int) : super(context, mode)

    constructor(context: Context) : super(context)

    private fun itemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        itemSelectedListener?.invoke(parent, view, position, id)
    }

    private fun nothingSelected(parent: AdapterView<*>?) {
        nothingSelectedListener?.invoke(parent)
    }

    /**
     * An interface for special spinner events, specifically when it's been opened and closed
     */
    interface OnSpinnerEventsListener {

        /**
         * Called when the spinner is initially opened, however it may fade in so
         */
        fun onSpinnerOpened()

        fun onSpinnerClosed()

    }

    override fun performClick(): Boolean {
        val click = super.performClick()

        // register that the Spinner was opened so we have a status
        // indicator for the activity(which may lose focus for some other
        // reasons)
        mOpenInitiated = true
        if (mListener != null) {
            mListener!!.onSpinnerOpened()
        }

        try {
            val popup = Spinner::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true

            // Get private mPopup member variable and try cast to ListPopupWindow
            val popupWindow = popup.get(this) as android.widget.ListPopupWindow

            // Always show scroll bar
            popupWindow.listView.isScrollbarFadingEnabled = false
        } catch (e: Throwable) {
            // Silently fail...
        }

        return click
    }

    /**
     * Sets an event listener for this spinner
     */
    fun setSpinnerEventsListener(onSpinnerEventsListener: OnSpinnerEventsListener) {
        mListener = onSpinnerEventsListener
    }

    /**
     * Propagate the closed Spinner event to the listener from outside.
     */
    fun performClosedEvent() {
        mOpenInitiated = false
        if (mListener != null) {
            mListener!!.onSpinnerClosed()
        }
    }

    /**
     * A boolean flag indicating that the Spinner triggered an open event.
     *
     * @return true for opened Spinner
     */
    fun hasBeenOpened(): Boolean {
        return mOpenInitiated
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        Log.d(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasBeenOpened() && hasWindowFocus) {
            Log.d(TAG, "closing popup")
            performClosedEvent()
        }
    }

}
