package benl.student.androidjournal;

import java.io.FileOutputStream;
import java.util.ArrayList;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class Stroke {
	private static final String DELIMITER = ";";
	
	private ArrayList<Point> points;
	private int[] size = null;
	private boolean enabled;
	private Paint paint;
	private Path path = null;
	private boolean erase;
	
	public Stroke(Paint paint) {
		this.paint = new Paint(paint);
		
		enabled = true;
		
		points = new ArrayList<Point>();
		
		erase = false;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void addPoint(Point point) {
		points.add(point);
	}
	
	public Path getPath() {
		if (path == null) {
			return getTempPath();
		}
        return path;
	}
	
	public Path getTempPath() {
		path = new Path();
		int mX = 0;
		int mY = 0;
        for (int i = 0; i < points.size(); i++) {
        	Point point = points.get(i);
			if (i == 0) {
				path.moveTo(point.x, point.y);
		        mX = point.x;
		        mY = point.y;
			} else if (i >= points.size()-1) {
				path.lineTo(mX, mY);
			} else {
				path.quadTo(mX, mY, (point.x + mX)/2, (point.y + mY)/2);
		        mX = point.x;
		        mY = point.y;
			}
		}
        return path;
	}
	
	public boolean isEnabled() {
		if (erase) {
			return true;
		} else {
			return enabled;
		}
		
	}
	
	public Paint getPaint() {
		return paint;
	}
	
	public int[] getSize() {
		if (size == null) {
			int maxX = 0;
			int maxY = 0;
			int minX = 0;
			int minY = 0;
			for (int i = 0; i < points.size(); i++) {
				Point point = points.get(i);
				if (i == 0) {
					maxX = point.x;
					maxY = point.y;
					minX = point.x;
					minY = point.y;
				} else {
					if (point.x > maxX) {
						maxX = point.x;
					} else if (point.x < minX) {
						minX = point.x;
					}
					if (point.y > maxY) {
						maxY = point.y;
					} else if (point.y < minY) {
						minY = point.y;
					}
				}
			}
			size = new int[] { minX, minY, maxX, maxY};
		}
		return size;
	}


	public int getNumPoints() {
		return points.size();
	}
	
	public Point getPoint(int i) {
		return points.get(i);
	}
	
	public float getStrokeWidth() {
		return paint.getStrokeWidth();
	}
	
	public void setErase(boolean erase) {
		this.erase = erase;
	}

	
	public void save(FileOutputStream fos) {
		try {
			int temp = enabled ? 1:0;
			fos.write((temp + DELIMITER).getBytes());
			temp = erase ? 1:0;
			fos.write((temp + DELIMITER).getBytes());
			fos.write((paint.getColor() + DELIMITER).getBytes());
			fos.write((paint.getStrokeWidth() + DELIMITER).getBytes());
			fos.write((paint.getFlags() + DELIMITER).getBytes());
			//Do I put in blur and other stuff...?
			//Account for the Xfermode using erase
			
			fos.write((points.size() + DELIMITER).getBytes());
			for (int i = 0; i < points.size(); i++) {
				fos.write((points.get(i).x + DELIMITER).getBytes());
				if (i == points.size() - 1) {
					fos.write((points.get(i).y+"").getBytes());
				} else {
					fos.write((points.get(i).y + DELIMITER).getBytes());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void restoreHere(String[] strokeVariables) {
		try {
			int counter = 0;
			enabled = strokeVariables[counter].equals("1");
			erase = strokeVariables[++counter].equals("1");
			paint.setColor(Integer.parseInt(strokeVariables[++counter]));
			paint.setStrokeWidth(Float.parseFloat(strokeVariables[++counter]));
			paint.setFlags(Integer.parseInt(strokeVariables[++counter]));
			if (erase) {
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			}
			
			int size = Integer.parseInt(strokeVariables[++counter]);
			for (int i = 0; i < size; i++) {
				points.add(new Point(Integer.parseInt(strokeVariables[++counter]),Integer.parseInt(strokeVariables[++counter])));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
