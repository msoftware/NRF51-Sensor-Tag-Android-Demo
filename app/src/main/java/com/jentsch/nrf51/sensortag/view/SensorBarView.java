package com.jentsch.nrf51.sensortag.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.jentsch.nrf51.sensortag.UartService;

public class SensorBarView extends View {

    float rawX;
    float rawY;
    float rawZ;

    Paint xPaint;
    Paint yPaint;
    Paint zPaint;
    Paint textPaint;

    public SensorBarView(Context context) {
        super(context);
        init();
    }

    public SensorBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init () {
        xPaint = new Paint();
        yPaint = new Paint();
        zPaint = new Paint();
        textPaint = new Paint();

        xPaint.setColor(Color.RED);
        yPaint.setColor(Color.BLUE);
        zPaint.setColor(Color.GREEN);
        textPaint.setColor(Color.BLUE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        try {
            int width = canvas.getWidth();
            int height = canvas.getHeight() / 3;

            float xFactor = rawX / 0xffff;
            float yFactor = rawY / 0xffff;
            float zFactor = rawZ / 0xffff;

            int xWidth = (int)(width * xFactor);
            int yWidth = (int)(width * yFactor);
            int zWidth = (int)(width * zFactor);

            canvas.drawRect(0,0, xWidth, height, xPaint);
            canvas.drawRect(0,height, yWidth, height * 2, yPaint);
            canvas.drawRect(0,height * 2, zWidth, height * 3, zPaint);

            textPaint.setTextSize(40);
            canvas.drawText((int)(rawX) - UartService.offset + "", width * 3 / 4, height * 1 - 10, textPaint);
            canvas.drawText((int)(rawY) - UartService.offset + "", width * 3 / 4, height * 2 - 10, textPaint);
            canvas.drawText((int)(rawZ) - UartService.offset + "", width * 3 / 4, height * 3 - 10, textPaint);

        } catch (Exception e) {
            String stackTrace = Log.getStackTraceString(e);
        }
    }

    public float getRawX() {
        return rawX;
    }

    public void setRawX(float rawX) {
        this.rawX = rawX;
    }

    public float getRawY() {
        return rawY;
    }

    public void setRawY(float rawY) {
        this.rawY = rawY;
    }

    public float getRawZ() {
        return rawZ;
    }

    public void setRawZ(float rawZ) {
        this.rawZ = rawZ;
    }

    public void setRawBytes(byte[] txValue) {

        setRawX(UartService.getLongValue(txValue[0],txValue[1]));
        setRawY(UartService.getLongValue(txValue[2],txValue[3]));
        setRawZ(UartService.getLongValue(txValue[4],txValue[5]));
    }
}
