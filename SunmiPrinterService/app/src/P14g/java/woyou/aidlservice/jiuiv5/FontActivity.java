package woyou.aidlservice.jiuiv5;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sunmi.printerservice.ui.widget.ButtonSwitch;
import com.sunmi.printerservice.utils.Adaptation;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.PreferencesLoader;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Author : kaltin
 * Create : 2018/3/22 15:53
 * Describe :
 */

public class FontActivity extends AppCompatActivity {

    ButtonSwitch bs;
    RadioButton rb1, rb2;
    TextView tv_abc, tv_show1, tv_show2, tv_function;
    PreferencesLoader pl;
    LinearLayout ll;
    boolean isFontEnable;
    boolean isDefault;
    Dialog mDialog;
    Typeface mtypeface_old;
    Typeface mtypeface_new;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.title_font);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initData();
        initView();
    }

    private void initData() {
        pl = new PreferencesLoader(this, "settings");
        isFontEnable = pl.getBoolean("fontEnable");
        isDefault = pl.getBoolean("fontDefault");
        mtypeface_old = Typeface.createFromAsset(getAssets(),"gh.ttf");
        mtypeface_new = Typeface.createFromAsset(getAssets(),"kaltin.ttf");
    }

    private void initView() {
        bs = (ButtonSwitch) findViewById(R.id.bs_font_set);
        rb1 = (RadioButton) findViewById(R.id.rb_font_1);
        rb2 = (RadioButton) findViewById(R.id.rb_font_2);
        tv_show1 = (TextView) findViewById(R.id.font_explain_show1);
        tv_show2 = (TextView) findViewById(R.id.font_explain_show2);
        tv_function = (TextView) findViewById(R.id.font_explain);
        tv_abc = (TextView) findViewById(R.id.font_abc);
        ll = (LinearLayout) findViewById(R.id.font_show);

        if(isFontEnable){
            rb1.setChecked(!isDefault);
            rb2.setChecked(isDefault);
            bs.setOnCheck(true);
            ll.setVisibility(View.VISIBLE);
            if(isDefault){
                tv_abc.setTypeface(mtypeface_old);
            }else{
                tv_abc.setTypeface(mtypeface_new);
            }
            tv_abc.setText(R.string.text_char_preview);
        }
        findViewById(R.id.font_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bs.setOnCheck(!bs.isChecked());
            }
        });

        bs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    makeDialog();
                }else{
                    ll.setVisibility(View.GONE);
                    pl.saveBoolean("fontEnable", false);
                }
            }
        });

        findViewById(R.id.font_select_1).setOnClickListener(select);
        findViewById(R.id.font_select_2).setOnClickListener(select);
        tv_function.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tv_show1.getVisibility() == View.VISIBLE){
                    tv_show1.setVisibility(View.GONE);
                    tv_show2.setVisibility(View.GONE);
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_down,null);
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    tv_function.setCompoundDrawables(null, null, drawable, null);
                }else{
                    tv_show1.setVisibility(View.VISIBLE);
                    tv_show2.setVisibility(View.VISIBLE);
                    if(isDefault){
                        tv_show2.setText(R.string.text_char_explain1);
                    }else{
                        tv_show2.setText(R.string.text_char_explain2);
                    }
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow_up,null);
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    tv_function.setCompoundDrawables(null, null,  drawable, null);
                }
            }
        });
    }


    View.OnClickListener enter = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.textView_btn2){
                ll.setVisibility(bs.isChecked()?View.VISIBLE:View.INVISIBLE);
                pl.saveBoolean("fontEnable", true);
            }else{
                bs.setOnCheck(false);
            }
            mDialog.cancel();
        }
    };

    View.OnClickListener select = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean now = (v.getId() == R.id.font_select_1);
            rb1.setChecked(now);
            rb2.setChecked(!now);
            if(!now){
                tv_abc.setTypeface(mtypeface_old);
                tv_abc.setText(R.string.text_char_preview);
                tv_show2.setText(R.string.text_char_explain1);
            }else{
                tv_abc.setTypeface(mtypeface_new);
                tv_abc.setText(R.string.text_char_preview);
                tv_show2.setText(R.string.text_char_explain2);
            }
            setFont(now);
        }
    };

    private void makeDialog(){
        if(mDialog == null){
            mDialog = new Dialog(this, R.style.defaultDialogStyle);
            if (Adaptation.proportion == Adaptation.SCREEN_4_3 || Adaptation.proportion == Adaptation.SCREEN_16_9) {
                mDialog.setContentView(R.layout.t1_dialog_one_button);
            } else {
                mDialog.setContentView(R.layout.v1_dialog_one_button);
            }
            mDialog.setCancelable(false);
            ImageView imgIcon = (ImageView) mDialog.findViewById(R.id.imageView_icon);
            imgIcon.setBackgroundResource(R.drawable.v5_icon_hot);
            ImageView imgVoice = (ImageView) mDialog.findViewById(R.id.img_voice);
            imgVoice.setVisibility(View.GONE);
            TextView txtMsg = (TextView) mDialog.findViewById(R.id.textView_msg);
            txtMsg.setText(R.string.text_char_dialog);
            TextView bt1 = (TextView) mDialog.findViewById(R.id.textView_btn);
            bt1.setClickable(true);
            bt1.setText(R.string.action_abandon);
            bt1.setOnClickListener(enter);
            mDialog.findViewById(R.id.textView_cut).setVisibility(View.VISIBLE);
            TextView bt2 = (TextView) mDialog.findViewById(R.id.textView_btn2);
            bt2.setVisibility(View.VISIBLE);
            bt2.setClickable(true);
            bt2.setText(R.string.action_continue);
            bt2.setOnClickListener(enter);
        }
        mDialog.show();
    }

    //rb1是新字体 rb2是老字体 所以 true为新字体
    private void setFont(boolean now) {
        isDefault = !now;
        pl.saveBoolean("fontEnable", true);
        pl.saveBoolean("fontDefault", !now);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fontDefault", !now);
            Intent intent = new Intent(C.INNER_ACTION);
            Bundle bundle = new Bundle();
            bundle.putString("set", jsonObject.toString());
            intent.putExtras(bundle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
