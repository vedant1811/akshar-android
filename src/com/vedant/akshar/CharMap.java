package com.vedant.akshar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import com.vedant.akshar.Mapper.Flags;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class CharMap {
	private static final int SIDE_OF_BITMAP = 8;
	
	public HashSet<Integer> map;
	public final String name;
	public final int[] flags;
	
	public CharMap(File file) {
		String[] parts = file.getName().split("_");
		name = file.getName().replace(".png", "");
		String[] flagsInString = parts[2].split("\\.")[0].split(",");
		flags = new int[flagsInString.length];
		for (int i = 0; i < flagsInString.length; i++)
			flags[i] = Integer.parseInt(flagsInString[i]);

		Bitmap standardBitmap = BitmapFactory.decodeFile(file.getPath());
		if (standardBitmap != null) {
			drawPixelMap(standardBitmap);
			Log.v("Char Map", name + " mapped");
		} else
			Log.e("Char Map", name + " not mapped");
	}
	
	public CharMap(String name){
		this.name = name;
		flags = null;
	}

	public CharMap(Bitmap standardBitmap, ArrayList<Flags> flagList) {
		name = Mapper.USER_GENERATED;
		flags = new int[flagList.size()];
		for(int i=0;i<flags.length;i++)
			flags[i]=flagList.get(i).ordinal();
		drawPixelMap(standardBitmap);
	}
	
	private void drawPixelMap(Bitmap standardBitmap){
		int[] pixels = new int[SIDE_OF_BITMAP*SIDE_OF_BITMAP];
		map = new HashSet<Integer> ();
		standardBitmap.getPixels(pixels, 0, SIDE_OF_BITMAP, 0, 0, SIDE_OF_BITMAP, SIDE_OF_BITMAP);
		for(int i=0; i<pixels.length;i++ ){
			if( pixels[i] != 0)
				map.add(i);
		}
	}
	
	public String prettyName(){
		return name.split("_")[0];
	}
}
