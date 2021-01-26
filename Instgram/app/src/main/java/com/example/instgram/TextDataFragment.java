package com.example.instgram;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import java.util.HashMap;
import java.util.Map;

// Reference: https://www.bianchengquan.com/article/10368.html
public class TextDataFragment extends Fragment {
    public static final String TAG = "textSaver";
    private Map<String, String> textMap;
    private Boolean textSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.textMap = new HashMap<String, String>();
        this.textSet = false;
        setRetainInstance(true);
    }

    public Map<String, String> getData() {
        return textMap;
    }

    public void setData(String k, String v) {
        this.textMap.put(k, v);
        this.textSet = true;
    }

    public boolean isDataExisted() {
        return textSet;
    }
}
