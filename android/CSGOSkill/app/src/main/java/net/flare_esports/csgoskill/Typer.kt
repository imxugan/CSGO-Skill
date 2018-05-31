/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.text.Editable
import android.text.TextWatcher

/**
 * Helps us set listeners without having to manually override each function
 * every time we need to make one.
 *
 * Example Usage:
 * textView.addTextChangedListener(Typer()
 *     .afterChanged { s: Editable? ->
 *         doSomething(s.toString())
 *     }
 *     .beforeChanged { s: CharSequence?, start: Int, count: Int, after: Int ->
 *         s = s.substring(start)
 *     }
 * )
 */

class Typer : TextWatcher {

    private var before: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit = { _, _, _, _ -> }
    private var after: (s: Editable?) -> Unit = { _ -> }
    private var on: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit = { _, _, _, _ -> }

    /**
     * For validating text after you type it. Be careful about changing text here, because it will
     * cause the function to be called again!
     */
    fun afterChanged(after: (s: Editable?) -> Unit): Typer {
        this.after = after
        return this
    }

    /**
     * For seeing what text is about to be inserted and where in the [Editable]. It is an error to
     * attempt to make changes to <code>s</code> from this callback.
     */
    fun beforeChanged(before: (s: CharSequence?, start: Int, count: Int, after: Int) -> Unit): Typer {
        this.before = before
        return this
    }

    /**
     * For seeing what text has just been added and where in the [Editable]. It is an error to
     * attempt to make changes to <code>s</code> from this callback.
     */
    fun onChanged(on: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit): Typer {
        this.on = on
        return this
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { before(s, start, count, after) }

    override fun afterTextChanged(s: Editable?) { after(s) }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { on(s, start, before, count) }
}
