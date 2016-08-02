package com.example.miker_000.breadcrumms;

import android.app.Dialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by miker_000 on 8/2/2016.
 */
public class PrivacyPolicyDialogPreference extends DialogPreference {
    public PrivacyPolicyDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
        setDialogLayoutResource(R.layout.privacy_policy_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView privacyPolicyTextview = (TextView) view.findViewById(R.id.privacyPolicyTextView);
        Spanned sp = Html.fromHtml(getContext().getString(R.string.privacy_policy));
        privacyPolicyTextview.setText(sp);
        privacyPolicyTextview.setMovementMethod(new ScrollingMovementMethod());
    }
}
