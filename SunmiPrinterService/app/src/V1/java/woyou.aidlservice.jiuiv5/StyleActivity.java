package woyou.aidlservice.jiuiv5;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.sunmi.printerservice.entity.GlobalStyle;
import com.sunmi.printerservice.ui.widget.ButtonSwitch;
import com.sunmi.printerservice.ui.widget.EditTextDialog;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.tencent.stat.StatService;

public class StyleActivity extends AppCompatActivity implements View.OnClickListener{
    ButtonSwitch buttonSwitch1,buttonSwitch2,buttonSwitch3,buttonSwitch4,buttonSwitch5,buttonSwitch6;
    LinearLayout ll1, ll2, ll3, ll4, ll5, ll6, ll7;
    TextView textview;
    GlobalStyle globalStyle;
    EditTextDialog ed;
    PreferencesLoader pl;

    private void set(){
        Gson gs = new Gson();
        String rs = gs.toJson(globalStyle);
        pl.saveString("local_style", rs);
        int mark = pl.getInt("mark");
        if(0 != (pl.getInt("mark")&0x04)){
            pl.saveInt("mark", mark&(~0x04));
        }
        Intent intent = new Intent(C.INNER_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString("set", rs);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.title_style);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ll1 = (LinearLayout) findViewById(R.id.ll_height);
        ll2 = (LinearLayout) findViewById(R.id.ll_width);
        ll3 = (LinearLayout) findViewById(R.id.ll_weight);
        ll4 = (LinearLayout) findViewById(R.id.ll_inverse);
        ll5 = (LinearLayout) findViewById(R.id.ll_underline);
        ll6 = (LinearLayout) findViewById(R.id.ll_lineheight);
        ll7 = (LinearLayout) findViewById(R.id.ll_setlineheight);

        buttonSwitch1 = (ButtonSwitch)findViewById(R.id.style_height);
        buttonSwitch2 = (ButtonSwitch)findViewById(R.id.style_width);
        buttonSwitch3 = (ButtonSwitch)findViewById(R.id.style_weight);
        buttonSwitch4 = (ButtonSwitch)findViewById(R.id.style_inverse);
        buttonSwitch5 = (ButtonSwitch)findViewById(R.id.style_underline);
        buttonSwitch6 = (ButtonSwitch) findViewById(R.id.style_lineheight);
        textview = (TextView)findViewById(R.id.text_setlineheight);

        ll1.setOnClickListener(this);
        ll2.setOnClickListener(this);
        ll3.setOnClickListener(this);
        ll4.setOnClickListener(this);
        ll5.setOnClickListener(this);
        ll6.setOnClickListener(this);
        ll7.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initStyle();
        initView();
    }

    private void initStyle() {
        pl = new PreferencesLoader(this, "settings");
        Gson gs = new Gson();
        if(0 == (pl.getInt("mark")&0x04)){
            String string = pl.getString("local_style");
            globalStyle = gs.fromJson(string, GlobalStyle.class);
        }else{
            String string = pl.getString("web_style");
            globalStyle = gs.fromJson(string, GlobalStyle.class);
        }
        if(globalStyle == null){
            globalStyle = new GlobalStyle();
        }
    }

    private void initView() {
        // TODO Auto-generated method stub
        buttonSwitch1.setOnCheck(globalStyle.getFont_height() != 0);
        buttonSwitch2.setOnCheck((globalStyle.getFont_width() != 0));
        buttonSwitch3.setOnCheck((globalStyle.getFont_weight() != 0));
        buttonSwitch4.setOnCheck(globalStyle.getInverse_white() != 0);
        buttonSwitch5.setOnCheck(globalStyle.getUnderline() != 0);
        buttonSwitch6.setOnCheck(globalStyle.getRow_height() != 0);
        if(globalStyle.getRow_height() != 0){
            ll7.setVisibility(View.VISIBLE);
            textview.setText((int)globalStyle.getRow_space() + "");
        }
        buttonSwitch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_2");
                globalStyle.setFont_height(isChecked?1:0);
                set();
            }
        });
        buttonSwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_3");
                globalStyle.setFont_width(isChecked?1:0);
                set();
            }
        });
        buttonSwitch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_4");
                globalStyle.setFont_weight(isChecked?1:0);
                set();
            }
        });
        buttonSwitch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_5");
                globalStyle.setInverse_white(isChecked?1:0);
                set();
            }
        });
        buttonSwitch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_6");
                globalStyle.setUnderline(isChecked?1:0);
                set();
            }
        });
        buttonSwitch6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                StatService.trackCustomEvent(StyleActivity.this, "printer_settings_7");
                globalStyle.setRow_height(isChecked?1:0);
                if(isChecked){
                    ll7.setVisibility(View.VISIBLE);
                    textview.setText((int)globalStyle.getRow_space() + "");
                }else{
                    ll7.setVisibility(View.GONE);
                }
                set();
            }
        });
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


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    private void setLineHeight() {
        ed = new EditTextDialog(StyleActivity.this, getResources().getString(R.string.action_cancel), getResources().getString(R.string.action_confirm), getResources().getString(R.string.action_set_line), new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ed.cancel();
            }
        }, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int height;
                if(ed.getText() != null && !ed.getText().equals("")){
                    height = Integer.parseInt(ed.getText());
                }else{
                    height = -1;
                }

                if(height < 0 || height > 255){
                    ed.showAlert(true);
                }else{
                    StatService.trackCustomEvent(StyleActivity.this, "printer_settings_8");
                    globalStyle.setRow_space(height);
                    textview.setText(height+"");
                    set();
                    ed.cancel();
                }
            }
        }, null);

        ed.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_height:
                buttonSwitch1.setOnCheck(!buttonSwitch1.isChecked());
                break;
            case R.id.ll_width:
                buttonSwitch2.setOnCheck(!buttonSwitch2.isChecked());
                break;
            case R.id.ll_weight:
                buttonSwitch3.setOnCheck(!buttonSwitch3.isChecked());
                break;
            case R.id.ll_inverse:
                buttonSwitch4.setOnCheck(!buttonSwitch4.isChecked());
                break;
            case R.id.ll_underline:
                buttonSwitch5.setOnCheck(!buttonSwitch5.isChecked());
                break;
            case R.id.ll_lineheight:
                buttonSwitch6.setOnCheck(!buttonSwitch6.isChecked());
                break;
            case R.id.ll_setlineheight:
                setLineHeight();
                break;
            default:
                break;
        }
    }
}
