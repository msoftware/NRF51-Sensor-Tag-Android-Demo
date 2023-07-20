package com.jentsch.nrf51.sensortag.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class LineChartView extends View {

    private int anz = 6;

    private Paint[] p = new Paint[anz];

    private ArrayList<Sensor> ax;
    private ArrayList<Sensor> gx;

    private int[] color = {
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA
    };

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        for (int i = 0; i < anz; i++) {
            p[i] = new Paint();
            p[i].setStrokeWidth(4);
            p[i].setColor(color[i]);
            p[i].setStyle(Paint.Style.FILL);
        }
        ax = new ArrayList<>();
        gx = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight() / anz;
        for (int i = 0; i < anz; i++) { // Anz = 6
            int xpos = width;
            if (i < 3) {
                // ax
                for (int j = ax.size() - 1 ; j >= 0 && xpos > 0; j --) {
                    float ypos = (ax.get(j).data[i] - 32768f) / 32768f * height;
                    canvas.drawCircle(xpos, ypos + height * (i + 0.5f), 5, p[i]);
                    xpos = xpos - 8;
                }
            } else {
                // gx
                for (int j = gx.size() - 1; j >= 0 && xpos > 0; j --) {
                    float ypos = (gx.get(j).data[i-3] - 32768f) / 32768f * height;
                    canvas.drawCircle(xpos, ypos + height * (i + 0.5f), 5, p[i]);
                    xpos = xpos - 8;
                }
            }
        }
    }

    public void addAx(long x, long y, long z) {
        this.ax.add(new Sensor(x,y,z));
    }

    public void addGx(long x, long y, long z) {
        this.gx.add(new Sensor(x,y,z));
    }

    private class Sensor
    {
        private final long[] data;
        public Sensor(long x, long y, long z) {
            this.data = new long[]{x, y, z};
        }
    }
}
