package qa.edu.hbku.blockchain.quarantinecovid19;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class DrawView extends View {

    Paint mPaint = new Paint();

    public DrawView(Context context) {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas) {

        Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG |
                Paint.DITHER_FLAG |
                Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setColor(Color.GRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);

        int size = 200;
        int radius = 190;
        int delta = size - radius;
        int arcSize = (size - (delta / 2)) * 2;
        int percent = 42;

        //Thin circle
        canvas.drawCircle(size, size, radius, mPaint);

        //Arc
        mPaint.setColor(Color.parseColor("#33b5e5"));
        mPaint.setStrokeWidth(15);
        RectF box = new RectF(delta,delta,arcSize,arcSize);
        float sweep = 360 * percent * 0.01f;
        canvas.drawArc(box, 0, sweep, false, mPaint);

    }

}
