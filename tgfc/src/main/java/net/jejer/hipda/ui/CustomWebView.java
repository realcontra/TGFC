package net.jejer.hipda.ui;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        Boolean result = listener.onPost(url, postData);
        if (result) {
            return;
        }
        super.postUrl(url, postData);
    }

    private OnPostListener listener;

    public void setOnPostListener(OnPostListener listener) {
        this.listener = listener;
    }

    interface OnPostListener {
        public Boolean onPost(String url, byte[] postData);
    }
}
