package com.example.instgram;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

// Reference: https://www.bianchengquan.com/article/10368.html
public class BitmapDataFragment extends Fragment {
    public static final String TAG = "bitmapSaver";
    private Bitmap bitmap;
    private Boolean bitmapSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.bitmapSet = false;
        setRetainInstance(true);
    }

    public Bitmap getData() {
        return bitmap;
    }

    public void setData(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.bitmapSet = true;
    }

    public boolean isDataExisted() {
        return bitmapSet;
    }
}
