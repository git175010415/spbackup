package woyou.aidlservice.jiuiv5;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.gson.Gson;
import com.sunmi.printerservice.entity.GlobalStyle;
import com.sunmi.printerservice.ui.widget.ListDialog;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.tencent.stat.StatService;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingActivity extends AppCompatActivity {

    private int[] darkness = new int[]{0x0600, 0x0500, 0x0400, 0x0300, 0x0200, 0x0100, 0, 0xffff, 0xfeff, 0xfdff, 0xfcff, 0xfbff, 0xfaff};
    private String[] concentration = new String[]{"130%", "125%","120%","115%","110%","105%","100%","95%","90%","85%","80%","75%","70%"};
    private IWoyouService sendservice;
    private ServiceConnection connService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            sendservice = IWoyouService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            sendservice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.title_printer);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        bindMyService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    private void bindMyService() {
        // TODO Auto-generated method stub
        bindService(new Intent(this, WoyouService.class), connService, BIND_AUTO_CREATE);
    }

    private void initView() {
        PreferencesLoader pl = new PreferencesLoader(SettingActivity.this, "settings");
        if(pl.getInt("darkness") != -1){
            ((TextView)findViewById(R.id.textview)).setText(concentration[pl.getInt("darkness") ]);
        }
        if(pl.getBoolean("fontEnable")){
            ((TextView)findViewById(R.id.setFontStatus)).setText(R.string.action_on);
        }else{
            ((TextView)findViewById(R.id.setFontStatus)).setText(R.string.action_off);
        }

        // TODO Auto-generated method stub
        findViewById(R.id.setDarkness).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                final ListDialog listDialog = new ListDialog(SettingActivity.this, getResources().getString(R.string.printer_darkness_select), concentration);
                listDialog.setItemClickListener(new ListDialog.ItemClickListener() {

                    @Override
                    public void OnItemClick(int position) {
                        // TODO Auto-generated method stub
                        ((TextView)findViewById(R.id.textview)).setText(concentration[position]);
                        try {
                            sendservice.sendRAWData(setPrinterDarkness(darkness[position]), null);
                            PreferencesLoader pl = new PreferencesLoader(SettingActivity.this, "settings");
                            pl.saveInt("darkness", position);
                            StatService.trackCustomEvent(SettingActivity.this, "printer_settings_1");
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        listDialog.cancel();
                    }
                });
                listDialog.show();
            }
        });


        findViewById(R.id.setStyle).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                startActivity(new Intent(SettingActivity.this, StyleActivity.class));
            }
        });

        findViewById(R.id.setFont).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                startActivity(new Intent(SettingActivity.this, FontActivity.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            clear();
            return true;
        }else if(id == R.id.action_printing){
            printing();
            return true;
        }else if(id == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //打印浓度指令
    private static byte[] setPrinterDarkness(int value){
        byte[] result = new byte[9];
        result[0] = 0x1D;
        result[1] = 40;
        result[2] = 69;
        result[3] = 4;
        result[4] = 0;
        result[5] = 5;
        result[6] = 5;
        result[7] = (byte) (value >> 8);
        result[8] = (byte) value;
        return result;
    }

    private void printing() {
        // TODO Auto-generated method stub
        if(sendservice == null){
            return;
        }

        try {
            sendservice.printerInit(null);
            sendservice.setAlignment(1, null);
            sendservice.printTextWithFont(getResources().getString(R.string.printer_sample), null, 32, null);
            sendservice.setAlignment(0, null);
            sendservice.printText("********************************\n", null);
            sendservice.printText("修改打印设置中的选项会影响最终打印机的输出\n", null);
            sendservice.printText("此用例向用户展示打印设置的选项在实际打印纸上的输出效果\n", null);
            sendservice.printText("Modifying the options in the print Settings will affect the output of the final printer This use case shows the user the option to print the Settings on the actual printer The output effect\n", null);
            sendservice.printText("********************************\n", null);
            sendservice.lineWrap(3, null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void clear() {
        // TODO Auto-generated method stub
        Gson gs = new Gson();
        GlobalStyle globalStyle;
        PreferencesLoader pl = new PreferencesLoader(SettingActivity.this, "settings");
        pl.saveInt("mark", -1);
        pl.saveString("local_style", "");
        pl.saveBoolean("fontEnable", false);
        pl.saveBoolean("fontDefault", false);
        String string = pl.getString("web_style");
        if(!string.equals("")){
            globalStyle = gs.fromJson(string, GlobalStyle.class);
        }else{
            globalStyle = new GlobalStyle();
        }
        string = gs.toJson(globalStyle);
        Intent intent = new Intent(C.INNER_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString("set", string);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fontDefault", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bundle.putString("set", jsonObject.toString());
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unbindService(connService);
    }
}
