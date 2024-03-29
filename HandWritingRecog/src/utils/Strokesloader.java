package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import Character_Stroke.Character_Stroke;

public class Strokesloader {
	
	public static HashMap<String,float[]> loadStrokes(String filename) throws Exception 
	{
		ObjectInputStream oin;
		oin=new ObjectInputStream(new FileInputStream(new File(filename)));
		HashMap<String,float[]> readObject = ((HashMap<String,float[]>) oin.readObject());	
		oin.close();
		return readObject;
	}
	
	public static HashMap<String,String> loadForwardLUT(String filename) throws Exception 
	{
		ObjectInputStream oin;
		oin=new ObjectInputStream(new FileInputStream(new File(filename)));
		HashMap<String,String> readObject = ((HashMap<String,String>) oin.readObject());	
		oin.close();
		return readObject;
	}
	
	public static HashMap<String,ArrayList<String>> loadbackwardLUT(String filename) throws Exception 
	{
		ObjectInputStream oin;
		oin=new ObjectInputStream(new FileInputStream(new File(filename)));
		HashMap<String,ArrayList<String>> readObject = ((HashMap<String,ArrayList<String>>) oin.readObject());	
		oin.close();
		return readObject;
	}
	
	public static HashMap<String,ArrayList<Character_Stroke>> loadStrokesClass(String filename) throws Exception 
	{
		ObjectInputStream oin;
		oin=new ObjectInputStream(new FileInputStream(new File(filename)));
		HashMap<String,ArrayList<Character_Stroke>> readObject = ((HashMap<String,ArrayList<Character_Stroke>>) oin.readObject());	
		oin.close();
		return readObject;
	}
	
	
	
}
