package com.example.instgram;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import java.util.LinkedList;

// Reference: https://www.bianchengquan.com/article/10368.html
public class ContentImgListFragment extends Fragment {
    public static final String TAG = "contentImgSaver";
    private LinkedList<byte[]> byteArrList;
    private Boolean listSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.byteArrList = new LinkedList<byte[]>();
        this.listSet = false;
        setRetainInstance(true);
    }

    public LinkedList<byte[]> getData() {
        return byteArrList;
    }

    public void setData(byte[] bytes) {
        this.byteArrList.addFirst(bytes);
        this.listSet = true;
    }

    public boolean isDataExisted() {
        return listSet;
    }
}
