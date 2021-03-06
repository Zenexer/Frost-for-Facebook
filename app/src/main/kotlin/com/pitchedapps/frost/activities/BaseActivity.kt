package com.pitchedapps.frost.activities

import android.os.Bundle
import ca.allanwang.kau.internal.KauBaseActivity
import com.pitchedapps.frost.R
import com.pitchedapps.frost.utils.Prefs
import com.pitchedapps.frost.utils.materialDialogThemed
import com.pitchedapps.frost.utils.setFrostTheme

/**
 * Created by Allan Wang on 2017-06-12.
 */
abstract class BaseActivity : KauBaseActivity() {
    override fun onBackPressed() {
        if (isTaskRoot && Prefs.exitConfirmation) {
            materialDialogThemed {
                title(R.string.kau_exit)
                content(R.string.kau_exit_confirmation)
                positiveText(R.string.kau_yes)
                negativeText(R.string.kau_no)
                onPositive { _, _ -> super.onBackPressed() }
                checkBoxPromptRes(R.string.kau_do_not_show_again, false, { _, b -> Prefs.exitConfirmation = !b })
            }
        } else super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFrostTheme()
    }

}