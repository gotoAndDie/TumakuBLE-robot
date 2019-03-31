package com.tumaku.msmble;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FloatingTouchEventView extends View {
    private Paint paint = new Paint();
    private Path path = new Path();
    TextView tw;
    private static final String TAG = "FingerDraw";
    private static final double EDGE = 0.2;
    float x,y;
    double proportionX, proportionY;
    private Path joyPoint = new Path();
    float centreX = 0, centreY = 0;
    HM10Activity context;

    private OutputStream output = null;
    private InputStream input = null;

    public FloatingTouchEventView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.BLACK);
        paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        this.context = (HM10Activity) context;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawARGB(255, 235, 235, 235);
        //canvas.drawPath(path, paint);
        joyPoint.reset();
        joyPoint.addCircle(x,y, 5, Path.Direction.CW);
        joyPoint.addCircle(centreX, centreY, (float) EDGE * getWidth(), Path.Direction.CW);
        joyPoint.moveTo(centreX + (float) (EDGE + 0.1) * getWidth(), centreY);
        joyPoint.lineTo(centreX - (float) (EDGE + 0.1) * getWidth(), centreY);
        canvas.drawPath(joyPoint, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        x = event.getX();
        y = event.getY();
        proportionX = x / getWidth();
        proportionY = y / getWidth();

        if (action == MotionEvent.ACTION_DOWN) {
            path.moveTo(x,y);
            centreX = x;
            centreY = y;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            path.lineTo(x,y);
        }

        double left, right;

        double processX, processY;

        // Using a center of your finger location
        if(proportionY <= centreY / getHeight()) // Positive part
            processY = -(proportionY - centreY / getHeight());
        else // Negative part
            processY = proportionY - centreY / getHeight();

        processX = proportionX - centreX / getWidth();
        double magnitude = Math.sqrt(processX * processX + processY * processY);
        double divider = magnitude > EDGE ? magnitude / EDGE : 1;
        processX /= divider; processY /= divider; // Cap the magnitude at EDGE
        magnitude = magnitude > EDGE ? EDGE : magnitude;

        // Each direction is max if pointing towards them, scales down with increasing magnitude
        // of opposite if pointing away
        left = processX > 0 ? (EDGE - processX) * magnitude * (1/EDGE): magnitude;
        right = processX < 0 ? (EDGE + processX) * magnitude * (1/EDGE): magnitude;

        if(proportionY <= centreY / getHeight()) { // Positive part
            left = (1 / EDGE) * left;
            right = (1 / EDGE) * right;
        } else { // Negative part
            left = -(1 / EDGE) * left;
            right = -(1 / EDGE) * right;
            double swap = left; left = right; right = swap; // Invert back-control for better intuitiveness
        }

        // Reset to center on up
        if (action == MotionEvent.ACTION_UP) {
            x = centreX; y = centreY;
            left = 0; right = 0;
        }

        if(tw != null)
            tw.setText("Left driver: " + left + " Right driver: " + right);
        //context.sendCoord("l " + left + " r " + right);
        int eightBitLeft = (int) Math.abs(left * 255);
        int eightBitRight = (int) Math.abs(right * 255);

        if(left > 0.5) {
            if (right > 0.5)
                context.sendCoord("f");
            else
                context.sendCoord("l");
        } else if(left >= 0){
            if (right > 0.5)
                context.sendCoord("r");
            else
                context.sendCoord("s");
        }

        if(left < 0 && right < 0)
            context.sendCoord("b");
        if(output != null)
            try { output.write(("l " + left + " r " + right).getBytes()); }
            catch (IOException e) {e.printStackTrace();}



        invalidate();
        return true;
    }

    public void setInfoBox(TextView tw)
    {
        this.tw = tw;
    }
    public void setIO(InputStream input, OutputStream output) { this.input = input; this.output = output; }
}