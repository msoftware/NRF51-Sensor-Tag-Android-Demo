package com.jentsch.nrf51.sensortag.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/*
 Based on https://github.com/fzhzx/Spider
 */
public class SpiderView  extends View {

    private Paint linePaint, axPaint, gxPaint,textPaint;
    private float x, y, r;
    private int count = 3;
    private double angle = 360/count;

    private double axArray[] = {0.0,0.0,0.0};
    private double gxArray[] = {0.0,0.0,0.0};

    private String[] titles = {"X", "Y", "Z"};

    private int beelineColor = Color.BLACK;
    private int footColor = Color.GREEN;
    private int faceColor = Color.RED;

    private int gxAlpha = 160;
    private int axAlpha = 160;

    public SpiderView(Context context) {
        super(context);
        init();
    }

    public SpiderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpiderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(beelineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);

        axPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axPaint.setColor(footColor);
        axPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        gxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gxPaint.setColor(faceColor);
        gxPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        textPaint = new Paint();
        textPaint.setTextSize(20);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.r = Math.min(h, w)/2*0.9f;
        this.x = w/2;
        this.y = h/2;
        postInvalidate();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLines(canvas);
        drawShadow(canvas, axPaint, axArray, axAlpha);
        drawShadow(canvas, gxPaint, gxArray, gxAlpha);
        drawText(canvas);
    }

    private void drawLines(Canvas canvas) {
        Path path = new Path();
        for (int i = 0; i < count; i++) {
            path.moveTo(x, y);
            path.lineTo((float)(x + r*Math.sin(Math.toRadians(i*angle))), (float)(y - r*Math.cos(Math.toRadians(i*angle))));
            path.close();
            canvas.drawPath(path, linePaint);
        }
    }

    private void drawShadow(Canvas canvas, Paint paint, double array[], int alpha) {
        Path path = new Path();
        float xx, yy;
        paint.setAlpha(255);
        for (int i = 0; i < count; i++) {
            xx = (float)(x + array[i]*r*Math.sin(Math.toRadians(i*angle)));
            yy = (float)(y - array[i]*r*Math.cos(Math.toRadians(i*angle)));
            if (i == 0) {
                path.moveTo(xx, yy);
            } else {
                path.lineTo(xx, yy);
            }
        }
        path.lineTo((float)(x + array[0]*r*Math.sin(Math.toRadians(0))), (float)(y - array[0]*r*Math.cos(Math.toRadians(0))));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
        paint.setAlpha(alpha);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(path, paint);
    }

    private void drawText(Canvas canvas) {
        for(int i=0;i<count;i++){
            float xx = (float)(x + r*Math.sin(Math.toRadians(i*angle)));
            float yy = (float)(y - r*Math.cos(Math.toRadians(i*angle)));
            if(angle*i >= 0 && angle*i <= 90){//第1象限
                canvas.drawText(titles[i], xx,yy,textPaint);
            }else if(angle*i >= 270 && angle*i <= 360){//第4象限
                float dis = textPaint.measureText(titles[i]);//文本长度
                canvas.drawText(titles[i], xx-dis,yy,textPaint);
            }else if(angle*i >= 90 && angle*i < 180){//第2象限
                canvas.drawText(titles[i], xx,yy,textPaint);
            }else if(angle*i > 180 && angle*i < 270){//第3象限
                float dis = textPaint.measureText(titles[i]);//文本长度
                canvas.drawText(titles[i], xx-dis,yy,textPaint);
            } else if(angle*i == 180) {
                canvas.drawText(titles[i], xx,yy + 20,textPaint);
            } else if(angle*i == 0) {
                canvas.drawText(titles[i], xx,yy - 10,textPaint);
            }
        }
    }

    public double[] getAxArray() {
        return axArray;
    }

    public void setAxArray(double[] axArray) {
        this.axArray = axArray;
    }

    public double[] getGxArray() {
        return gxArray;
    }

    public void setGxArray(double[] gxArray) {
        this.gxArray = gxArray;
    }
}