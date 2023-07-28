package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.embed.swing.SwingFXUtils;

import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;

import org.json.JSONObject;
import org.json.JSONArray;

import com.orangomango.logicsim.ui.*;
import com.orangomango.logicsim.core.*;

public class MainApplication extends Application{
	private static final double WIDTH = 1000;
	private static final double HEIGHT = 800;
	public static final int FPS = 40;
	private static final double TOOLBAR_X = 650;
	private static final double TOOLBAR_Y = 75;

	private SideArea sideArea;
	private File currentFile = null;
	private int selectedId = -1;
	private Point2D mouseMoved = new Point2D(0, 0);
	private List<Gate> gates = new ArrayList<>();
	private List<Wire> wires = new ArrayList<>();
	private Pin connG;
	private List<Point2D> pinPoints = new ArrayList<>();
	private List<Gate> selectedGates = new ArrayList<>();
	private List<Wire.WirePoint> selectedWirePoints = new ArrayList<>();
	private List<UiButton> buttons = new ArrayList<>();
	private File selectedChipFile;
	private Point2D movePoint, deltaMove = new Point2D(0, 0); // For camera movement
	private double cameraX, cameraY;
	private double cameraScale = 1;
	private boolean rmWire = false, rmGate = false;
	private Pin rmW;
	private List<Wire> wiresToRemove = new ArrayList<>();
	private List<Gate> gatesToRemove = new ArrayList<>();
	private List<Pin> pinsToRemove = new ArrayList<>();
	private Point2D selectedRectanglePoint = null;
	private double selectedAreaWidth, selectedAreaHeight;
	private Point2D selectionMoveStart;
	private Point2D busStartPoint, busTempEndPoint;
	private int busAmount = 1;
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();
		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);

		Util.toggleCircuitPower(this.gates); // Circuit starts with power off.

		UiButton saveButton = new UiButton(gc, "SAVE", new Rectangle2D(50, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.setTitle("Save project");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			File file = this.currentFile == null ? fc.showSaveDialog(stage) : this.currentFile;
			if (file != null){
				this.currentFile = file;
				save(file);
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
			}
		});
		UiButton loadButton = new UiButton(gc, "LOAD", new Rectangle2D(175, 20, 100, 35), () -> {
			FileChooser fc = new FileChooser();
			fc.setTitle("Load project");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim files", "*.lsim"));
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showOpenDialog(stage);
			List<Gate> gates = new ArrayList<>();
			List<Wire> wires = new ArrayList<>();
			if (file != null){
				int backup = Pin.PIN_ID;
				Pin.PIN_ID = 0;
				JSONObject json = load(file, gc, gates, wires);
				if (json == null){
					Pin.PIN_ID = backup;
					return;
				}
				this.currentFile = file;
				this.gates = gates;
				this.wires = wires;
			}
		});
		UiButton saveChipButton = new UiButton(gc, "SAVE CHIP", new Rectangle2D(300, 20, 150, 35), () -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Create chip");
			alert.setTitle("Create chip");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setHgap(5);
			gpane.setVgap(5);
			Label nameL = new Label("Name: ");
			TextField name = new TextField();
			ColorPicker colorPicker = new ColorPicker(Color.BLUE);
			gpane.add(nameL, 0, 0);
			gpane.add(name, 1, 0);
			gpane.add(colorPicker, 0, 1, 2, 1);
			alert.getDialogPane().setContent(gpane);
			alert.showAndWait();
			FileChooser fc = new FileChooser();
			fc.setTitle("Save chip");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
			File file = fc.showSaveDialog(stage);
			if (file != null){
				this.currentFile = file;
				save(file, name.getText(), colorPicker.getValue());
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
			}
		});
		UiButton clearButton = new UiButton(gc, "CLEAR", new Rectangle2D(475, 20, 100, 35), () -> {
			this.wires = new ArrayList<Wire>();
			this.gates = new ArrayList<Gate>();
			Pin.PIN_ID = 0;
			this.currentFile = null;
		});
		UiButton rmWireButton = new UiButton(gc, "RM WIRE", new Rectangle2D(600, 20, 75, 35), () -> {
			this.rmWire = true;
			this.rmGate = false;
		});
		UiButton rmGateButton = new UiButton(gc, "RM GATE", new Rectangle2D(700, 20, 75, 35), () -> {
			this.rmGate = true;
			this.rmWire = false;
		});
		UiButton exportButton = new UiButton(gc, "EXPORT", new Rectangle2D(800, 20, 75, 35), () -> {
			double minPosX = Double.POSITIVE_INFINITY;
			double maxPosX = Double.NEGATIVE_INFINITY;
			double minPosY = Double.POSITIVE_INFINITY;
			double maxPosY = Double.NEGATIVE_INFINITY;
			for (Gate g : this.gates){
				if (g.getRect().getMinX() < minPosX){
					minPosX = g.getRect().getMinX();
				}
				if (g.getRect().getMaxX() > maxPosX){
					maxPosX = g.getRect().getMaxX();
				}
				if (g.getRect().getMinY() < minPosY){
					minPosY = g.getRect().getMinY();
				}
				if (g.getRect().getMaxY() > maxPosY){
					maxPosY = g.getRect().getMaxY();
				}
			}
			boolean changeX = false;
			boolean changeY = false;
			if (minPosX < 0){
				changeX = true;
				minPosX = -minPosX;
			}
			if (minPosY < 0){
				changeY  = true;
				minPosY = -minPosY;
			}
			int w = (int)(maxPosX+minPosX)+100;
			int h = (int)(maxPosY+minPosY)+100;
			Canvas tempCanvas = new Canvas(w, h);
			tempCanvas.getGraphicsContext2D().translate(50, 50);
			if (changeX) tempCanvas.getGraphicsContext2D().translate(minPosX, 0);
			if (changeY) tempCanvas.getGraphicsContext2D().translate(0, minPosY);
			for (Gate g : this.gates){
				g.render(tempCanvas.getGraphicsContext2D());
			}
			for (Wire wire : this.wires){
				wire.render(tempCanvas.getGraphicsContext2D());
			}
			WritableImage image = tempCanvas.snapshot(null, new WritableImage(w, h));
			FileChooser fc = new FileChooser();
			fc.setTitle("Export to png image");
			fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
			File file = fc.showSaveDialog(stage);
			if (file != null){
				if (!file.getName().endsWith(".png")){
					file = new File(file.getParent(), file.getName()+".png");
				}
				RenderedImage ri = SwingFXUtils.fromFXImage(image, null);
				try {
					ImageIO.write(ri, "png", file);
				} catch (IOException ex){
					ex.printStackTrace();
				}
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
			}
		});
		this.buttons.add(saveButton);
		this.buttons.add(loadButton);
		this.buttons.add(saveChipButton);
		this.buttons.add(clearButton);
		this.buttons.add(rmWireButton);
		this.buttons.add(rmGateButton);
		this.buttons.add(exportButton);
		
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(TOOLBAR_X, 0, 350, 800));
		this.sideArea.addButton("Switch", () -> this.selectedId = 0);
		this.sideArea.addButton("Wire", () -> this.selectedId = 1);
		this.sideArea.addButton("Light", () -> this.selectedId = 2);
		this.sideArea.addButton("NOT", () -> this.selectedId = 3);
		this.sideArea.addButton("AND", () -> this.selectedId = 4);
		this.sideArea.addButton("CHIP", () -> this.selectedId = 5);
		this.sideArea.addButton("DISPLAY7", () -> this.selectedId = 7);
		this.sideArea.addButton("BUS", () -> this.selectedId = 8);
		this.sideArea.addButton("3SBUFFER", () -> this.selectedId = 9);

		this.sideArea.startSection();
		for (File file : (new File(System.getProperty("user.dir"), "projects")).listFiles()){
			String nm = file.getName();
			if (nm.endsWith(".lsimc")){
				this.sideArea.addButton(nm.substring(0, nm.lastIndexOf(".")), () -> {
					this.selectedId = 6;
					this.selectedChipFile = file;
				});
			}
		}

		canvas.setFocusTraversable(true);
		canvas.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.P){
				Util.toggleCircuitPower(this.gates);
			} else if (e.getCode() == KeyCode.DELETE){
				if (e.isShiftDown()){
					List<Pin> pins = new ArrayList<>();
					for (Gate g : this.selectedGates){
						for (Pin p : g.getPins()){
							pins.add(p);
						}
					}
					for (Wire w : this.wires){
						if (pins.contains(w.getPin1()) && pins.contains(w.getPin2())){
							this.wiresToRemove.add(w);
						}
					}
				} else {
					this.gatesToRemove.addAll(this.selectedGates);
					this.selectedGates.clear();
				}
			} else if (e.getCode() == KeyCode.Z){
				if (this.busStartPoint != null && this.busAmount > 1){
					this.busAmount--;
				}
			} else if (e.getCode() == KeyCode.X){
				if (this.busStartPoint != null){
					this.busAmount++;
				}
			} else if (e.getCode() == KeyCode.F1){
				Util.SHOW_PIN_ID = !Util.SHOW_PIN_ID;
			}
		});

		canvas.setOnMousePressed(e -> {
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
			if (e.getButton() == MouseButton.PRIMARY){
				if (e.getY() < TOOLBAR_Y && (e.getX() < TOOLBAR_X || !this.sideArea.isOpen())){
					for (UiButton ub : this.buttons){
						ub.onClick(e.getX(), e.getY());
					}
				} else {
					if (this.selectedId >= 0){
						Gate g = null;
						boolean loaded = true;
						switch (this.selectedId){
							case 0:
								g = new Switch(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 1:
								Pin found = null;
								for (Gate gt : this.gates){
									Pin pin = gt.getPin(clickPoint.getX(), clickPoint.getY());
									if (pin != null){
										found = pin;
										break;
									}
								}
								if (found != null && found != this.connG){
									if (this.connG == null){
										this.connG = found;
									} else {
										if (e.isShiftDown() && this.pinPoints.size() > 0){
											Point2D thisPoint = new Point2D(found.getX(), found.getY());
											Point2D ref = this.pinPoints.get(this.pinPoints.size()-1);
											if (Math.abs(thisPoint.getX()-ref.getX()) > Math.abs(thisPoint.getY()-ref.getY())){
												ref = new Point2D(ref.getX(), thisPoint.getY());
											} else {
												ref = new Point2D(thisPoint.getX(), ref.getY());
											}
											this.pinPoints.set(this.pinPoints.size()-1, ref);
										}
										this.wires.add(new Wire(gc, this.connG, found, new ArrayList<Point2D>(this.pinPoints)));
										this.connG = null;
										this.selectedId = -1;
										this.pinPoints.clear();
									}
								} else if (this.connG != null){
									Point2D finalPoint = clickPoint;
									if (e.isControlDown()){
										if (this.pinPoints.size() >= 1){
											this.pinPoints.remove(this.pinPoints.size()-1);
										}
									} else {
										if (e.isShiftDown()){
											Point2D ref = null;
											if (this.pinPoints.size() == 0){
												ref = new Point2D(this.connG.getX(), this.connG.getY());
											} else {
												ref = this.pinPoints.get(this.pinPoints.size()-1);
											}
											if (Math.abs(finalPoint.getX()-ref.getX()) > Math.abs(finalPoint.getY()-ref.getY())){
												finalPoint = new Point2D(finalPoint.getX(), ref.getY());
											} else {
												finalPoint = new Point2D(ref.getX(), finalPoint.getY());
											}
										} else if (e.isAltDown()){
											double minDistance = Double.POSITIVE_INFINITY;
											Point2D ref = null;
											for (Wire w : this.wires){
												for (Wire.WirePoint wp : w.getPoints()){
													Point2D p = new Point2D(wp.getX(), wp.getY());
													if (p.distance(finalPoint) < minDistance){
														ref = p;
														minDistance = p.distance(finalPoint);
													}
												}
											}
											if (ref != null){
												finalPoint = new Point2D(ref.getX(), ref.getY());
											}
										}
										this.pinPoints.add(finalPoint);
									}
								}
								break;
							case 2:
								g = new Light(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50));
								break;
							case 3:
								g = new NotGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50));
								break;
							case 4:
								g = new AndGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50));
								break;
							case 5:
								FileChooser fc = new FileChooser();
								fc.setTitle("Load chip");
								fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
								File file = fc.showOpenDialog(stage);
								if (file == null) return;
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), file);
								loaded = ((Chip)g).getJSONData() != null;
								break;
							case 6:
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), this.selectedChipFile);
								loaded = ((Chip)g).getJSONData() != null;
								break;
							case 7:
								g = new Display7(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 0, 0));
								break;
							case 8:
								this.busStartPoint = clickPoint;
								break;
							case 9:
								g = new TriStateBuffer(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50));
								break;
						}
						if (g != null){
							if (loaded) this.gates.add(g);
							this.selectedId = -1;
						}
					} else {
						boolean sideAreaClicked = this.sideArea.onClick(e.getX(), e.getY());
						if (!sideAreaClicked){
							boolean voidClick = true;
							for (Gate g : this.gates){
								Pin pin = g.getPin(clickPoint.getX(), clickPoint.getY());
								if (pin == null){
									boolean inside = g.getRect().contains(clickPoint.getX(), clickPoint.getY());
									if (inside){
										if (this.rmGate){
											this.gatesToRemove.add(g);
											this.rmGate = false;
										} else {
											// Add pin if this is a Bus
											if (g instanceof Bus){
												if (g.getRect().getWidth() > g.getRect().getHeight()){
													g.getPins().add(new Pin(new Rectangle2D(e.getX()-7.5, g.getRect().getMinY()+g.getRect().getHeight()/2-7.5, 15, 15), e.isShiftDown()));
												} else {
													g.getPins().add(new Pin(new Rectangle2D(g.getRect().getMinX()+g.getRect().getWidth()/2-7.5, e.getY()-7.5, 15, 15), e.isShiftDown()));
												}
											} else {
												g.click(e);
											}
										}
										voidClick = false;
									}
								} else {
									voidClick = false;
									if (this.rmWire){
										if (this.rmW == null){
											this.rmW = pin;
										} else {
											Wire foundWire = Util.getWire(this.wires, this.rmW, pin);
											if (foundWire != null){
												this.wiresToRemove.add(foundWire);
											} else {
												System.out.println("No wire found");
											}
											this.rmW = null;
											this.rmWire = false;
										}
									} else {
										this.selectedId = 1;
										this.connG = pin;	
									}
								}
							}
							if (voidClick || e.isControlDown()){ // No gates found
								this.selectedRectanglePoint = clickPoint;
								if (!e.isControlDown()){
									this.selectedGates.clear();
									this.selectedWirePoints.clear();
								}
								this.selectedAreaWidth = 0;
								this.selectedAreaHeight = 0;
							}
						}
					}
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				Gate found = null;
				Pin pinFound = null;
				for (Gate g : this.gates){
					Pin pin = g.getPin(clickPoint.getX(), clickPoint.getY());
					if (g.getRect().contains(clickPoint.getX(), clickPoint.getY())){
						found = g;
						if (pin != null){
							pinFound = pin;
						}
						break;
					}
				}
				Wire.WirePoint foundPoint = null;
				wiresLoop:
				for (Wire w : this.wires){
					for (Wire.WirePoint wp : w.getPoints()){
						if (wp.contains(clickPoint.getX(), clickPoint.getY())){
							foundPoint = wp;
							break wiresLoop;
						}
					}
				}
				this.rmGate = false;
				this.rmWire = false;
				this.rmW = null;
				if (found == null && foundPoint == null){
					this.selectedId = -1;
					this.pinPoints.clear();
					this.connG = null;
					this.movePoint = new Point2D(e.getX(), e.getY());
					this.deltaMove = new Point2D(0, 0);
				} else if (found != null && (!this.selectedGates.contains(found) || e.getClickCount() == 2)){
					ContextMenu cm = new ContextMenu();
					if (found instanceof Chip){
						MenuItem showChip = new MenuItem("Look inside");
						final Chip chip = (Chip)found;
						showChip.setOnAction(ev -> {
							Alert alert = new Alert(Alert.AlertType.INFORMATION);
							alert.setTitle(chip.getName());
							alert.setHeaderText(chip.getName());
							ChipCanvas cc = new ChipCanvas(chip);
							alert.getDialogPane().setContent(cc.getPane());
							alert.showAndWait();
							cc.destroy();
						});
						cm.getItems().add(showChip);
					}
					final Gate gate = found;
					MenuItem changeLabel = new MenuItem("Change label");
					changeLabel.setOnAction(ev -> {
						TextInputDialog dialog = new TextInputDialog(gate.getLabel());
						dialog.setTitle("Set label");
						dialog.setHeaderText("Label name");
						dialog.showAndWait().ifPresent(v -> {
							gate.setLabel(v);
						});
					});
					if (pinFound != null){
						final Pin pin = pinFound;
						if (gate instanceof Bus){
							MenuItem removePin = new MenuItem("Remove pin");
							removePin.setOnAction(ev -> this.pinsToRemove.add(pin));
							cm.getItems().add(removePin);
						}
					}
					cm.getItems().add(changeLabel);
					cm.show(canvas, e.getScreenX(), e.getScreenY());
				} else if (this.selectedGates.size() > 0 || this.selectedWirePoints.size() > 0){
					this.selectionMoveStart = new Point2D(e.getX(), e.getY());
					this.deltaMove = new Point2D(0, 0);
				}
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
		});

		canvas.setOnMouseDragged(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.selectedRectanglePoint != null){
					this.selectedAreaWidth = clickPoint.getX()-this.selectedRectanglePoint.getX();
					this.selectedAreaHeight = clickPoint.getY()-this.selectedRectanglePoint.getY();
				} else if (this.busStartPoint != null){
					this.busTempEndPoint = clickPoint;
				}
			} else if (e.getButton() == MouseButton.SECONDARY){
				if (this.selectedGates.size() == 0 && this.selectedWirePoints.size() == 0 || this.selectionMoveStart == null){
					if (this.movePoint != null){
						this.deltaMove = new Point2D(e.getX()-this.movePoint.getX(), e.getY()-this.movePoint.getY());
					}
				} else if (this.selectionMoveStart != null){
					this.deltaMove = new Point2D(e.getX()-this.selectionMoveStart.getX(), e.getY()-this.selectionMoveStart.getY());
					this.selectionMoveStart = new Point2D(e.getX(), e.getY());
					for (Gate g : this.selectedGates){
						g.setPos(g.getRect().getMinX()+this.deltaMove.getX()/this.cameraScale, g.getRect().getMinY()+this.deltaMove.getY()/this.cameraScale);
					}
					for (Wire.WirePoint wp : this.selectedWirePoints){
						wp.setX(wp.getX()+this.deltaMove.getX()/this.cameraScale);
						wp.setY(wp.getY()+this.deltaMove.getY()/this.cameraScale);
					}
				}
			}
		});

		canvas.setOnMouseReleased(e -> {
			if (e.getButton() == MouseButton.PRIMARY){
				if (this.selectedRectanglePoint != null){
					Rectangle2D selection = Util.buildRect(this.selectedRectanglePoint, this.selectedAreaWidth, this.selectedAreaHeight);
					for (Gate g : this.gates){
						if (g.getRect().intersects(selection)){
							this.selectedGates.add(g);
						}
					}
					for (Wire w : this.wires){
						for (Wire.WirePoint p : w.getPoints()){
							if (selection.contains(p.getX(), p.getY())){
								this.selectedWirePoints.add(p);
							}
						}
					}
				} else if (this.busStartPoint != null){
					if (this.busTempEndPoint != null){
						double width = this.busTempEndPoint.getX()-this.busStartPoint.getX();
						double height = this.busTempEndPoint.getY()-this.busStartPoint.getY();
						if (width > height){
							for (int i = 0; i < this.busAmount; i++) this.gates.add(new Bus(gc, new Rectangle2D(this.busStartPoint.getX(), this.busStartPoint.getY()+i*20, width, 10)));
						} else {
							for (int i = 0; i < this.busAmount; i++) this.gates.add(new Bus(gc, new Rectangle2D(this.busStartPoint.getX()+i*20, this.busStartPoint.getY(), 10, height)));
						}
					}
					this.busStartPoint = null;
					this.busTempEndPoint = null;
					this.selectedId = -1;
					this.busAmount = 1;
				}
				this.selectedRectanglePoint = null;
			} else if (e.getButton() == MouseButton.SECONDARY){
				if (this.movePoint != null){
					this.movePoint = null;
					this.cameraX += this.deltaMove.getX();
					this.cameraY += this.deltaMove.getY();
				} else if (this.selectionMoveStart != null){
					this.selectionMoveStart = null;
				}
			}
		});

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() != 0){
				this.cameraScale += (e.getDeltaY() > 0 ? 0.05 : -0.05);
				this.cameraScale = Math.max(0.3, Math.min(this.cameraScale, 2));
			}
		});

		Scene scene = new Scene(pane, WIDTH, HEIGHT);

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> {
			update(gc);
			if (this.rmWire || this.rmGate){
				scene.setCursor(Cursor.CROSSHAIR);
			} else if (this.movePoint != null){
				scene.setCursor(Cursor.MOVE);
			} else if (this.selectionMoveStart != null){
				scene.setCursor(Cursor.CLOSED_HAND);
			} else if (this.selectedId >= 0 && this.selectedId != 1){
				scene.setCursor(Cursor.HAND);
			} else {
				scene.setCursor(Cursor.DEFAULT);
			}
			stage.setTitle("LogicSim"+(this.currentFile == null ? "" : " - "+this.currentFile.getName()));
		}));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();

		Thread simulation = new Thread(() -> {
			while (true){
				try {
					for (int i = 0; i < this.gates.size(); i++){
						Gate g = this.gates.get(i);
						g.update();
					}
					Thread.sleep(2);
				} catch (InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
		simulation.setDaemon(true);
		simulation.start();
		
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();
	}

	public Point2D getClickPoint(double x, double y){
		Point2D clickPoint = new Point2D(x, y);
		clickPoint = clickPoint.subtract(this.cameraX, this.cameraY);
		if (this.movePoint != null){
			clickPoint = clickPoint.subtract(this.deltaMove);
		}
		clickPoint = clickPoint.multiply(1/this.cameraScale);
		return clickPoint;
	}

	private void save(File file){
		save(file, null, null);
	}

	private void save(File file, String chipName, Color color){
		if (!file.getName().endsWith(".lsim") && !file.getName().endsWith(".lsimc")){
			file = new File(file.getParent(), file.getName()+".lsim"+(chipName == null ? "" : "c"));
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			JSONObject json = new JSONObject();
			JSONArray data = new JSONArray();
			for (Gate gate : this.gates){
				data.put(gate.getJSON());
			}
			json.put("gates", data);
			data = new JSONArray();
			for (Wire wire : this.wires){
				data.put(wire.getJSON());
			}
			json.put("wires", data);
			if (chipName != null && color != null){
				json.put("chipName", chipName);
				JSONObject cl = new JSONObject();
				cl.put("red", color.getRed());
				cl.put("green", color.getGreen());
				cl.put("blue", color.getBlue());
				json.put("color", cl);
			}
			writer.write(json.toString(4));
			writer.close();
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public static JSONObject load(File file, GraphicsContext gc, List<Gate> tempGates, List<Wire> tempWires){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder builder = new StringBuilder();
			reader.lines().forEach(builder::append);
			reader.close();
			JSONObject json = new JSONObject(builder.toString());

			int backupId = Pin.PIN_ID;

			// Load gates
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				String name = gate.getString("name");
				Color color = gate.optJSONObject("color") == null ? null : Color.color(gate.getJSONObject("color").getDouble("red"), gate.getJSONObject("color").getDouble("green"), gate.getJSONObject("color").getDouble("blue"));
				Rectangle2D rect = new Rectangle2D(gate.getJSONObject("rect").getDouble("x"), gate.getJSONObject("rect").getDouble("y"), gate.getJSONObject("rect").getDouble("w"), gate.getJSONObject("rect").getDouble("h"));
				List<Pin> pins = new ArrayList<>();
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					Pin p = new Pin(pin);
					pins.add(p);
				}
				Gate gt = null;
				int lastPinId = Pin.PIN_ID; // Save pin id
				boolean lastPinFlag = Pin.UPDATE_PIN_ID;
				if (name.equals("AND")){
					gt = new AndGate(gc, rect);
				} else if (name.equals("LIGHT")){
					gt = new Light(gc, rect);
				} else if (name.equals("NOT")){
					gt = new NotGate(gc, rect);
				} else if (name.equals("SWITCH")){
					gt = new Switch(gc, rect);
				} else if (name.equals("CHIP")){
					File chipFile = new File(file.getParent(), gate.getString("fileName"));
					if (!chipFile.exists()){
						Alert error = new Alert(Alert.AlertType.ERROR);
						error.setTitle("Missing dependency");
						error.setHeaderText("Missing dependency");
						error.setContentText("The following dependency is missing: "+gate.getString("fileName"));
						error.showAndWait();
						Pin.PIN_ID = backupId;
						return null;
					}
					gt = new Chip(gc, rect, chipFile);
				} else if (name.equals("DISPLAY7")){
					gt = new Display7(gc, rect);
				} else if (name.equals("BUS")){
					gt = new Bus(gc, rect);
				} else if (name.equals("3SBUFFER")){
					gt = new TriStateBuffer(gc, rect);
				}
				Pin.PIN_ID = lastPinId; // Restore the last pin id
				Pin.UPDATE_PIN_ID = lastPinFlag;
				gt.setPins(pins);
				tempGates.add(gt);
			}

			// Attach gates' pins
			for (Object o : json.getJSONArray("gates")){
				JSONObject gate = (JSONObject)o;
				for (Object o2 : gate.getJSONArray("pins")){
					JSONObject pin = (JSONObject)o2;
					int pinId = pin.getInt("id");
					Pin currentPin = getPinById(tempGates, pinId);
					for (Object o3 : pin.getJSONArray("attached")){
						Integer apinId = (Integer)o3;
						Pin apin = getPinById(tempGates, apinId);
						currentPin.attach(apin);
					}
				}
			}

			// Load wires
			for (Object o : json.getJSONArray("wires")){
				JSONObject wire = (JSONObject)o;
				Pin p1 = getPinById(tempGates, wire.getInt("pin1"));
				Pin p2 = getPinById(tempGates, wire.getInt("pin2"));
				List<Point2D> points = new ArrayList<>();
				for (Object o2 : wire.getJSONArray("points")){
					JSONObject p = (JSONObject)o2;
					points.add(new Point2D(p.getDouble("x"), p.getDouble("y")));
				}
				tempWires.add(new Wire(gc, p1, p2, points));
			}

			return json;
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}

	private static Pin getPinById(List<Gate> gates, int id){
		for (Gate g : gates){
			for (Pin p : g.getPins()){
				if (p.getId() == id){
					return p;
				}
			}
		}
		return null;
	}
	
	private void update(GraphicsContext gc){
		gc.clearRect(0, 0, WIDTH, HEIGHT);
		gc.setFill(Color.web("#EDEDED"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		// Render the grid
		double gridSize = 50*this.cameraScale;
		gridSize = Math.min(100, Math.max(gridSize, 15));
		gc.save();
		gc.setStroke(Color.web("#A2A2A2"));
		gc.setLineWidth(1.5);
		double offsetX = (this.cameraX+(this.movePoint != null ? this.deltaMove.getX() : 0)) % gridSize;
		double offsetY = (this.cameraY+(this.movePoint != null ? this.deltaMove.getY() : 0)) % gridSize;
		for (double i = offsetX; i < WIDTH; i += gridSize){
			gc.strokeLine(i, 0, i, HEIGHT);
		}
		for (double i = offsetY; i < HEIGHT; i += gridSize){
			gc.strokeLine(0, i, WIDTH, i);
		}
		gc.restore();

		gc.save();
		gc.translate(this.cameraX, this.cameraY);
		if (this.movePoint != null){
			gc.translate(this.deltaMove.getX(), this.deltaMove.getY());
		}
		gc.scale(this.cameraScale, this.cameraScale);

		for (Gate g : this.gates){
			g.render();
			if (this.selectedGates.contains(g)){
				gc.save();
				gc.setFill(Color.LIME);
				gc.setGlobalAlpha(0.6);
				gc.fillRect(g.getRect().getMinX(), g.getRect().getMinY(), g.getRect().getWidth(), g.getRect().getHeight());
				gc.restore();
			}
		}
		for (Wire w : this.wires){
			w.render();
			for (Wire.WirePoint wp : w.getPoints()){
				if (this.selectedWirePoints.contains(wp)){
					wp.render(gc);
				}
			}
		}

		if (this.connG != null){
			gc.setStroke(Color.BLACK);
			gc.setLineWidth(3);
			Point2D end = getClickPoint(this.mouseMoved.getX(), this.mouseMoved.getY());
			List<Point2D[]> renderingPoints = Util.getPointsList(new Point2D(this.connG.getX(), this.connG.getY()), end, this.pinPoints);
			for (Point2D[] line : renderingPoints){
				gc.strokeLine(line[0].getX(), line[0].getY(), line[1].getX(), line[1].getY());
			}
		}

		if (this.busStartPoint != null && this.busTempEndPoint != null){
			double busWidth = this.busTempEndPoint.getX()-this.busStartPoint.getX();
			double busHeight = this.busTempEndPoint.getY()-this.busStartPoint.getY();
			gc.setFill(Color.GRAY);
			if (busWidth > busHeight){
				for (int i = 0; i < this.busAmount; i++) gc.fillRect(this.busStartPoint.getX(), this.busStartPoint.getY()+i*20, busWidth, 10);
			} else {
				for (int i = 0; i < this.busAmount; i++) gc.fillRect(this.busStartPoint.getX()+i*20, this.busStartPoint.getY(), 10, busHeight);
			}
		}

		// Selection rectangle
		if (this.selectedRectanglePoint != null){
			gc.save();
			gc.setFill(Color.LIME);
			gc.setGlobalAlpha(0.6);
			Rectangle2D selection = Util.buildRect(this.selectedRectanglePoint, this.selectedAreaWidth, this.selectedAreaHeight);
			gc.fillRect(selection.getMinX(), selection.getMinY(), selection.getWidth(), selection.getHeight());
			gc.restore();
		}

		gc.restore();

		// UI
		gc.save();
		gc.setFill(Color.BLACK);
		gc.fillText(String.format("ID: %d\nPower: %s\nScale: %.2f", Pin.PIN_ID, Util.isPowerOn(), this.cameraScale), 60, 300);
		gc.setGlobalAlpha(0.5);
		gc.fillRect(0, 0, WIDTH, TOOLBAR_Y);
		gc.restore();

		for (UiButton ub : this.buttons){
			ub.render();
		}
		if (this.selectedId == -1) this.sideArea.render();

		// Remove selected gates
		for (int i = 0; i < this.gatesToRemove.size(); i++){
			Gate g = this.gatesToRemove.get(i);
			g.destroy(this.wires, this.wiresToRemove);
			this.gates.remove(g);
		}
		if (this.gatesToRemove.size() > 0){
			Pin.PIN_ID = this.gates.stream().flatMap(g -> g.getPins().stream()).mapToInt(p -> p.getId()).max().orElse(-1)+1;
		}
		this.gatesToRemove.clear();

		// Remove selected pins
		for (int i = 0; i < this.pinsToRemove.size(); i++){
			Pin p = this.pinsToRemove.get(i);
			p.destroy(this.gates, this.wires, this.wiresToRemove);
		}
		this.pinsToRemove.clear();

		// Remove selected wires
		for (int i = 0; i < this.wiresToRemove.size(); i++){
			Wire w = this.wiresToRemove.get(i);
			w.destroy();
			this.wires.remove(w);
		}
		this.wiresToRemove.clear();
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
