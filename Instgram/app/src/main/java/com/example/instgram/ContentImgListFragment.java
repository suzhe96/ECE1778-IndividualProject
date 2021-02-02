package com.example.instgram;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

// Reference: https://www.bianchengquan.com/article/10368.html
public class ContentImgListFragment extends Fragment {
    public static final String TAG = "contentImgSaver";
    private LinkedList<byte[]> byteArrList;
    private ArrayList<Long> timestampArrList;
    private Boolean listSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.byteArrList = new LinkedList<byte[]>();
        this.timestampArrList = new ArrayList<Long>();
        this.listSet = false;
        setRetainInstance(true);
    }

    public LinkedList<byte[]> getData() {
        return byteArrList;
    }

    public ArrayList<Long> getTimestampData() {
        return timestampArrList;
    }

    public void setData(byte[] bytes) {
        this.byteArrList.addLast(bytes);
        this.listSet = true;
    }

    public void setTimestampData(Long timestamp) {
        this.timestampArrList.add(timestamp);
    }

    public boolean isDataExisted() {
        return listSet;
    }
}
