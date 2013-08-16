package com.vedant.akshar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

public class MapDb {
	private final File storageDir;

	int numOfPixels;
	SparseArray<CharMap[]> mapsGroup;

	MapDb() {
		storageDir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "akshar");
		if (!storageDir.isDirectory()) {
			storageDir.mkdir();
			return;
		}
		File[] fileList = storageDir.listFiles();
		mapsGroup = new SparseArray<CharMap[]>();

		for (File file : fileList) {
			if (file.isDirectory())
				parseDir(file);
		}
	}

	private final void parseDir(File dir) {
		File[] files = dir.listFiles();
		int pos = Integer.parseInt(dir.getName());
		HashSet<CharMap> fileSet = new HashSet<CharMap>();
		for (File file : files) {
			CharMap charMap = new CharMap(file);
			if (charMap.map != null)
				fileSet.add(charMap);
		}
		mapsGroup.put(pos, fileSet.toArray(new CharMap[fileSet.size()]));
	}

	public class Result implements Comparable<Result> {
		float confidence;
		String fileName;

		public Result(float confidence, String fileName) {
			this.confidence = confidence;
			this.fileName = fileName;
		}

		@Override
		public int compareTo(Result another) {
			return -Float.compare(confidence, another.confidence);
		}

		@Override
		public String toString() {
			return fileName.replace(".png", "") + "\t: " + confidence;
		}

	}

	public ArrayList<Result> verboseTest(Mapper pixMap) {
		CharMap[] maps = mapsGroup.get(pixMap.otherDataToInt());
		ArrayList<Result> results = new ArrayList<Result>();
		if (maps == null)
			return null;
		for (CharMap map : maps) {
			results.add(new Result(pixMap.compareWith(map), map.name));
		}
		Collections.sort(results);
		return results;
	}

	public char test(Mapper pixMap) {
		float confidence;
		String name=pixMap.getName();
		if (name.equals(Mapper.USER_GENERATED)) {
			CharMap[] maps = mapsGroup.get(pixMap.otherDataToInt());
			if (maps == null)
				return '\0';
			confidence = 0;
			float max = 0;
			name = "";
			for (CharMap map : maps) {
				confidence = pixMap.compareWith(map);
				if (confidence > max) {
					max = confidence;
					name = map.prettyName();
				}
			}
		}
		else confidence =100;

		if (confidence > 0.1) {
			if (name.equalsIgnoreCase("backspace"))
				return '\b';
			if (name.charAt(0) == 'h')
				return toHindiChar(Integer.parseInt(name.substring(1, 2)));
			else
				return name.charAt(0);
		} else
			return '\0';
	}

	public File createFileToStore(String chrName, Mapper map) {
		int n = 0;
		File dir = new File(storageDir + File.separator + map.otherDataToInt());
		if (!dir.isDirectory() && !dir.mkdir())
			Log.e("map db", "error creating " + dir.getPath());
		File file;
		do {
			n++;
			file = new File(dir, chrName + "_" + n + "_" + map.flagsToString()
					+ ".png");
		} while (file.exists());
		return file;
	}

	public static char toHindiChar(int n) {
		switch (n) {
		case 0:
			return '०';
		case 1:
			return '१';
		case 2:
			return '२';
		case 3:
			return '३';
		case 4:
			return '४';
		case 5:
			return '५';
		case 6:
			return '६';
		case 7:
			return '७';
		case 8:
			return '८';
		case 9:
			return '९';
		}
		return '0';
	}
}
