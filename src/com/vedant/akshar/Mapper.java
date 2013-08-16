package com.vedant.akshar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;

import com.vedant.akshar.InputWidget.CharDrawnListener.PathData;

public class Mapper {
	
	private static final float FLAG_CONFIDENCE_UPSCALE = 5;

	public enum Flags{
		NO_EDGE,
		ONLY_ONE_EDGE,
		BEGINS_WITH_EDGE,
		ENDS_WITH_EDGE,
		ALL_EDGES_ON_TOP,
		ONE_EDGE_BEFORE_MIDWAY,// 5
		ONE_EDGE_MIDWAY,
		ONE_EDGE_AHEAD_MIDWAY,
		FIRST_EDGE_AT_2,
		LAST_EDGE_TOWARDS_END,
		ONE_EDGE_AT_SEC_7,// 10
		
		RIGHT_TO_LEFT,
		FIRST_EDGE_NOT_AT_2,
		NO_OF_FLAGS
		// upto 20 flags can be created without changing the database
	}
		

	void setFlags(int[] edges, float[] dists) {
		flags = new ArrayList<Flags>();
		final int len = edges.length;

		if (rightToLeft(edges))
			flags.add(Flags.RIGHT_TO_LEFT);
		if (len == 0) {
			flags.add(Flags.NO_EDGE);
			return;
		}

		if (len == 1)
			flags.add(Flags.ONLY_ONE_EDGE);
		if (dists[0] < 0.1)
			flags.add(Flags.BEGINS_WITH_EDGE);
		if (dists[len - 1] > 0.9)
			flags.add(Flags.ENDS_WITH_EDGE);
		if (allEdgesOnTop(edges))
			flags.add(Flags.ALL_EDGES_ON_TOP);
		for (float dist : dists) {
			if (dist > 0.3 && dist < 0.5)
				flags.add(Flags.ONE_EDGE_BEFORE_MIDWAY);
			else if (dist > 0.5 && dist < 0.6)
				flags.add(Flags.ONE_EDGE_MIDWAY);
			else if (dist > 0.6 && dist < 0.8)
				flags.add(Flags.ONE_EDGE_AHEAD_MIDWAY);
		}
		if (edges[0] == 2)
			flags.add(Flags.FIRST_EDGE_AT_2);
		else
			flags.add(Flags.FIRST_EDGE_NOT_AT_2);
		if (dists[len - 1] > 0.9)
			flags.add(Flags.LAST_EDGE_TOWARDS_END);
		for (int edge : edges)
			if (edge == 7) {
				flags.add(Flags.ONE_EDGE_AT_SEC_7);
				break;
			}
		Collections.sort(flags);
	}
	
	public int flagsToInt(){
		int n=0;
		for(Flags flag : flags)
			n=n*20+flag.ordinal();
		return n;
	}
	
	public String flagsToString(){
		StringBuilder flags = new StringBuilder();
		Iterator<Flags> i = this.flags.iterator();
		while(true){
			flags.append(i.next().ordinal());
			if(i.hasNext())
				flags.append(',');
			else
				break;
		}
		return flags.toString();
	}

	public float confidenceOfFlags(CharMap other) {
		
		if( (other.flags[0]==1||charMap.flags[0]==1)// i.e. either has only 1 edge
				&& other.flags[0]!=charMap.flags[0])
			return 0;
		
		try {
			if (other.flags[0]==1&&charMap.flags[0]==1
					&& other.flags[1]!=charMap.flags[1])
				return 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			// do nothing
		}
		
		float n = 0;
		for (int flag : other.flags) {
			if (Arrays.binarySearch(charMap.flags, flag)>=0)
				n++;
		}
		return n/Math.max(other.flags.length, charMap.flags.length);
	}

	public ArrayList<Flags> listFlags() {
		return flags;
	}

	private boolean allEdgesOnTop(int[] edges) {
		for (int edge : edges) {
			if (edge > 2)
				return false;
		}
		return true;
	}
	
	private boolean rightToLeft(int[] edges){
		if((startEndQuad%4)%2!=1||(startEndQuad/4)%2!=0)
			return false;
		for(int i=0; i<edges.length-1;i++)
			if(edges[i]%4<edges[i+1]%4)
				return false;
		return true;
	}

	private boolean sectorCorrespondsToQuad(int edge, int quad) {
		switch (quad) {
		case 0:
			if (edge == 0 || edge == 1 || edge == 3)
				return true;
			break;
		case 1:
			if (edge == 1 || edge == 2 || edge == 5)
				return true;
			break;
		case 2:
			if (edge == 3 || edge == 6 || edge == 7)
				return true;
			break;
		case 3:
			if (edge == 7 || edge == 8 || edge == 5)
				return true;
		}
		return false;
	}

	public static final int SIDE_OF_BITMAP = 8;
	public static final String USER_GENERATED = "user";
	
	private Bitmap standardBitmap = null;// may be null
	private CharMap charMap = null;
	private int startEndQuad;
	private ArrayList<Flags> flags = null;
	private boolean redrawn;
//	public PathData data = null;
	
//	public Mapper(String filePath){
//		standardBitmap = BitmapFactory.decodeFile( filePath );
//		if(standardBitmap != null)
//			charMap = new CharMap(standardBitmap);
//	}
//	
	public Mapper(PathData data){
		
		Paint canvasPaint = new Paint();
		canvasPaint.setAntiAlias(false);
		canvasPaint.setColor(Color.BLACK);
		canvasPaint.setStyle(Style.STROKE);
		
		RectF bounds = new RectF();
		data.path.computeBounds(bounds, true);
		if(bounds.height()/bounds.width()<0.1 && rightToLeft(data.edges)){
			charMap = new CharMap("backspace");
			return;
		}
		
		int height,width;
		if(bounds.height()>bounds.width()){
			height=SIDE_OF_BITMAP;
			width=(int) Math.ceil(SIDE_OF_BITMAP*bounds.width()/bounds.height());
		}else{
			width=SIDE_OF_BITMAP;
			height=(int) Math.ceil(SIDE_OF_BITMAP*bounds.height()/bounds.width());
		}
		RectF dst = new RectF(0,0,width,height);
		Matrix transMatrix = new Matrix();
		transMatrix.setRectToRect(bounds, dst, ScaleToFit.START);
		data.path.transform(transMatrix);
		
		try {
			Bitmap smallBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(smallBitmap);
			canvas.drawPath(data.path, canvasPaint);
			standardBitmap = Bitmap.createScaledBitmap(smallBitmap, SIDE_OF_BITMAP, SIDE_OF_BITMAP, true);
			
//			this.data = data;
			startEndQuad = data.startEndQuad;
			setFlags(data.edges, data.distOfEdges);
			redrawn = data.redrawn;
			charMap = new CharMap(standardBitmap,flags);
		} catch (IllegalArgumentException e) {
			// class variables are null
		}
	}
	
	public int otherDataToInt(){
		if(redrawn)
			return -1;
		return startEndQuad;
	}
	
	public float compareWith(CharMap other){
		if(other == null||charMap==null)
			return 0;
		float confidence = 0;
		HashSet<Integer> min,max;
		if( charMap.map.size() > other.map.size() ){
			max = charMap.map;
			min = other.map;
		}
		else{
			max = other.map;
			min = charMap.map;
		}
		
		for(int pos : min)
			if(max.contains(pos))
				confidence++;
		confidence/=max.size();
		confidence+=confidenceOfFlags(other)/Flags.NO_OF_FLAGS.ordinal()*FLAG_CONFIDENCE_UPSCALE;
		return confidence;
	}
	
	public String saveToStorage(File file){
		try{
			OutputStream out = new FileOutputStream(file);
			boolean result = standardBitmap.compress(CompressFormat.PNG, 100, out);
			out.close();
			if( result )
				return file.getPath();
			else
				return "error compressing";
		}
		catch (FileNotFoundException e){
			Log.e("save", "cannot save file", e);
			return "File not found exception";
		}
		catch (IOException e){
			Log.e("save", "IO error", e);
			return "IO Exception";
		}
	}
	
//	public boolean loadFileFromStorage(String name){
//		File fileName = new File( storageDir, name + ".png");
//		if( !fileName.exists() )
//			return false;
//		Bitmap bitmapFromFile = BitmapFactory.decodeFile( fileName.getPath() );
//		if( bitmapFromFile==null )
//			return false;
//		standardBitmap=bitmapFromFile;
//		return true;
//	}
	
	public Bitmap getBitmap(){
		return standardBitmap;
	}

	public Bitmap getScaledBitmap(){
		if( standardBitmap != null )
			return Bitmap.createScaledBitmap(standardBitmap, 128, 128, false);
		else
			return null;
	}
	
	public String getName(){
		return charMap.name;
	}
	
//	public Bitmap getLargeBitmap(){
//		if(largeBitmap != null)
//			return largeBitmap;
//		else
//			return null;
//	}

}
