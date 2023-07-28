package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.Util;

public class Pin{
	private Rectangle2D rect;
	private volatile boolean on;
	private List<Pin> attached = new ArrayList<>();
	private boolean doInput;
	private int id;
	private static final Font FONT = new Font("sans-serif", 10);

	public static int PIN_ID = 0;
	public static boolean UPDATE_PIN_ID = true;

	public Pin(Rectangle2D r, boolean doIn){
		this.rect = r;
		this.doInput = doIn;
		this.id = PIN_ID++;
	}

	public Pin(JSONObject json){
		this.rect = new Rectangle2D(json.getJSONObject("rect").getDouble("x"), json.getJSONObject("rect").getDouble("y"), json.getJSONObject("rect").getDouble("w"), json.getJSONObject("rect").getDouble("h"));
		this.doInput = json.getBoolean("doInput");
		this.id = json.getInt("id");
		if (UPDATE_PIN_ID){
			PIN_ID = Math.max(PIN_ID, this.id+1);
		}
	}

	public JSONObject getJSON(){
		JSONObject json = new JSONObject();
		json.put("id", this.id);
		JSONObject r = new JSONObject();
		r.put("x", this.rect.getMinX());
		r.put("y", this.rect.getMinY());
		r.put("w", this.rect.getWidth());
		r.put("h", this.rect.getHeight());
		json.put("rect", r);
		json.put("doInput", this.doInput);
		JSONArray array = new JSONArray();
		for (Pin p : this.attached){
			array.put(p.getId());
		}
		json.put("attached", array);
		return json;
	}

	public double getX(){
		return (this.rect.getMinX()+this.rect.getMaxX())/2;
	}

	public double getY(){
		return (this.rect.getMinY()+this.rect.getMaxY())/2;
	}

	public boolean contains(double x, double y){
		return this.rect.contains(x, y);
	}

	public void move(double x, double y){
		this.rect = new Rectangle2D(this.rect.getMinX()+x, this.rect.getMinY()+y, this.rect.getWidth(), this.rect.getHeight());
	}

	public boolean isInput(){
		return this.doInput;
	}

	public void attach(Pin o){
		if (!this.attached.contains(o)){
			this.attached.add(o);
		}
	}

	public List<Pin> getAttachedPins(){
		return this.attached;
	}

	private boolean hasOnPin(){
		for (Pin p : this.attached){
			if (p.isOn() && !p.isInput()){
				return true;
			}
		}
		return false;
	}

	public void updateAttachedPins(boolean power){
		if (!this.isInput()){
			if (this.on){
				for (Pin p : this.attached){
					p.setSignal(true, power);
				}
			} else {
				for (Pin p : this.attached){
					if (!p.hasOnPin()){
						p.setSignal(false, power);	
					}
				}
			}
		}
	}

	public void setSignal(boolean on, boolean power){
		if (!power && on) return; // Power disabled
		this.on = on;
	}

	public boolean isOn(){
		return this.on;
	}

	public int getId(){
		return this.id;
	}

	public void render(GraphicsContext gc, Color gateColor){
		gc.setFill(this.on ? Color.GREEN : Color.BLACK);
		gc.fillOval(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		if (Util.SHOW_PIN_ID){
			gc.setFill(Color.RED);
			gc.save();
			gc.setTextAlign(this.isInput() ? TextAlignment.RIGHT : TextAlignment.LEFT);
			gc.setFont(FONT);
			gc.fillText(Integer.toString(this.id), this.rect.getMinX()+(this.isInput() ? -8 : 23), this.rect.getMinY()+13);
			gc.restore();
		}
	}
}