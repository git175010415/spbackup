package woyou.aidlservice.jiuiv5;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sunmi.printerservice.utils.C;
import com.tencent.stat.StatService;

import org.json.JSONException;
import org.json.JSONObject;

public class CutterActivity extends AppCompatActivity {
    ImageView mImageView1, mImageView2;
    LinearLayout mImageView3;
    SeekBar mSeekBar;
    TextView mTextView;
    Button mButton;
    int temp = 0;


    private IWoyouService sendservice;
    private ServiceConnection connService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            sendservice = IWoyouService.Stub.asInterface(service);
            if(sendservice != null){
                try {
                    int value = sendservice.getPrinterBBMDistance();
                    int set = (int) ((value*0.125 - 7.5)/0.5);
                    mSeekBar.setProgress(set);
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
        setContentView(R.layout.activity_blackline);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.t1_title_cutting);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initView();
        bindMyService();
    }

    private void bindMyService() {
        // TODO Auto-generated method stub
        bindService(new Intent(CutterActivity.this, WoyouService.class), connService, BIND_AUTO_CREATE);
    }

    private void initView() {
        // TODO Auto-generated method stub
        mImageView1 = (ImageView)findViewById(R.id.decrease);
        mImageView2 = (ImageView)findViewById(R.id.increase);
        mImageView3 = (LinearLayout)findViewById(R.id.black);
        mSeekBar = (SeekBar)findViewById(R.id.set_positon);
        mTextView = (TextView)findViewById(R.id.show_position);
        mButton = (Button)findViewById(R.id.update_position);
        mButton.setTextColor(Color.parseColor("#cccccc"));

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                StatService.trackCustomEvent(CutterActivity.this, "printer_settings_11");
                try {
                    int value = (int)((mSeekBar.getProgress()*0.5 + 7.5)/0.125);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("print_mode", 2);
                    jsonObject.put("cutter_location", value);
                    Intent intent = new Intent(C.INNER_ACTION);
                    Bundle bundle = new Bundle();
                    bundle.putString("set", jsonObject.toString());
                    intent.putExtras(bundle);
                    LocalBroadcastManager.getInstance(CutterActivity.this).sendBroadcast(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mImageView1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mButton.setEnabled(true);
                mButton.setTextColor(Color.parseColor("#ff3c00"));
                int progress = mSeekBar.getProgress() == 0?0:mSeekBar.getProgress() - 1;
                mSeekBar.setProgress(progress);
                mTextView.setText((0.5*progress - 10)+"mm");
                mImageView3.scrollBy(0, (temp -progress)*3);
                temp = progress;
            }
        });

        mImageView2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mButton.setEnabled(true);
                mButton.setTextColor(Color.parseColor("#ff3c00"));
                int progress = mSeekBar.getProgress() == 50?50:mSeekBar.getProgress() + 1;
                mSeekBar.setProgress(progress);
                mTextView.setText((0.5*progress - 10)+"mm");
                mImageView3.scrollBy(0, (progress - temp)*3);
                temp = progress;
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                mButton.setEnabled(true);
                mButton.setTextColor(Color.parseColor("#ff3c00"));
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                mTextView.setText((0.5*progress - 10)+"mm");
                mImageView3.scrollBy(0, 3*(temp - progress));
                temp = progress;
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
        unbindService(connService);
    }
}
