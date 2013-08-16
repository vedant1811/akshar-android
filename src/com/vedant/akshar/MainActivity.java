package com.vedant.akshar;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.vedant.akshar.InputWidget.CharDrawnListener;
import com.vedant.akshar.MapDb.Result;
import com.vedant.akshar.Mapper.Flags;

public class MainActivity extends Activity implements InputWidget.CharDrawnListener, OnClickListener{

	private InputWidget inputWidget;
	private EditText outputEditText;
	private ImageView imageView1;
	private Button saveButton, debugButton;
	private ListView listView, listView2;
	private Typeface hindiTf;
	
	private Mapper map;
	private MapDb db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inputWidget = (InputWidget) findViewById(R.id.inputWidget);
		outputEditText = (EditText) findViewById(R.id.outputEditText);
		imageView1 = (ImageView) findViewById(R.id.imageView);
//		imageView2 = (ImageView) findViewById(R.id.imageView2);
		saveButton = (Button) findViewById(R.id.saveButton);
		debugButton = (Button) findViewById(R.id.loadButton);
		listView = (ListView) findViewById(R.id.listView);
		listView2 = (ListView) findViewById(R.id.listView2);
		
		saveButton.setOnClickListener(this);
		debugButton.setOnClickListener(this);
		inputWidget.setCharDrawnListener(this);
		
		inputWidget.requestFocus();
		
		db = new MapDb();
		
		hindiTf = Typeface.createFromAsset(this.getAssets(), "vigyapti.ttf");
		
		changeToUserMode();
	}
	
	private void changeToUserMode(){
		imageView1.setVisibility(View.INVISIBLE);
		listView.setVisibility(View.INVISIBLE);
		saveButton.setVisibility(View.INVISIBLE);
		outputEditText.setTypeface(hindiTf, Typeface.NORMAL);
		outputEditText.setText("");
		debugButton.setText("debug");
	}
	
	private void changeToDebugMode(){
		imageView1.setVisibility(View.VISIBLE);
		listView.setVisibility(View.VISIBLE);
		saveButton.setVisibility(View.VISIBLE);
		outputEditText.setTypeface(Typeface.DEFAULT);
		debugButton.setText("close");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onCharDrawn(PathData data) {
//		RectF bounds = new RectF();
//		path.computeBounds(bounds, true);
//		Log.v("on onCharDrawn", ("("+bounds.right+","+bounds.bottom+")"));
		try {
			map = new Mapper(data);
			imageView1.setImageBitmap(map.getScaledBitmap());
			// imageView1.setScaleType(ScaleType.FIT_START);
			
			
			float timeElapsed = System.nanoTime();
			final ArrayList<Result> verboseTest = db.verboseTest(map);
			timeElapsed = (float) ((System.nanoTime() - timeElapsed) / Math.pow(10,6));
			String output=db.test(map)+"";
			if (verboseTest != null) {
				ArrayAdapter<MapDb.Result> arrayAdapter = new ArrayAdapter<MapDb.Result>(
						this, android.R.layout.simple_list_item_1, verboseTest);
				listView2.setAdapter(arrayAdapter);
				listView2.setVisibility(View.VISIBLE);
				output += timeElapsed + "ms";
			} else {
				output = "not in db";
			}

			final ArrayList<Flags> flagsList = map.listFlags();
			if (flagsList != null) {
				ArrayAdapter<Mapper.Flags> arrayAdapter = new ArrayAdapter<Mapper.Flags>(
						this, android.R.layout.simple_list_item_1, flagsList);
				listView.setAdapter(arrayAdapter);
			}
			// if(map != null){
			// outputEditText.setText( earlierMap.compareWith(newMap) +"");
			// outputEditText.setText( db.test(map, redrawn) );
			// imageView1.setImageBitmap(map.getScaledBitmap());
			// imageView2.setScaleType(ScaleType.FIT_START);
			// }
			if (saveButton.getVisibility() == View.INVISIBLE)  {
				listView.setVisibility(View.INVISIBLE);
				listView2.setVisibility(View.INVISIBLE);
				char result = db.test(map);
				if (result != '\0') {
					SpannableStringBuilder existing = (SpannableStringBuilder) outputEditText.getText();
					if ((data.redrawn || result == '\b') && existing.length() > 0) 
						existing.delete(existing.length() - 1, existing.length());
					if (result != '\b')
						existing.append(Character.toString(result));
				}
			}else
				outputEditText.setText(output);
		} catch (NullPointerException e) {
			if(saveButton.getVisibility() == View.VISIBLE)
				outputEditText.setText("some null pointer exception");
			Log.e("Main Activity", "in Char Drawn", e);
		}
		outputEditText.requestFocus();
	}

	@Override
	public void onClick(View view) {
		if( view.equals(saveButton) ){
			String result = map.saveToStorage( db.createFileToStore(
					outputEditText.getText().toString(), map) );
			outputEditText.setText(result);
			db = new MapDb();
		}
		else if (view.equals(debugButton)) {
//			final String input = outputEditText.getText().toString();
//			if (!input.equals("")) {
//				String fileName = Environment.getExternalStorageDirectory()
//						+ File.separator + "akshar" + File.separator + input;
//				Mapper newMap = new Mapper(fileName);
//				Bitmap bitmap = newMap.getScaledBitmap();
//				if (bitmap != null) {
//					map = newMap;
//					outputEditText.setText("success!");
//					imageView1.setImageBitmap(bitmap);
//				} else
//					outputEditText.setText("failed");
//			}
	 		if(saveButton.getVisibility()==View.VISIBLE)
	 			changeToUserMode();
	 		else
	 			changeToDebugMode();
		}
	}

}
