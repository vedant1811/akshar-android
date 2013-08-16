package com.vedant.akshar;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/*
 * This widget takes touch input from a user and displays the same (as a line) instantly.
 * Once a shape is complete (i.e. touch lifted) it returns a trimmed shape to its listener
 */
public class InputWidget extends View {
	private static final int MAIN_PATH_STROKE_WIDTH = 3,
								NO_OF_NEAR_POINTS_TOLERANCE = 3,
								POINT_NEAR_TOLERANCE = 5,
								BATCH_TOLERANCE = 3,
								RADIUS = 4;
	private static final double ANGLE_TOLDERANCE = 4*Math.PI/10;
//	private static final int SIDE_INDEX = POINT_NEAR_TOLERANCE*2-1;
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "InputWidget";
	Path path, otherPath, greyPath, redPath;
//	ArrayList<Float> edges;
	ArrayList<Point> points;
	private Paint pathPaint, greyPaint, redPaint;
	private CharDrawnListener charDrawnListener;
	
	private RectF rectAroundPath;
	final float scaleRectTop;
//	int pixels[];
	
//	boolean strokeComplete;
	boolean redrawn;
	
//	private Paint greyPaint;
//	private Path greyPath;
	
	WidgetTimer timer;
//	private Bitmap bitmap;
	private Matrix transMatrix;
	private RectF dst;
	
	public interface CharDrawnListener{
		
		public class PathData{
			public final Path path;
			public final int startEndQuad;// = startQuad + 4*endQuad. both are zero indexed
			public final int[] edges;// in 9 sector form
			public final float[] distOfEdges;
			public final boolean redrawn;
			
			
//			private static final RectF dst= new RectF(0,0, Mapper.SIDE_OF_BITMAP, Mapper.SIDE_OF_BITMAP);
//			private static final Matrix transMatrix = new Matrix();
			private static final RectF rectAroundPath = new RectF();
			
			private PathData(InputWidget outer){
				path = new Path(outer.path);
				path.computeBounds(rectAroundPath, true);
				
				redrawn = outer.redrawn;
				int startQuad = quad(outer.points.get(0));
				startEndQuad= startQuad+ 4*quad(outer.points.get(outer.points.size()-1));
				
				ArrayList<ArrayList<Integer>> batches = new ArrayList<ArrayList<Integer>>();
				
				final int nPoints = outer.points.size();
				int lastIndexAdded = 0;
				for(int curPointIndex=1;curPointIndex<outer.points.size()-2;curPointIndex++){
					int n=0;
					for(int j=curPointIndex+1;j<outer.points.size()-1;j++){
						if(outer.points.get(curPointIndex).isNear(outer.points.get(j)))
							n++;
					}
					if(outer.points.get(curPointIndex).angleDiff(outer.points.get(curPointIndex+1),outer.points.get(curPointIndex-1))>
						ANGLE_TOLDERANCE || n> NO_OF_NEAR_POINTS_TOLERANCE){
						ArrayList<Integer> batch;
						Log.v(TAG, "index: "+curPointIndex+"/"+nPoints+"="+curPointIndex/(float)nPoints);
						
						if(	lastIndexAdded==0 ||	(curPointIndex-lastIndexAdded>BATCH_TOLERANCE &&
						sector(outer.points.get(curPointIndex))!=sector(outer.points.get(lastIndexAdded)))){
							batch = new ArrayList<Integer>();
							batches.add(batch);
						}else
							batch = batches.get(batches.size()-1);
						batch.add(curPointIndex);
						lastIndexAdded=curPointIndex;
					}
				}
//				edges = new int[sectors.size()];
//				int nEdges =0;
//				distOfEdges = new float[sectors.size()];
//				for(int i=0;i<9;i++){
//					ArrayList<Integer> sector = sectors.get(i);
//					if(sector != null){
//						edges[nEdges]=i;
//						distOfEdges[nEdges]=sector.get(0)/nPoints;
//						Log.d(TAG,edges[nEdges]+" at a dist of "+distOfEdges[nEdges]);
//						nEdges++;
//					}
//				}
				
				edges = new int[batches.size()];
				distOfEdges = new float[batches.size()];
				for(int i=0;i<edges.length;i++){
					final ArrayList<Integer> batch = batches.get(i);
					edges[i] = sector(outer.points.get(batch.get(batch.size()/2)));// median's sector
					float sum=0;
					for(int index : batch)
						sum+=index;
					distOfEdges[i] = sum/batch.size()/nPoints;// avg dist
					Log.d(TAG,edges[i]+" at a dist of "+distOfEdges[i]);
				}
			}
			
			private int quad(Point p){
				int quad = p.x<(rectAroundPath.left + rectAroundPath.width()/2)?0:1;
				quad += p.y<(rectAroundPath.top + rectAroundPath.height()/2)?0:2;
				return quad;
			}
			private int sector(Point p){
				int sec=trisection(p.x-rectAroundPath.left, rectAroundPath.width());
				sec+=trisection(p.y-rectAroundPath.top, rectAroundPath.height())*3;
				return sec;
			}
			private static int trisection(float point, float line){
				float ratio=point/line;
				if(ratio<1/3f)
					return 0;
				else if(ratio<2/3f)
					return 1;
				else return 2;
			}
		}
		
		public void onCharDrawn(PathData data);
	}
	
	private class WidgetTimer extends CountDownTimer{

		public WidgetTimer(int interval) {
			super(interval, interval+1);
		}

		@Override
		public void onFinish() {
//			strokeComplete = true;
//			charDrawn(false);
			redrawn = false;
			path.reset();
			points.clear();
			greyPath.reset();
			redPath.reset();
			invalidate();
		}

		@Override
		public void onTick(long millisUntilFinished) {
		}
		
	}

	public InputWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDrawingCacheEnabled(true);
		setFocusableInTouchMode(true);
		path = new Path();
		otherPath = new Path();
		
		pathPaint = new Paint();
		pathPaint.setColor(Color.BLUE);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeWidth(MAIN_PATH_STROKE_WIDTH);
		pathPaint.setAntiAlias(true);
		
		charDrawnListener = null;
		
		rectAroundPath = new RectF();
		scaleRectTop = (float) (getHeight()*0.05);
//		pixels = new int [(TOLERANCE*2-1)*4];
		
		timer = new WidgetTimer(500);
		transMatrix = new Matrix();
		dst = new RectF();
		
		greyPaint = new Paint();
		greyPaint.setColor(Color.GRAY);
		greyPaint.setStyle(Paint.Style.STROKE);
		greyPaint.setStrokeWidth(0);
		greyPath = new Path();
		
		redPaint = new Paint();
		redPaint.setColor(Color.RED);
		redPaint.setStyle(Style.STROKE);
		redPaint.setStrokeWidth(0);
		redPath = new Path();
		
//		edges = new ArrayList<Float> (4);
		points = new ArrayList<Point> (70);
	}
	
	public void setCharDrawnListener(CharDrawnListener listener){
		charDrawnListener = listener;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event){

		final float x = event.getX();
		final float y = event.getY();
		final int xint = (int)x,yint=(int)y;
		switch(event.getActionMasked()){
		case MotionEvent.ACTION_DOWN:
			if (!path.isEmpty()) {// means timer has not fired
				timer.cancel();
				path.computeBounds(rectAroundPath, true);
				rectAroundPath.top -= rectAroundPath.height() * scaleRectTop;
				if (!rectAroundPath.contains(x, y) || redrawn)
					timer.onFinish();
				else
					redrawn = true;
			}
			path.moveTo(x, y);
			points.add(new Point(xint, yint));
			break;
		case MotionEvent.ACTION_MOVE:
			path.lineTo(x, y);
			if(!points.get(points.size()-1).equals(xint, yint))
				points.add(new Point(xint, yint));
			break;
		case MotionEvent.ACTION_UP:
			path.lineTo(x, y);
			if(!points.get(points.size()-1).equals(xint, yint))
				points.add(new Point(xint, yint));
			charDrawn();
			if(!redrawn)
				timer.start();
			else{
				redrawn=false;
				timer.onFinish();
			}
		}
		
//		path.computeBounds(rectAroundPath, true);
////		shrinkRect(0.005f);
//		
//		path.computeBounds(rectAroundPath, true);
//		greyPath.reset();
		
//		
//		int requiredColor;
//		if(!rectAroundPath.contains(event.getX(),event.getY()))
//			requiredColor = Color.GRAY;
////			Log.v(TAG, "GRAY");
//		else
//			requiredColor = Color.RED;
//		if( greyPaint.getColor() != requiredColor){
//			if (requiredColor == Color.RED) {
//				Log.d(TAG, "changed at " + event.getX() + " , " + event.getY());
//				redPath.addCircle(event.getX(), event.getY(), 3,
//						Path.Direction.CCW);
//				edges.add(event.getX());
//				edges.add(event.getY());
//			}
//			greyPaint.setColor(requiredColor);
//		}
//			
////			Log.v(TAG, "RED");
		invalidate();
		return true;
	}
	
	private void charDrawn() {
//		strokeComplete = redrawn;
		path.computeBounds(rectAroundPath, true);
		if(rectAroundPath.height()*rectAroundPath.width()<64)
			return;
		path.addCircle(points.get(points.size()-1).x, points.get(points.size()-1).y,
				MAIN_PATH_STROKE_WIDTH, Direction.CCW);
		
//		greyPath.reset();
//		greyPath.addRect(rectAroundPath , Path.Direction.CCW);
//		rectAroundPath.left+=rectAroundPath.width()/3f;
//		rectAroundPath.right-=rectAroundPath.width()/3f;
//		greyPath.addRect(rectAroundPath, Direction.CCW);
//		path.computeBounds(rectAroundPath, true);
//		rectAroundPath.top+=rectAroundPath.height()/3f;
//		rectAroundPath.bottom-=rectAroundPath.height()/3f;
//		greyPath.addRect(rectAroundPath, Direction.CCW);
		
//		else{
//			pos = quad(x_i,y_i);
//			pos+= 4*quad(x_f,y_f);
//			Iterator<Float> i = edges.iterator();
//			String quads = "";
//			while(i.hasNext()){
//				int quad = quad(i.next(), i.next());
//				quads+=quad+",";
//			}
//			Log.d(TAG, quads);
//			edges.clear();
//		}
		Log.d(TAG, "no of points: "+points.size());
		for(int i=0;i<points.size()-1;i++){
			int n=0;
			for(int j=i+1;j<points.size();j++){
				if(points.get(i).isNear(points.get(j)))
					n++;
			}
			if (n > NO_OF_NEAR_POINTS_TOLERANCE){
				greyPath.addCircle(points.get(i).x, points.get(i).y, RADIUS+1, Direction.CCW);
				Log.v(TAG, i+"("+points.get(i).x+","+points.get(i).y+")"+"is near after "+n+" points");
			}
		}
		for(int i=1;i<points.size()-1;i++){
			if(points.get(i).angleDiff(points.get(i+1),points.get(i-1))>ANGLE_TOLDERANCE){
				redPath.addCircle(points.get(i).x, points.get(i).y, RADIUS-1, Direction.CCW);
				Log.v(TAG, i+"("+points.get(i).x+","+points.get(i).y+")"+"changes direction");
			}
		}
		invalidate();
		charDrawnListener.onCharDrawn(new CharDrawnListener.PathData(this));
	}
	
	private class Point extends android.graphics.Point{
		private Point(int x, int y) {
			super(x, y);
		}
		
		private boolean isNear(Point other){
			if(Math.abs(x-other.x)<POINT_NEAR_TOLERANCE&&Math.abs(y-other.y)<POINT_NEAR_TOLERANCE)
				return true;
			else
				return false;
		}
		
		private double angleDiff(Point next, Point prev){
			double angle1= Math.atan2(y-prev.y,x-prev.x);
			double angle2= Math.atan2(next.y-y,next.x-x);
			return Math.abs(angle2-angle1);
		}
	}

	@Override
	protected void onDraw(Canvas canvas){
		canvas.drawPath(path, pathPaint);
//		greyPath.rMoveTo(-rectAroundPath.width()/2, 0);
//		greyPath.rLineTo(0, rectAroundPath.height());
//		greyPath.rMoveTo(-rectAroundPath.width()/2, -rectAroundPath.height()/2);
//		greyPath.rLineTo(rectAroundPath.width(), 0);
//		
		canvas.drawPath(greyPath, greyPaint);
		canvas.drawPath(redPath, redPaint);
//		canvas.drawPath(otherPath, redPaint);
//		path.computeBounds(rectAroundPathOLD, true);
	}
	
	private void shrinkRect(float scale){
		rectAroundPath.top+=rectAroundPath.height()*scale;
		rectAroundPath.bottom-=rectAroundPath.height()*scale;
		rectAroundPath.left+=rectAroundPath.width()*scale;
		rectAroundPath.right-=rectAroundPath.width()*scale;
	}
}
