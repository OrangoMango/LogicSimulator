package com.orangomango.logicsim.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.io.File;
import org.json.JSONObject;

import com.orangomango.logicsim.MainApplication;

public class Chip extends Gate{
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private List<Gate> inputGates = new ArrayList<>();
	private List<Gate> outputGates = new ArrayList<>();
	private List<Gate.Pin> inputPins = new ArrayList<>();
	private List<Gate.Pin> outputPins = new ArrayList<>();
	private File file;
	private String name;
	private JSONObject json;

	public Chip(GraphicsContext gc, Rectangle2D rect, File file){
		super(gc, "CHIP", rect, Color.BLUE);
		this.file = file;

		// Load data
		Gate.Pin.UPDATE_PIN_ID = false;
		this.json = MainApplication.load(this.file, this.gc, this.gates, this.wires);
		Gate.Pin.UPDATE_PIN_ID = true;
		if (this.json == null) return;

		this.color = Color.color(this.json.getJSONObject("color").getDouble("red"), this.json.getJSONObject("color").getDouble("green"), this.json.getJSONObject("color").getDouble("blue"));
		this.name = this.json.getString("chipName");

		for (Gate g : this.gates){
			if (g.getName().equals("SWITCH")){
				this.inputGates.add(g);
			} else if (g.getName().equals("LIGHT")){
				this.outputGates.add(g);
			}
		}

		this.rect = new Rectangle2D(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), Math.max(this.inputGates.size(), this.outputGates.size())*20);
		for (int i = 0; i < this.inputGates.size(); i++){
			Gate.Pin pin = new Gate.Pin(new Rectangle2D(this.rect.getMinX()-15, this.rect.getMinY()+i*20, 15, 15), true);
			this.pins.add(pin);
			this.inputPins.add(pin);
		}
		for (int i = 0; i < this.outputGates.size(); i++){
			Gate.Pin pin = new Gate.Pin(new Rectangle2D(this.rect.getMaxX(), this.rect.getMinY()+i*20, 15, 15), false);
			this.pins.add(pin);
			this.outputPins.add(pin);
		}
	}

	public JSONObject getJSONData(){
		return this.json;
	}

	public String getName(){
		return this.name;
	}

	public List<Gate> getGates(){
		return this.gates;
	}

	@Override
	public JSONObject getJSON(){
		JSONObject json = super.getJSON();
		json.put("fileName", this.file.getName());
		return json;
	}

	@Override
	public void setPins(List<Pin> pins){
		super.setPins(pins);
		this.outputPins.clear();
		this.inputPins.clear();
		for (Gate.Pin pin : this.pins){
			if (pin.isInput()){
				this.inputPins.add(pin);
			} else {
				this.outputPins.add(pin);
			}
		}
	}

	public void renderInside(GraphicsContext gc){
		for (Gate g : this.gates){
			g.render(gc);
		}
		for (Wire w : this.wires){
			w.render(gc);
		}
	}

	@Override
	public void update(){
		super.update();
		for (Gate g : this.gates){
			g.update();
		}

		for (int i = 0; i < this.inputPins.size(); i++){
			Gate.Pin pin = this.inputPins.get(i);
			((Switch)this.inputGates.get(i)).setOn(pin.isOn());
		}
		for (int i = 0; i < this.outputGates.size(); i++){
			Light l = (Light)this.outputGates.get(i);
			this.outputPins.get(i).setSignal(l.isOn());
		}
	}

	@Override
	public void render(GraphicsContext gc){
		super.render(gc);
		gc.setFill(Color.BLACK);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.fillText(this.name, this.rect.getMinX()+this.rect.getWidth()/2, this.rect.getMinY()+this.rect.getHeight()/2);
	}
}