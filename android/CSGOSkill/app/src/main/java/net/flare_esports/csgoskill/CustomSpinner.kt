package net.flare_esports.csgoskill

import android.content.Context
import android.util.AttributeSet
import android.support.v7.widget.AppCompatSpinner
import android.view.View
import android.widget.AdapterView

class CustomSpinner : AppCompatSpinner {
    private val TAG = "CustomSpinner"
    private var mListener: OnSpinnerEventsListener? = null
    private var mOpenInitiated = false
    var itemSelectedListener: ((parent: AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit)? = null
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

    interface OnSpinnerEventsListener {

        fun onSpinnerOpened()

        fun onSpinnerClosed()

    }

    override fun performClick(): Boolean {
        // register that the Spinner was opened so we have a status
        // indicator for the activity(which may lose focus for some other
        // reasons)
        mOpenInitiated = true
        if (mListener != null) {
            mListener!!.onSpinnerOpened()
        }
        return super.performClick()
    }

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
