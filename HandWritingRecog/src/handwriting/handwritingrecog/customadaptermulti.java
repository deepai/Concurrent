package handwriting.handwritingrecog;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;


import Character_Stroke.Character_Stroke;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/*********************************ADAPTER CLASS FOR MULTISELECT DISPLAY************************************************************************/
public class customadaptermulti extends ArrayAdapter<String> { 
	
	HashMap<String,ArrayList<Character_Stroke>> characterStrokes;
    ArrayList<String> obj;
    Context context = null;
	private int width=1160;
	private int height=648;
	public customadaptermulti(Context context, int textViewResourceId,ArrayList<String> t,HashMap<String,ArrayList<Character_Stroke>> tz) {
		super(context, textViewResourceId,t);
		// TODO Auto-generated constructor stub
		obj=t;
		this.context=context;
		characterStrokes=tz;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {//set an Image and set its tag
		// TODO Auto-generated method stub
		final int i=position;
		if (convertView == null) {
		    
			// This a new view we inflate the new layout
		    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    convertView = inflater.inflate(R.layout.editcharacter, parent, false);
		}
		ImageView img=(ImageView) convertView.findViewById(R.id.imageView1);
		
		
		Bitmap bp=Bitmap.createScaledBitmap(getImage(characterStrokes.get(obj.get(i))), 200,200, true);
		//bp=codec(bp,Bitmap.CompressFormat.JPEG,20);
		img.setImageBitmap(bp); //pass the corresponding Stroke Sequence
		img.setTag(obj.get(i));
		return convertView;
	}
	private Bitmap codec(Bitmap src, Bitmap.CompressFormat format,
			int quality) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		src.compress(format, quality, os);
 
		byte[] array = os.toByteArray();
		return BitmapFactory.decodeByteArray(array, 0, array.length);
	}
	private Bitmap getImage(ArrayList<Character_Stroke> temp) //create ImageThumbnails
	{
		if(temp==null)
			System.out.println("debug:null recieved");
		else
			System.out.println("debug:"+temp.size());
		Paint pt=new Paint();
		pt.setColor(Color.BLACK);
		pt.setStrokeWidth(10);
	/*
	 * Set the image as the background 	
	 */
		
		Bitmap bp=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
		Canvas ct=new Canvas(bp);
		ct.drawColor(Color.WHITE);
		for(Character_Stroke s:temp)
		{
			ct.drawPoints(s.getStroke(), pt); //draw the strokes
		}
		return bp;
	}


}