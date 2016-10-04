package benl.student.androidjournal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class JournalPage extends Activity {
	private static final String TAG = "JournalPage";
	private static final String TEMP_FILE = "temp_file";
	private static final String SECTION_DELIMITER = ";/;";

	private DrawPage page;
	private LinearLayout buttonll;
	private LinearLayout ll;
	//	private ToggleButton tbButton;
	private Button button;
	private boolean holdPan;

	//On startup-------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(createLayout());
		PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
	}

	private LinearLayout createLayout() {
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		page = new DrawPage(this);

		//New Stuff BackgroundPage

		buttonll = new LinearLayout(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
		page.setLayoutParams(lp);
		LinearLayout.LayoutParams buttonLP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);

		buttonll.addView(createButton(0, buttonLP));
		buttonll.addView(createButton(1, buttonLP));
		buttonll.addView(createButton(2, buttonLP));
		buttonll.addView(createButton(3, buttonLP));
		buttonll.addView(createButton(4, buttonLP));
		buttonll.addView(createButton(buttonLP));
		//		buttonll.addView(createToggle(buttonLP));

		int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
		LinearLayout.LayoutParams llLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pixels);
		buttonll.setLayoutParams(llLP);

		multiTouch(ll);
		ll.addView(page);
		ll.addView(buttonll);

		return ll;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void multiTouch(LinearLayout ll) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ll.setMotionEventSplittingEnabled(true);
		}
	}

	private ImageButton createButton(final int index, LinearLayout.LayoutParams buttonLP) {
		final ImageButton button = new ImageButton(this);
		button.setContentDescription("Button");
		button.setAdjustViewBounds(false);
		button.setScaleType(ScaleType.CENTER_INSIDE);

		switch (index) {
		case 0:
			button.setImageResource(R.drawable.draw);
			break;
		case 1:
			button.setImageResource(R.drawable.color);
			break;
		case 2:
			button.setImageResource(R.drawable.eraser);
			break;
		case 3:
			button.setImageResource(R.drawable.undo);
			break;
		case 4:
			button.setImageResource(R.drawable.redo);
			break;
		default:
			break;
		}
		View.OnClickListener writeClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (index) {
				case 0:
					if (page.isDraw()) {
						alertStroke();
					} else {
						//						tbButton.setChecked(false);
						JournalPage.this.button.setTextAppearance(JournalPage.this, R.style.normalText);
						page.setDraw();
					}
					break;
				case 1:
					//ColourPicker
					//new ColourPickerDialog(JournalPage.this, JournalPage.this, page.getPaintColour()).show();
					//					alertColourPicker();
					if (ll.getChildCount() < 3) {
						ll.addView(colourSelector(), 1);
					} else {
						alertColourPicker();
					}
					break;
				case 2:
					//Eraser
					if (page.isEraser()) {
						alertEraser();
					} else {
						//						tbButton.setChecked(false);
						JournalPage.this.button.setTextAppearance(JournalPage.this, R.style.normalText);
						page.setEraser();
					}
					break;
				case 3:
					//Undo
					page.undo();
					break;
				case 4:
					//Redo
					page.redo();
					break;

				default:
					break;
				}
			}
		};
		button.setOnClickListener(writeClick);


		button.setLayoutParams(buttonLP);
		return button;
	}

	private Button createButton(LinearLayout.LayoutParams buttonLP) {
		button = new Button(this);
		button.setText("Pan");
		button.setLayoutParams(buttonLP);
		holdPan = false;
		View.OnTouchListener writeTouch = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					if (holdPan || !page.isPan()) {
						button.setTextAppearance(JournalPage.this, R.style.boldText);
						page.setPan(true);
					} else {
						button.setTextAppearance(JournalPage.this, R.style.normalText);
						page.setPan(false);
					}
					break;

				case MotionEvent.ACTION_UP:
					if (holdPan) {
						button.setTextAppearance(JournalPage.this, R.style.normalText);
						page.setPan(false);
					}
					break;
				}
				return true;
			}
		};
		button.setOnTouchListener(writeTouch);
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		//		button.setBackgroundResource(android.R.drawable.list_selector_background);
		return button;
	}


	private HorizontalScrollView colourSelector() {
		int[] colours = new int[] {DrawPage.BLACK, DrawPage.WHITE,DrawPage.BLUE, DrawPage.GREEN, DrawPage.RED, 
				DrawPage.ORANGE, DrawPage.PURPLE, DrawPage.TURQUOISE, DrawPage.YELLOW, DrawPage.DARK_BLUE};
		HorizontalScrollView hScrollView = new HorizontalScrollView(this);
		LinearLayout coloursll = new LinearLayout(this);


		Button button = new Button(this);
		int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
		LinearLayout.LayoutParams buttonLP = new LinearLayout.LayoutParams(
				pixels, LinearLayout.LayoutParams.MATCH_PARENT);
		button.setLayoutParams(buttonLP);
		View.OnClickListener closeClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ll.removeViewAt(1);
			}
		};
		button.setOnClickListener(closeClick);
		coloursll.addView(button);
		for (int i = 0; i < colours.length; i++) {
			coloursll.addView(createButton(colours[i]));
		}


		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pixels);
		hScrollView.setLayoutParams(lp);
		hScrollView.addView(coloursll);

		return hScrollView;
	}

	private Button createButton(final int colour) {
		final Button button = new Button(this);
		int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
		LinearLayout.LayoutParams buttonLP = new LinearLayout.LayoutParams(
				pixels, LinearLayout.LayoutParams.MATCH_PARENT);
		button.setBackgroundColor(colour);
		View.OnClickListener colourClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setColour(colour);
			}
		};
		button.setOnClickListener(colourClick);

		button.setLayoutParams(buttonLP);
		return button;
	}

	private void setColour(int colour) {
		page.setPaintColour(colour);
	}

	@Override
	protected void onStart() {
		super.onStart();
		page.setSettings(this);
		buttonll.setBackgroundColor(page.getBackgroundColour());
		//		tbButton.setTextColor(page.getDrawColour());
		button.setTextColor(page.getDrawColour());
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		holdPan = sharedPref.getBoolean(SettingsActivity.PAN, false);
		
		restoreSession();
		page.invalidate();
	}

	//Menu-------------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_journal_page, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			switchPage(SettingsActivity.class);
			return true;

		case R.id.menu_save:
			save();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void switchPage(Class<?> cls) {
		startActivity(new Intent(this, cls));
	}

	private void save() {
		// Saves totals to score file for score sheet
		int itemNumber = 0;
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File file = new File(path, "name_" + itemNumber+".png");
		while (file.exists()) {
			file = new File(path, "name_" + ++itemNumber +".png");
		}
//		String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/name_" + itemNumber+".png";

		try {
			OutputStream os = new FileOutputStream(file);
			
//			page.setDrawingCacheEnabled(true);
//			page.getDrawingCache().compress(CompressFormat.PNG, 100, os);
			page.save().compress(CompressFormat.PNG, 100, os);
			
			os.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "save: FileNotFoundException" + e.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void tempSave() {
		// Saves totals to score file for score sheet
		String fileName = TEMP_FILE;

		try {
			FileOutputStream fos;
			fos = openFileOutput(fileName, Context.MODE_PRIVATE);
			fos.write((SECTION_DELIMITER).getBytes());// Splits off the first section which shall contain information (what...? time, date, name etc...?)
			page.savePage(fos);
			fos.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "save: FileNotFoundException" + e.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void restoreSession() {
		final String fileName = TEMP_FILE;

		File file = new File(getFilesDir(), fileName);
		if (!file.exists()) {
  			Log.d(TAG, "restoreSession: File not found");
			return;
		}
  		byte[] buffer = new byte[1024];
  		StringBuffer fileContent = new StringBuffer("");
  		String fileData;
  		
  		FileInputStream fis;
  		try {
  			fis = openFileInput(fileName);
  			while ((fis.read(buffer)) != -1) {
  				fileContent.append(new String(buffer));
  			}
  			fis.close();
  			
  			fileData = fileContent.substring(0,fileContent.lastIndexOf(SECTION_DELIMITER));

  			String[] temp = fileData.split(SECTION_DELIMITER);
  			Log.d(TAG, "fileData = " + fileData);
  			Log.d(TAG, "temp.length = " + temp.length);
  			if (temp.length == 4) {
				restoreHere(temp[0]);
				page.restoreHere(temp);
			}
  			
  		} catch (FileNotFoundException e) {
  			Log.e(TAG, "restoreSession: FileNotFoundException: " + e.toString());
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
	}
	
	private void restoreHere(String here) {
		//Any extra information for restoration.
		
	}
	
	//Alerts-----------------------------------------------------------------------------
	private void alertColourPicker() {
		//Alert popup to select spot size
		new AlertDialog.Builder(this)
		.setView(colourPickerLayout())
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Colour Picker")
		.setMessage("Select the colour you want")
		.setPositiveButton("Okay", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog alert = (AlertDialog) dialog;
				SeekBar seekBarR = (SeekBar) alert.findViewById(R.id.seekBarR);
				SeekBar seekBarG = (SeekBar) alert.findViewById(R.id.seekBarG);
				SeekBar seekBarB = (SeekBar) alert.findViewById(R.id.seekBarB);
				SeekBar seekBarO = (SeekBar) alert.findViewById(R.id.seekBarO);
				Log.d(TAG, "seekBarR.getProgress()" + seekBarR.getProgress());
				Log.d(TAG, "seekBarG.getProgress()" + seekBarG.getProgress());
				Log.d(TAG, "seekBarB.getProgress()" + seekBarB.getProgress());
				Log.d(TAG, "seekBarO.getProgress()" + seekBarO.getProgress());
				page.setPaintColour(Color.argb(seekBarO.getProgress(), seekBarR.getProgress(), 
						seekBarG.getProgress(), seekBarB.getProgress()));
			}

		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	private View colourPickerLayout() {
		View view = getLayoutInflater().inflate(R.layout.alert_colour_picker,null);
		final SeekBar seekBarR = (SeekBar) view.findViewById(R.id.seekBarR);
		final SeekBar seekBarG = (SeekBar) view.findViewById(R.id.seekBarG);
		final SeekBar seekBarB = (SeekBar) view.findViewById(R.id.seekBarB);
		final SeekBar seekBarO = (SeekBar) view.findViewById(R.id.seekBarO);
		final View viewColour = view.findViewById(R.id.viewColour);

		//Handle current paint colour
		int colour = page.getPaintColour();
		viewColour.setBackgroundColor(colour);
		seekBarR.setProgress(Color.red(colour));
		seekBarG.setProgress(Color.green(colour));
		seekBarB.setProgress(Color.blue(colour));
		seekBarO.setProgress(Color.alpha(colour));

		//Listening to changes in the SeekBars
		SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				viewColour.setBackgroundColor(Color.argb(seekBarO.getProgress(), 
						seekBarR.getProgress(), seekBarG.getProgress(), seekBarB.getProgress()));
			}
		};
		seekBarR.setOnSeekBarChangeListener(seekBarListener);
		seekBarG.setOnSeekBarChangeListener(seekBarListener);
		seekBarB.setOnSeekBarChangeListener(seekBarListener);
		seekBarO.setOnSeekBarChangeListener(seekBarListener);

		return view;
	}

	private void alertStroke() {
		//Alert popup to select spot size
		new AlertDialog.Builder(this)
		.setView(strokeLayout())
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Pen Size")
		.setMessage("Select the Pen Size")
		.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog alert = (AlertDialog) dialog;
				SeekBar seekBarStroke = (SeekBar) alert.findViewById(R.id.seekBarStroke);
				page.setStrokeWidth(seekBarStroke.getProgress()+2);
			}

		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	private View strokeLayout() {
		View view = getLayoutInflater().inflate(R.layout.alert_stroke,null);
		final SeekBar seekBarStroke = (SeekBar) view.findViewById(R.id.seekBarStroke);
		final LinearLayout viewll = (LinearLayout) view.findViewById(R.id.viewll);

		final StrokeView strokeView = new StrokeView(this);
		strokeView.setColour(page.getPaintColour());
		strokeView.setRadius((int) page.getStrokeWidth());

		viewll.addView(strokeView);
		seekBarStroke.setProgress((int) page.getStrokeWidth()-2);

		//Listening to changes in the SeekBars
		SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				strokeView.setRadius(seekBar.getProgress()+2);
			}
		};
		seekBarStroke.setOnSeekBarChangeListener(seekBarListener);

		return view;
	}


	private void alertEraser() {
		//Alert popup to select spot size
		new AlertDialog.Builder(this)
		.setView(eraserLayout())
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Eraser Size")
		.setMessage("Select the Eraser Size")
		.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog alert = (AlertDialog) dialog;
				SeekBar seekBarStroke = (SeekBar) alert.findViewById(R.id.seekBarStroke);
				CheckBox checkBoxStroke = (CheckBox) alert.findViewById(R.id.checkBoxStroke);

				page.setEraserWidth(seekBarStroke.getProgress()+2);
				page.setEraserStroke(checkBoxStroke.isChecked());
			}

		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	private View eraserLayout() {
		View view = getLayoutInflater().inflate(R.layout.alert_eraser,null);
		final SeekBar seekBarStroke = (SeekBar) view.findViewById(R.id.seekBarStroke);
		final LinearLayout viewll = (LinearLayout) view.findViewById(R.id.viewll);
		CheckBox checkBoxStroke = (CheckBox) view.findViewById(R.id.checkBoxStroke);
		checkBoxStroke.setChecked(page.isErasingStroke());

		final StrokeView strokeView = new StrokeView(this);
		strokeView.setColour(page.getEraserColour());
		strokeView.setRadius((int) page.getEraserWidth());

		viewll.addView(strokeView);

		seekBarStroke.setProgress((int) page.getEraserWidth()-2);

		//Listening to changes in the SeekBars
		SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				strokeView.setRadius(seekBar.getProgress()+2);
			}
		};
		seekBarStroke.setOnSeekBarChangeListener(seekBarListener);

		return view;
	}

	//Leaving--------------------------------------------------------------------------
	private void alertLeave() {
		//Alert popup when leaving
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Quit")
		.setMessage("Do you want to save your session?")
		.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				File file = new File(getFilesDir(), TEMP_FILE);
				if (file.exists() && file.delete()) {
					Log.i(TAG, "alertLeave: File deleted: " + file.getName());
				} else {
					Log.i(TAG, "alertLeave: File not found: " + file.getName());
				}

				JournalPage.this.finish();
			}
		})
		.setNeutralButton("Save & Quit", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				tempSave();
				JournalPage.this.finish();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && page.hasHistory()) {
			alertLeave();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
