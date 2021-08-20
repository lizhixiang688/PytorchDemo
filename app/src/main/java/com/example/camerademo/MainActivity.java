package com.example.camerademo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {

    public static final int CAMERA = 1000;
    public static final int ALBUM = 1001;
    private PopupWindow popupWindow;
    private View contentView;
    private ImageView iv;
    public TextView tv;
    private MyHandler myHandler=new MyHandler();
    private Bitmap bitmap = null;
    private Module module=null;
    private String path;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv=(ImageView)findViewById(R.id.imga_select);
        tv=(TextView)findViewById(R.id.text_result);

        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("正在识别中");
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        showPopwindow();

    }
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
        return dstbmp;
    }

    private void showPopwindow() {
        //加载弹出框的布局
        contentView = LayoutInflater.from(MainActivity.this).inflate(
                R.layout.pop, null);
        popupWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);// 取得焦点
        //注意  要是点击外部空白处弹框消息  那么必须给弹框设置一个背景色  不然是不起作用的
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        //点击外部消失
        popupWindow.setOutsideTouchable(true);
        //设置可以点击
        popupWindow.setTouchable(true);
        //进入退出的动画，指定刚才定义的style
        popupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);

        // 按下android回退物理键 PopipWindow消失解决
        contentView.findViewById(R.id.button_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!=
                        PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);
                }else {
                    openCamera();
                    popupWindow.dismiss();
                }

            }
        });
        contentView.findViewById(R.id.button_album).setOnClickListener(v -> {

            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
            }else {
                openAlbum();
                popupWindow.dismiss();
            }
        });
        contentView.findViewById(R.id.button_cancel).setOnClickListener(v -> popupWindow.dismiss());
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK){
            if(popupWindow!=null&&popupWindow.isShowing()){
                popupWindow.dismiss();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    public void openCamera(){
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(new File("/sdcard/test.jpg")));
        startActivityForResult(intent,CAMERA);

    }
    public void openAlbum(){
        Intent intent=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,ALBUM);

       /* Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,ALBUM);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==ALBUM){
            if(data!=null) {
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    if (path != null) {
                        iv.setImageBitmap(BitmapFactory.decodeFile(path));
                    }
                }
            }
        }
        if(requestCode==CAMERA){
            if(data!=null){
                Bitmap  bitmap1=data.getParcelableExtra("data");
                iv.setImageBitmap(bitmap1);
            }else {
                Bitmap bitmap2=BitmapFactory.decodeFile("/sdcard/test.jpg");
                iv.setImageBitmap(bitmap2);
                File file=new File("/sdcard/test.jpg");
                file.delete();
            }
        }
        if(((BitmapDrawable)iv.getDrawable()).getBitmap()==null){
            iv.setImageResource(R.drawable.icon_img);
        }
    }

    @Override
     public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        if(requestCode==1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
             openCamera();
            }else {
                Toast.makeText(this, "你可以在设置中更改权限", Toast.LENGTH_SHORT).show();
                //finish();
            }
        }
        if(requestCode==2){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
             openAlbum();
            }else {
                Toast.makeText(this, "你可以在设置中更改权限", Toast.LENGTH_SHORT).show();
                //finish();
            }
        }
    }
    public void start(View view) {
        popupWindow.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);
    }

    public void identify(View view) {

        progressDialog.show();
        if(!tv.getText().toString().isEmpty()){
            tv.setText("");
        }

        new Thread(
                ()->{
                    try {

                        // 1. 获取图片
                        bitmap=((BitmapDrawable)iv.getDrawable()).getBitmap();
                        bitmap=imageScale(bitmap,268,268);
                        // 2. 加载模型
                        module =Module.load(assetFilePath(this, "model_cat_dog.pt"));
                        // 3. bitmap -> Tensor
                        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                        // 4. 运行模型
                        Tensor resultTensor = module.forward(IValue.from(inputTensor)).toTensor();
                        String text=GetReusltFromTensor(resultTensor);

                        Message message=new Message();
                        message.obj=text;
                        myHandler.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                        finish();
                    }
                }
        ).start();

    }




    public  class MyHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            tv.setText((String)msg.obj);
             progressDialog.dismiss();
        }
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    public String GetReusltFromTensor(Tensor resultTensor){
        final float[] scores = resultTensor.getDataAsFloatArray();
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {    //这里是寻找可能性最大的值
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
        return CatDogClass.DATA_CLASSES[maxScoreIdx];
    }
}