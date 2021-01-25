package com.example.instgram;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

// Reference: https://www.bianchengquan.com/article/10368.html
public class BitmapDataFragment extends Fragment {
    public static final String TAG = "bitmapSaver";
    public static final String EXISTED = "bitmapFragExisted";
    private Bitmap bitmap;

//    private BitmapDataFragment(Bitmap bitmap) {
//        this.bitmap = bitmap;
//    }
//
//    public static BitmapDataFragment newInstance(Bitmap bitmap) {
//        return new BitmapDataFragment(bitmap);
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public Bitmap getData() {
        return bitmap;
    }

    public void setData(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
