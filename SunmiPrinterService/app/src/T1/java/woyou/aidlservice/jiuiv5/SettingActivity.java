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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sunmi.printerservice.ui.widget.ListDialog;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.tencent.stat.StatService;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
	LinearLayout ll1, ll2, ll3;
	TextView tv ,tv1;

	private PreferencesLoader pl;
	private IWoyouService sendservice;
	private ServiceConnection connService = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			sendservice = IWoyouService.Stub.asInterface(service);
			if(sendservice != null){
				try {
					int mode;
					mode = sendservice.getPrinterMode();
					if(mode == 0){
						ll2.setVisibility(View.GONE);
						tv.setText(R.string.printer_mode_normal);
					}else{
						ll2.setVisibility(View.VISIBLE);
						tv.setText(R.string.printer_mode_bl);
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
			actionBar.setTitle(R.string.t1_title_printer);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	    initView();
	    bindMyService();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initDatatoView();
	}
	
	private void bindMyService() {
		// TODO Auto-generated method stub
		bindService(new Intent(SettingActivity.this, WoyouService.class), connService, BIND_AUTO_CREATE);
	}

	private void initView(){
	    ll1 = (LinearLayout)findViewById(R.id.setMode);
	    ll1.setOnClickListener(this);
	    ll2 = (LinearLayout)findViewById(R.id.setPosition);
	    ll2.setOnClickListener(this);
		ll3 = (LinearLayout)findViewById(R.id.setPaper);
		ll3.setOnClickListener(this);
	    tv = (TextView)findViewById(R.id.textview);
		tv1 = (TextView)findViewById(R.id.tv_paper);
	}

	private void initDatatoView(){
		pl = new PreferencesLoader(this, "settings");
		if(0 == (pl.getInt("mark")&0x2)){
			if(pl.getInt("local_paper") == 2){
				tv1.setText("58mm");
			}else{
				tv1.setText("80mm");
			}
		}else{
			if(pl.getInt("web_paper") == 2){
				tv1.setText("58mm");
			}else{
				tv1.setText("80mm");
			}
		}
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
			clearSetting();
			return true;
		}else if(id == android.R.id.home){
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 清除配置
	 *  mark说明： 初值（clear值）-1 清除配置
	 *  		黑标设置 低1位：0 设置 1未设置
	 *  		纸张设置 低2位：0设置 1未设置
	 *  		扩展
	 */
	private void clearSetting() {
		// TODO Auto-generated method stub
		try {
			boolean flag = pl.getBoolean("bbm_flag");
			int value = pl.getInt("bbm_value");
			pl.saveInt("mark", -1);
			if(!flag){
				ll2.setVisibility(View.GONE);
				tv.setText(R.string.printer_mode_normal);
			}else{
				ll2.setVisibility(View.VISIBLE);
				tv.setText(R.string.printer_mode_bl);
			}
			if(value == -1){
				value = 140;
			}
			if(pl.getInt("web_paper") == 2){
				tv1.setText("58mm");
			}else{
				tv1.setText("80mm");
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("print_mode", flag?2:1);
			jsonObject.put("cutter_location", value);
			jsonObject.put("local_paper", pl.getInt("web_paper"));
			Intent intent = new Intent(C.INNER_ACTION);
			Bundle bundle = new Bundle();
			bundle.putString("set", jsonObject.toString());
			intent.putExtras(bundle);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId() == R.id.setMode){
			setMode();
		}else if(v.getId() == R.id.setPosition){
			setPosition();
		}else if(v.getId() == R.id.setPaper){
			setPaper();
		}
	}

	//跳转到设置打印纸规格页面
	private void setPaper() {
		// TODO Auto-generated method stub
		startActivity(new Intent(this, PaperActivity.class));
	}

	/**
	 * 跳转到黑标模式下设置切刀位置页面
	 */
	private void setPosition() {
		// TODO Auto-generated method stub
		startActivity(new Intent(this, CutterActivity.class));
	}

	/**
	 * 切换打印机普通、黑标模式
	 * 注意mark记录是否操作过
	 */
	private void setMode() {
		// TODO Auto-generated method stub
		int mark = pl.getInt("mark");
		pl.saveInt("mark", mark&(~0x01));
		final ListDialog listDialog = new ListDialog(this, getResources().getString(R.string.printer_mode_select), new String[]{getResources().getString(R.string.printer_mode_normal),getResources().getString(R.string.printer_mode_bl)});
		listDialog.setItemClickListener(new ListDialog.ItemClickListener() {
			
			@Override
			public void OnItemClick(int position) {
				// TODO Auto-generated method stub
				StatService.trackCustomEvent(SettingActivity.this, "printer_settings_10");
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("print_mode", position+1);
					jsonObject.put("cutter_location", -1);
					Intent intent = new Intent(C.INNER_ACTION);
					Bundle bundle = new Bundle();
					bundle.putString("set", jsonObject.toString());
					intent.putExtras(bundle);
					LocalBroadcastManager.getInstance(SettingActivity.this).sendBroadcast(intent);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				if(position == 0){
					ll2.setVisibility(View.GONE);
					tv.setText(R.string.printer_mode_normal);
				}else{
					ll2.setVisibility(View.VISIBLE);
					tv.setText(R.string.printer_mode_bl);
				}
				
				listDialog.cancel();
			}
		});
		listDialog.show();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unbindService(connService);
	}

}
