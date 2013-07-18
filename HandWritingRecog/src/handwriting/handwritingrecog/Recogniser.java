package handwriting.handwritingrecog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import preprocessing.BoundingBox;
import preprocessing.Scaling;
import preprocessing.smoothing;
import Character_Stroke.Character_Stroke;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGestureListener;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Recogniser extends Activity {
	
	/********************************************* Fields **************************************/
	private static final String PREFS_NAME = Messages.getString("Recogniser.0"); //$NON-NLS-1$
	HashMap<String, float[]> Strokes;
	ConcurrentHashMap<String,float[]> concurrentStrokesmap=new ConcurrentHashMap<String, float[]>() ; //concurrent version of the Strokemap
	HashMap<String, ArrayList<String>> LUTback;
	HashMap<String, String> uniVals;
	HashMap<String, String> unicodeGrid;
	CharLUT LutMatcher;
	GestureOverlayView mv;  //the view that is responsible for collecting user input character
	EditText PhoneEntry;    //Phone Entry 
	EditText textContent;   //The placeholder where the content will be written
	Button userCorrection;  //Usercorrection Button
	ImageButton Exit;		//RedCross button to exit the program// preprocessing
	ImageButton backspace;
	customview img;
	ExpandableListView expListView;
	ArrayAdapter<String> charchoiceAdapt;
	Paint pt[]=new Paint[6];
	Gesture gv;
	
	//
	HashMap<String,String> listChild;
	List<String> items;
	String[] mappedStrokesinput;
	ArrayList<float[]> InputCharacter; // to hold the UserInput Character after  preprocessing					
	ArrayList<BoundingBox> InputCharacterHeight;
	ArrayList<unicodeMapping> Unicodemapper = new ArrayList<unicodeMapping>();
	Matcher mt;
	String correctedChar;
	SharedPreferences preference;
	final ArrayList<Character_Stroke> finallist = new ArrayList<Character_Stroke>();
	final Context context = this;
	public HashMap<String, ArrayList<Character_Stroke>> characterStrokes;
	boolean showDialog = true;
	ExecutorService executor = Executors.newFixedThreadPool(2);
	Path gestureshape;
	SharedPreferences settings;
	String CharacterID;
	
    


	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		//Strokes=Strokes;
		    	Strokes.putAll(concurrentStrokesmap);
		    	utils.SaveFile.WriteFile(Messages.getString("Recogniser.1"),Strokes); //$NON-NLS-1$

			
		
		if(textContent.getText().length()>0)
		{
			SharedPreferences.Editor editor = settings.edit();
	      	editor.putString(Messages.getString("Recogniser.2"),textContent.getText().toString()); //$NON-NLS-1$
	      	editor.putLong(Messages.getString("Recogniser.3"),mv.getFadeOffset()); //$NON-NLS-1$
	      	editor.commit();
	      	
		}
			


	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (!utils.SaveFile.exists(Messages.getString("Recogniser.LibraryPath"))) { //$NON-NLS-1$
			{
				Toast.makeText(getApplicationContext(), "Sorry this application will now shutdown due to data corruption.Start the application again.",Toast.LENGTH_LONG).show();
				try {
					Thread.sleep(3500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finish();
			}
		} else {
			// load strokes

					try {
						Strokes = utils.Strokesloader.loadStrokes(Messages.getString("Recogniser.LibraryPath")); //$NON-NLS-1$

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(Strokes==null)
						finish();
					concurrentStrokesmap=new ConcurrentHashMap<String, float[]>(Strokes);
					


		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_recogniser, menu);

		return super.onCreateOptionsMenu(menu);
	}
	
	//initialise
	public void populate()
	{
		items=new ArrayList<String>();
		listChild=new HashMap<String, String>();
		
		items.add(Messages.getString("Recogniser.6")); //$NON-NLS-1$
		items.add(Messages.getString("Recogniser.7")); //$NON-NLS-1$
		items.add(Messages.getString("Recogniser.8")); //$NON-NLS-1$
		items.add(Messages.getString("Recogniser.9")); //$NON-NLS-1$
		items.add(Messages.getString("Recogniser.10")); //$NON-NLS-1$
		items.add(Messages.getString("Recogniser.11")); //$NON-NLS-1$
		
		listChild.put(items.get(0),getResources().getString(R.string.README));
		listChild.put(items.get(1),getResources().getString(R.string.Correction));
		listChild.put(items.get(2),getResources().getString(R.string.Space_Button));
		listChild.put(items.get(3),getResources().getString(R.string.backSpace_Button));
		listChild.put(items.get(4),getResources().getString(R.string.email));
		listChild.put(items.get(5),getResources().getString(R.string.combine));
		
		//listImages.put(items.get(1),BitmapFactory.decodeResource(context.getResources(), R.drawable.));
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		    switch (item.getItemId()) {
		        case R.id.item_combine:
		        {
		        	
		        	String temp = textContent.getText().toString();
		        	if(temp.length()<2)
		        		break;
					String toCombine = textContent.getText().toString().substring(temp.length() - 2);
					String finalString = character.combineChar(toCombine);
					String newString = textContent.getText().toString()
							.substring(0, temp.length() - 2);
					newString += finalString;
					textContent.setText(newString);
					// Toast.makeText(context,toCombine.length()+"",
					// Toast.LENGTH_SHORT).show();
					break;
		        }
		        case R.id.item_clear :
		        {
		        	img.clear();
		        	break;
		        }
		        case R.id.item_help :
		        {
		        	final Dialog Helpdialog = new Dialog(context);
					Helpdialog.setTitle(Messages.getString("Recogniser.12")); //$NON-NLS-1$
					
					Helpdialog.setContentView(R.layout.expandlayout);
					expListView=(ExpandableListView) Helpdialog.findViewById(R.id.expandableListView_help);
					help_list exadapter=new help_list(this,listChild,items);
					expListView.setAdapter(exadapter);
					exadapter.notifyDataSetChanged();
					Helpdialog.show();
					break;		        	
		        }
		        case R.id.item_send :
		        {
		        	AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage(Messages.getString("Recogniser.13")).setTitle(Messages.getString("Recogniser.14")); //$NON-NLS-1$ //$NON-NLS-2$
					builder.setPositiveButton(Messages.getString("Recogniser.15"),new DialogInterface.OnClickListener() { //$NON-NLS-1$
								@Override
								public void onClick(DialogInterface dialog, int id) {
									// User clicked OK button
									/*
									 * SHOW SMS DIALOG HERE
									 */

									final Dialog SMSdialog = new Dialog(context);
									SMSdialog.setTitle(Messages.getString("Recogniser.16")); //$NON-NLS-1$
									SMSdialog.setContentView(R.layout.dialog_sms);
									final EditText SMScontent = (EditText) SMSdialog.findViewById(R.id.SMScontent);
									SMScontent.setText(textContent.getText().toString());// setting the text here
									final EditText SMSNumberfield = (EditText) SMSdialog.findViewById(R.id.phnNumber);
									final Button smsButton = (Button) SMSdialog	.findViewById(R.id.button_sms);
									smsButton.setOnClickListener(new OnClickListener() {

												@Override
												public void onClick(View arg0) {
													// TODO Auto-generated method
													// stub
													String message = SMScontent.getText().toString();
													message = unicodeVowelMod(message);
													String PhoneNumber = SMSNumberfield.getText().toString();

													if ((PhoneNumber.equals(Messages.getString("Recogniser.17")) || PhoneNumber == null)) { //$NON-NLS-1$
														Toast.makeText(getApplicationContext(),Messages.getString("Recogniser.NO_PHONE"),Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
													} else {
														SmsManager smsManager = SmsManager.getDefault();
														smsManager.sendTextMessage(PhoneNumber,null,message,null, null);
														Toast.makeText(getApplicationContext(),Messages.getString("Recogniser.SENT_SUCCESSFUL_TEXt"),Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
														SMSdialog.dismiss();
													}
												}
											});
									SMSdialog.show();
								}
							});
					builder.setNegativeButton("EMAIL",new DialogInterface.OnClickListener() { //$NON-NLS-1$
								@Override
								public void onClick(DialogInterface dialog, int id) {

									/*
									 * this part for email
									 */
									// TODO Auto-generated method stub
									Intent email = new Intent(android.content.Intent.ACTION_SEND);

									/* Fill it with Data */
									email.setType("plain/text"); //$NON-NLS-1$
									email.putExtra(android.content.Intent.EXTRA_TEXT,unicodeVowelMod(textContent.getText().toString()));

									/* Send it off to the Activity-Chooser */
									startActivity(Intent.createChooser(email,Messages.getString("Recogniser.SENDMAIL_TEXT"))); //$NON-NLS-1$
								}
							});

					AlertDialog dialog = builder.create();
					dialog.show();

		        	break;
		        }
		        case R.id.item_space :
		        {
		        	textContent.append(Messages.getString("Recogniser.23")); //$NON-NLS-1$
		        	break;
		        }
		        case R.id.item1 :
		        {
		        	mt.errorcount=0;
		        	Toast.makeText(context, "Error Count has been refreshed", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
		        	break;
		        }
		        case R.id.item_show :
		        {
		        	Toast.makeText(context,"The error count is: "+mt.errorcount, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
		        	break;
		        }
		        case R.id.item_selectTime :
		        {
		        	final long setvalue=mv.getFadeOffset();
		        	final Dialog SelectTimeout = new Dialog(context);
					SelectTimeout.setTitle("Set fadeoffset Interval(milliseconds)"); //$NON-NLS-1$
					
					SelectTimeout.setContentView(R.layout.select_timeout);
					final TextView scoreupdate=(TextView) SelectTimeout.findViewById(R.id.textView_setvalues);
					scoreupdate.setText(mv.getFadeOffset()+"/2000"); //$NON-NLS-1$
					final SeekBar seeker=(SeekBar) SelectTimeout.findViewById(R.id.seekBar1);
					seeker.setMax(2000);
					seeker.setProgress((int) mv.getFadeOffset());
					seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						
						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
							// TODO Auto-generated method stub
							scoreupdate.setText(progress+"/2000"); //$NON-NLS-1$
							
						}
					});
					Button select_ok=(Button) SelectTimeout.findViewById(R.id.button_dialogok);
					select_ok.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View arg0) {
							// TODO Auto-generated method stub
							mv.setFadeOffset(seeker.getProgress());
							SelectTimeout.dismiss();
						}
					});
					Button select_cancel=(Button) SelectTimeout.findViewById(R.id.button_dialogCancel);
					select_cancel.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							SelectTimeout.dismiss();
						}
					});
					SelectTimeout.setCancelable(false);
					SelectTimeout.show();
					
					break;		
		        }
		       
		       
		   
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recogniser);
		
		settings=getSharedPreferences(PREFS_NAME,0);               //settings contain the saved preferences
		String valueprev=settings.getString(Messages.getString("Recogniser.PREVIOUSTEXT"),"");        //retrieves the previously written text //$NON-NLS-1$ //$NON-NLS-2$
		long timeoutvalue=settings.getLong(Messages.getString("Recogniser.Timeout"),420);    //retrieves the previously saved timeout value(default 420) //$NON-NLS-1$
		
		try {
			/***************************** LOAD THE LIBRARY FILES *****************************************************/

			populate();
			Intent intent = getIntent();
			if (!utils.SaveFile.exists(Messages.getString("Recogniser.LibraryPath"))) { //$NON-NLS-1$
				Strokes = (HashMap<String, float[]>) intent.getSerializableExtra(Messages.getString("Recogniser.MainLibrary")); //$NON-NLS-1$
				File f = new File(Messages.getString("Recogniser.LibraryDir")); //$NON-NLS-1$
				f.mkdir();
				utils.SaveFile.WriteFile(Messages.getString("Recogniser.LibraryPath"), Strokes); //$NON-NLS-1$
			} else {
				try{
						Strokes = utils.Strokesloader.loadStrokes(Messages.getString("Recogniser.LibraryPath")); //$NON-NLS-1$
				}catch(Exception e)
				{
					Strokes = (HashMap<String, float[]>) intent.getSerializableExtra(Messages.getString("Recogniser.MainLibrary")); //$NON-NLS-1$
					utils.SaveFile.WriteFile(Messages.getString("Recogniser.LibraryPath"), Strokes); //$NON-NLS-1$
				}
			}
			concurrentStrokesmap.putAll(Strokes);
			
			/*
			 * LutMatcher = ForwardLUT uniVals = CharacterMap for storage
			 * unicodeGrid= for displaying the unicode values in the grid
			 * characterStrokes = contains the (Strokes[float array]+
			 * strokename) LUTback = BackwardLUT containing the mapping between  character to strokesequences
			 */

			LutMatcher = new CharLUT((HashMap<String, String>) intent.getSerializableExtra(Messages.getString("Recogniser.LUTFORWARD"))); //$NON-NLS-1$
			uniVals = character.initvalue(); // load the character map
			unicodeGrid = character.unicodeGridView(); // load the character map
														// for unicode gridview
			characterStrokes = (HashMap<String, ArrayList<Character_Stroke>>) intent.getSerializableExtra(Messages.getString("Recogniser.LUTCHARSTROKEs")); //$NON-NLS-1$
			LUTback = (HashMap<String, ArrayList<String>>) intent.getSerializableExtra(Messages.getString("Recogniser.LUTBACKWARD")); //$NON-NLS-1$
			//Strokes=(HashMap<String, float[]>) Collections.synchronizedMap(Strokes);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/*********************************************************************************************************/
		try {
			mt = new Matcher(characterStrokes,concurrentStrokesmap, this, LUTback);
			// Toast.makeText(context, "Success loading library files",
			// Toast.LENGTH_SHORT).show();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			Toast.makeText(context, e1.toString(), Toast.LENGTH_SHORT).show();
		}
		/************************ ATTACH THE UI COMPONENTS *****************************************************/

		for(int i=0;i<6;i++)
		{
			pt[i]=new Paint();
			pt[i].setStrokeWidth(8);
			pt[i].setStyle(Style.STROKE);
		}
	
		pt[0].setColor(Color.GREEN);
		pt[1].setColor(Color.RED);
		pt[2].setColor(Color.BLACK);
		pt[3].setColor(Color.BLUE);
        pt[4].setColor(Color.DKGRAY);
		pt[5].setColor(Color.MAGENTA);
		
		
		textContent = (EditText) findViewById(R.id.editText1); // TextArea for
															// output unicode
		textContent.setText(valueprev);
		mv = (GestureOverlayView) findViewById(R.id.gestureOverlayView1); // gestureoverlayview
																			// for
																			// display
		userCorrection = (Button) findViewById(R.id.button4); // Correct button
		Exit = (ImageButton) findViewById(R.id.button_quit); // Finish App

		backspace = (ImageButton) findViewById(R.id.imageButton_backspace); // for
																			// giving
																			// the
																			// backspace

		img = (customview) findViewById(R.id.customview1); // background view
															// for persisting
															// the userdrawn
															// samples

		/*********************************************************************************************************/

		mv.setGestureStrokeWidth(5);
		mv.setFadeOffset(timeoutvalue);
		for (String s : unicodeGrid.keySet()) // store all the unicode into
												// charchoices array
		{
			Unicodemapper.add(new unicodeMapping(s, unicodeGrid.get(s)));

		}

		/******************************************* ATTACH THE LISTENERS ******************************************/

		Exit.setOnClickListener(new OnClickListener() { // Finish Button

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				onStop();
				finish();
				System.exit(0);
			}
		});
		backspace.setOnClickListener(new OnClickListener() { // backspace button

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub

						String text = textContent.getText().toString();
						if(text.length()==0|| text.equals("")) //$NON-NLS-1$
							return;
						else if(text.length()==1)
							textContent.setText(""); //$NON-NLS-1$
						else
							textContent.setText(text.substring(0,text.length()-1));
						
					}
				});
		backspace.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				textContent.setText(""); //$NON-NLS-1$
				return false;
			}
		});
		
		mv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		mv.addOnGesturePerformedListener(new OnGesturePerformedListener() {

			@Override
			public void onGesturePerformed(GestureOverlayView arg0, Gesture arg1) {
				// TODO Auto-generated method stub
				final Gesture gesture = arg1;
				gv=arg1;
				new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						ArrayList<float[]> UserDrawnStroke = new ArrayList<float[]>(gesture.getStrokesCount()); // create an
															// arraylist to
															// temporary hold
															// the float arrays
						InputCharacterHeight=new ArrayList<BoundingBox>(UserDrawnStroke.size());
						for (int i = 0; i < gesture.getStrokesCount(); i++) {
							float[] temp = gesture.getStrokes().get(i).points; // float
																				// points
																				// of
																				// the
																				// gesture
							InputCharacterHeight.add(new BoundingBox());
							temp = Scaling.scale(temp,InputCharacterHeight.get(i)); // Apply Scaling
							temp = smoothing.smoothFunction(temp); // apply
																	// Smoothing
							UserDrawnStroke.add(temp);
						}
						
						InputCharacter = UserDrawnStroke;// store globally;
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try{
								// TODO Auto-generated method stub
								//Toast.makeText(getApplicationContext(), concurrentStrokesmap.size()+"", Toast.LENGTH_SHORT).show();
									new performRecognition().execute(InputCharacter);
								}
								//
								catch(Exception e)
								{
									Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
								}
							}
						});
					}
				}).start();
			}
		});

		mv.addOnGestureListener(new OnGestureListener() {

			@Override
			public void onGestureStarted(GestureOverlayView arg0,
					MotionEvent arg1) {

			}

			@Override
			public void onGestureEnded(GestureOverlayView arg0, MotionEvent arg1) {
				gestureshape = arg0.getGesturePath();
				img.drwshp(gestureshape,arg0.getWidth(),arg0.getHeight());
			}

			@Override
			public void onGestureCancelled(GestureOverlayView arg0,
					MotionEvent arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onGesture(GestureOverlayView arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub

			}
		});

		
		// img.setImageBitmap(Bitmap.createBitmap(img.getWidth(),img.getHeight(),Bitmap.Config.ARGB_8888));

		/************************ Matcher Dialog *****************************************************/
		userCorrection.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				StrokeMatcher(2);

			}
		});

		/************************ Matcher Ends here *****************************************************/

	}

	public void StrokeMatcher(int type) // Stroke Matcher function

	{
		final int valtype = type;
		final Dialog dialog = new Dialog(context);
		dialog.setContentView(R.layout.dialog_unicode);
		dialog.setTitle(Messages.getString("Recogniser.45")); //$NON-NLS-1$
		GridView charchoices = (GridView) dialog.findViewById(R.id.gridView_mul);
		charchoices.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				TextView t = (TextView) arg1.findViewById(R.id.text1);
				String charactername = (String) t.getTag(); // get the tag name
				CharacterID=charactername; //characterclass
				correctedChar = t.getText().toString();

				if (valtype == 1) {
					String preText = textContent.getText().toString();
					textContent.setText(preText + uniVals.get(charactername));// set
																			// the
																			// corrected
																			// character
				} else {
					String preText = textContent.getText().toString();

					if (InputCharacter == null) {
						preText += uniVals.get(charactername);
						textContent.setText(preText);
						dialog.dismiss();
						return;
					} else {
						if (preText.length() != 0) {
							preText = preText.substring(0, preText.length() - 1);
						}
						preText += uniVals.get(charactername);
						textContent.setText(preText);
					}

				}
				/*
				 * Number of Characters found
				 */
				if (InputCharacter.size() == 1) // handle single Stroke
				{
					final String InputCharName = mt	.getSingleStrokeName(charactername); //charactername
					if (InputCharName != null) {
						executor.execute(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub									
							
									double minimum_score=Double.MAX_VALUE;
									String minStroke=Messages.getString("Recogniser.46"); //$NON-NLS-1$
									
									Iterator<String> itr=concurrentStrokesmap.keySet().iterator();
									while(itr.hasNext())
									{
										String s=itr.next();
										if(InputCharName.equals(CharLUT.getStrokename(s)))
										{
											double score=DTWRecogniser.DTWDistance(Strokes.get(s),InputCharacter.get(0));
											if(score<minimum_score)
											{
												score=minimum_score;
												minStroke=s;
											}
										}
									}
									final String MinimumStroke=minStroke;
									runOnUiThread(new Runnable() {
										
										
										@Override
										public void run() {
											// TODO Auto-generated method stub
											final Dialog Confirm = new Dialog(context);
											Confirm.setCancelable(false);
											Confirm.setTitle("Corfirm Dialog");
											Confirm.setContentView(R.layout.confirm);
											TextView message=(TextView) Confirm.findViewById(R.id.textView_messagenew );
											message.setText("Are you sure you want to save this shape as "+uniVals.get(CharacterID)+"?");
											ImageView lv=(ImageView)Confirm.findViewById(R.id.imageSourceview);
											//lv.setImageBitmap(Bitmap.createScaledBitmap(getImage(gv), 315, 315, true));
											lv.setImageBitmap(gv.toBitmap(150,150,0,Color.YELLOW));
											Button yes=(Button) Confirm.findViewById(R.id.button_confirm_yes);
											yes.setOnClickListener(new OnClickListener() {
												
												@Override
												public void onClick(View arg0) {
													// TODO Auto-generated method stub
													executor.execute(new Runnable() {
														
														@Override
														public void run() {
															// TODO Auto-generated method stub
															mt.LRUReplace(InputCharName,InputCharacter.get(0), concurrentStrokesmap,MinimumStroke);
															mt.errorcount++;
														}
													});
													Toast.makeText(context,Messages.getString("Recogniser.SUCCESSFUL_MESSAGE")+InputCharName,Toast.LENGTH_SHORT).show();
													Confirm.dismiss();
												}
											});
											Button no=(Button) Confirm.findViewById(R.id.button_confirm_no);
											no.setOnClickListener(new OnClickListener() {
												
												@Override
												public void onClick(View arg0) {
													// TODO Auto-generated method stub
													Confirm.dismiss();
												}
											});
											Confirm.show();
										}
									});
										
										//Log.v("debugHWRECOGNISER","SingleStroke");
										//Log.v("debugHWRECOGNISER",InputCharName);
										//Log.v("debugHWRECOGNISER",minStroke);
									// SaveFile.WriteFile("/mnt/sdcard/HWREcogfiles/Library.dat",Strokes);
										 //increase the count
								
								}
							
						});

						//Toast.makeText(context,Messages.getString("Recogniser.SUCCESSFUL_MESSAGE")+InputCharName,Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
						dialog.dismiss();//
					} else {
						//Toast.makeText(context, "Found Null",Toast.LENGTH_SHORT).show();
						dialog.dismiss();//
					}
					return;
				}

				final ArrayList<String> Charactersequences = mt.NumStrokesSeq(charactername, InputCharacter.size()); // number of
																// Strokes
																// present in
																// the
																// InputCharacter
				if (Charactersequences.size() == 1) {
					runOnUiThread(new Runnable() {
						
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							final Dialog Confirm = new Dialog(context);
							Confirm.setCancelable(false);
							Confirm.setTitle("Corfirm Dialog");
							Confirm.setContentView(R.layout.confirm);
							TextView message=(TextView) Confirm.findViewById(R.id.textView_messagenew);
							message.setText("Are you sure you want to save this shape as "+uniVals.get(CharacterID)+"?");
							ImageView lv=(ImageView)Confirm.findViewById(R.id.imageSourceview);
							//lv.setImageBitmap(Bitmap.createScaledBitmap(getImage(gv), 315, 315, true));
							lv.setImageBitmap(gv.toBitmap(150,150,0,Color.YELLOW));
							Button yes=(Button) Confirm.findViewById(R.id.button_confirm_yes);
							yes.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View arg0) {
									// TODO Auto-generated method stub
									executor.execute(new Runnable() {
										
										@Override
										public void run() {
											// TODO Auto-generated method stub
											mt.StrokeMatchnonCentroid(Charactersequences.get(0),InputCharacter,mappedStrokesinput,CharacterID,InputCharacterHeight);
											
										}
									});
									Toast.makeText(context, Messages.getString("Recogniser.SUCCESS"),Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
									//Toast.makeText(context,Messages.getString("Recogniser.SUCCESSFUL_MESSAGE")+InputCharName,Toast.LENGTH_SHORT).show();
									Confirm.dismiss();
								}
							});
							Button no=(Button) Confirm.findViewById(R.id.button_confirm_no);
							no.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View arg0) {
									// TODO Auto-generated method stub
									Confirm.dismiss();
								}
							});
							Confirm.show();
						}
					});
					dialog.dismiss();

				}

				else {

					dialog.dismiss();
					if (Charactersequences.size() != 0) {
						final Dialog multiselect = new Dialog(context);
						multiselect.setTitle(Messages.getString("Recogniser.49")); //$NON-NLS-1$
						multiselect.setContentView(R.layout.dialogmulchoice); // show
																				// the
																				// multiple
																				// dialog
						
						ListView lv = (ListView) multiselect.findViewById(R.id.listView12);
						customadaptermulti adapter = new customadaptermulti(context, R.layout.editcharacter,Charactersequences, characterStrokes);
						lv.setAdapter(adapter);
						adapter.notifyDataSetChanged(); // update view
						lv.setOnItemClickListener(new OnItemClickListener() { 
							@Override
							public void onItemClick(AdapterView<?> arga0,View arga1, int arga2, long arga3) {
								// TODO Auto-generated method stub
								ImageView v = (ImageView) arga1.findViewById(R.id.imageView_group);
								final String seq = (String) v.getTag(); 
								runOnUiThread(new Runnable() {
									
									
									@Override
									public void run() {
										// TODO Auto-generated method stub
										final Dialog Confirm = new Dialog(context);
										Confirm.setCancelable(false);
										Confirm.setTitle("Corfirm Dialog");
										Confirm.setContentView(R.layout.confirm);
										TextView message=(TextView) Confirm.findViewById(R.id.textView_messagenew);
										message.setText("Are you sure you want to save this shape as "+uniVals.get(CharacterID)+"?");
										ImageView lv=(ImageView)Confirm.findViewById(R.id.imageSourceview);
										//lv.setImageBitmap(Bitmap.createScaledBitmap(getImage(gv), 315, 315, true));
										lv.setImageBitmap(gv.toBitmap(150,150,0,Color.YELLOW));
										Button yes=(Button) Confirm.findViewById(R.id.button_confirm_yes);
										yes.setOnClickListener(new OnClickListener() {
											
											@Override
											public void onClick(View arg0) {
												// TODO Auto-generated method stub
												executor.execute(new Runnable() {
													
													@Override
													public void run() {
														// TODO Auto-generated method stub
														mt.StrokeMatchnonCentroid(seq, InputCharacter,mappedStrokesinput,CharacterID,InputCharacterHeight);
													}
												});
												multiselect.dismiss();
												Toast.makeText(context, Messages.getString("Recogniser.SUCCESS"),Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
												//Toast.makeText(context,Messages.getString("Recogniser.SUCCESSFUL_MESSAGE")+InputCharName,Toast.LENGTH_SHORT).show();
												Confirm.dismiss();
											}
										});
										Button no=(Button) Confirm.findViewById(R.id.button_confirm_no);
										no.setOnClickListener(new OnClickListener() {
											
											@Override
											public void onClick(View arg0) {
												// TODO Auto-generated method stub
												multiselect.dismiss();
												Confirm.dismiss();
											}
										});
										Confirm.show();
									}
								});
								
								

							}
						});
						multiselect.show();
					} else {
						Toast.makeText(context, Messages.getString("Recogniser.ERROR"), Toast.LENGTH_SHORT) //$NON-NLS-1$
								.show();
					}

				}

			}

		});
		customAdapterSingle adapt = new customAdapterSingle(context,
				R.layout.listview, Unicodemapper); // attach the adapter
		charchoices.setAdapter(adapt);
		adapt.notifyDataSetChanged();
		dialog.show();
	}

	public String unicodeVowelMod(String str) {
		return ((str.replaceAll("([\u09C7])(.)([\u09BE])", "$2\u09CB")) //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("([\u09C7])(.)([\u09D7])", "$2\u09CC")).replaceAll( //$NON-NLS-1$ //$NON-NLS-2$
				"([\u09C7|\u09BF|\u09C8])(.)", "$2$1"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	

	/************************ Recogniser Class Separate Thread *****************************************************/

	class performRecognition extends
			AsyncTask<ArrayList<float[]>, Void, String> {

		@Override
		protected String doInBackground(ArrayList<float[]>... params) { // /Compute
																		// the
																		// Recognition
																		// here
			
				
			
			String finalCharacterClass = null;
			String[] RecognizedStrokes = new String[params[0].size()];
			// Set<String> libraryClassesKeys=Strokes.keySet(); //obtain the
			// keys
			
				for (int i = 0; i < params[0].size(); i++) {
					double minValue = Double.MAX_VALUE;
					String ClassRecognizedMin = null;
					Iterator<String> key = concurrentStrokesmap.keySet().iterator();
					while (key.hasNext()) {
						String tempClass = key.next();
						double score = DTWRecogniser.DTWDistance(params[0].get(i),	concurrentStrokesmap.get(tempClass));
						if (minValue > score) {
							minValue = score; // set as minimum score
							ClassRecognizedMin = tempClass; // set as minimum Score
															// corresponding class
						}

					}
					RecognizedStrokes[i] = ClassRecognizedMin;

				}
			
			
			
			mappedStrokesinput=RecognizedStrokes;
			finalCharacterClass = LutMatcher.getValue(RecognizedStrokes); //obtain character class from combination of strokes
			
			// finalCharacterClass=RecognizedStrokes.toString();
			
			return finalCharacterClass;
		}

		@Override
		/************************THE END OF JOB TO PERFORM AFTER RECOGNITION*****************************************************/
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			String previoustext = textContent.getText().toString();
			//Toast.makeText(getApplicationContext(), "result:" + result,Toast.LENGTH_SHORT).show();
			String newresult = uniVals.get(LutMatcher.LUTforward.get(result));
			if (newresult == null) {
				StrokeMatcher(1);
				previoustext += correctedChar;

			} else {
				previoustext += newresult;

				textContent.setText(previoustext);
				//Toast.makeText(getApplicationContext(), result,Toast.LENGTH_SHORT).show();
			}

		}

	}

}
