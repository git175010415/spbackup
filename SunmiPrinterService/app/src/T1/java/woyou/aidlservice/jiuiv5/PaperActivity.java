package woyou.aidlservice.jiuiv5;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.tencent.stat.StatService;

public class PaperActivity extends AppCompatActivity{
    FrameLayout fl1, fl2;
    RadioButton rb1,rb2;
    PreferencesLoader pl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.t1_title_paper);
        actionBar.setDisplayHomeAsUpEnabled(true);
        initView();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        initDataToView();
    }

    private void initDataToView() {
        // TODO Auto-generated method stub
        pl = new PreferencesLoader(this, "settings");
        if(0 == (pl.getInt("mark")&0x2)){
            if(pl.getInt("local_paper") == 2){
                rb2.setChecked(true);
            }else{
                rb1.setChecked(true);
            }
        }else{
            if(pl.getInt("web_paper") == 2){
                rb2.setChecked(true);
            }else{
                rb1.setChecked(true);
            }
        }
    }

    private void initView() {
        // TODO Auto-generated method stub
        fl1 = (FrameLayout) findViewById(R.id.fl_paper_one);
        fl2 = (FrameLayout) findViewById(R.id.fl_paper_two);
        rb1 = (RadioButton) findViewById(R.id.rb_paper_one);
        rb2 = (RadioButton) findViewById(R.id.rb_paper_two);
        ((RadioGroup)findViewById(R.id.rg_paper)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                switch (checkedId) {
                    case R.id.rb_paper_one:
                        fl1.setVisibility(View.VISIBLE);
                        fl2.setVisibility(View.GONE);
                        setPaper(1);
                        break;
                    case R.id.rb_paper_two:
                        fl1.setVisibility(View.GONE);
                        fl2.setVisibility(View.VISIBLE);
                        setPaper(2);
                        break;
                    default:
                        fl1.setVisibility(View.GONE);
                        fl2.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }

    private void setPaper(int print_spec) {
        // TODO Auto-generated method stub
        StatService.trackCustomEvent(PaperActivity.this, "printer_settings_9");
        int mark = pl.getInt("mark");
        pl.saveInt("mark", mark&(~0x02));
        pl.saveInt("local_paper", print_spec);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("local_paper", print_spec);
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
