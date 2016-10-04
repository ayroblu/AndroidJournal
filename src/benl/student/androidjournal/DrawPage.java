package benl.student.androidjournal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Region;
import android.graphics.Paint.Style;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DrawPage extends View {
	private static final String TAG = "DrawPage";
	private static final int DRAW_WIDTH = 2;
	private static final int ERASER_COLOUR = 0xFFAAAAAA;
	private static final int ERASER_RADIUS = 20;
	private static final int BACKGROUND_CLEAR = 0x00000000;
	private static final int SPACING = 80;
	private static final int LINE_WIDTH = 1;
	private static final int MAX_TOUCH_POINTS = 10;

	public static final int WHITE = 0xFFFFFFFF;
	public static final int BLACK = 0xFF000000;
	public static final int YELLOW = 0xFFFFDD11;
	public static final int RED = 0xFFDD3333;
	public static final int BLUE = 0xFF00AAFF;
	public static final int GREEN = 0xFF00DD22;
	public static final int TURQUOISE = 0xFF22CCCC;
	public static final int PURPLE = 0xFFFF22DD;
	public static final int ORANGE = 0xFFFF5500;
	public static final int DARK_BLUE = 0xFF0022EE;

	private static final String DELIMITER = ";";
	private static final String SECTION_DELIMITER = ";/;";

	private ArrayList<Stroke> strokes;
	private ArrayList<Stack> history;

	private int stackCounter;
	private int strokeCounter;
	private boolean recentUndo;

	private int colour;
	private int width;
	private boolean drawHorizontal;
	private boolean drawLines;
	private int drawColour;
	private int backgroundColour;

	private Paint paint;
	private Paint eraserPaint;
	private Paint linePaint;

	private Point ref;
	private Point sRef; //Starting ref, used when panning
	private Point eraserPoint;

	private boolean pan;
	private boolean erasing;
	private boolean eraserStroke;
	private boolean touching; //Only used by the eraser so don't use for other stuff

	private Stroke[] tempStroke;

	private Bitmap  mBitmap;
	private Canvas  mCanvas;
	private Paint   mBitmapPaint;


	//Constructor----------------------------------------------------------------------
	public DrawPage(Context context) {
		super(context);

		setSettings(context);

		strokes = new ArrayList<Stroke>();
		history = new ArrayList<Stack>();

		stackCounter = -1;
		strokeCounter = -1;
		recentUndo = false;

		colour = drawColour;
		width = DRAW_WIDTH;

		setPaint();
		tempStroke = new Stroke[MAX_TOUCH_POINTS];

		ref = new Point(0,0);
		sRef = new Point(0,0);
		eraserPoint = new Point(0,0);

		pan = false;
		erasing = false;
		eraserStroke = true;
		touching = false;

		mBitmapPaint = new Paint(Paint.DITHER_FLAG);

	}

	public void setSettings(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		drawLines = sharedPref.getBoolean(SettingsActivity.LINES, false);
		if (drawLines) {
			String drawHorizontal = sharedPref.getString(SettingsActivity.DIRECTION, "Horizontal");
			this.drawHorizontal = drawHorizontal.equals("Horizontal");
		}

		String backgroundColour = sharedPref.getString(SettingsActivity.BACKGROUND, "White");
		if (backgroundColour.equals("Red")) {
			this.backgroundColour = RED;
			this.drawColour = TURQUOISE;
		} else if (backgroundColour.equals("Green")) {
			this.backgroundColour = GREEN;
			this.drawColour = PURPLE;
		} else if (backgroundColour.equals("Blue")) {
			this.backgroundColour = BLUE;
			this.backgroundColour = ORANGE;
		} else if (backgroundColour.equals("Yellow")) {
			this.backgroundColour = YELLOW;
			this.drawColour = DARK_BLUE;
		} else if (backgroundColour.equals("Black")) {
			this.backgroundColour = BLACK;
			this.drawColour = WHITE;
		} else {
			this.backgroundColour = WHITE;
			this.drawColour = BLACK;
		}
	}

	private void setPaint() {
		paint = new Paint();
		paint.setColor(colour);
		paint.setStrokeWidth(width);
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);

		eraserPaint = new Paint();
		eraserPaint.setColor(ERASER_COLOUR);
		eraserPaint.setStyle(Style.FILL);
		eraserPaint.setAntiAlias(true); //Causes "smudging" on the path... not anymore...?
		eraserPaint.setDither(true);
		eraserPaint.setStrokeWidth(ERASER_RADIUS*2);
		eraserPaint.setStrokeJoin(Paint.Join.ROUND);
		eraserPaint.setStrokeCap(Paint.Cap.ROUND);

		linePaint = new Paint();
		linePaint.setColor(BLUE);
		linePaint.setStrokeWidth(LINE_WIDTH);
		linePaint.setStyle(Style.STROKE);
		linePaint.setAntiAlias(true);
		linePaint.setDither(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
	}

	//What happens when the user touches the screen-------------------------------------
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (pan) {
			panMove(event);
		} else if (erasing) {
			erase(event);
		} else {
			drawStroke(event);
		}
		invalidate();
		return true;
	}

	//When pan is enabled
	private void panMove(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			sRef.set((int) event.getX(), (int) event.getY());
		} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
			ref.set(x-sRef.x+ref.x, y-sRef.y+ref.y);
			sRef.set(x, y);
		} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			ref.set(x-sRef.x+ref.x, y-sRef.y+ref.y);
		}
	}

	//When erase is enabled
	private void erase(MotionEvent event) {
		if (eraserStroke) {
			eraserPaint.setColor(ERASER_COLOUR);
			eraserPaint.setStyle(Style.FILL);
			eraseStroke(event);
		} else {
			eraserPaint.setStyle(Style.STROKE);
			//			eraserPaint.setColor(BACKGROUND_COLOUR);
			eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			drawErase(event);
		}
	}

	private void eraseStroke(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		//Handle variables necessary for drawing the eraser
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			history.add(++stackCounter, new Stack(new ArrayList<Integer>())); //For undo and redo purposes
			touching = true;
		} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			if (history.get(stackCounter).strokeDeletions.size() < 1) {
				history.remove(stackCounter--);
			}
			touching = false;
		}
		eraserPoint.set(x, y);


		Region screen = new Region(this.getLeft(),this.getTop(),this.getRight(),this.getBottom());
		Region region1 = new Region();
		Region region2 = new Region();
		Path erase = new Path();

		for (int i = 0; i < strokes.size(); i++) {
			Stroke stroke = strokes.get(i);
			if (stroke.isEnabled()) {
				Path path = new Path();
				stroke.getPath().offset(ref.x, ref.y, path);

				erase.addCircle(x, y, eraserPaint.getStrokeWidth()/2 + stroke.getStrokeWidth()/2, Path.Direction.CW);
				region1.setPath(erase, screen);
				region2.setPath(path,region1);
				if (!region2.getBoundaryPath().isEmpty()) {
					hideStroke(event, i);
				}
			}
		}
		//		for (int i = 0; i < tempStroke.length; i++) {
		//			if (tempStroke[i] != null && tempStroke[i].isEnabled()) {
		//				Path path = new Path();
		//				tempStroke[i].getPath().offset(ref.x, ref.y, path);
		//				region2.setPath(path,region1);
		//				if (!region2.getBoundaryPath().isEmpty()) {
		//					tempStroke[i].setEnabled(false);
		//					Log.d(TAG, "erase: INTERSECTION");
		//				}
		//			}
		//		}
	}

	//Run when there is an intersection of strokes with stroke deletion
	private void hideStroke(MotionEvent event, int index) {
		strokes.get(index).setEnabled(false);
		Log.d(TAG, "erase: INTERSECTION");

		deleteOldHistory();
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			history.get(stackCounter).strokeDeletions.add(index);
		} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
			history.get(stackCounter).strokeDeletions.add(index);
		} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			//This is probably unlikely...
			history.get(stackCounter).strokeDeletions.add(index);
			Log.e(TAG, "hideStroke: THIS SHOULD NOT BE CALLED!!!");
		}
	}

	private void drawErase(MotionEvent event) {
		int action = event.getActionMasked();
		int pointCount = event.getPointerCount();

		for (int i = 0; i < pointCount; i++) {
			int id = event.getPointerId(i);

			//Ignore pointer higher than our max.
			if(id < MAX_TOUCH_POINTS){
				int x = (int) event.getX(id) - ref.x;
				int y = (int) event.getY(id) - ref.y;

				if (action == MotionEvent.ACTION_DOWN) {
					tempStroke[id] = new Stroke(eraserPaint);
					tempStroke[id].setErase(true);
					tempStroke[id].addPoint(new Point(x,y));

				} else if (action == MotionEvent.ACTION_MOVE) {
					tempStroke[id].addPoint(new Point(x,y));

				} else if (action == MotionEvent.ACTION_UP) {
					if (tempStroke[id].getNumPoints() < 3) {
						tempStroke[id].addPoint(new Point(x+1,y+1));
						tempStroke[id].addPoint(new Point(x-1,y+1));
						tempStroke[id].addPoint(new Point(x,y-1));
					}
					tempStroke[id].addPoint(new Point(x,y));

					deleteOldHistory();
					strokes.add(++strokeCounter, tempStroke[id]);
					tempStroke[id] = null;
					history.add(++stackCounter, new Stack(null)); //For undo and redo purposes
				}
			}
		}

		//		int x = (int) event.getX() - ref.x;
		//		int y = (int) event.getY() - ref.y;
		//    	if (event.getAction() == MotionEvent.ACTION_DOWN) {
		//    		tempStroke = new Stroke(eraserPaint);
		//    		tempStroke.setErase(true);
		//    		tempStroke.addPoint(new Point(x,y));
		////        	Log.d(TAG, "drawStroke: actionDown");
		//    		
		//    	} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
		//    		tempStroke.addPoint(new Point(x,y));
		////        	Log.d(TAG, "drawStroke: actionMove");
		//    		
		//    	} else if (event.getAction() == MotionEvent.ACTION_UP) {
		//    		if (tempStroke.getNumPoints() < 3) {
		//    			tempStroke.addPoint(new Point(x+1,y+1));
		//    			tempStroke.addPoint(new Point(x-1,y+1));
		//    			tempStroke.addPoint(new Point(x,y-1));
		//			}
		//    		tempStroke.addPoint(new Point(x,y));
		//    		
		//    		deleteOldHistory();
		//    		
		//    		strokes.add(++strokeCounter, tempStroke);
		//    		
		//    		history.add(++stackCounter, new Stack(null)); //For undo and redo purposes

		//        	Log.d(TAG, "drawStroke: actionUp");
		//    		Log.i(TAG, "drawStroke: strokes.size = " + strokes.size());
		//    	}
		//    	Log.d(TAG, "drawStroke: (x,y) = " + x + ", "+ y);
	}

	//When not panning or erasing, normal drawing event
	private void drawStroke(MotionEvent event) {
		int action = event.getActionMasked();
		int pointer = event.getActionIndex();

//		Log.d(TAG, "drawStroke: pointer = " + pointer);
		int id = event.getPointerId(pointer);
		int index = event.findPointerIndex(id);

		try {
			//Ignore pointer higher than our max.
			if(id < MAX_TOUCH_POINTS){
				int x = (int) event.getX(index) - ref.x;
				int y = (int) event.getY(index) - ref.y;

				if (tempStroke[id] == null) {
					tempStroke[id] = new Stroke(paint);
				}

				if (action == MotionEvent.ACTION_DOWN) {
					deleteOldHistory();
					tempStroke[id].addPoint(new Point(x,y));
					Log.d(TAG, "drawStroke: ActionDown");
					Log.d(TAG, "drawStroke: id = " + id);

				} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
					tempStroke[id].addPoint(new Point(x,y));

					Log.d(TAG, "drawStroke: ActionPointerDown");
					Log.d(TAG, "drawStroke: id = " + id);

				} else if (action == MotionEvent.ACTION_MOVE) {
					int pointCount = event.getPointerCount();
					
					for (int i = 0; i < pointCount; i++) {
						id = event.getPointerId(i);
						x = (int) event.getX(i) - ref.x;
						y = (int) event.getY(i) - ref.y;
						if (tempStroke[id] != null) {
							tempStroke[id].addPoint(new Point(x,y));
						} else {
							Log.d(TAG, "drawStroke: pointCount = " + pointCount);
							Log.d(TAG, "drawStroke: id = " + id);
						}
					}
					

				} else if (action == MotionEvent.ACTION_POINTER_UP) {
					if (tempStroke[id].getNumPoints() < 3) {
						tempStroke[id].addPoint(new Point(x+1,y+1));
						tempStroke[id].addPoint(new Point(x-1,y+1));
						tempStroke[id].addPoint(new Point(x,y-1));
					}
					tempStroke[id].addPoint(new Point(x,y));

					strokes.add(++strokeCounter, tempStroke[id]);
					tempStroke[id] = null;
					history.add(++stackCounter, new Stack(null)); //For undo and redo purposes

					Log.d(TAG, "drawStroke: ActionPointerUp");
					Log.d(TAG, "drawStroke: id = " + id);

				} else if (action == MotionEvent.ACTION_UP) {
					if (tempStroke[id].getNumPoints() < 3) {
						tempStroke[id].addPoint(new Point(x+1,y+1));
						tempStroke[id].addPoint(new Point(x-1,y+1));
						tempStroke[id].addPoint(new Point(x,y-1));
					}
					tempStroke[id].addPoint(new Point(x,y));

					strokes.add(++strokeCounter, tempStroke[id]);
					tempStroke[id] = null;
					history.add(++stackCounter, new Stack(null)); //For undo and redo purposes

					Log.d(TAG, "drawStroke: ActionUp");
					Log.d(TAG, "drawStroke: id = " + id);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "drawStroke: id = " + id);
			e.printStackTrace();
		}
	}



	//Draw Events-----------------------------------------------------------------------
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//Principle: mCanvas.drawColor dictates the colour that you see INITIALLY
		//		canvas.drawColor dictates the colour that you see upon ERASE
		//		You want to set these colours the same, or clear the top canvas.
		canvas.drawColor(backgroundColour);
		if (drawLines) {
			drawRuledLines(canvas);
		}
		mCanvas.drawColor(BACKGROUND_CLEAR,PorterDuff.Mode.CLEAR);
		drawPaths(mCanvas);
		drawEraser(canvas);

		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
	}

	private void drawPaths(Canvas canvas) {
		drawPaths(canvas, null);
	}
	
	private void drawPaths(Canvas canvas, int[] offset) {
		for (int i = 0; i <= strokeCounter; i++) {
			Stroke stroke = strokes.get(i);
			if (stroke.isEnabled()) {
				Path path = new Path();
				if (offset == null) {
					stroke.getPath().offset(ref.x, ref.y, path);
				} else {
					stroke.getPath().offset(-offset[0], -offset[1], path);
				}
				canvas.drawPath(path, stroke.getPaint());
			}
		}
		for (int i = 0; i < tempStroke.length; i++) {
			if (tempStroke[i] != null && !recentUndo && tempStroke[i].isEnabled()) {
				Path path = new Path();
				if (offset == null) {
					tempStroke[i].getTempPath().offset(ref.x, ref.y, path);
				} else {
					tempStroke[i].getTempPath().offset(-offset[0], -offset[1], path);
				}
				canvas.drawPath(path, tempStroke[i].getPaint());
			}
		}
	}

	private void drawEraser(Canvas canvas) {
		if (touching) {
			canvas.drawCircle(eraserPoint.x, eraserPoint.y, eraserPaint.getStrokeWidth()/2, eraserPaint);
		}
	}

	private void drawRuledLines(Canvas canvas) {
		Point outSize = new Point(this.getRight()-this.getLeft(), this.getBottom()-this.getTop());
		if (drawHorizontal) {
			int difference = ref.x % SPACING;
			//Reverses the direction of drawing... (unnecessary really...?)
			//for (int i = outSize.x-difference; i >= 0; i -= SPACING) {
			for (int i = 0; i < outSize.x-difference; i += SPACING) {
				canvas.drawLine(difference + i, 0, difference + i, outSize.y, linePaint);
			}
		} else {
			int difference = ref.y % SPACING;
			for (int i = 0; i < outSize.y-difference; i += SPACING) {
				canvas.drawLine(0, difference + i, outSize.x, difference + i, linePaint);
			}
		}
	}

	public Bitmap save() {
		float[] fullSize = getSize();
		int[] size = new int[4];
		for (int i = 0; i < size.length; i++) {
			size[i] = (int) fullSize[i];
		}
		int paintWidth = (int) (fullSize[6] + fullSize[4]);
		int paintHeight = (int) (fullSize[7] + fullSize[5]);
		
		Bitmap saveBitmap = Bitmap.createBitmap(size[2] - size[0] +paintWidth+50, size[3] - size[1] +paintHeight+50, Bitmap.Config.ARGB_8888);
		Canvas saveCanvas = new Canvas(saveBitmap);
		Bitmap bitmap = Bitmap.createBitmap(size[2] - size[0]  +paintWidth+50, size[3] - size[1]  +paintHeight+50, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		size[0] -= 25 + fullSize[4];
		size[1] -= 25 + fullSize[5];
		
		canvas.drawColor(backgroundColour);
		if (drawLines) {
			drawRuledLines(canvas);
		}
		saveCanvas.drawColor(BACKGROUND_CLEAR,PorterDuff.Mode.CLEAR);
		drawPaths(saveCanvas, size);

		canvas.drawBitmap(saveBitmap, 0, 0, mBitmapPaint);
		return bitmap;
	}
	
	public float[] getSize() {
		int[] size = new int[4];
		float[] margin = new float[4]; //left, top, right, bottom
		for (int i = 0; i < strokes.size(); i++) {
			int[] dimension = strokes.get(i).getSize(); //minX, minY, maxX, maxY
			float width = strokes.get(i).getPaint().getStrokeWidth();
			if (i == 0) {
				size = dimension;
				for (int j = 0; j < margin.length; j++) {
					margin[i] = width;
				}
			} else {
				if (dimension[0]-width < size[0]-margin[0]) {
					size[0] = dimension[0];
					margin[0] = width;
				}
				if (dimension[1]-width < size[1]-margin[1]) {
					size[1] = dimension[1];
					margin[1] = width;
				}
				if (dimension[2]+width > size[2]+margin[2]) {
					size[2] = dimension[2];
					margin[2] = width;
				}
				if (dimension[3]+width > size[3]+margin[3]) {
					size[3] = dimension[3];
					margin[3] = width;
				}
			}
		}
		float[] fullSize = new float[size.length + margin.length];
		for (int i = 0; i < size.length; i++) {
			fullSize[i] = size[i];
		}
		for (int i = 0; i < margin.length; i++) {
			fullSize[i+size.length] = margin[i];
		}
		return fullSize;
	}


	//Interface Methods---------------------------------------------------------------
	public void setPan(boolean pan) {
		this.pan = pan;
	}

	public void setDraw() {
		erasing = false;
		pan = false;
	}

	public void setEraser() {
		erasing = true;
		pan = false;
	}



	public void setPaintColour(int colour) {
		paint.setColor(colour);
	}

	public int getPaintColour() {
		return paint.getColor();
	}

	public void setStrokeWidth(int strokeWidth) {
		paint.setStrokeWidth(strokeWidth);
	}

	public float getStrokeWidth() {
		return paint.getStrokeWidth();
	}

	public int getEraserColour() {
		//		return eraserPaint.getColor();
		return ERASER_COLOUR;
	}

	public float getEraserWidth() {
		return eraserPaint.getStrokeWidth();
	}

	public void setEraserWidth(int width) {
		eraserPaint.setStrokeWidth(width);
	}

	public void setEraserStroke(boolean stroke) {
		this.eraserStroke = stroke;
	}

	public boolean isDraw() {
		if (!erasing && !pan) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isEraser() {
		if (erasing && !pan) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isErasingStroke() {
		return eraserStroke;
	}

	public int getBackgroundColour() {
		return backgroundColour;
	}

	public int getDrawColour() {
		return drawColour;
	}

	public boolean isPan() {
		return pan;
	}

	public boolean hasHistory() {
		if (history.size() > 0) {
			return true;
		}
		return false;
	}

	//Undo/Redo------------------------------------------------------------------------
	public void undo() {
		if (!recentUndo && history.size() > 0) {
			stackCounter = history.size()-1;
			recentUndo = true;
		}
		if (stackCounter >= 0) {
			if (history.get(stackCounter).strokeDeletions == null) {
				//If a stroke was drawn
				--strokeCounter;
			} else {
				//If strokes were deleted
				ArrayList<Integer> deletions = history.get(stackCounter).strokeDeletions;
				for (int i = 0; i < deletions.size(); i++) {
					strokes.get(deletions.get(i)).setEnabled(true);
				}
			}

			--stackCounter;
			invalidate();
		}
	}

	public void redo() {
		if (recentUndo) {
			++stackCounter;
			if (history.get(stackCounter).strokeDeletions == null) {
				//If a stroke was drawn
				++strokeCounter;
			} else {
				//If strokes were deleted
				ArrayList<Integer> deletions = history.get(stackCounter).strokeDeletions;
				for (int i = 0; i < deletions.size(); i++) {
					strokes.get(deletions.get(i)).setEnabled(false);
				}
			}

			if (stackCounter >= history.size()-1) {
				recentUndo = false;
				stackCounter = history.size()-1;
				strokeCounter = strokes.size()-1;
			}
			invalidate();
		}
	}

	private void deleteOldHistory() {
		if (recentUndo) {
			for (int i = stackCounter+1; i < history.size(); i++) {
				history.remove(i);
			}
			for (int i = strokeCounter+1; i < strokes.size(); i++) {
				strokes.remove(i);
			}
			recentUndo = false;
		}
	}

	private class Stack {
		private ArrayList<Integer> strokeDeletions;

		private Stack(ArrayList<Integer> strokeDeletions) {
			this.strokeDeletions = strokeDeletions;
		}
	}


	//Saving and Reading----------------------------------------------------------------
	public void savePage(FileOutputStream fos) {
		try {
			fos.write((stackCounter + DELIMITER).getBytes());
			fos.write((strokeCounter + DELIMITER).getBytes());
			int temp = recentUndo ? 1:0;
			fos.write((temp + DELIMITER).getBytes());
			fos.write((colour + DELIMITER).getBytes());
			fos.write((width + DELIMITER).getBytes());
			fos.write((ref.x + DELIMITER).getBytes());
			fos.write((ref.y + DELIMITER).getBytes());
			temp = pan ? 1:0;
			fos.write((temp + DELIMITER).getBytes());
			temp = erasing ? 1:0;
			fos.write((temp + DELIMITER).getBytes());
			temp = eraserStroke ? 1:0;
			fos.write((temp+"").getBytes()); //NO DELIMITER... (due stops delimiting by 4)

			fos.write((SECTION_DELIMITER).getBytes()); //Split this section off

			//Save all Strokes;
			//Each stroke saves enabled, paint, erase and points; Double delimit each stroke
			for (int i = 0; i < strokes.size(); i++) {
				strokes.get(i).save(fos);
				if (i < strokes.size()-1) {
					fos.write((DELIMITER + DELIMITER).getBytes());
				}
			}
			fos.write((SECTION_DELIMITER).getBytes()); //Split this section off

			//History- null is "n", else, number of deletions, deletions
			fos.write((history.size()+DELIMITER).getBytes());
			for (int i = 0; i < history.size(); i++) {
				ArrayList<Integer> item = history.get(i).strokeDeletions;
				if (item == null) {
					fos.write(("n"+DELIMITER).getBytes());
				} else {
					fos.write((item.size()+DELIMITER).getBytes());
					for (int j = 0; j < item.size(); j++) {
						fos.write((item.get(j)+DELIMITER).getBytes());
					}
				}
			}
			fos.write((SECTION_DELIMITER).getBytes()); //End
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void restoreHere(String[] temp) {
		String[] localVariables = temp[1].split(DELIMITER);
		Log.d(TAG, "localVariables.length = " + localVariables.length);
		try {
			int counter = 0;
			stackCounter = Integer.parseInt(localVariables[counter]);
			strokeCounter = Integer.parseInt(localVariables[++counter]);
			recentUndo = localVariables[++counter].equals("1");
			colour = Integer.parseInt(localVariables[++counter]);
			width = Integer.parseInt(localVariables[++counter]);
			ref.x = Integer.parseInt(localVariables[++counter]);
			ref.y = Integer.parseInt(localVariables[++counter]);
			pan = localVariables[++counter].equals("1");
			erasing = localVariables[++counter].equals("1");
			eraserStroke = localVariables[++counter].equals("1");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] strokeVariables = temp[2].split(DELIMITER+DELIMITER);
		Log.d(TAG, "strokeVariables.length = " + strokeVariables.length);
		for (int i = 0; i < strokeVariables.length; i++) {
			if (!strokeVariables[i].equals("")) {
				strokes.add(new Stroke(paint));
				strokes.get(i).restoreHere(strokeVariables[i].split(DELIMITER));
			}
		}

		String[] historyVariables = temp[3].split(DELIMITER);
		Log.d(TAG, "historyVariables.length = " + historyVariables.length);
		int counter = 0;
		int historyLength = Integer.parseInt(historyVariables[counter]);
		Log.d(TAG, "historyLength = " + historyLength);
		for (int i = 0; i < historyLength; i++) {
			if (historyVariables[++counter].equals("n")) {
				history.add(new Stack(null));
			} else {
				history.add(new Stack(new ArrayList<Integer> ()));
				try {
					int length = Integer.parseInt(historyVariables[counter]);
					for (int j = 0; j < length; j++) {
						int num = Integer.parseInt(historyVariables[++counter]);
						history.get(i).strokeDeletions.add(num);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
