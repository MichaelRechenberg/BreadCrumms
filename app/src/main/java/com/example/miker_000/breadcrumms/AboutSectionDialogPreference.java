package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Displays the HTML of the About Section of the app within MainActivity's Settings
 */
public class AboutSectionDialogPreference extends DialogPreference {
    public AboutSectionDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
        //Remove the negative button
        setNegativeButtonText(null);
        setDialogLayoutResource(R.layout.display_html_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView aboutSectionTextView = (TextView) view.findViewById(R.id.htmlTextView);
        Spanned sp = Html.fromHtml(getContext().getString(R.string.aboutSection_text));
        aboutSectionTextView.setText(sp);
        aboutSectionTextView.setMovementMethod(new ScrollingMovementMethod());

    }
}