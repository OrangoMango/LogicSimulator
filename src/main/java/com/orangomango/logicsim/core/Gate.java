package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.MainApplication;

public abstract class Gate{
	protected GraphicsContext gc;
	protected Rectangle2D rect;
	protected Color color;
	protected Runnable onClick;
	protected List<Pin> pins = new ArrayList<>();
	private String name;

	public static class Pin{
		private Rectangle2D rect;
		private boolean on;
		private List<Pin> attached = new ArrayList<>();
		private boolean doInput;
		private int id;

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

		public void setSignal(boolean on){
			this.on = on;
			if (!this.isInput()){
				if (this.on){
					for (Pin p : this.attached){
						p.on = true;
					}
				} else {
					for (Pin p : this.attached){
						if (!p.hasOnPin()){
							p.on = false;	
						}
					}
				}
			}
		}

		public boolean isOn(){
			return this.on;
		}

		public int getId(){
			return this.id;
		}

		public void render(GraphicsContext gc){
			gc.setFill(this.on ? Color.GREEN : Color.BLACK);
			gc.fillOval(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		}
	}

	public Gate(GraphicsContext gc, String name, Rectangle2D rect, Color color){
		this.gc = gc;
		this.rect = rect;
		this.color = color;
		this.name = name;
	}

	public void setPins(List<Pin> pins){
		this.pins = pins;
	}

	public List<Pin> getPins(){
		return this.pins;
	}

	public void onClick(double x, double y){
		if (onClick != null && this.rect.contains(x, y)){
			this.onClick.run();
		}
	}

	public boolean contains(double x, double y){
		return this.rect.contains(x, y);
	}

	public void setPos(double x, double y){
		double deltaX = x-this.rect.getMinX();
		double deltaY = y-this.rect.getMinY();
		this.rect = new Rectangle2D(x, y, this.rect.getWidth(), this.rect.getHeight());
		for (Pin p : this.pins){
			p.move(deltaX, deltaY);
		}
	}

	public Pin getPin(double x, double y){
		for (Pin pin : this.pins){
			if (pin.contains(x, y)){
				return pin;
			}
		}
		return null;
	}

	public String getName(){
		return this.name;
	}

	public void destroy(List<Wire> wires, List<Wire> wiresToRemove){
		for (Pin p : this.pins){
			for (Pin attached : p.getAttachedPins()){
				Wire w = MainApplication.getWire(wires, p, attached);
				wiresToRemove.add(w);
			}
		}
	}

	public abstract void update();

	public final void render(){
		render(this.gc);
	}

	public void render(GraphicsContext gc){
		gc.setFill(this.color);
		gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		for (Pin pin : pins){
			pin.render(gc);
		}
	}

	public JSONObject getJSON(){
		JSONObject json = new JSONObject();
		json.put("name", this.name);
		JSONObject r = new JSONObject();
		r.put("x", this.rect.getMinX());
		r.put("y", this.rect.getMinY());
		r.put("w", this.rect.getWidth());
		r.put("h", this.rect.getHeight());
		json.put("rect", r);
		JSONObject c = new JSONObject();
		c.put("red", this.color.getRed());
		c.put("green", this.color.getGreen());
		c.put("blue", this.color.getBlue());
		json.put("color", c);
		JSONArray array = new JSONArray();
		for (Pin p : this.pins){
			array.put(p.getJSON());
		}
		json.put("pins", array);
		return json;
	}
}