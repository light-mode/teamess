package com.example.myapplication.utilities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Converter {
    public static String convertTimestampToString(String seconds) {
        String pattern;
        long timestampInMillis = Long.parseLong(seconds) * 1000;
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (timestampInMillis >= calendar.getTimeInMillis()) {
            pattern = "hh:mm a";
        } else {
            calendar.add(Calendar.YEAR, -1);
            if (timestampInMillis > calendar.getTimeInMillis()) {
                pattern = "MMM dd hh:mm a";
            } else {
                pattern = "MMM dd, yyyy hh:mm a";
            }
        }
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(new Date(timestampInMillis));
    }

    public static Bitmap convertDrawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
