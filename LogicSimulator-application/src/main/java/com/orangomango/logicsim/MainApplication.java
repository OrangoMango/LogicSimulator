package com.orangomango.logicsim;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyCode;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
//import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
//import javafx.scene.control.ButtonType;

import java.util.*;
import java.util.stream.Collectors;

import dev.webfx.platform.json.*;
import dev.webfx.platform.file.FileReader;
import dev.webfx.platform.file.File;
import dev.webfx.platform.file.Blob;
import dev.webfx.platform.file.spi.BlobProvider;
import dev.webfx.extras.filepicker.FilePicker;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.resource.Resource;
import dev.webfx.platform.console.Console;

import com.orangomango.logicsim.ui.*;
import com.orangomango.logicsim.core.*;

/**
 * Logic simulator made in Java/JavaFX
 * Using AND and NOT gates you can build every other chip.
 * 
 * @author OrangoMango [https://orangomango.github.io]
 * @version 1.0
 */
public class MainApplication extends Application{
	private static final double WIDTH = 1000;
	private static final double HEIGHT = 800;
	public static final int FPS = 40;
	private static final double TOOLBAR_X = 650;
	private static final double TOOLBAR_Y = 100;

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
	private boolean rmWire = false, rmGate = false, connBus = false;
	private Pin rmW;
	private List<Wire> wiresToRemove = new ArrayList<>();
	private List<Gate> gatesToRemove = new ArrayList<>();
	private List<Pin> pinsToRemove = new ArrayList<>();
	private Point2D selectedRectanglePoint = null;
	private double selectedAreaWidth, selectedAreaHeight;
	private Point2D selectionMoveStart;
	private Point2D busStartPoint, busTempEndPoint;
	private int busAmount = 1;
	private UiTooltip tooltip;
	private Bus resizingBus = null;
	private Pin movingBusPin = null;
	private Bus connB;
	private File pickedFile;
	private static List<File> uploadedFiles = new ArrayList<>();
	private boolean rightMouseDrag = false;
	
	@Override
	public void start(Stage stage){
		StackPane pane = new StackPane();

		final Label uploadInfo = new Label("No file uploaded");
		final Label uploadedFilesInfo = new Label("No dependecies uploaded");

		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		pane.getChildren().add(canvas);

		FilePicker picker = FilePicker.create();
		picker.setGraphic(new ImageView(new Image(Resource.toUrl("/images/icon.png", MainApplication.class))));
		picker.selectedFileProperty().addListener((ob, oldV, newV) -> {
			this.pickedFile = newV;
			uploadInfo.setText(this.pickedFile.getName());
		});

		FilePicker uploader = FilePicker.create();
		uploader.setMultiple(true);
		uploader.setGraphic(new ImageView(new Image(Resource.toUrl("/images/icon.png", MainApplication.class))));
		uploader.getSelectedFiles().addListener((javafx.beans.InvalidationListener)obs -> {
			List<File> files = uploader.getSelectedFiles();
			uploadedFiles = files;
			uploadedFilesInfo.setText(uploadedFiles.stream().map(file -> file.getName()).collect(Collectors.toList()).toString());
			buildSideArea(gc);
		});

		UiButton saveButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_save.png", MainApplication.class)), "SAVE", new Rectangle2D(50, 20, 50, 50), () -> {
			File file = this.pickedFile;
			if (file != null){
				this.currentFile = file;
				save(file);
				Alert info = new Alert(Alert.AlertType.INFORMATION);
				info.setTitle("Saved");
				info.setHeaderText("File saved");
				info.setContentText("File saved successfully");
				info.showAndWait();
				buildSideArea(gc);
			}
		});
		UiButton loadButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_load.png", MainApplication.class)), "LOAD", new Rectangle2D(150, 20, 50, 50), () -> {
			File file = this.pickedFile;
			List<Gate> gates = new ArrayList<>();
			List<Wire> wires = new ArrayList<>();
			if (file != null){
				int backup = Pin.PIN_ID;
				Pin.PIN_ID = 0;
				FileReader.create().readAsText(file).onSuccess(jsonData -> {
					JsonObject json = load(jsonData, gc, gates, wires, true);
					if (json == null){
						Pin.PIN_ID = backup;
						return;
					}
					this.currentFile = file;
					this.gates = gates;
					this.wires = wires;
					buildSideArea(gc);
				});
			}
		});
		UiButton saveChipButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_savechip.png", MainApplication.class)), "SAVE CHIP", new Rectangle2D(250, 20, 50, 50), () -> {
			/*String defaultName = "";
			Color defaultColor = Color.BLUE;
			try {
				if (this.currentFile != null && this.currentFile.getName().endsWith(".lsimc")){
					BufferedReader reader = new BufferedReader(new FileReader(this.currentFile));
					StringBuilder builder = new StringBuilder();
					reader.lines().forEach(builder::append);
					reader.close();
					JSONObject json = new JSONObject(builder.toString());
					defaultColor = Color.color(json.getJSONObject("color").getDouble("red"), json.getJSONObject("color").getDouble("green"), json.getJSONObject("color").getDouble("blue"));
					defaultName = json.getString("chipName");
				}
			} catch (IOException ex){
				ex.printStackTrace();
			}
				
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Create chip");
			alert.setTitle("Create chip");
			GridPane gpane = new GridPane();
			gpane.setPadding(new Insets(5, 5, 5, 5));
			gpane.setHgap(5);
			gpane.setVgap(5);
			Label nameL = new Label("Name: ");
			TextField name = new TextField(defaultName);
			ColorPicker colorPicker = new ColorPicker(defaultColor);
			gpane.add(nameL, 0, 0);
			gpane.add(name, 1, 0);
			gpane.add(colorPicker, 0, 1, 2, 1);
			alert.getDialogPane().setContent(gpane);
			ButtonType btn = alert.showAndWait().orElse(null);
			if (btn == ButtonType.OK){
				FileChooser fc = new FileChooser();
				fc.setTitle("Save chip");
				fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicSim chips", "*.lsimc"));
				File file = this.currentFile == null ? fc.showSaveDialog(stage) : this.currentFile;
				if (file != null){
					this.currentFile = file;
					save(file, name.getText(), colorPicker.getValue());
					Alert info = new Alert(Alert.AlertType.INFORMATION);
					info.setTitle("Saved");
					info.setHeaderText("File saved");
					info.setContentText("File saved successfully");
					info.showAndWait();
					buildSideArea(gc);
				}
			}*/
		});
		UiButton clearButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_clear.png", MainApplication.class)), "CLEAR", new Rectangle2D(350, 20, 50, 50), () -> {
			this.wires = new ArrayList<Wire>();
			this.gates = new ArrayList<Gate>();
			Pin.PIN_ID = 0;
			this.currentFile = null;
		});
		UiButton rmWireButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_rmwire.png", MainApplication.class)), "RM WIRE", new Rectangle2D(450, 20, 50, 50), () -> {
			this.rmWire = true;
			this.rmGate = false;
			this.connBus = false;
		});
		UiButton rmGateButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_rmgate.png", MainApplication.class)), "RM GATE", new Rectangle2D(550, 20, 50, 50), () -> {
			this.rmGate = true;
			this.rmWire = false;
			this.connBus = false;
		});
		UiButton exportButton = new UiButton(gc, new Image(Resource.toUrl("/images/button_export.png", MainApplication.class)), "EXPORT", new Rectangle2D(650, 20, 50, 50), () -> {
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
			tempCanvas.getGraphicsContext2D().clearRect(0, 0, w, h);
			tempCanvas.getGraphicsContext2D().setFill(Color.web("#9595D3"));
			tempCanvas.getGraphicsContext2D().fillRect(0, 0, w, h);
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

			// Download the image

			Alert info = new Alert(Alert.AlertType.INFORMATION);
			info.setTitle("Saved");
			info.setHeaderText("File saved");
			info.setContentText("File saved successfully");
			info.showAndWait();
		});
		UiButton busConnectButton  = new UiButton(gc, new Image(Resource.toUrl("/images/button_connbus.png", MainApplication.class)), "CONNECT BUS", new Rectangle2D(750, 20, 50, 50), () -> {
			this.connBus = true;
			this.rmGate = false;
			this.rmWire = false;
		});;
		this.buttons.add(saveButton);
		this.buttons.add(loadButton);
		this.buttons.add(saveChipButton);
		this.buttons.add(clearButton);
		this.buttons.add(rmWireButton);
		this.buttons.add(rmGateButton);
		this.buttons.add(exportButton);
		this.buttons.add(busConnectButton);
		
		buildSideArea(gc);

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
			} else if (e.getCode() == KeyCode.R){
				boolean allBus = !this.selectedGates.stream().filter(g -> !(g instanceof Bus)).findAny().isPresent();
				if (this.selectedGates.size() > 0 && allBus){
					Gate ref = this.selectedGates.get(0);
					for (int i = 1; i < this.selectedGates.size(); i++){
						Gate g = this.selectedGates.get(i);
						if (ref.getRect().getWidth() > ref.getRect().getHeight()){
							((Bus)g).setRect(new Rectangle2D(ref.getRect().getMinX(), ref.getRect().getMinY()+i*20, ref.getRect().getWidth(), ref.getRect().getHeight()));
						} else {
							((Bus)g).setRect(new Rectangle2D(ref.getRect().getMinX()+i*20, ref.getRect().getMinY(), ref.getRect().getWidth(), ref.getRect().getHeight()));
						}
					}
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
						switch (this.selectedId){
							case 0:
								g = new Switch(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50), true);
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
								g = new Light(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 50, 50), true);
								break;
							case 3:
								g = new NotGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50), true);
								break;
							case 4:
								g = new AndGate(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50), true);
								break;
							case 5:
								File file = this.pickedFile;
								if (file == null){
									this.selectedId = -1;
									return;
								}
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), file, true);
								break;
							case 6:
								g = new Chip(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 125, 0), this.selectedChipFile, true);
								break;
							case 7:
								g = new Display7(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 0, 0), true);
								break;
							case 8:
								this.busStartPoint = clickPoint;
								break;
							case 9:
								g = new TriStateBuffer(gc, new Rectangle2D(clickPoint.getX(), clickPoint.getY(), 100, 50), true);
								break;
						}
						if (g != null){
							this.gates.add(g);
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
												Bus bus = (Bus)g;
												if (this.connBus){
													if (this.connB == null){
														this.connB = bus;
													} else {
														this.connB.connectBus(bus);
														this.connB = null;
														this.connBus = false;
													}
												} else if (g.getRect().getWidth() > g.getRect().getHeight()){
													boolean isOnBorder = bus.isOnBorder(clickPoint.getX(), clickPoint.getY());
													if (isOnBorder){
														this.resizingBus = bus;
													} else if (clickPoint.getX()-g.getRect().getMinX() > 30 && g.getRect().getMaxX()-clickPoint.getX() > 30){
														g.getPins().add(new Pin(g, new Rectangle2D(clickPoint.getX()-7.5, g.getRect().getMinY()+g.getRect().getHeight()/2-7.5, 15, 15), e.isShiftDown()));
													}
												} else {
													boolean isOnBorder = bus.isOnBorder(clickPoint.getX(), clickPoint.getY());
													if (isOnBorder){
														this.resizingBus = bus;
													} else if (clickPoint.getY()-g.getRect().getMinY() > 30 && g.getRect().getMaxY()-clickPoint.getY() > 30){
														g.getPins().add(new Pin(g, new Rectangle2D(g.getRect().getMinX()+g.getRect().getWidth()/2-7.5, clickPoint.getY()-7.5, 15, 15), e.isShiftDown()));
													}
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
										if (e.isShiftDown() && g instanceof Bus){
											this.movingBusPin = pin;
										} else {
											this.selectedId = 1;
											this.connG = pin;
										}
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
				this.connBus = false;
				this.rmW = null;
				if (found == null && foundPoint == null){
					this.selectedId = -1;
					this.pinPoints.clear();
					this.connG = null;
					this.movePoint = new Point2D(e.getX(), e.getY());
					this.deltaMove = new Point2D(0, 0);
					if (this.busStartPoint != null){
						this.busStartPoint = null;
						this.busTempEndPoint = null;
					}
				} else if (found != null && (!this.selectedGates.contains(found) || e.getClickCount() == 2)){
					ContextMenu cm = new ContextMenu();
					if (found instanceof Chip){
						MenuItem showChip = new MenuItem("Look inside");
						final Chip chip = (Chip)found;
						showChip.setOnAction(ev -> {
							Console.log(chip.getName());
							/*Alert alert = new Alert(Alert.AlertType.INFORMATION);
							alert.setTitle(chip.getName());
							alert.setHeaderText(chip.getName());
							ChipCanvas cc = new ChipCanvas(chip);
							alert.getDialogPane().setContent(cc.getPane());
							alert.showAndWait();
							cc.destroy();*/
						});
						cm.getItems().add(showChip);
					} else if (found instanceof Bus){
						MenuItem clearConn = new MenuItem("Clear connections");
						final Bus bus = (Bus)found;
						clearConn.setOnAction(ev -> bus.clearConnections());
						cm.getItems().add(clearConn);
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
					cm.getItems().add(changeLabel);
					if (pinFound != null){
						final Pin pin = pinFound;
						if (gate instanceof Bus){
							MenuItem removePin = new MenuItem("Remove pin");
							removePin.setOnAction(ev -> this.pinsToRemove.add(pin));
							cm.getItems().add(removePin);
						}
					}
					cm.show(canvas, e.getScreenX(), e.getScreenY());
				} else if (this.selectedGates.size() > 0 || this.selectedWirePoints.size() > 0){
					this.selectionMoveStart = new Point2D(e.getX(), e.getY());
					this.deltaMove = new Point2D(0, 0);
				}

				this.rightMouseDrag = true;
			}
		});

		canvas.setOnMouseMoved(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
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

			if (found != null && pinFound != null){
				String extra = pinFound.isConnected() ? "Connected" : "Disconnected";
				if (found instanceof Chip){
					this.tooltip = new UiTooltip(gc, ((Chip)found).getLabel(pinFound)+"\n"+extra, e.getX(), e.getY());
				} else {
					this.tooltip = new UiTooltip(gc, (pinFound.isInput() ? "Input pin" : "Output pin")+"\n"+extra, e.getX(), e.getY());
				}
			} else if (found != null){
				if (found instanceof Bus){
					Bus bus = (Bus)found;
					String output = "Bus #"+bus.getId()+"\nConnected:\n"+bus.getConnections().stream().mapToInt(Bus::getId).boxed().collect(Collectors.toList());
					this.tooltip = new UiTooltip(gc, output, e.getX(), e.getY());
				}
			} else {
				this.tooltip = null;
			}
		});

		canvas.setOnMouseDragged(e -> {
			this.mouseMoved = new Point2D(e.getX(), e.getY());
			Point2D clickPoint = getClickPoint(e.getX(), e.getY());
			if (this.rightMouseDrag){ // MouseButton.SECONDARY
				if ((this.selectedGates.size() == 0 && this.selectedWirePoints.size() == 0) || this.selectionMoveStart == null){
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
			} else { // MouseButton.PRIMARY
				if (this.selectedRectanglePoint != null){
					this.selectedAreaWidth = clickPoint.getX()-this.selectedRectanglePoint.getX();
					this.selectedAreaHeight = clickPoint.getY()-this.selectedRectanglePoint.getY();
				} else if (this.busStartPoint != null){
					if (Math.abs(clickPoint.getX()-this.busStartPoint.getX()) > 100 || Math.abs(clickPoint.getY()-this.busStartPoint.getY()) > 100) this.busTempEndPoint = clickPoint;
				} else if (this.resizingBus != null){
					this.resizingBus.resize(clickPoint.getX(), clickPoint.getY());
				} else if (this.movingBusPin != null){
					Gate found = this.movingBusPin.getOwner();
					if (found.getRect().getWidth() > found.getRect().getHeight()){
						if (clickPoint.getX()+7.5 > found.getRect().getMinX()+20 && clickPoint.getX()+7.5 < found.getRect().getMaxX()-20){
							this.movingBusPin.setRect(new Rectangle2D(clickPoint.getX(), this.movingBusPin.getRect().getMinY(), 15, 15));
						}
					} else {
						if (clickPoint.getY()+7.5 > found.getRect().getMinY()+20 && clickPoint.getY()+7.5 < found.getRect().getMaxY()-20){
							this.movingBusPin.setRect(new Rectangle2D(this.movingBusPin.getRect().getMinX(), clickPoint.getY(), 15, 15));
						}
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
						if (Math.abs(width) > Math.abs(height)){
							for (int i = 0; i < this.busAmount; i++) this.gates.add(new Bus(gc, Util.buildRect(new Point2D(this.busStartPoint.getX(), this.busStartPoint.getY()+i*20), width, 10)));
						} else {
							for (int i = 0; i < this.busAmount; i++) this.gates.add(new Bus(gc, Util.buildRect(new Point2D(this.busStartPoint.getX()+i*20, this.busStartPoint.getY()), 10, height)));
						}
					}
					this.busStartPoint = null;
					this.busTempEndPoint = null;
					this.selectedId = -1;
					this.busAmount = 1;
				}
				this.selectedRectanglePoint = null;
				this.resizingBus = null;
				this.movingBusPin = null;
			} else if (e.getButton() == MouseButton.SECONDARY){
				if (this.movePoint != null){
					this.movePoint = null;
					this.cameraX += this.deltaMove.getX();
					this.cameraY += this.deltaMove.getY();
				} else if (this.selectionMoveStart != null){
					this.selectionMoveStart = null;
				}

				this.rightMouseDrag = false;
			}
		});

		canvas.setOnScroll(e -> {
			if (e.getDeltaY() != 0){
				this.cameraScale += (e.getDeltaY() > 0 ? -0.05 : 0.05);
				this.cameraScale = Math.max(0.3, Math.min(this.cameraScale, 2));
			}
		});

		VBox vbox = new VBox(5, new HBox(5, picker.getView(), uploadInfo), new HBox(5, uploader.getView(), uploadedFilesInfo), pane);
		vbox.setPadding(new Insets(10, 10, 10, 10));
		Scene scene = new Scene(vbox, WIDTH+50, HEIGHT+100);

		Timeline loop = new Timeline(new KeyFrame(Duration.millis(1000.0/FPS), e -> {
			update(gc);
			if (this.rmWire || this.rmGate || this.connBus){
				vbox.setCursor(Cursor.CROSSHAIR);
			} else if (this.movePoint != null){
				vbox.setCursor(Cursor.MOVE);
			} else if (this.selectionMoveStart != null){
				vbox.setCursor(Cursor.CLOSED_HAND);
			} else if (this.selectedId >= 0 && this.selectedId != 1){
				vbox.setCursor(Cursor.HAND);
			} else if (this.resizingBus != null){
				if (this.resizingBus.getRect().getWidth() > this.resizingBus.getRect().getHeight()){
					vbox.setCursor(Cursor.H_RESIZE);
				} else {
					vbox.setCursor(Cursor.V_RESIZE);
				}
			} else {
				vbox.setCursor(Cursor.DEFAULT);
			}
			stage.setTitle("LogicSim v1.0"+(this.currentFile == null ? "" : " - "+this.currentFile.getName()));
		}));
		loop.setCycleCount(Animation.INDEFINITE);
		loop.play();

		Scheduler.schedulePeriodic(5, () -> {
			for (int i = 0; i < this.gates.size(); i++){
				Gate g = this.gates.get(i);
				g.update();
			}
		});
		
		stage.setResizable(false);
		stage.getIcons().add(new Image(Resource.toUrl("/images/icon.png", MainApplication.class)));
		stage.setScene(scene);
		stage.show();
	}

	private void buildSideArea(GraphicsContext gc){
		this.sideArea = new SideArea(gc, new Rectangle2D(950, 250, 50, 75), new Rectangle2D(TOOLBAR_X, 0, 350, 800));
		this.sideArea.addButton("Switch", () -> this.selectedId = 0);
		this.sideArea.addButton("Wire", () -> this.selectedId = 1);
		this.sideArea.addButton("Light", () -> this.selectedId = 2);
		this.sideArea.addButton("NOT gate", () -> this.selectedId = 3);
		this.sideArea.addButton("AND gate", () -> this.selectedId = 4);
		this.sideArea.addButton("Chip", () -> this.selectedId = 5);
		this.sideArea.addButton("7 segment display", () -> this.selectedId = 7);
		this.sideArea.addButton("Bus", () -> this.selectedId = 8);
		this.sideArea.addButton("Tri-state buffer", () -> this.selectedId = 9);

		this.sideArea.startSection();
		for (File file : uploadedFiles){
			String nm = file.getName();
			if (nm.endsWith(".lsimc")){
				this.sideArea.addButton(nm.substring(0, nm.lastIndexOf(".")), () -> {
					this.selectedId = 6;
					this.selectedChipFile = file;
				});
			}
		}
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

	private static File getUploadedFile(String name){
		for (File file : uploadedFiles){
			if (file.getName().equals(name)){
				return file;
			}
		}
		return null;
	}

	private void save(File file){
		save(file, null, null);
	}

	private void save(File file, String chipName, Color color){
		JsonObject json = Json.createObject();
		JsonArray data = Json.createArray();
		for (Gate gate : this.gates){
			data.push(gate.getJSON());
		}
		json.set("gates", data);
		data = Json.createArray();
		for (Wire wire : this.wires){
			data.push(wire.getJSON());
		}
		json.set("wires", data);
		if (chipName != null && color != null){
			json.set("chipName", chipName);
			JsonObject cl = Json.createObject();
			cl.set("red", color.getRed());
			cl.set("green", color.getGreen());
			cl.set("blue", color.getBlue());
			json.set("color", cl);
		}

		Blob textBlob = BlobProvider.get().createTextBlob(JsonFormatter.toJsonString(json));
		BlobProvider.get().exportBlob(textBlob, file.getName());
	}

	public static JsonObject load(String jsonData, GraphicsContext gc, List<Gate> tempGates, List<Wire> tempWires, boolean updatePinId){
		JsonObject json = Json.parseObjectSilently(jsonData);

		Map<Bus, ReadOnlyJsonArray> busConnections = new HashMap<>();

		// Load gates
		for (int i = 0; i < json.getArray("gates").size(); i++){
			ReadOnlyJsonObject gate = json.getArray("gates").getObject(i);
			String name = gate.getString("name");
			Color color = gate.getObject("color") == null ? null : Color.color(gate.getObject("color").getDouble("red"), gate.getObject("color").getDouble("green"), gate.getObject("color").getDouble("blue"));
			Rectangle2D rect = new Rectangle2D(gate.getObject("rect").getDouble("x"), gate.getObject("rect").getDouble("y"), gate.getObject("rect").getDouble("w"), gate.getObject("rect").getDouble("h"));
			List<Pin> pins = new ArrayList<>();
			for (int j = 0; j < gate.getArray("pins").size(); j++){
				ReadOnlyJsonObject pin = gate.getArray("pins").getObject(j);
				Pin p = new Pin(pin, updatePinId);
				pins.add(p);
			}
			Gate gt = null;
			if (name.equals("AND")){
				gt = new AndGate(gc, rect, false);
			} else if (name.equals("LIGHT")){
				gt = new Light(gc, rect, false);
			} else if (name.equals("NOT")){
				gt = new NotGate(gc, rect, false);
			} else if (name.equals("SWITCH")){
				gt = new Switch(gc, rect, false);
			} else if (name.equals("CHIP")){
				File chipFile = getUploadedFile(gate.getString("fileName"));
				if (chipFile == null){
					Alert error = new Alert(Alert.AlertType.ERROR);
					error.setTitle("Missing dependency");
					error.setHeaderText("Missing dependency");
					error.setContentText("The following dependency is missing: "+gate.getString("fileName"));
					error.showAndWait();
					return null;
				}
				gt = new Chip(gc, rect, chipFile, false);
			} else if (name.equals("DISPLAY7")){
				gt = new Display7(gc, rect, false);
			} else if (name.equals("BUS")){
				gt = new Bus(gc, rect, gate.getInteger("id"));
				busConnections.put((Bus)gt, gate.getArray("connections"));
			} else if (name.equals("3SBUFFER")){
				gt = new TriStateBuffer(gc, rect, false);
			}
			gt.setPins(pins);
			gt.setLabel(gate.getString("label"));
			tempGates.add(gt);
		}

		// Attach gates' pins
		for (int i = 0; i < json.getArray("gates").size(); i++){
			ReadOnlyJsonObject gate = json.getArray("gates").getObject(i);
			for (int j = 0; j < gate.getArray("pins").size(); j++){
				ReadOnlyJsonObject pin = gate.getArray("pins").getObject(j);
				int pinId = pin.getInteger("id");
				Pin currentPin = getPinById(tempGates, pinId);
				for (int k = 0; k < pin.getArray("attached").size(); k++){
					int apinId = pin.getArray("attached").getInteger(k);
					Pin apin = getPinById(tempGates, apinId);
					currentPin.attach(apin);
				}
			}
		}

		// Load wires
		for (int i = 0; i < json.getArray("wires").size(); i++){
			ReadOnlyJsonObject wire = json.getArray("wires").getObject(i);
			Pin p1 = getPinById(tempGates, wire.getInteger("pin1"));
			Pin p2 = getPinById(tempGates, wire.getInteger("pin2"));
			List<Point2D> points = new ArrayList<>();
			for (int j = 0; j < wire.getArray("points").size(); j++){
				ReadOnlyJsonObject p = wire.getArray("points").getObject(j);
				points.add(new Point2D(p.getDouble("x"), p.getDouble("y")));
			}
			tempWires.add(new Wire(gc, p1, p2, points));
		}

		// Connect buses
		for (Gate g : tempGates){
			if (g instanceof Bus){
				Bus bus = (Bus)g;
				for (int i = 0; i < busConnections.get(bus).size(); i++){
					int busId = busConnections.get(bus).getInteger(i);
					Bus attachedBus = (Bus)tempGates.stream().filter(gt -> gt instanceof Bus && ((Bus)gt).getId() == busId).findFirst().get();
					bus.connectBus(attachedBus);
				}
			}
		}

		return json;
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
		gc.setFill(Color.web("#9595D3"));
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		// Render the grid
		double gridSize = 50*this.cameraScale;
		gridSize = Math.min(100, Math.max(gridSize, 15));
		gc.save();
		gc.setStroke(Color.web("#3A5D73"));
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

		Point2D topLeft = getClickPoint(0, 0);
		Point2D bottomRight = getClickPoint(WIDTH, HEIGHT);
		Rectangle2D screen = new Rectangle2D(topLeft.getX(), topLeft.getY(), bottomRight.getX()-topLeft.getX(), bottomRight.getY()-topLeft.getY());

		for (Gate g : this.gates){
			if (!screen.intersects(g.getRect())){
				continue;
			}
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
			if (!screen.contains(w.getPin1().getX(), w.getPin1().getY()) && !screen.contains(w.getPin2().getX(), w.getPin2().getY())){
				if (!w.getPoints().stream().filter(wp -> screen.contains(wp.getX(), wp.getY())).findAny().isPresent()){
					continue;
				}
			}
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
			if (Math.abs(busWidth) > Math.abs(busHeight)){
				for (int i = 0; i < this.busAmount; i++){
					Rectangle2D busRect = Util.buildRect(new Point2D(this.busStartPoint.getX(), this.busStartPoint.getY()+i*20), busWidth, 10);
					gc.fillRect(busRect.getMinX(), busRect.getMinY(), busRect.getWidth(), busRect.getHeight());
				}
			} else {
				for (int i = 0; i < this.busAmount; i++){
					Rectangle2D busRect = Util.buildRect(new Point2D(this.busStartPoint.getX()+i*20, this.busStartPoint.getY()), 10, busHeight);
					gc.fillRect(busRect.getMinX(), busRect.getMinY(), busRect.getWidth(), busRect.getHeight());
				}
			}
		}

		// Selection rectangle
		if (this.selectedRectanglePoint != null){
			gc.save();
			gc.setFill(Color.LIME);
			gc.setGlobalAlpha(0.6);
			Rectangle2D selection = Util.buildRect(this.selectedRectanglePoint, this.selectedAreaWidth, this.selectedAreaHeight);
			gc.fillRect(selection.getMinX(), selection.getMinY(), selection.getWidth(), selection.getHeight());
			gc.setStroke(Color.GREEN);
			gc.setLineWidth(1);
			gc.strokeRect(selection.getMinX(), selection.getMinY(), selection.getWidth(), selection.getHeight());
			gc.restore();
		}

		gc.restore();

		// UI
		gc.save();
		gc.setFill(Color.BLACK);
		//gc.fillText(String.format("ID: %d\nPower: %s\nScale: %.2f", Pin.PIN_ID, Util.isPowerOn(), this.cameraScale), 60, 700);
		gc.setGlobalAlpha(0.5);
		gc.fillRect(0, 0, WIDTH, TOOLBAR_Y);
		gc.restore();

		for (UiButton ub : this.buttons){
			ub.render();
		}
		gc.setFill(Util.isPowerOn() ? Color.LIME : Color.RED);
		gc.fillRoundRect(850, 15, 45, 45, 15, 15);
		if (this.selectedId == -1) this.sideArea.render();

		if (this.tooltip != null) this.tooltip.render();

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
