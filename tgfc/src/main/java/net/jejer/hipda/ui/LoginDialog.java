package net.jejer.hipda.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.jejer.hipda.R;
import net.jejer.hipda.async.FavoriteHelper;
import net.jejer.hipda.async.LoginHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.utils.Constants;

/**
 * dialog for login
 * Created by GreenSkinMonster on 2015-04-18.
 */
public class LoginDialog extends Dialog {

    private static boolean isShown = false;

    private Context mCtx;
    private HiProgressDialog progressDialog;
    private Handler mHandler;
    private WebView webView;

    private LoginDialog(Context context) {
        super(context);
        mCtx = context;
    }

    public static LoginDialog getInstance(Context context) {
        if (!isShown) {
            isShown = true;
            return new LoginDialog(context);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_login, null);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        DisplayMetrics displayMetrics = this.getContext().getResources().getDisplayMetrics();
        attributes.width = displayMetrics.widthPixels;
        getWindow().setAttributes(attributes);
        final EditText etUsername = (EditText) view.findViewById(R.id.login_username);
        final EditText etPassword = (EditText) view.findViewById(R.id.login_password);
        final Button btnVerify = (Button) view.findViewById(R.id.verify_btn);
        final Spinner spSecQuestion = (Spinner) view.findViewById(R.id.login_question);
        final EditText etSecAnswer = (EditText) view.findViewById(R.id.login_answer);
        webView = view.findViewById(R.id.wv);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                if (url.contains("user")) {
                    // 1秒后获取
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.loadUrl("javascript:android.getCapchaToken(grecaptcha.getResponse());");
                        }
                    }, 1000);
                }
                return super.shouldInterceptRequest(view, url);
            }
        });

        final KeyValueArrayAdapter adapter = new KeyValueArrayAdapter(mCtx, R.layout.spinner_row);
        adapter.setEntryValues(mCtx.getResources().getStringArray(R.array.pref_login_question_list_values));
        adapter.setEntries(mCtx.getResources().getStringArray(R.array.pref_login_question_list_titles));
        spSecQuestion.setAdapter(adapter);

        etUsername.setText(HiSettingsHelper.getInstance().getUsername());
        etPassword.setText(HiSettingsHelper.getInstance().getPassword());

        if (!TextUtils.isEmpty(HiSettingsHelper.getInstance().getSecQuestion())
                && TextUtils.isDigitsOnly(HiSettingsHelper.getInstance().getSecQuestion())) {
            int idx = Integer.parseInt(HiSettingsHelper.getInstance().getSecQuestion());
            if (idx > 0 && idx < adapter.getCount())
                spSecQuestion.setSelection(idx);
        }
        etSecAnswer.setText(HiSettingsHelper.getInstance().getSecAnswer());

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl("https://wap.tgfcer.com/index.php?action=login");
                webView.addJavascriptInterface(LoginDialog.this, "android");
            }
        });

        Button btnLogin = (Button) view.findViewById(R.id.login_btn);
        btnLogin.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {

                InputMethodManager imm = (InputMethodManager) mCtx.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etPassword.getWindowToken(), 0);
                if (HiSettingsHelper.getInstance().getReCaptchaToken().isEmpty()) {
                    Toast.makeText(v.getContext(), "请先进行 Google 验证", Toast.LENGTH_SHORT).show();
                    return;
                }
                HiSettingsHelper.getInstance().setUsername(etUsername.getText().toString());
                HiSettingsHelper.getInstance().setPassword(etPassword.getText().toString());
                HiSettingsHelper.getInstance().setSecQuestion(adapter.getEntryValue(spSecQuestion.getSelectedItemPosition()));
                HiSettingsHelper.getInstance().setSecAnswer(etSecAnswer.getText().toString());
                HiSettingsHelper.getInstance().setUid("");

                progressDialog = HiProgressDialog.show(mCtx, "正在登录...");

                final LoginHelper loginHelper = new LoginHelper(mCtx, null);

                new AsyncTask<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        return loginHelper.login();
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        if (result == Constants.STATUS_SUCCESS) {
                            Toast.makeText(mCtx, "登录成功", Toast.LENGTH_SHORT).show();
                            dismiss();
                            isShown = false;
                            if (mHandler != null) {
                                Message msg = Message.obtain();
                                msg.what = ThreadListFragment.STAGE_REFRESH;
                                mHandler.sendMessage(msg);
                            }
                            FavoriteHelper.getInstance().updateCache();
                        } else {
                            Toast.makeText(mCtx, loginHelper.getErrorMsg(), Toast.LENGTH_SHORT).show();
                            if (result == Constants.STATUS_SECCODE_FAIL_ABORT) {
                            } else {
                                HiSettingsHelper.getInstance().setUsername("");
                                HiSettingsHelper.getInstance().setPassword("");
                                HiSettingsHelper.getInstance().setSecQuestion("");
                                HiSettingsHelper.getInstance().setSecAnswer("");
                            }
                            if (mHandler != null) {
                                Message msg = Message.obtain();
                                msg.what = ThreadListFragment.STAGE_ERROR;
                                Bundle b = new Bundle();
                                b.putString(ThreadListFragment.STAGE_ERROR_KEY, loginHelper.getErrorMsg());
                                msg.setData(b);
                                mHandler.sendMessage(msg);
                            }
                        }
                        progressDialog.dismiss();
                    }
                }.execute();
            }
        });

        setContentView(view);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isShown = false;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @JavascriptInterface
    public void getCapchaToken(String token) {
        if (token.isEmpty()) {
            return;
        }
        HiSettingsHelper.getInstance().setReCaptchaToken(token);
        Toast.makeText(getContext(), "设置验证码成功，验证码过期时间较短，请及时登陆", Toast.LENGTH_SHORT).show();
        webView.setVisibility(View.GONE);
    }
}

