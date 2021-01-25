package com.example.instgram;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;

import java.io.ByteArrayOutputStream;

public class Utils {
    private static final String LOG_TAG = Utils.class.getSimpleName();


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
//        return Bitmap.createScaledBitmap(backgroundBitMap,
//                150, 150, false);
        return backgroundBitMap;
    }

    public Bitmap cropProfileBitmap(Bitmap bitmap, Boolean recycled) {
        int minLength = Math.min(bitmap.getWidth(), bitmap.getHeight());
        if (recycled) {
            return ThumbnailUtils.extractThumbnail(bitmap, minLength, minLength,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return ThumbnailUtils.extractThumbnail(bitmap, minLength, minLength);
    }

    public byte[] compressBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
        return blob.toByteArray();
    }

    // Reference: https://stackoverflow.com/questions/8077530/android-get-current-timestamp
    public String getCurrentTimestampString() {
        long tsLong = System.currentTimeMillis()/1000;
        return Long.toString(tsLong);
    }

    public String fireAuthExceptionCode(Exception exception) {
        String error;
        if (exception instanceof FirebaseAuthException) {
            error = ((FirebaseAuthException) exception).getErrorCode();
            Log.w(LOG_TAG, error);
        } else {
            error = "ERROR_INTERNET_CONN_AUTH";
            Log.w(LOG_TAG, exception.getMessage());
        }
        return error;
    }

    public String fireStoreExceptionCode(Exception exception) {
        String error;
        if (exception instanceof FirebaseFirestoreException) {
            error = (((FirebaseFirestoreException) exception).getCode()).name();
            Log.w(LOG_TAG, error);
        } else {
            error = "ERROR_INTERNAL_FIRESTORE";
            Log.w(LOG_TAG, exception.getMessage());
        }
        return error;
    }

    // https://developers.google.com/android/reference/com/google/firebase/storage/StorageException?hl=en
    public String fireStorageExceptionCode(Exception exception) {
        String error;
        if (exception instanceof StorageException) {
            int errorCode = ((StorageException) exception).getErrorCode();
            switch(errorCode) {
                case -13011:
                    error = "ERROR_BUCKET_NOT_FOUND";
                    break;
                case -13040:
                    error = "ERROR_CANCELED";
                    break;
                case -13031:
                    error = "ERROR_INVALID_CHECKSUM";
                    break;
                case -13020:
                    error = "ERROR_NOT_AUTHENTICATED";
                    break;
                case -13021:
                    error = "ERROR_NOT_AUTHORIZED";
                    break;
                case -13010:
                    error = "ERROR_OBJECT_NOT_FOUND";
                    break;
                case -13012:
                    error = "ERROR_PROJECT_NOT_FOUND";
                    break;
                case -13013:
                    error = "ERROR_QUOTA_EXCEEDED";
                    break;
                case -13030:
                    error = "ERROR_RETRY_LIMIT_EXCEEDED";
                    break;
                default:
                    error = "ERROR_UNKNOWN";
                    break;
            }
        } else {
            error = "ERROR_INTERNAL_CLOUD_STORAGE";
        }
        Log.w(LOG_TAG, error);
        return error;
    }
}
