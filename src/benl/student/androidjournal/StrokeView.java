package benl.student.androidjournal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class StrokeView extends View {
	private static final int DEFAULT_COLOUR = 0xFF000000;
	private static final int DEFAULT_RADIUS = 10;
	
	private Paint paint;
	private int radius;
	
	public StrokeView(Context context) {
		super(context);
		
		paint = new Paint();
		paint.setColor(DEFAULT_COLOUR);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setDither(true);
		
		radius = DEFAULT_RADIUS;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawCircle(canvas);
	}
	
	private void drawCircle(Canvas canvas) {
		int x = (this.getRight() - this.getLeft())/2;
		int y = (this.getBottom() - this.getTop())/2;
		canvas.drawCircle(x, y, radius, paint);
	}
	
	public void setColour(int colour) {
		paint.setColor(colour);
	}
	
	public void setRadius(int radius) {
		this.radius = radius;
		invalidate();
	}
}
