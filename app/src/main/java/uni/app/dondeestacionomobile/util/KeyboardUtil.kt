package uni.app.dondeestacionomobile.util

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class KeyboardUtil {
    companion object {
        fun hideSoftKeyboard(editText: EditText, activity: Activity?) {
            if (editText.hasFocus()) {
                val view = activity?.currentFocus
                view?.let { v ->
                    val imm =
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
                editText.clearFocus()
            }
        }
    }
}