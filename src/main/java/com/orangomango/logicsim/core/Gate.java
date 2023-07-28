package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.MouseEvent;

import java.util.*;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.Util;

public abstract class Gate{
	protected GraphicsContext gc;
	protected Rectangle2D rect;
	protected Color color;
	protected Consumer<MouseEvent> onClick;
	protected List<Pin> pins = new ArrayList<>();
	private boolean power;
	private String name;
	protected String label = "Gate";
	protected boolean labelDown = true;

	public Gate(GraphicsContext gc, String name, Rectangle2D rect, Color color){
		this.gc = gc;
		this.rect = rect;
		this.color = color;
		this.name = name;
		this.power = Util.isPowerOn();
	}

	public void setPins(List<Pin> pins){
		this.pins = pins;
	}

	public List<Pin> getPins(){
		return this.pins;
	}

	public void click(MouseEvent e){
		if (onClick != null){
			this.onClick.accept(e);
		}
	}

	public Rectangle2D getRect(){
		return this.rect;
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
				Wire w = Util.getWire(wires, p, attached);
				wiresToRemove.add(w);
			}
		}
	}

	public void setLabel(String value){
		this.label = value;
	}

	public String getLabel(){
		return this.label;
	}

	public void setPower(boolean v){
		this.power = v;
		if (!this.power){
			for (Pin p : this.pins){
				p.setSignal(false, this.power);
			}
		}
	}

	protected boolean isPowered(){
		return this.power;
	}

	public void update(){
		for (Pin p : this.pins){
			p.updateAttachedPins(this.power);
		}
	}

	public final void render(){
		render(this.gc);
	}

	protected void renderGate(GraphicsContext gc){
		gc.setFill(this.color);
		gc.fillRect(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
	}

	public void render(GraphicsContext gc){
		renderGate(gc);
		for (Pin pin : this.pins){
			pin.render(gc, this.color);
		}
		gc.setFill(Color.BLACK);
		gc.save();
		if (this.labelDown){
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText(Util.wrapString(this.label, 7), this.rect.getMinX()+this.rect.getWidth()/2, this.rect.getMaxY()+20);
		} else {
			gc.setTextAlign(TextAlignment.LEFT);
			gc.fillText(Util.wrapString(this.label, 7), this.rect.getMaxX()+6, this.rect.getMinY()+this.rect.getHeight()/2);
		}
		gc.restore();
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
		if (this.color != null){
			JSONObject c = new JSONObject();
			c.put("red", this.color.getRed());
			c.put("green", this.color.getGreen());
			c.put("blue", this.color.getBlue());
			json.put("color", c);
		}
		JSONArray array = new JSONArray();
		for (Pin p : this.pins){
			array.put(p.getJSON());
		}
		json.put("pins", array);
		return json;
	}
}