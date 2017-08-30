package com.example.davidchen.blogdemo.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.davidchen.blogdemo.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * 选择照片
 * Created by DavidChen on 2017/8/25.
 */

public class ChoosePhotoActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int CHOOSE_PHOTO = 2;
    private static final String TAG = "ChoosePhotoActivity";

    ImageView result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photo);
        result = (ImageView) findViewById(R.id.result);
        Handler handler = new Handler() {
            WeakReference<Activity> activityWeakReference;
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0x1:
                        // 更新UI
                        break;
                }
            }
        };
        Message message = handler.obtainMessage();
        message.what = 0x1;
        handler.sendMessage(handler.obtainMessage());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               // to do something
            }
        }, 3000);
    }

    /**
     * 请求读写SDK权限
     */
    public void choosePhoto(View view) {
        if (ContextCompat.checkSelfPermission(ChoosePhotoActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChoosePhotoActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            openAlbum();
        }
    }

    /**
     * 打开图片浏览器
     */
    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "拒绝读取SD卡权限将无法获取照片", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        handleImageAfterKitKat(data);
                    } else {
                        handleImage(data);
                    }
                }
                break;
        }
    }

    /**
     * API19之后处理内容
     *
     * @param data onActivityResult 返回结果
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleImageAfterKitKat(Intent data) {
        /*ParcelFileDescriptor parcelFileDescriptor =
                null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(data.getData(), "r");
            Log.i(TAG, "handleImageAfterKitKat: " + parcelFileDescriptor.toString());
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            result.setImageBitmap(image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，通过documentId处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];    // 解析出数字格式id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    /**
     * 处理返回结果
     *
     * @param data onActivityResult 返回结果
     */
    private void handleImage(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    /**
     * 根据图片路径，显示图片
     */
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            result.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "照片路径为空", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 使用ContentProvider获取图片路径
     *
     * @param uri       图片uri
     * @param selection 条件
     * @return 图片路径
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private static class MyHandler extends Handler {
        WeakReference<Activity> mActivityWeakReference;
        public MyHandler(Activity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            Activity activity = mActivityWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case 0x1:
                        // 更新UI
                        break;
                }
            }
        }
    };
}
