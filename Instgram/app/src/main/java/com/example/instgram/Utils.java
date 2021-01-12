package com.example.instgram;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

public class Utils {
    public String processEmailString(String email) {
        return email.replaceAll("\\.", "#d*o*t#").toLowerCase();
    }

    // Reference: https://blog.csdn.net/m0_37358427/article/details/83012857
    public Bitmap toRoundBitMap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int mRadius = Math.min(height, width) / 2;
        float mScale = (mRadius * 2.0f) / Math.min(height, width);
        BitmapShader bitmapShader = new BitmapShader(bitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        Paint mPaint = new Paint();
        mPaint.setAntiAlias(true);
        Bitmap backgroundBitMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(backgroundBitMap);
        matrix.setScale(mScale, mScale);
        bitmapShader.setLocalMatrix(matrix);
        mPaint.setShader(bitmapShader);
        canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);
        return backgroundBitMap;
    }
}
