package com.insensitest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

//custom view class that will be responsible for our drawing
@SuppressLint("ClickableViewAccessibility")
public class DrawingView extends View{

	//Create Object of Paint
	private Paint paint,canvasPaint;
	
	//Create Object of Path to trace the touch movement
	private Path path;
	
	//Select the initial color
	private int color = Color.BLACK;
	
	//create object of canvas
	private Canvas canvas;
	
	//create canvas bitmap
	private Bitmap canvasBitmap;
	
	//Flag for indicating state of eraser
	private Boolean eraser=false;
	
	//constructor of our custom view i.e DrawingView class
	public DrawingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		//instantiate the above declared objects
		paint=new Paint();
		path=new Path();
		
		//set the initial properties
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(10);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		
		//instantiate the canvas paint object
		canvasPaint = new Paint(Paint.DITHER_FLAG);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
	//	return super.onTouchEvent(event);
		
		//get the coordinates of touch
		float x_pos=event.getX();
		float y_pos=event.getY();
		
		//Perform operations based on the type of touch
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		    						path.moveTo(x_pos, y_pos);
		    						break;
		case MotionEvent.ACTION_MOVE:
		    						path.lineTo(x_pos, y_pos);
		    						break;
		case MotionEvent.ACTION_UP:
		    						canvas.drawPath(path, paint);
		    						path.reset();
		    						break;
		default:
		    	return false;
		}
		
		//cause the onDraw method to execute by calling invalidate
		invalidate();
		//If one of the 3 types of touch has occured then return true
		return true;
	}

	//Method to be called when custom view changes the size
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(canvasBitmap);
	}

	@Override
	protected void onDraw(Canvas m_canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		
		//Draw the path based on touch movement
		m_canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
		if(!eraser){
			m_canvas.drawPath(path, paint);
		}else{
			canvas.drawPath(path, paint);
		}
		
		
	}
	
	//Method for starting a new drawing
	public void startNew(){
		//Clear the canvas and update the UI
	    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
	    invalidate();
	}
	
	//Method for Erasing the drawing
	public void setErase(Boolean isErase){
		eraser=isErase;
		if(eraser){
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		}else{ 
			paint.setXfermode(null);
		}
		invalidate();
	}
	
	//Method to setColor
	public void setColor(String newColor){
		//set color     
		invalidate();
		color = Color.parseColor(newColor);
		paint.setColor(color);
	}
}
