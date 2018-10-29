package com.panku.studiozxing;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public int type = 0;
    private String mData = "";
    private EditText editText1, editText2, edt_config_url;
    private Button btn_reset,btn_submit;
    private TextView tv_num ;
    private Map<String, Integer> map = new HashMap<>();
    private static final  String CONFIG_URL = "config_url";
    private String config_url_ip = "";

    private Dialog mLoadingDialog;
    private View mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText1 = (EditText) findViewById(R.id.edt_kuwei);
        editText2 = (EditText) findViewById(R.id.edt_code);
        btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_submit = (Button) findViewById(R.id.btn_submit);
        edt_config_url = (EditText) findViewById(R.id.edt_config_url);
        tv_num = findViewById(R.id.tv_text_num);
        findViewById(R.id.btn_kuwei).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBarCode(1);
            }
        });
        findViewById(R.id.btn_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBarCode(2);
            }
        });


        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }


        });

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });

        config_url_ip = getSp(this,CONFIG_URL);
        if (!"".equals(config_url_ip)){
            edt_config_url.setText(config_url_ip);
        }

        editText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mData = s != null ? s.toString(): "";
               String mdataStr = mData.replace("\n","");
                String[] strs = mdataStr.split(";");
                int num = mdataStr.split(";").length;
                Log.e("num","num:"+ num);
               tv_num.setText(getString(R.string.app_text_num,mdataStr.equals("") || !mdataStr.contains(";") ? "0" : num+""));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    /**
     *  清空数据
     */
    private void clearData() {
        map.clear();
        editText1.setText("");
        editText2.setText("");
        type = 0;
        mData = "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (scanResult != null) {
            String result = scanResult.getContents();

            if (result == null)
                return;
            Log.e("HYN", result);

            if (type == 2) {
                if (map.get(result) == null) {
                    map.put(result, 0);
                    mData += result + ";\n";
                    editText2.setText(mData);
                } else {
                    Toast.makeText(MainActivity.this, "存在重复条码", Toast.LENGTH_LONG).show();

                    return;
                }
                startBarCode(2);

            } else if (type == 1) {
                mData = "";
                editText1.setText(result);
            }

            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 进入到拍照页面
     *
     * @param sType
     */
    public void startBarCode(int sType) {
        type = sType;
        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
        // 设置要扫描的条码类型，ONE_D_CODE_TYPES：一维码，QR_CODE_TYPES-二维码
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        integrator.setCaptureActivity(BarcodeCameraActivity.class);
        integrator.setPrompt(sType == 1 ? "请扫库位条码" : "请扫产品条码"); //底部的提示文字，设为""可以置空
        integrator.setCameraId(0); //前置或者后置摄像头
        integrator.setBeepEnabled(true); //扫描成功的「哔哔」声，默认开启
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }


    /**
     * @param dtype
     */
    public void setType(int dtype) {
        type = dtype;
    }


    public void submit() {
        String kuweiStr = editText1.getText().toString();
        String codeStr = editText2.getText().toString();
        String config_url = edt_config_url.getText().toString();
        if ("".equals(config_url)) {
            Toast.makeText(MainActivity.this, "配置IP地址", Toast.LENGTH_SHORT).show();
            return;
        }
        // 保存url
        save(this,"config_url",config_url);
        String paramesStr = "position=" +kuweiStr +"&barCode="+codeStr;
         final Request request= new Request.Builder().url("http://"+config_url+":8080/q?"+ paramesStr).build();
        showLoading();
        getOkhttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity",e.toString());
                final String estr = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showdialog(estr);
                        hidLoading();
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body=response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ("OK".equals(body)){
                            clearData();
                        }
                        hidLoading();
                        showdialog(body);
                    }
                });
            }
        });
    }





    public void save(Context context,String key,String value){
        SharedPreferences sharedPreferences = getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String  getSp(Context context,String  key){
        SharedPreferences sharedPreferences = getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, "");
    }


    public void showdialog(String message){
        final AlertDialog.Builder
                normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("提示");
        normalDialog.setMessage(message);
        normalDialog.setCancelable(false);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface
                                                dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
        //显示
        normalDialog.show();
    }


    public OkHttpClient getOkhttpClient(){
        return new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20,TimeUnit.SECONDS)
                .writeTimeout(20,TimeUnit.SECONDS)
                .build();
    }

    public void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            TextView loadingText = (TextView) mLoadingView.findViewById(R.id.id_tv_loading_dialog_text);
//            loadingText.setText("加载中...");
//            loadingText.setVisibility(View.GONE);
            mLoadingDialog = new Dialog(this, R.style.CustomProgressDialog);
            mLoadingDialog.setCanceledOnTouchOutside(false);
            mLoadingDialog.setContentView(mLoadingView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.show();
        }
    }

    public void hidLoading(){
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
