package kst4contest.view;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import kst4contest.controller.ChatController;
import kst4contest.controller.Utils4KST;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ClusterMessage;

public class Kst4ContestApplication extends Application {

	String chatState;
	ChatController chatcontroller;
	Button MYQRGButton; // TODO: clean code? Got the myqrg button out of the factory method to modify
						// the text later
	Button MYCALLSetQRGButton;

	Timer timer_buildWindowTitle;
	Timer timer_chatMemberTableSortTimer; // need that because javafx bug, it´s the only way to actualize the table...
	Timer timer_updatePrivatemessageTable; // same here

	private TableView<ChatMember> initChatMemberTable() {

		TableView<ChatMember> tbl_chatMemberTable = new TableView<ChatMember>();
		tbl_chatMemberTable.setTooltip(new Tooltip(
				"Stations available \n\n Use right click to a station to select predefined texts \n\n Texts can be changed in the config-file"));

		TableColumn<ChatMember, String> callSignCol = new TableColumn<ChatMember, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty callsgn = new SimpleStringProperty();

				callsgn.setValue(cellDataFeatures.getValue().getCallSign());

				return callsgn;
			}
		});

		TableColumn<ChatMember, String> nameCol = new TableColumn<ChatMember, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				name.setValue(cellDataFeatures.getValue().getName());

				return name;
			}
		});

		TableColumn<ChatMember, String> qraCol = new TableColumn<ChatMember, String>("QRA");
		qraCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				qra.setValue(cellDataFeatures.getValue().getQra());

				return qra;
			}
		});

		TableColumn<ChatMember, String> qrgCol = new TableColumn<ChatMember, String>("QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
//				StringProperty qrg = new SimpleStringProperty();

//				qrg.setValue(cellDataFeatures.getValue().getFrequency());
//				qrg = (cellDataFeatures.getValue().getFrequency());

//				if (!qrg.getValue().equals("")) {
//					
//				}

				return cellDataFeatures.getValue().getFrequency();
			}

		});

		TableColumn<ChatMember, String> airScoutCol = new TableColumn<ChatMember, String>("AP [minutes / pot%]");
		airScoutCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty airPlaneInfo = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes() == null) {
					airPlaneInfo.setValue("nil");
				}

				else if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().size() <= 0) {
					airPlaneInfo.setValue("nil");
				} else {
					String apInfoText = ""
							+ cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
									.getArrivingDurationMinutes()
							+ " (" + cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
									.getPotential()
							+ "%)";
//					
//					
					if (cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().size() > 1) {
						apInfoText += " / "
								+ cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes().get(1)
										.getArrivingDurationMinutes()
								+ " (" + cellDataFeatures.getValue().getAirPlaneReflectInfo().getRisingAirplanes()
										.get(1).getPotential()
								+ "%)";
					}

					airPlaneInfo.setValue(apInfoText);
				}

				return airPlaneInfo;
			}
		});
		/**
		 * HIGH EXPERIMENTAL::::::::
		 */
		airScoutCol.setCellFactory(new Callback<TableColumn<ChatMember, String>, TableCell<ChatMember, String>>() {
			public TableCell call(TableColumn param) {
				return new TableCell<ChatMember, String>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!isEmpty()) {
							this.setTextFill(Color.BLACK);
							// Get fancy and change color based on data
							if (item.contains("100%")) {
								this.setTextFill(Color.BLUEVIOLET);
							} else if (item.contains("75%") && !item.contains("100%")) {
								this.setTextFill(Color.RED);
							} else if (item.contains("50%") && ((!item.contains("100%")) || (!item.contains("75%")))) {
								this.setTextFill(Color.ORANGE);
							}

							setText(item);
						}
					}
				};
			}
		});
		/**
		 * END HIGH EXPERIMENTAL::::::::
		 */

		TableColumn<ChatMember, String> workedCol = new TableColumn<ChatMember, String>("worked");
		workedCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMember, String> wkdAny_subcol = new TableColumn<ChatMember, String>("wkd");
		wkdAny_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		wkdAny_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> vhfCol_subcol = new TableColumn<ChatMember, String>("144");
		vhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked144()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		vhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> uhfCol_subcol = new TableColumn<ChatMember, String>("432");
		uhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked432()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});

		uhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> shf23_subcol = new TableColumn<ChatMember, String>("23");
		shf23_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked1240()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf23_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf13_subcol = new TableColumn<ChatMember, String>("13");
		shf13_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked2300()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf13_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf9_subcol = new TableColumn<ChatMember, String>("9");
		shf9_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked3400()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf9_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf6_subcol = new TableColumn<ChatMember, String>("6");
		shf6_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked5600()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf6_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf3_subcol = new TableColumn<ChatMember, String>("3");
		shf3_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked10G()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		shf3_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));

//		TableColumn uhfCol_subcol = new TableColumn("432"); //TODO: Worked band analysis
//        TableColumn shf23_subcol = new TableColumn("23");
//        TableColumn shf13_subcol = new TableColumn("13");
//        TableColumn shf9_subcol = new TableColumn("9");
//        TableColumn shf6_subcol = new TableColumn("6");
//        TableColumn shf3_subcol = new TableColumn("3");
		workedCol.getColumns().addAll(wkdAny_subcol, vhfCol_subcol, uhfCol_subcol, shf23_subcol, shf13_subcol,
				shf9_subcol, shf6_subcol, shf3_subcol); // TODO: automatize enabling to users bandChoice

		tbl_chatMemberTable.getColumns().addAll(callSignCol, nameCol, qraCol, qrgCol, airScoutCol, workedCol);

		tbl_chatMemberTable.setItems(chatcontroller.getLst_chatMemberList());

//		chatcontroller.getLst_chatMemberList().addListener(new ListChangeListener<ChatMember>() {
////		ObservableStringValue chatState = new SimpleStringProperty();
//		
//			@Override
//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends ChatMember> pChange) {
////				while (pChange.next()) {
////					System.out.println("List changed");
//					
//					//TODO: Das kann man ggf anders machen
//					
//					String chatState = chatcontroller.getChatPreferences().getProgramVersion() + " / "
//							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
//							+ chatcontroller.getLst_chatMemberList().size() + " users online.";
//					chatcontroller.getChatPreferences().setChatState(chatState);
//					
////					chatcontroller.getChatPreferences().setChatState(chatcontroller.getChatPreferences().getProgramVersion() + " / "
////							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
////							+ chatcontroller.getLst_chatMemberList().size() + " users online.");
////					primaryStage.setTitle("asdf");
////					primaryStage.setTitle(chatcontroller.getChatPreferences().getProgramVersion() + " / "
////							+ "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory() + " / "
////							+ chatcontroller.getLst_chatMemberList().size() + " users online.");
////				}
//			}
//		});

		tbl_chatMemberTable.getSortOrder().add(callSignCol);

		/**
		 * timer_chatMemberTableSortTimer -->
		 * This part fixes a javafx bug. The update of the Chatmember fields is (for any
		 * reason) not visible in the ui. Its neccessarry to sort the table in intervals
		 * to keep the table up to date.
		 */

		timer_chatMemberTableSortTimer = new Timer();

		timer_chatMemberTableSortTimer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				Thread.currentThread().setName("chatMemberTableSortTimer");

				Platform.runLater(() -> {

					try {
						
//						tbl_chatMemberTable.sort();

					} catch (Exception e) {
						System.out.println("[Main.java, Warning:] Table sorting (actualizing) failed this time.");
					}

					tbl_chatMemberTable.refresh();

				});
			}
		}, new Date(), 3000);

		tbl_chatMemberTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		tbl_chatMemberTable.autosize();

		return tbl_chatMemberTable;
	}

	/**
	 * Initializes the right click contextmenu for the chatmember-table, sets the
	 * clickhandler for the contextmenu out of a string array (each menuitam will be
	 * created out of exact one array-entry). These are initialized by the
	 * chatpreferences object out of the config-xml
	 * 
	 * @param menuTexts
	 * @return
	 */
//	private ContextMenu initChatMemberTableContextMenu(String[] menuTexts) { old mechanic
//
//		ContextMenu chatMemberContextMenu = new ContextMenu();
//
//		for (int i = 0; i < menuTexts.length; i++) {
//			final MenuItem menuItem = new MenuItem(menuTexts[i]);
//			menuItem.setOnAction(new EventHandler<ActionEvent>() {
//				public void handle(ActionEvent event) {
//					txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText() + menuItem.getText());
//				}
//			});
//
//			chatMemberContextMenu.getItems().add(menuItem);
//		}
//
////		MenuItem macro1 = new MenuItem("Pse Sked?");
////		macro1.setOnAction(new EventHandler<ActionEvent>() {
////	         public void handle(ActionEvent event) {
////	        	 txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText() + macro1.getText());
////	          }
////	       });
////		MenuItem macro10 = new MenuItem("Pse qrg 2m?");
////		MenuItem macro20 = new MenuItem("Pse Call at ");
////		MenuItem macro30 = new MenuItem("In qso nw, pse qrx, I will meep you");
////		MenuItem macro40 = new MenuItem("Pse qrg 70cm?");
////		MenuItem macro50 = new MenuItem("pse qrg 23cm?");
////		MenuItem macro60 = new MenuItem("____________________________________");
////		MenuItem macro70 = new MenuItem("Watch QSO history");
////
////		chatMemberContextMenu.getItems().add(macro1);
////		chatMemberContextMenu.getItems().add(macro10);
////		chatMemberContextMenu.getItems().add(macro20);
////		chatMemberContextMenu.getItems().add(macro30);
////		chatMemberContextMenu.getItems().add(macro40);
////		chatMemberContextMenu.getItems().add(macro50);
////		chatMemberContextMenu.getItems().add(macro60);
////		chatMemberContextMenu.getItems().add(macro70);
//
//		return chatMemberContextMenu;
//
//	}

	/**
	 * Initializes the right click contextmenu for the chatmember-table, sets the
	 * clickhandler for the contextmenu out of a string array (each menuitam will be
	 * created out of exact one array-entry). These are initialized by the
	 * chatpreferences object out of the config-xml
	 * 
	 * @param menuTexts
	 * @return
	 */
	private ContextMenu initChatMemberTableContextMenu(ObservableList<String> contextMenuEntries) { // new mechanic

		ContextMenu chatMemberContextMenu = new ContextMenu();

		for (Iterator iterator = contextMenuEntries.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			final MenuItem menuItem = new MenuItem(string);
			menuItem.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText() + menuItem.getText());
				}
			});

			chatMemberContextMenu.getItems().add(menuItem);

		}

//		MenuItem macro1 = new MenuItem("Pse Sked?");
//		macro1.setOnAction(new EventHandler<ActionEvent>() {
//	         public void handle(ActionEvent event) {
//	        	 txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText() + macro1.getText());
//	          }
//	       });
//		MenuItem macro10 = new MenuItem("Pse qrg 2m?");
//		MenuItem macro20 = new MenuItem("Pse Call at ");
//		MenuItem macro30 = new MenuItem("In qso nw, pse qrx, I will meep you");
//		MenuItem macro40 = new MenuItem("Pse qrg 70cm?");
//		MenuItem macro50 = new MenuItem("pse qrg 23cm?");
//		MenuItem macro60 = new MenuItem("____________________________________");
//		MenuItem macro70 = new MenuItem("Watch QSO history");
//
//		chatMemberContextMenu.getItems().add(macro1);
//		chatMemberContextMenu.getItems().add(macro10);
//		chatMemberContextMenu.getItems().add(macro20);
//		chatMemberContextMenu.getItems().add(macro30);
//		chatMemberContextMenu.getItems().add(macro40);
//		chatMemberContextMenu.getItems().add(macro50);
//		chatMemberContextMenu.getItems().add(macro60);
//		chatMemberContextMenu.getItems().add(macro70);

		return chatMemberContextMenu;

	}

	/**
	 * initializes the tableview in which the cq- and beacon-texts are shown
	 * 
	 * @return
	 */
	private TableView initChatGeneralMSGTable() {

		TableView<ChatMessage> tbl_generalMSGTable = new TableView<ChatMessage>();
		tbl_generalMSGTable.setTooltip(new Tooltip("General messages are shown here (handle it like CQ messages)"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignCol = new TableColumn<ChatMessage, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty callSign = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
				} else {

					callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return callSign;
			}
		});

		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					name.setValue(cellDataFeatures.getValue().getSender().getName());
				} else {

					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return name;
			}
		});

		TableColumn<ChatMessage, String> qrgCol = new TableColumn<ChatMessage, String>("Last QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg = cellDataFeatures.getValue().getSender().getFrequency();
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_generalMSGTable.widthProperty().divide(2));

		msgCol.setCellFactory(new Callback<TableColumn<ChatMessage, String>, TableCell<ChatMessage, String>>() {
			public TableCell call(TableColumn param) {
				return new TableCell<ChatMessage, String>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!isEmpty()) {
							this.setTextFill(Color.BLACK);
							// Get fancy and change color based on data
							if (item.toUpperCase()
									.contains(chatcontroller.getChatPreferences().getLoginCallSign().toUpperCase())) {
								this.setTextFill(Color.GREEN);
							}
							setText(item);
						}
					}
				};
			}
		});

		tbl_generalMSGTable.getColumns().addAll(timeCol, callSignCol, nameCol, msgCol, qrgCol);

		ObservableList<ChatMessage> generalMSGList = chatcontroller.getLst_toAllMessageList();
		tbl_generalMSGTable.setItems(generalMSGList);

		return tbl_generalMSGTable;
	}

	private TableView<ChatMessage> initChatprivateMSGTable() {

		TableView<ChatMessage> tbl_privateMSGTable = new TableView<ChatMessage>();
		tbl_privateMSGTable.setTooltip(new Tooltip("Private messages to you are shown here"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignCol = new TableColumn<ChatMessage, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty callSign = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
				} else {

					callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return callSign;
			}
		});

		callSignCol.setCellFactory(new Callback<TableColumn<ChatMessage, String>, TableCell<ChatMessage, String>>() {
			public TableCell call(TableColumn param) {
				return new TableCell<ChatMessage, String>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!isEmpty()) {
							this.setTextFill(Color.BLACK);
							// Get fancy and change color based on data
							if (item.contains(chatcontroller.getChatPreferences().getLoginCallSign())) {
								this.setTextFill(Color.GREEN);

//	                            AudioClip sound = new AudioClip(getClass().getResource("/K.mp3").toExternalForm());
//	                            sound.play();

							}
							setText(item);
						}
					}
				};
			}
		});

		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty name = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					name.setValue(cellDataFeatures.getValue().getSender().getName());
				} else {

					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return name;
			}
		});

		TableColumn<ChatMessage, String> qraCol = new TableColumn<ChatMessage, String>("QRA");
		qraCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					qra.setValue(cellDataFeatures.getValue().getSender().getQra());
				} else {

					qra.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qra;
			}
		});

		TableColumn<ChatMessage, String> qrgCol = new TableColumn<ChatMessage, String>("Last known QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg = cellDataFeatures.getValue().getSender().getFrequency();
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_privateMSGTable.widthProperty().divide(2.5));

		TableColumn<ChatMessage, String> airScoutCol = new TableColumn<ChatMessage, String>("AP [minutes / pot%]");
		airScoutCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			boolean isnull =false;
			
			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty airPlaneInfo = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes() == null) {
					airPlaneInfo.setValue("nil");
				}

				else if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().size() <= 0) {
					airPlaneInfo.setValue("nil");
				} else {
					String apInfoText = ""
							+ cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
							.getArrivingDurationMinutes()
							+ " (" + cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(0)
							.getPotential()
							+ "%)";
//
//
					if (cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().size() > 1) {
						apInfoText += " / "
								+ cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes().get(1)
								.getArrivingDurationMinutes()
								+ " (" + cellDataFeatures.getValue().getSender().getAirPlaneReflectInfo().getRisingAirplanes()
								.get(1).getPotential()
								+ "%)";
					}

					airPlaneInfo.setValue(apInfoText);
				}

				return airPlaneInfo;

			}
		});
		/**
		 * HIGH EXPERIMENTAL::::::::
		 */
		airScoutCol.setCellFactory(new Callback<TableColumn<ChatMessage, String>, TableCell<ChatMessage, String>>() {
			public TableCell call(TableColumn param) {
				return new TableCell<ChatMessage, String>() {

					@Override
					public void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!isEmpty()) {
							this.setTextFill(Color.BLACK);
							// Get fancy and change color based on data
							if (item.contains("100%")) {
								this.setTextFill(Color.BLUEVIOLET);
							} else if (item.contains("75%") && !item.contains("100%")) {
								this.setTextFill(Color.RED);
							} else if (item.contains("50%") && ((!item.contains("100%")) || (!item.contains("75%")))) {
								this.setTextFill(Color.ORANGE);
							}

							setText(item);
						}
					}
				};
			}
		});
		/**
		 * END HIGH EXPERIMENTAL::::::::
		 */

		tbl_privateMSGTable.getColumns().addAll(timeCol, callSignCol, nameCol, qraCol, msgCol, qrgCol, airScoutCol);

		ObservableList<ChatMessage> privateMSGList = chatcontroller.getLst_toMeMessageList();
		tbl_privateMSGTable.setItems(privateMSGList);

		return tbl_privateMSGTable;
	}

	private TableView<ClusterMessage> initDXClusterTable() {

		TableView<ClusterMessage> tbl_DXCTable = new TableView<ClusterMessage>();
		tbl_DXCTable.setTooltip(new Tooltip("Cluster Messages are shown here"));

		TableColumn<ClusterMessage, String> timeCol = new TableColumn<ClusterMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(
						new Utils4KST().time_convertEpochToReadable(cellDataFeatures.getValue().getTimeGenerated()));

				return time;
			}
		});

		TableColumn<ClusterMessage, String> callSignCol = new TableColumn<ClusterMessage, String>("Call tx");
		callSignCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty callSign = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getSender() != null) {

							callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
						} else {

							callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSign;
					}
				});

		TableColumn<ClusterMessage, String> locTXCol = new TableColumn<ClusterMessage, String>("LOC tx");
		locTXCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty locTX = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					locTX.setValue(cellDataFeatures.getValue().getSender().getQra());
				} else {

					locTX.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return locTX;
			}
		});

		TableColumn<ClusterMessage, String> callSignRXCol = new TableColumn<ClusterMessage, String>("Call rx");
		callSignRXCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty callSignRX = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getReceiver() != null) {

							callSignRX.setValue(cellDataFeatures.getValue().getReceiver().getCallSign());
						} else {

							callSignRX.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSignRX;
					}
				});

		TableColumn<ClusterMessage, String> locRXCol = new TableColumn<ClusterMessage, String>("LOC rx");
		locRXCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty locRX = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

					locRX.setValue(cellDataFeatures.getValue().getReceiver().getQra());
				} else {

					locRX.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return locRX;
			}
		});

		TableColumn<ClusterMessage, String> qrgCol = new TableColumn<ClusterMessage, String>("QRG");
		qrgCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency());
					qrg = cellDataFeatures.getValue().getReceiver().getFrequency();
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ClusterMessage, String> msgCol = new TableColumn<ClusterMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageInhibited() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageInhibited());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});

		TableColumn<ClusterMessage, String> workedCol = new TableColumn<ClusterMessage, String>("wkd");
		workedCol
				.setCellValueFactory(new Callback<CellDataFeatures<ClusterMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ClusterMessage, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						wkd.setValue(cellDataFeatures.getValue().isReceiverWkd() + "");

						if (cellDataFeatures.getValue().isReceiverWkd()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});

		tbl_DXCTable.getColumns().addAll(timeCol, callSignCol, locTXCol, callSignRXCol, locRXCol, qrgCol, msgCol,
				workedCol);

		ObservableList<ClusterMessage> clusterMSGList = chatcontroller.getLst_clusterMemberList();
		tbl_DXCTable.setItems(clusterMSGList);

		return tbl_DXCTable;
	}

	private TableView<ChatMessage> initChatToOtherMSGTable() {

		TableView<ChatMessage> tbl_toOtherMSGTable = new TableView<ChatMessage>();
		tbl_toOtherMSGTable.setTooltip(new Tooltip("Messages between other member are shown here"));

		TableColumn<ChatMessage, String> timeCol = new TableColumn<ChatMessage, String>("Time");
		timeCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty time = new SimpleStringProperty();

				time.setValue(new Utils4KST()
						.time_convertEpochToReadable(cellDataFeatures.getValue().getMessageGeneratedTime()));

				return time;
			}
		});

		TableColumn<ChatMessage, String> callSignTRCVCol = new TableColumn<ChatMessage, String>("Call TX");
		callSignTRCVCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callSign = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getSender() != null) {

							callSign.setValue(cellDataFeatures.getValue().getSender().getCallSign());
						} else {

							callSign.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callSign;
					}
				});

		TableColumn<ChatMessage, String> callSignRCVRCol = new TableColumn<ChatMessage, String>("Call RX");
		callSignRCVRCol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
						SimpleStringProperty callTX = new SimpleStringProperty();

						if (cellDataFeatures.getValue().getReceiver().getCallSign() != null) {

							callTX.setValue(cellDataFeatures.getValue().getReceiver().getCallSign());
						} else {

							callTX.setValue("");// TODO: Prevents a bug of not setting all values as a default
						}
						return callTX;
					}
				});

//		TableColumn<ChatMessage, String> nameCol = new TableColumn<ChatMessage, String>("Name");
//		nameCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {
//
//			@Override
//			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
//				SimpleStringProperty name = new SimpleStringProperty();
//
//				if (cellDataFeatures.getValue().getSender() != null) {
//
//					name.setValue(cellDataFeatures.getValue().getSender().getName());
//				} else {
//
//					name.setValue("");// TODO: Prevents a bug of not setting all values as a default
//				}
//				return name;
//			}
//		});

		TableColumn<ChatMessage, String> qrgTXerCol = new TableColumn<ChatMessage, String>("Last QRG TX");
		qrgTXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getSender().getFrequency());
					qrg = cellDataFeatures.getValue().getSender().getFrequency();
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> qrgRXerCol = new TableColumn<ChatMessage, String>("Last QRG RX");
		qrgRXerCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				StringProperty qrg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver() != null) {

//					qrg.setValue(cellDataFeatures.getValue().getReceiver().getFrequency());
					qrg = cellDataFeatures.getValue().getReceiver().getFrequency();
					
				} else {

					qrg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return qrg;
			}
		});

		TableColumn<ChatMessage, String> msgCol = new TableColumn<ChatMessage, String>("Message");
		msgCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty msg = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getMessageText() != null) {

					msg.setValue(cellDataFeatures.getValue().getMessageText());
				} else {

					msg.setValue("");// TODO: Prevents a bug of not setting all values as a default
				}
				return msg;
			}
		});
		msgCol.prefWidthProperty().bind(tbl_toOtherMSGTable.widthProperty().divide(2));

		TableColumn<ChatMessage, String> workedRXCol = new TableColumn<ChatMessage, String>("wkd RX?");
		workedRXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getReceiver().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMessage, String> workedTXCol = new TableColumn<ChatMessage, String>("wkd TX?");
		workedRXCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		tbl_toOtherMSGTable.getColumns().addAll(timeCol, callSignTRCVCol, qrgTXerCol, workedTXCol, callSignRCVRCol,
				qrgRXerCol, workedRXCol, msgCol);

		ObservableList<ChatMessage> toOtherMSGList = chatcontroller.getLst_toOtherMessageList();
		tbl_toOtherMSGTable.setItems(toOtherMSGList);

		return tbl_toOtherMSGTable;
	}

	private TableView<String> initShortcutTable() {

		TableView<String> tbl_txtShorts = new TableView<String>();
		tbl_txtShorts.setTooltip(new Tooltip("Personalize your shortcut-buttons here"));

		TableColumn<String, String> ShortCol = new TableColumn<String, String>("Shortcut-Buttontext");
		ShortCol.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<String, String> cellDataFeatures) {
				SimpleStringProperty shortCT = new SimpleStringProperty();
				shortCT.setValue(cellDataFeatures.getValue());
				return shortCT;
			}
		});
		ShortCol.setCellFactory(TextFieldTableCell.forTableColumn());

		ShortCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
			@Override
			public void handle(CellEditEvent<String, String> t) {

				String newValue = t.getNewValue();
				t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);

				if (newValue == "") { // delete lines which had been cleared
					t.getTableView().getItems().remove(t.getTablePosition().getRow());
				}

				flwPane_textSnippets.getChildren().clear();
				flwPane_textSnippets.getChildren()
						.addAll(buttonFactory(chatcontroller.getChatPreferences().getLst_txtShortCutBtnList()));

//TODO: redraw panel
//				chatMessageContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList()); // TODO: thats not
//																									// clean, there had
//																									// to be a listener
//																									// triggered update
//																									// method
//				chatMemberContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList());

			}
		});

		tbl_txtShorts.getColumns().addAll(ShortCol);

		tbl_txtShorts.setEditable(true);
//		tbl_txtSnips.set

//		ObservableList<String> lst_textSnipList = );
		tbl_txtShorts.setItems(chatcontroller.getChatPreferences().getLst_txtShortCutBtnList());
//		tbl_txtSnips.bind

		return tbl_txtShorts;
	} // TODO: Textsnippets table

	private TableView<String> initTextSnippetsTable() {

		TableView<String> tbl_txtSnips = new TableView<String>();
		tbl_txtSnips.setTooltip(new Tooltip("Personalize your textsnippets here"));

//		TableColumn<Integer, String> idCol = new TableColumn<Integer, String>("Index");
//		idCol.setCellValueFactory(new Callback<CellDataFeatures<Integer, String>, ObservableValue<Integer>>() {
//
//			@Override
//			public ObservableValue<String> call(CellDataFeatures<Integer, String> cellDataFeatures) {
//				int index = 0;
//
////				index.setValue(cellDataFeatures.getValue().);
//
//				return (index);
//			}
//		});

		TableColumn<String, String> snipCol = new TableColumn<String, String>("Snippet");
		snipCol.setCellValueFactory(new Callback<CellDataFeatures<String, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<String, String> cellDataFeatures) {
				SimpleStringProperty snippet = new SimpleStringProperty();
				snippet.setValue(cellDataFeatures.getValue());
				return snippet;
			}
		});
		snipCol.setCellFactory(TextFieldTableCell.forTableColumn());
		// TODO: https://www.youtube.com/watch?v=M_kp20qrtLw = tutorial dafuer

//		snipCol.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).replace(".*", e.getNewValue()));

		snipCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
			@Override
			public void handle(CellEditEvent<String, String> t) {

				String newValue = t.getNewValue();
				t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);

				if (newValue == "") { // delete lines which had been cleared
					t.getTableView().getItems().remove(t.getTablePosition().getRow());
				}

				chatMessageContextMenu = initChatMemberTableContextMenu(
						chatcontroller.getChatPreferences().getLst_txtSnipList()); // TODO: thats not
				// clean, there had
				// to be a listener
				// triggered update
				// method
				chatMemberContextMenu = initChatMemberTableContextMenu(
						chatcontroller.getChatPreferences().getLst_txtSnipList());

			}
		});

		tbl_txtSnips.getColumns().addAll(snipCol);

		tbl_txtSnips.setEditable(true);
//		tbl_txtSnips.set

//		ObservableList<String> lst_textSnipList = );
		tbl_txtSnips.setItems(chatcontroller.getChatPreferences().getLst_txtSnipList());
//		tbl_txtSnips.bind

		return tbl_txtSnips;
	}

	private TableView<ChatMember> initWkdStnTable() {

		TableView<ChatMember> tbl_chatMemberWkdDBTable = new TableView<ChatMember>();
		tbl_chatMemberWkdDBTable.setTooltip(new Tooltip("worked info DB"));

		TableColumn<ChatMember, String> callSignCol = new TableColumn<ChatMember, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty callsgn = new SimpleStringProperty();

				callsgn.setValue(cellDataFeatures.getValue().getCallSign());

				return callsgn;
			}
		});

		TableColumn<ChatMember, String> workedCol = new TableColumn<ChatMember, String>("worked");
		workedCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		TableColumn<ChatMember, String> wkdAny_subcol = new TableColumn<ChatMember, String>("wkd");
		wkdAny_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		wkdAny_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> vhfCol_subcol = new TableColumn<ChatMember, String>("144");
		vhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked144()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});
		vhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> uhfCol_subcol = new TableColumn<ChatMember, String>("432");
		uhfCol_subcol
				.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

					@Override
					public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
						SimpleStringProperty wkd = new SimpleStringProperty();

						if (cellDataFeatures.getValue().isWorked432()) {
							wkd.setValue("X");
						} else {
							wkd.setValue("");
						}

						return wkd;
					}
				});

		uhfCol_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(28));

		TableColumn<ChatMember, String> shf23_subcol = new TableColumn<ChatMember, String>("23");
		shf23_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked1240()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf23_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf13_subcol = new TableColumn<ChatMember, String>("13");
		shf13_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked2300()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf13_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(30));

		TableColumn<ChatMember, String> shf9_subcol = new TableColumn<ChatMember, String>("9");
		shf9_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked3400()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf9_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf6_subcol = new TableColumn<ChatMember, String>("6");
		shf6_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked5600()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});
		shf6_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		TableColumn<ChatMember, String> shf3_subcol = new TableColumn<ChatMember, String>("3");
		shf3_subcol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				if (cellDataFeatures.getValue().isWorked10G()) {
					wkd.setValue("X");
				} else {
					wkd.setValue("");
				}

				return wkd;
			}
		});

		shf3_subcol.prefWidthProperty().bind(tbl_chatMemberWkdDBTable.widthProperty().divide(32));

		workedCol.getColumns().addAll(wkdAny_subcol, vhfCol_subcol, uhfCol_subcol, shf23_subcol, shf13_subcol,
				shf9_subcol, shf6_subcol, shf3_subcol); // TODO: automatize enabling to users bandChoice

		tbl_chatMemberWkdDBTable.getColumns().addAll(callSignCol, workedCol);

		tbl_chatMemberWkdDBTable.setItems(chatcontroller.getLst_DBBasedWkdCallSignList());

		// TODO: https://www.youtube.com/watch?v=M_kp20qrtLw = tutorial dafuer

//		snipCol.setOnEditCommit(e->e.getTableView().getItems().get(e.getTablePosition().getRow()).replace(".*", e.getNewValue()));

//		callSignCol.setOnEditCommit(new EventHandler<CellEditEvent<String, String>>() {
//			@Override
//			public void handle(CellEditEvent<String, String> t) {
//
//				String newValue = t.getNewValue();
//				t.getTableView().getItems().set(t.getTablePosition().getRow(), newValue);
//				
//				if (newValue == "") { //delete lines which had been cleared
//					t.getTableView().getItems().remove(t.getTablePosition().getRow());
//				}
//
//				chatMessageContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList()); // TODO: thats not
//																									// clean, there had
//																									// to be a listener
//																									// triggered update
//																									// method
//				chatMemberContextMenu = initChatMemberTableContextMenu(chatcontroller.getChatPreferences().getLst_txtSnipList());
//
//			}
//		});

//		tbl_chatMemberWkdDBTable.getColumns().addAll(callSignCol);

		tbl_chatMemberWkdDBTable.setEditable(true);
//		tbl_txtSnips.set

//		ObservableList<String> lst_textSnipList = );
//		tbl_wkdStn.setItems(chatcontroller.getChatPreferences().getLst_txtSnipList());
//		tbl_txtSnips.bind

		return tbl_chatMemberWkdDBTable;
	}

	private MenuBar initMenuBar() {

		Menu fileMenu = new Menu("File");

		// create menuitems
		MenuItem m1 = new MenuItem("Disconnect");
		m1.setDisable(true);

		MenuItem m10 = new MenuItem("Exit + disconnect");
		m10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				closeWindowEvent(null);
			}
		});

		// add menu items to menu
		fileMenu.getItems().add(m1);
		fileMenu.getItems().add(m10);

		Menu optionsMenu = new Menu("Options");
		MenuItem options1 = new MenuItem("Set QRG as name in Chat");
		MenuItem options10 = new MenuItem("Show options");
		options10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (settingsStage.isShowing()) {
					settingsStage.hide();
				} else {
					settingsStage.show();
				}
			}
		});

		optionsMenu.getItems().addAll(options1, options10);

		Menu macroMenu = new Menu("Macros");

		MenuItem macro1 = new MenuItem("Pse Sked?");
		MenuItem macro10 = new MenuItem("Pse qrg 2m?");
		MenuItem macro20 = new MenuItem("Pse Call at ");
		MenuItem macro30 = new MenuItem("In qso nw, pse qrx, I will meep you");
		MenuItem macro40 = new MenuItem("Pse qrg 70cm?");
		MenuItem macro50 = new MenuItem("pse qrg 23cm?");

		macroMenu.getItems().addAll(macro1, macro10, macro20, macro30, macro40, macro50);

		Menu windowMenu = new Menu("Windows");
		MenuItem window1 = new MenuItem("Hide cluster / stranger QSOs");
		window1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (clusterAndQSOMonStage.isShowing()) {
					clusterAndQSOMonStage.hide();
					window1.setText("Show cluster / stranger QSOs");
				} else {
					clusterAndQSOMonStage.show();
					window1.setText("Hide cluster / stranger QSOs");
				}
			}
		});
		MenuItem window20 = new MenuItem("hide options");
		window20.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (settingsStage.isShowing()) {
					window20.setText("show options");
					settingsStage.hide();
				} else {
					settingsStage.show();
					window20.setText("hide options");
				}
			}
		});

		windowMenu.getItems().addAll(window1, window20);

		Menu helpMenu = new Menu("Info");

		MenuItem help1 = new MenuItem("No help here.");
		MenuItem help2 = new MenuItem("Support the chatclient development via PayPal");
		MenuItem help3 = new MenuItem("_______________________");
		help3.setDisable(true);
		MenuItem help4 = new MenuItem("Visit DARC X08-Homepage");
		MenuItem help5 = new MenuItem("_______________________");
		help5.setDisable(true);
		MenuItem help6 = new MenuItem("Contact the author using default mail app");
		MenuItem help8 = new MenuItem("Join kst4Contest newsgroup");
		MenuItem help9 = new MenuItem("Download the changelog / roadmap");

		// Changelog
		// https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV

		help2.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://www.paypal.com/paypalme/do5amf");

//				Alert a = new Alert(AlertType.INFORMATION);
//		        
//		        a.setTitle("About this software");
//		        a.setHeaderText("Who made it and how can you support it?");
//		        a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
//		        a.show();
			}
		});

		help4.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("http://www.x08.de");

//				Alert a = new Alert(AlertType.INFORMATION);
//		        
//		        a.setTitle("About this software");
//		        a.setHeaderText("Who made it and how can you support it?");
//		        a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
//		        a.show();
			}
		});

		help6.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("mailto:praktimarc+kst4contest@gmail.com");

			}
		});

		help8.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://groups.google.com/g/kst4contest/about");

			}
		});

		help9.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices()
						.showDocument("https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV");

			}
		});

		MenuItem help10 = new MenuItem("About...");
		help10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				Alert a = new Alert(AlertType.INFORMATION);

				a.setTitle("About kst4contest");
				a.setHeaderText("ON4KST Chatclient by DO5AMF");
				a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
				a.show();
			}
		});

//		helpMenu.getItems().add(help1);
		helpMenu.getItems().addAll(help2, help4, help5, help6, help8, help9, help10);

//		helpMenu.getItems().add(help2);
//		helpMenu.getItems().add(help4);
//		
//		helpMenu.getItems().add(help10);

		MenuBar menubar = new MenuBar();
		menubar.getMenus().addAll(fileMenu, optionsMenu, windowMenu, helpMenu); // macromenu deleted

		return menubar;

	}

	TextField txt_chatMessageUserInput = new TextField();
	TextField txt_ownqrg = new TextField();
	TextField txt_myQTF = new TextField();
	Button btnOptionspnlConnect;
	ContextMenu chatMessageContextMenu; // public due need to update it on modify
	ContextMenu chatMemberContextMenu;// public due need to update it on modify
	FlowPane flwPane_textSnippets;

	Stage clusterAndQSOMonStage;

	Stage settingsStage;

	/**
	 * Generates buttons out of pre made Strings, one button per given string in the
	 * buttontext-array. Buttonclick will add the buttontext + " " to the
	 * send-Textfield <br/>
	 * <br/>
	 * 
	 * <b>ATTENTION: MYQRG-Button adds myqrg-textfield-string. <br/>
	 * For identification of the button in the dom and make it functional, the
	 * init-value have to be "MYQRG"! </b>
	 * 
	 * @param buttonText
	 * @return
	 */
	private Node[] buttonFactory(ObservableList<String> shortcuts) {

		Button[] txMessageButtons = new Button[shortcuts.size()];

		for (int i = 0; i < shortcuts.size(); i++) {

			txMessageButtons[i] = new Button(shortcuts.get(i));

			if (shortcuts.get(i).equals("MYQRG")) {
				txMessageButtons[i].setTooltip(new Tooltip("MYQRG"));
				txMessageButtons[i]
						.setStyle("-fx-background-color:\r\n" + "        linear-gradient(#c8fac0, #c8fac0),\r\n"
								+ "        radial-gradient(center 50% -40%, radius 200%, #c8ee36 45%, #c0c800 50%);\r\n"
								+ "    -fx-background-radius: 6, 5;\r\n" + "    -fx-background-insets: 0, 1;\r\n"
								+ "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );\r\n"
								+ "    -fx-text-fill: #395306");
				MYQRGButton = txMessageButtons[i];

			}

			if (shortcuts.get(i).equals("/SETNAME MYQRG")) {
//				txMessageButtons[i].setTooltip(new Tooltip("Set your qrg as name in Chat"));
				txMessageButtons[i]
						.setStyle("-fx-background-color:\r\n" + "        linear-gradient(#c8fac0, #c8fac0),\r\n"
								+ "        radial-gradient(center 50% -40%, radius 200%, #c8ee36 45%, #c0c800 50%);\r\n"
								+ "    -fx-background-radius: 6, 5;\r\n" + "    -fx-background-insets: 0, 1;\r\n"
								+ "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );\r\n"
								+ "    -fx-text-fill: #395306");
				MYCALLSetQRGButton = txMessageButtons[i];
			}

			txMessageButtons[i].setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent arg0) {

					if (((Button) arg0.getSource()).getText().equals("MYQRG")) {
						((Button) arg0.getSource()).setTooltip(new Tooltip("MYQRG"));

					}

					if (((Button) arg0.getSource()).getTooltip() != null) {

						if (((Button) arg0.getSource()).getText().equals("MYQRG")
								|| ((Button) arg0.getSource()).getTooltip().getText().equals("MYQRG")) {

//							((Button) arg0.getSource()).setText(txt_ownqrg.getText());
							((Button) arg0.getSource()).setTooltip(new Tooltip("MYQRG"));

							if (((Button) arg0.getSource()).getTooltip().getText().equals("MYQRG")) {
//								((Button) arg0.getSource()).setText(txt_ownqrg.getText());
								txt_chatMessageUserInput
										.setText(txt_chatMessageUserInput.getText() + txt_ownqrg.getText() + " ");
							}
						}
					} else {

//						System.out.println("Button clicked " + ((Button) arg0.getSource()).getText());
						txt_chatMessageUserInput.setText(
								txt_chatMessageUserInput.getText() + ((Button) arg0.getSource()).getText() + " ");

					}

				}
			});
		}

		return txMessageButtons;
	}

	@Override
	public void stop() {
		System.out.println("[Main.java, Info:] Stage is closing, killing all resources");
		timer_buildWindowTitle.purge();
		timer_buildWindowTitle.cancel();

		timer_chatMemberTableSortTimer.purge();
		timer_chatMemberTableSortTimer.cancel();

		timer_updatePrivatemessageTable.purge();
		timer_updatePrivatemessageTable.cancel();
		chatcontroller.disconnect("CLOSEALL");

//	    Platform.exit();

	}

	@Override
	public void start(Stage primaryStage) throws InterruptedException, IOException {
//		this.primaryStage = primaryStage;
//		ChatCategory category = new ChatCategory(0); //TODO: get the Category out of the preferences-object

		ChatMember ownChatMemberObject = new ChatMember();
//		ownChatMemberObject.setCallSign("DM5M");
//		ownChatMemberObject.setPassword("antennen");
//		ownChatMemberObject.setName("QRO 15dBd");
//		ownChatMemberObject.setQra("JO51IJ");

		chatcontroller = new ChatController(ownChatMemberObject); // instantiate the Chatcontroller with the user object
//		chatcontroller.execute(); //TODO:THAT IS THE MAIN POINT WHERE THE CHAT WILL BE STARTED --- MOVED TO CONNECT BUTTON EVENTHANDLER

//		System.out.println(chatcontroller.getChatMemberTable().size());

		try {
			txt_ownqrg.setStyle("-fx-text-inner-color: #BA55D3;");

			txt_ownqrg.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
						Boolean newPropertyValue) {
					if (newPropertyValue) {
//			            System.out.println("Textfield on focus");
						// Do nothing until field loses focus, user will enter his frequency
					} else {
						System.out.println(
								"[Main.java, Info]: Set the frequency property by hand to: " + txt_ownqrg.getText());
//			            chatcontroller.getChatPreferences().setMYQRG(txt_ownqrg.getText());
						chatcontroller.getChatPreferences().getMYQRG().set(txt_ownqrg.getText());
						;
//			            MYQRGButton.setText(txt_ownqrg.getText());
					}
				}
			});

			txt_myQTF.setStyle("-fx-text-inner-color: #BA55D3;");

			txt_myQTF.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
						Boolean newPropertyValue) {
					if (newPropertyValue) {
//			            System.out.println("Textfield on focus");
						// Do nothing until field loses focus, user will enter his frequency
					} else {
						System.out.println(
								"[Main.java, Info]: Set the MYQTF property by hand to: " + txt_myQTF.getText());
						chatcontroller.getChatPreferences().getActualQTF().set(Integer.parseInt(txt_myQTF.getText()));
					}
				}
			});

			txt_myQTF.setPrefSize(40, 0);
//			txt_ownqrg.setMinSize(40, 0);
			txt_myQTF.setAlignment(Pos.BASELINE_RIGHT);
			txt_myQTF.setTooltip(new Tooltip("Enter/update your actual qtf here for using path suggestions"));

			SplitPane mainWindowLeftSplitPane = new SplitPane();
			mainWindowLeftSplitPane.setOrientation(Orientation.HORIZONTAL);

			BorderPane bPaneChatWindow = new BorderPane();

			Scene scene = new Scene(bPaneChatWindow, 1024, 768);

//			primaryStage.setTitle(this.chatcontroller.getChatPreferences().getChatState());

//			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			MenuBar mainScreenMenuBar = initMenuBar();
			bPaneChatWindow.setTop(mainScreenMenuBar);

//			bPaneChatWindow.setLeft(new Label("This will be at the left"));  

//			bPaneChatWindow.setRight(scrollabeUserListPanel);  

			SplitPane messageSectionSplitpane = new SplitPane();
			messageSectionSplitpane.setOrientation(Orientation.VERTICAL);

			HBox textInputFlowPane = new HBox();

//			FlowPane textInputFlowPane = new FlowPane();

			Button sendButton = new Button("send");
			sendButton.setMinSize(60, 0);
			sendButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {

					ChatMessage sendMe = new ChatMessage();
					sendMe.setMessageText(txt_chatMessageUserInput.getText());
					sendMe.setMessageDirectedToServer(false);

					chatcontroller.getMessageTXBus().add(sendMe);

					txt_chatMessageUserInput.clear();

				}
			});

			Button btn_clear = new Button("clear");
			btn_clear.setMinSize(60, 0);
			btn_clear.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
//			    	System.out.println("clear clicked: " + event.toString());
					txt_chatMessageUserInput.clear();

				}
			});

//			TextField txt_chatMessageUserInput
//			txt_chatMessageUserInput.setPrefWidth("80%");
			txt_chatMessageUserInput.setPrefSize(500, 0);
			txt_chatMessageUserInput.setText("");
			txt_chatMessageUserInput.setTooltip(new Tooltip("Textmessage to Chat"));
			txt_chatMessageUserInput.setOnKeyPressed(new EventHandler<KeyEvent>() {

				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.ENTER)) {
//			        	System.out.println("Enter pressed");

						ChatMessage sendMe = new ChatMessage();
						sendMe.setMessageText(txt_chatMessageUserInput.getText());
						sendMe.setMessageDirectedToServer(false);

						chatcontroller.getMessageTXBus().add(sendMe);

						txt_chatMessageUserInput.clear();
					}
				}
			});
			txt_chatMessageUserInput.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

					if (txt_chatMessageUserInput.getText().contains("MYQRG")) {
						System.out.println("MYQRG erkannt");

//						txt_chatMessageUserInput.getText().replaceAll("MYQRG", chatcontroller.getChatPreferences().getMYQRG());
//						txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("MYQRG", chatcontroller.getChatPreferences().getMYQRG()));
						txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("MYQRG",
								chatcontroller.getChatPreferences().getMYQRG().getValue()));
					}
					;

					if (txt_chatMessageUserInput.getText().contains("MYLOCATOR")) {
						System.out.println("MYLOCATOR erkannt");

//						txt_chatMessageUserInput.getText().replaceAll("MYQRG", chatcontroller.getChatPreferences().getMYQRG());
						txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("MYLOCATOR",
								chatcontroller.getChatPreferences().getLoginLocator()));
					}
					;
				}
			});

			final Separator sepVert1 = new Separator();
			sepVert1.setOrientation(Orientation.VERTICAL);
			sepVert1.setValignment(VPos.CENTER);
//	        sepVert1.setPrefHeight(80);
			sepVert1.setPrefWidth(30);

			txt_ownqrg.setText("MYQRG");
			txt_ownqrg.setPrefSize(80, 0);
//			txt_ownqrg.setMinSize(40, 0);
			txt_ownqrg.setAlignment(Pos.BASELINE_RIGHT);
//			System.out.println(txt_ownqrg.textProperty();

			primaryStage.setTitle(chatcontroller.getChatPreferences().getChatState());

			timer_buildWindowTitle = new Timer();
			timer_buildWindowTitle.scheduleAtFixedRate(new TimerTask() {
				public void run() {

					Thread.currentThread().setName("buildWindowTitleTimer");

					Platform.runLater(() -> {

						String chatState = "";
						if (chatcontroller.isConnectedAndLoggedIn()) {

							chatState = "Connected to: " + chatcontroller.getChatPreferences().getLoginChatCategory()
									+ " as " + chatcontroller.getChatPreferences().getLoginCallSign() + " ("
									+ chatcontroller.getChatPreferences().getLoginName() + ")" + " in "
									+ chatcontroller.getChatPreferences().getLoginLocator() + " ("
									+ chatcontroller.getLst_chatMemberList().size() + " users online.),"
									+ (chatcontroller.getLst_toAllMessageList().size()
											+ chatcontroller.getLst_toOtherMessageList().size()
											+ chatcontroller.getLst_toMeMessageList().size())
									+ " messages total.";
							chatcontroller.getChatPreferences().setChatState(chatState);
						}

						else {
							chatState = "DISCONNECTED, CHECK YOUR INTERNET-CONNECTION!";
						}

						primaryStage.setTitle(chatcontroller.getChatPreferences().getChatState());

//						System.out.println(chatcontroller.getChatPreferences().getChatState());
					});
				}
			}, new Date(), 5000);

			textInputFlowPane.getChildren().addAll(txt_chatMessageUserInput, sendButton, btn_clear, sepVert1,
					txt_ownqrg, txt_myQTF);

//			HBox hbx_textSnippets = new HBox();

			flwPane_textSnippets = new FlowPane();

			flwPane_textSnippets.getChildren()
					.addAll(buttonFactory(this.chatcontroller.getChatPreferences().getLst_txtShortCutBtnList()));

//			hbx_textSnippets.getChildren().add(flwPane_textSnippets);
//			hbx_textSnippets.set				

			TableView<ChatMessage> privateMessageTable = initChatprivateMSGTable();
//
//			ContextMenu chatMessageContextMenu = initChatMemberTableContextMenu( old mechanic
//					this.chatcontroller.getChatPreferences().getTextSnippets());

			chatMessageContextMenu = initChatMemberTableContextMenu(
					this.chatcontroller.getChatPreferences().getLst_txtSnipList()); // new mechanic

			privateMessageTable.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMessageContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());

					}
				}
			});

			TableViewSelectionModel<ChatMessage> privateChatselectionModelChatMessage = privateMessageTable
					.getSelectionModel();
			privateChatselectionModelChatMessage.setSelectionMode(SelectionMode.SINGLE);

//			tbl_chatMember.getda

			ObservableList<ChatMessage> selectedChatMessageList = privateChatselectionModelChatMessage
					.getSelectedItems();
			selectedChatMessageList.addListener(new ListChangeListener<ChatMessage>() {
				@Override
				public void onChanged(Change<? extends ChatMessage> selectedChatMemberPrivateChat) {
					if (privateChatselectionModelChatMessage.getSelectedItems().isEmpty()) {
						// do nothing, that was a deselection-event!
					} else {

						txt_chatMessageUserInput.clear();
						txt_chatMessageUserInput.setText("/cq "
								+ selectedChatMemberPrivateChat.getList().get(0).getSender().getCallSign() + " ");
						System.out.println("privChat selected ChatMember: "
								+ selectedChatMemberPrivateChat.getList().get(0).getSender());
						// selectedChatMemberList.clear();
//						selectionModelChatMember.clearSelection(0);
					}
				}
			});

			timer_updatePrivatemessageTable = new Timer();
			timer_updatePrivatemessageTable.scheduleAtFixedRate(new TimerTask() {

				public void run() {
					Thread.currentThread().setName("UpdatePrivateMessageTableTimer");
					Platform.runLater(() -> {

						privateMessageTable.refresh();

					});
				}
			}, new Date(), 5000);

			messageSectionSplitpane.getItems().addAll(privateMessageTable, flwPane_textSnippets, textInputFlowPane,
					initChatGeneralMSGTable());

			bPaneChatWindow.setCenter(mainWindowLeftSplitPane);

			TableView<ChatMember> tbl_chatMember = new TableView<ChatMember>();
			tbl_chatMember = initChatMemberTable();

			TableViewSelectionModel<ChatMember> selectionModelChatMember = tbl_chatMember.getSelectionModel();
			selectionModelChatMember.setSelectionMode(SelectionMode.SINGLE);

			tbl_chatMember.autosize();

//			tbl_chatMember.getda

			ObservableList<ChatMember> selectedChatMemberList = selectionModelChatMember.getSelectedItems();
			selectedChatMemberList.addListener(new ListChangeListener<ChatMember>() {
				@Override
				public void onChanged(Change<? extends ChatMember> selectedChatMember) {
					if (selectionModelChatMember.getSelectedItems().isEmpty()) {
						// do nothing, that was a deselection-event!
					} else {

						txt_chatMessageUserInput.clear();
						txt_chatMessageUserInput
								.setText("/cq " + selectedChatMember.getList().get(0).getCallSign() + " ");
						System.out.println(
								"##################selected ChatMember: " + selectedChatMember.getList().get(0));
						// selectedChatMemberList.clear();
//						selectionModelChatMember.clearSelection(0);
					}
				}
			});

			// TODO: Take together contextmenu and macromenu, generate together

			// Creates the Contextmenu for right clicks to the chatmember-list
			// TODO: If the old selection is identical with the new selection, /CQ station
			// will not be written by the contextmenu clicklistener. Have to improve that
			// some time
//			ContextMenu chatMemberContextMenu = initChatMemberTableContextMenu(
//					this.chatcontroller.getChatPreferences().getTextSnippets()); old mechanic

			chatMemberContextMenu = initChatMemberTableContextMenu(
					this.chatcontroller.getChatPreferences().getLst_txtSnipList());

			tbl_chatMember.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMemberContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());

					}
				}
			});

			mainWindowLeftSplitPane.getItems().addAll(messageSectionSplitpane, tbl_chatMember);

			primaryStage.setScene(scene);

			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}

		clusterAndQSOMonStage = new Stage();
//		clusterAndQSOMonStage.initStyle(StageStyle.UTILITY);
		clusterAndQSOMonStage.setTitle("Cluster & QSO of the other");
		SplitPane pnl_directedMSGWin = new SplitPane();
		pnl_directedMSGWin.setOrientation(Orientation.VERTICAL);

		pnl_directedMSGWin.getItems().addAll(initDXClusterTable(), initChatToOtherMSGTable());

		clusterAndQSOMonStage.setScene(new Scene(pnl_directedMSGWin, 700, 500));
		clusterAndQSOMonStage.show();

		/*****************************************************************************
		 * 
		 * Settings Scene
		 * 
		 * 
		 ****************************************************************************/
		settingsStage = new Stage();
		settingsStage.setTitle("Change Client seetings");

		BorderPane optionsPanel = new BorderPane();

		TabPane tabPaneOptions = new TabPane();

		/*************************************************************************************
		 * 
		 * Stations settings Tab in settings scene
		 * 
		 *************************************************************************************/

		GridPane grdPnlStation = new GridPane();
		grdPnlStation.setPadding(new Insets(10, 10, 10, 10));
		grdPnlStation.setVgap(5);
		grdPnlStation.setHgap(5);

		Label lblCallSign = new Label("Login-Callsign:");
//        TextField txtFldCallSign = new TextField("dm5m");
		TextField txtFldCallSign = new TextField(this.chatcontroller.getChatPreferences().getLoginCallSign());

		txtFldCallSign.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				System.out.println("[Main.java, Info]: Setted the Login Callsign: " + txtFldCallSign.getText());
				chatcontroller.getChatPreferences().setLoginCallSign(txtFldCallSign.getText());
			}
		});

		Label lblPassword = new Label("Login-Password:");
		PasswordField txtFldPassword = new PasswordField();
		txtFldPassword.setText(this.chatcontroller.getChatPreferences().getLoginPassword());
		txtFldPassword.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				System.out.println("[Main.java, Info]: Setted the Login password... ");
				chatcontroller.getChatPreferences().setLoginPassword(txtFldPassword.getText());
			}
		});

		Label lblName = new Label("Name in Chat:");
		TextField txtFldName = new TextField(this.chatcontroller.getChatPreferences().getLoginName());

		txtFldName.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				System.out.println("[Main.java, Info]: Setted the Login name: " + txtFldName.getText());
				chatcontroller.getChatPreferences().setLoginName(txtFldName.getText());
			}
		});

		Label lblLocator = new Label("Locator in Chat:");
		TextField txtFldLocator = new TextField(this.chatcontroller.getChatPreferences().getLoginLocator());

		txtFldLocator.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				System.out.println("[Main.java, Info]: Setted the Login locator: " + txtFldLocator.getText());
				chatcontroller.getChatPreferences().setLoginLocator(txtFldLocator.getText());
			}
		});

		Label lblChatCategory = new Label("Chatcategory:");
		ChoiceBox<ChatCategory> choiceBxChatChategory = new ChoiceBox<ChatCategory>();
		ChatCategory chatCategoryChoice = new ChatCategory(0);
		choiceBxChatChategory.setValue(this.chatcontroller.getChatPreferences().getLoginChatCategory());

		for (int i = 0; i < chatCategoryChoice.getPossibleCategoryNumbers().length; i++) {
			ChatCategory temp = new ChatCategory(i + 1);
			choiceBxChatChategory.getItems().add(temp);
		}

		choiceBxChatChategory.getSelectionModel().selectedItemProperty()
				.addListener((ChangeListener) (ov, old, newval) -> {
					ChatCategory idx = (ChatCategory) newval;
					System.out.println("Changed Choice: "
							+ choiceBxChatChategory.getSelectionModel().selectedItemProperty().toString());
					this.chatcontroller.getChatPreferences()
							.setLoginChatCategory(new ChatCategory(idx.getCategoryNumber()));
					btnOptionspnlConnect.setText("Connect to " + choiceBxChatChategory.getSelectionModel()
							.selectedItemProperty().get().getChatCategoryName(
									choiceBxChatChategory.getSelectionModel().getSelectedItem().getCategoryNumber()));

//        	this.chatcontroller.getChatPreferences().setLoginChatCategory(idx);
				});

//        choiceBxChatChategory.getItems().add("2"); //hard coded...
//        choiceBxChatChategory.setValue("2"); //setting default value

//        HBox labeledSeparator = new HBox();        
//        Label lblStationPanelInfo = new Label();
//        Separator leftSeparator = new Separator();
//        leftSeparator.setPrefWidth(100);
//        Separator rightSeparator = new Separator();
//        rightSeparator.setPrefWidth(100);
//        labeledSeparator.getChildren().add(leftSeparator);
//        labeledSeparator.getChildren().add(lblStationPanelInfo);
//        labeledSeparator.getChildren().add(rightSeparator);
//        labeledSeparator.setAlignment(Pos.CENTER);

		grdPnlStation.add(lblCallSign, 0, 0);
		grdPnlStation.add(txtFldCallSign, 1, 0);
		grdPnlStation.add(lblPassword, 0, 1);
		grdPnlStation.add(txtFldPassword, 1, 1);
		grdPnlStation.add(lblName, 0, 2);
		grdPnlStation.add(txtFldName, 1, 2);
		grdPnlStation.add(lblLocator, 0, 3);
		grdPnlStation.add(txtFldLocator, 1, 3);
		grdPnlStation.add(lblChatCategory, 0, 4);
		grdPnlStation.add(choiceBxChatChategory, 1, 4);

		VBox vbxStation = new VBox();
		vbxStation.setPadding(new Insets(10, 10, 10, 10));
		vbxStation.getChildren().addAll(
				generateLabeledSeparator(100, "Set your Login Credentials and Station Parameters here"), grdPnlStation);
		vbxStation.getChildren().addAll(generateLabeledSeparator(50,
				"Don´t forget to reset the worked stations information before starting a new contest!"));

		/*************************************************************************************
		 * Log synch settings Tab
		 *************************************************************************************/

		GridPane grdPnlLog = new GridPane();
		grdPnlLog.setPadding(new Insets(10, 10, 10, 10));
		grdPnlLog.setVgap(5);
		grdPnlLog.setHgap(5);

		Label lblEnableFileBased = new Label("Use universal File based callsign Interpreter (readOnly!)");
		CheckBox chkBxEnableFileBasedInterpreterUCX = new CheckBox();
		chkBxEnableFileBasedInterpreterUCX
				.setSelected(this.chatcontroller.getChatPreferences().isLogsynch_fileBasedWkdCallInterpreterEnabled());

		chkBxEnableFileBasedInterpreterUCX.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//                chk2.setSelected(!newValue);
				chatcontroller.getChatPreferences().setLogsynch_fileBasedWkdCallInterpreterEnabled(
						chkBxEnableFileBasedInterpreterUCX.isSelected());
				System.out.println("[Main.java, Info]: setted the file based worked-station-list to: "
						+ chatcontroller.getChatPreferences().isLogsynch_fileBasedWkdCallInterpreterEnabled());
			}
		});

		Label lblWkdInterpreterPathToFileTitle = new Label("Worked stations will be read there: ");
		Label lblWkdInterpreterPathToFile = new Label(
				this.chatcontroller.getChatPreferences().getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());

		Label lblUDPbyUCXLogBackupFilePathAndNameTitle = new Label("Backup UDP msgs to");
		Label lblUDPbyUCXLogBackupFilePathAndName = new Label(
				this.chatcontroller.getChatPreferences().getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup());

		Label lblEnableUDPbyUCX = new Label("Receive UCXLog network based UDP log messages");
		CheckBox chkBxEnableUCXLogUDPReceiver = new CheckBox();
		chkBxEnableUCXLogUDPReceiver
				.setSelected(this.chatcontroller.getChatPreferences().isLogsynch_ucxUDPWkdCallListenerEnabled());
		chkBxEnableUCXLogUDPReceiver.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//                chk2.setSelected(!newValue);
				chatcontroller.getChatPreferences()
						.setLogsynch_ucxUDPWkdCallListenerEnabled(chkBxEnableUCXLogUDPReceiver.isSelected());
				System.out.println("[Main.java, Info]: setted the udp worked-station receiver to: "
						+ chatcontroller.getChatPreferences().isLogsynch_ucxUDPWkdCallListenerEnabled());
			}
		});

		Label lblUDPByUCX = new Label("UDP-Port for message-listener (default is 12060)");
		TextField txtFldUDPPortforUCX = new TextField("");
		txtFldUDPPortforUCX
				.setText(this.chatcontroller.getChatPreferences().getLogsynch_ucxUDPWkdCallListenerPort() + "");
		txtFldUDPPortforUCX.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
					if (GuiUtils.isNumeric(txtFldUDPPortforUCX.getText())) {

						System.out.println("[Main.java, Info]: Set the ucx-listener port property by hand to: "
								+ txtFldUDPPortforUCX.getText());
//		            chatcontroller.getChatPreferences().setMYQRG(txt_ownqrg.getText());
						chatcontroller.getChatPreferences()
								.setLogsynch_ucxUDPWkdCallListenerPort(Integer.parseInt(txtFldUDPPortforUCX.getText()));
//		            MYQRGButton.setText(txt_ownqrg.getText());
					} else {
						txtFldUDPPortforUCX.setText(txtFldUDPPortforUCX.getText() + " is an invalid Port");
					}

				}
			}
		});

		HBox labeledSeparatorLogSynch = new HBox();
		
		Button btn_changeFilePathAndName = new Button("Choose...");
		
		btn_changeFilePathAndName.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				File filechooserSelectedfile;
				
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Choose Readonly-Loginterpreter-File");
				fileChooser.setInitialDirectory(
			            new File(System.getProperty("user.home"))
			        ); 
				
				try {
					
					filechooserSelectedfile = fileChooser.showOpenDialog(primaryStage);
					
				} catch (NullPointerException e) {
					
					filechooserSelectedfile = new File(chatcontroller.getChatPreferences().getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());
				}
				
				System.out.println(filechooserSelectedfile.getAbsolutePath());
				
				chatcontroller.getChatPreferences().setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(filechooserSelectedfile.getAbsolutePath());

				lblWkdInterpreterPathToFile.setText(chatcontroller.getChatPreferences().getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());
				
				
			}
		});
		
		
		
		

//		grdPnlLog.add(new Label("Settings for the file interpreter, which can interprete ASCII Callsigns out of all kinds of files by Patternmatching"), 0,0,1,1);
		grdPnlLog.add(generateLabeledSeparator(100, "File polling for worked callsigns"), 0, 0, 2, 1);
		grdPnlLog.add(lblEnableFileBased, 0, 1);
		grdPnlLog.add(chkBxEnableFileBasedInterpreterUCX, 1, 1);
		grdPnlLog.add(lblWkdInterpreterPathToFileTitle, 0, 2);
		grdPnlLog.add(lblWkdInterpreterPathToFile, 1, 2);
		grdPnlLog.add(btn_changeFilePathAndName, 2, 2);
		grdPnlLog.add(generateLabeledSeparator(100, "N1MM/UCXLog/DXLog.net Network-Listener"), 0, 3, 2, 1);
		grdPnlLog.add(lblEnableUDPbyUCX, 0, 4);
		grdPnlLog.add(chkBxEnableUCXLogUDPReceiver, 1, 4);
		grdPnlLog.add(lblUDPByUCX, 0, 5);
		grdPnlLog.add(txtFldUDPPortforUCX, 1, 5);
//		grdPnlLog.add(lblUDPbyUCXLogBackupFilePathAndNameTitle, 0, 6); removed due to db usage now
//		grdPnlLog.add(lblUDPbyUCXLogBackupFilePathAndName, 1, 6); removed due to db usage now
//		grdPnlLog.add(new Button("Change..."), 2, 6); removed due to db usage now

		VBox vbxLog = new VBox();
		vbxLog.setPadding(new Insets(10, 10, 10, 10));
		vbxLog.getChildren().addAll(grdPnlLog);

		/*************************************************************************************
		 * TRX synch settings Tab
		 *************************************************************************************/

		GridPane grdPnltrx = new GridPane();
		grdPnltrx.setPadding(new Insets(10, 10, 10, 10));
		grdPnltrx.setVgap(5);
		grdPnltrx.setHgap(5);

		Label lblEnableTRXMsgbyUCX = new Label("Receive UCXLog network based UDP trx messages at Port 12060");
		CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		chkBxEnableTRXMsgbyUCX
				.setSelected(this.chatcontroller.getChatPreferences().isTrxSynch_ucxLogUDPListenerEnabled());

		chkBxEnableTRXMsgbyUCX.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//                chk2.setSelected(!newValue);
				if (!newValue) {
					chatcontroller.getChatPreferences()
							.setTrxSynch_ucxLogUDPListenerEnabled(chkBxEnableTRXMsgbyUCX.isSelected());
					txt_ownqrg.textProperty().unbind();
					txt_ownqrg.setTooltip(new Tooltip("Your cq qrg will be updated by hand (watch prefs!)"));
					System.out.println("[Main.java, Info]: MYQRG will be changed only by User input");
					System.out.println("[Main.java, Info]: setted the trx-frequency updated by ucxlog to: "
							+ chatcontroller.getChatPreferences().isTrxSynch_ucxLogUDPListenerEnabled());

				} else {
					chatcontroller.getChatPreferences()
							.setTrxSynch_ucxLogUDPListenerEnabled(chkBxEnableTRXMsgbyUCX.isSelected());
					txt_ownqrg.textProperty().bind(chatcontroller.getChatPreferences().getMYQRG());
					txt_ownqrg.setTooltip(new Tooltip("Your cq qrg will be updated by the log program (watch prefs!)"));
					System.out.println("[Main.java, Info]: setted the trx-frequency updated by ucxlog to: "
							+ chatcontroller.getChatPreferences().isTrxSynch_ucxLogUDPListenerEnabled());
				}
			}
		});

		// Thats the default behaviour of the myqrg textfield
		if (this.chatcontroller.getChatPreferences().isTrxSynch_ucxLogUDPListenerEnabled()) {
			txt_ownqrg.setTooltip(new Tooltip("Your cq qrg will be updated by the log program (watch prefs!)"));
			txt_ownqrg.textProperty().bind(this.chatcontroller.getChatPreferences().getMYQRG());// TODO: Bind darf nur
																								// gemacht werden, wenn
																								// ucxlog-Frequenznachrichten
																								// ausgewerttet werden!
//        	System.out.println("[Main.java, Info]: MYQRG will be changed only by UCXListener");
		} else {
			txt_ownqrg.setTooltip(new Tooltip("enter your cq qrg here"));
//        	System.out.println("[Main.java, Info]: MYQRG will be changed only by User input");
			txt_ownqrg.textProperty().addListener((observable, oldValue, newValue) -> {

				System.out.println(
						"[Main.java, Info]: MYQRG  Text changed from " + oldValue + " to " + newValue + " by hand");
				MYQRGButton.textProperty().set(newValue);
			});

		}

		grdPnltrx.add(generateLabeledSeparator(100, "Receive UCXLog TRX info"), 0, 0, 2, 1);
		grdPnltrx.add(lblEnableTRXMsgbyUCX, 0, 1);
		grdPnltrx.add(chkBxEnableTRXMsgbyUCX, 1, 1);

		VBox vbxTRXSynch = new VBox();
		vbxTRXSynch.setPadding(new Insets(10, 10, 10, 10));
		vbxTRXSynch.getChildren().addAll(grdPnltrx);

		/*************************************************************************************
		 * Airscout settings Tab
		 *************************************************************************************/

		GridPane grdPnlAirScout = new GridPane();
		grdPnlAirScout.setPadding(new Insets(10, 10, 10, 10));
		grdPnlAirScout.setVgap(5);
		grdPnlAirScout.setHgap(5);

		Label lblASEnableUDPMsgbyAS = new Label("Enable Airscout integration (needs AirScout >= V 0.9.9.5)");
		Label lblASServerName = new Label("Servername (AS options>network>UDP Server settings) [AS]:");
		Label lblASChatClientName = new Label("ChatClient name for returning server answers [KST]:");
		Label lblASUdpPort = new Label("Network-UDP-Port for Airscout-Server communication [9872]:");
		Label lblASBandName = new Label("Band-Setting for Airscout-Queries [1440000]:");

		CheckBox chkBxEnableUDPMsgbyAS = new CheckBox();
		chkBxEnableUDPMsgbyAS.setSelected(this.chatcontroller.getChatPreferences().isAirScout_asUDPListenerEnabled());

		chkBxEnableUDPMsgbyAS.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				chatcontroller.getChatPreferences()
						.setAirScout_asUDPListenerEnabled(chkBxEnableUDPMsgbyAS.isSelected());
				System.out.println("[Main.java, Info]: AS communication enabled: " + newValue);
			}
		});

		TextField txtFld_asServerNameString = new TextField(
				chatcontroller.getChatPreferences().getAirScout_asServerNameString());
		txtFld_asServerNameString.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
//					if (GuiUtils.isNumeric(txtFldUDPPortforUCX.getText())) {

					System.out.println(
							"[Main.java, Info]: AS server name resetted to: " + txtFld_asServerNameString.getText());
					chatcontroller.getChatPreferences()
							.setAirScout_asServerNameString(txtFld_asServerNameString.getText());

//					} else {
//						txtFldUDPPortforUCX.setText(txtFldUDPPortforUCX.getText() + " is an invalid Port");
//					}

				}
			}
		});

		TextField txtFld_asClientNameString = new TextField(
				chatcontroller.getChatPreferences().getAirScout_asClientNameString());
		txtFld_asClientNameString.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
//					if (GuiUtils.isNumeric(txtFldUDPPortforUCX.getText())) {

					System.out.println(
							"[Main.java, Info]: AS client name resetted to: " + txtFld_asClientNameString.getText());
					chatcontroller.getChatPreferences()
							.setAirScout_asServerNameString(txtFld_asClientNameString.getText());

//					} else {
//						txtFldUDPPortforUCX.setText(txtFldUDPPortforUCX.getText() + " is an invalid Port");
//					}

				}
			}
		});

		TextField txtFld_asUDPPortInt = new TextField(
				chatcontroller.getChatPreferences().getAirScout_asCommunicationPort() + "");
		txtFld_asUDPPortInt.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
					if (GuiUtils.isNumeric(txtFld_asUDPPortInt.getText())) {

						System.out.println("[Main.java, Info]: AS port resetted to: " + txtFld_asUDPPortInt.getText());
						chatcontroller.getChatPreferences()
								.setAirScout_asCommunicationPort((Integer.parseInt(txtFld_asUDPPortInt.getText())));

					} else {
						txtFld_asUDPPortInt.setText(txtFld_asUDPPortInt.getText() + " is an invalid Port");
					}

				}
			}
		});

		TextField txtFld_asQRGInt = new TextField(chatcontroller.getChatPreferences().getAirScout_asBandString() + "");
		txtFld_asQRGInt.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
					if (GuiUtils.isNumeric(txtFld_asQRGInt.getText())) {

						System.out.println("[Main.java, Info]: AS qrg resetted to: " + txtFld_asQRGInt.getText());
						chatcontroller.getChatPreferences()
								.setAirScout_asBandString(((Integer.parseInt(txtFld_asQRGInt.getText()))) + "");
						;

					} else {
						txtFld_asQRGInt.setText(txtFld_asQRGInt.getText() + " is an invalid bandvalue");
					}

				}
			}
		});

		grdPnlAirScout.add(generateLabeledSeparator(100, "Settings for Airscout-network-communication"), 0, 0, 2, 1);
		grdPnlAirScout.add(lblASEnableUDPMsgbyAS, 0, 1);
		grdPnlAirScout.add(chkBxEnableUDPMsgbyAS, 1, 1);
		grdPnlAirScout.add(lblASServerName, 0, 2);
		grdPnlAirScout.add(txtFld_asServerNameString, 1, 2);
		grdPnlAirScout.add(lblASChatClientName, 0, 3);
		grdPnlAirScout.add(txtFld_asClientNameString, 1, 3);

		grdPnlAirScout.add(lblASUdpPort, 0, 4);
		grdPnlAirScout.add(txtFld_asUDPPortInt, 1, 4);
		grdPnlAirScout.add(lblASBandName, 0, 5);
		grdPnlAirScout.add(txtFld_asQRGInt, 1, 5);

		VBox vbxAirScout = new VBox();
		vbxAirScout.setPadding(new Insets(10, 10, 10, 10));
		vbxAirScout.getChildren().addAll(grdPnlAirScout);

		/*************************************************************************************
		 * Notification settings Tab
		 *************************************************************************************/

		GridPane grdPnlNotify = new GridPane();
		grdPnlNotify.setPadding(new Insets(10, 10, 10, 10));
		grdPnlNotify.setVgap(5);
		grdPnlNotify.setHgap(5);

		Label lblNitificationInfo = new Label(
				"Switch bands, prefix worked by others alert, direction notifications, notification pattern matchers");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		grdPnlNotify.add(generateLabeledSeparator(100, "Notification settings"), 0, 0, 2, 1);
		grdPnlNotify.add(lblNitificationInfo, 0, 1);
//        grdPnltrx.add(chkBxEnableTRXMsgbyUCX, 1, 1);

		VBox vbxNotify = new VBox();
		vbxNotify.setPadding(new Insets(10, 10, 10, 10));
		vbxNotify.getChildren().addAll(grdPnlNotify);

		/*************************************************************************************
		 * shorts & snippets tab
		 *************************************************************************************/

		GridPane grdPnlShorts = new GridPane();
		grdPnlShorts.setPadding(new Insets(10, 10, 10, 10));
		grdPnlShorts.setVgap(5);
		grdPnlShorts.setHgap(5);

//        Label lblEnableTRXMsgbyUCX = new Label("Receive UCXLog network based UDP trx messages");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		grdPnlShorts.add(generateLabeledSeparator(100, "Set the shortcut-Buttons (above Sendtext-field)"), 0, 0, 2, 1);

		TableView<String> tblVw_shortcuts = new TableView<String>();
		tblVw_shortcuts = initShortcutTable();
		tblVw_shortcuts.setItems(this.chatcontroller.getChatPreferences().getLst_txtShortCutBtnList());

		Button btn_Short_addLine = new Button("Add new shorcut-button");
		btn_Short_addLine.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				String newTextSnippet = "CHANGE THIS TEXT VIA DOUBLECLICK or remove by deleting all text. Then hit enter key";
				chatcontroller.getChatPreferences().getLst_txtShortCutBtnList().add(0, newTextSnippet);
			}
		});

		Button btn_Short_changePosPlus = new Button("move marked down");
		btn_Short_changePosPlus.setDisable(true);
		Button btn_Short_changePosMinus = new Button("move marked up");
		btn_Short_changePosMinus.setDisable(true);

		HBox hbxTxtShortBtnBox = new HBox();

		grdPnlShorts.add(hbxTxtShortBtnBox, 0, 2, 2, 1);
		hbxTxtShortBtnBox.getChildren().addAll(btn_Short_addLine, btn_Short_changePosPlus, btn_Short_changePosMinus);

		grdPnlShorts.add(tblVw_shortcuts, 0, 1, 2, 1);

		TableView<String> tblVw_textsnippets = new TableView<String>();
		tblVw_textsnippets = initTextSnippetsTable();
		tblVw_textsnippets.setItems(this.chatcontroller.getChatPreferences().getLst_txtSnipList());

		grdPnlShorts.add(tblVw_textsnippets, 0, 4, 2, 1);

		Button btn_Snip_addLine = new Button("Add new snippet");
		btn_Snip_addLine.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				String newTextSnippet = "CHANGE THIS TEXT VIA DOUBLECLICK or remove by deleting all text. Then hit enter key";
				chatcontroller.getChatPreferences().getLst_txtSnipList().add(0, newTextSnippet);
			}
		});

		Button btn_Snbip_changePosPlus = new Button("move marked down");
		btn_Snbip_changePosPlus.setDisable(true);
		Button btn_Snip_changePosMinus = new Button("move marked up");
		btn_Snip_changePosMinus.setDisable(true);

		HBox hbxTxtSnipBtnBox = new HBox();

		grdPnlShorts.add(hbxTxtSnipBtnBox, 0, 5, 2, 1);
		hbxTxtSnipBtnBox.getChildren().addAll(btn_Snip_addLine, btn_Snbip_changePosPlus, btn_Snip_changePosMinus);

//        grdPnlShorts.add(lblEnableTRXMsgbyUCX, 0, 1);
//        grdPnlShorts.add(chkBxEnableTRXMsgbyUCX, 1, 1);
		grdPnlShorts.add(generateLabeledSeparator(100, "Set the Text-snippets (userlist right-click textsnippets)"), 0,
				3, 2, 1);

		VBox vbxShorts = new VBox();
		vbxShorts.setPadding(new Insets(10, 10, 10, 10));
		vbxShorts.getChildren().addAll(grdPnlShorts);

		/*************************************************************************************
		 * Beacons / CQ messages
		 *************************************************************************************/

		GridPane grdPnlBeacon = new GridPane();
		grdPnlBeacon.setPadding(new Insets(10, 10, 10, 10));
		grdPnlBeacon.setVgap(5);
		grdPnlBeacon.setHgap(5);

//        Label lblEnableTRXMsgbyUCX = new Label("Receive UCXLog network based UDP trx messages");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		grdPnlBeacon.add(generateLabeledSeparator(100, "Set the Beacon (autointervalled CQ messages to public chat)"),
				0, 0, 2, 1);
		grdPnlBeacon.add(new Label("Enable CQ-like beacons:"), 0, 1);
		CheckBox chkBxBeaconsEnabled = new CheckBox();
		chkBxBeaconsEnabled.setSelected(this.chatcontroller.getChatPreferences().isBcn_beaconsEnabled());

		chkBxBeaconsEnabled.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				chatcontroller.getChatPreferences().setBcn_beaconsEnabled(chkBxBeaconsEnabled.isSelected());
				System.out.println("[Main.java, Info]: Beacons turned on: " + newValue);
			}
		});

		grdPnlBeacon.add(chkBxBeaconsEnabled, 1, 1);

		grdPnlBeacon.add(new Label("Beacon message [<100 Chars]:"), 0, 2);

		TextField txtFldBeaconText = new TextField(this.chatcontroller.getChatPreferences().getBcn_beaconText());
		grdPnlBeacon.add(txtFldBeaconText, 1, 2);
		txtFldBeaconText.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
					System.out.println("[Main.java, Info]: Set the beacon text to: "
							+ chatcontroller.getChatPreferences().getBcn_beaconText());
//		            chatcontroller.getChatPreferences().setMYQRG(txt_ownqrg.getText());

					if (txtFldBeaconText.getText().length() <= 120) {
						chatcontroller.getChatPreferences().setBcn_beaconText(txtFldBeaconText.getText());
					} else {
						txtFldBeaconText.setText(
								"That was too long, settig " + chatcontroller.getChatPreferences().getBcn_beaconText());
					}
//		            MYQRGButton.setText(txt_ownqrg.getText());
				}
			}
		});

		grdPnlBeacon.add(new Label("Beacon-interval [minutes, >=5]:"), 0, 3);
		TextField txtFldBeaconInterval = new TextField();
		txtFldBeaconInterval.setText(this.chatcontroller.getChatPreferences().getBcn_beaconIntervalInMinutes() + "");

		txtFldBeaconInterval.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue) {
//		            System.out.println("Textfield on focus");
					// Do nothing until field loses focus, user will enter his frequency
				} else {
					if (GuiUtils.isNumeric(txtFldBeaconInterval.getText())) {

//		            chatcontroller.getChatPreferences().setMYQRG(txt_ownqrg.getText());
						chatcontroller.getChatPreferences()
								.setBcn_beaconIntervalInMinutes((Integer.parseInt(txtFldBeaconInterval.getText())));
						System.out.println("[Main.java, Info]: resetted the beacon-interval to: "
								+ txtFldBeaconInterval.getText());

					} else {
						txtFldBeaconInterval.setText(txtFldBeaconInterval.getText() + " is an invalid time value");
					}

				}
			}
		});

		grdPnlBeacon.add(txtFldBeaconInterval, 1, 3);

		VBox vbxBeacon = new VBox();
		vbxBeacon.setPadding(new Insets(10, 10, 10, 10));
		vbxBeacon.getChildren().addAll(grdPnlBeacon);

		/*************************************************************************************
		 * Unworked beacon PM
		 *************************************************************************************/

		GridPane grdPnlUnwkdStnBeacon = new GridPane();
		grdPnlUnwkdStnBeacon.setPadding(new Insets(10, 10, 10, 10));
		grdPnlUnwkdStnBeacon.setVgap(5);
		grdPnlUnwkdStnBeacon.setHgap(5);

//        Label lblEnableTRXMsgbyUCX = new Label("Receive UCXLog network based UDP trx messages");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		grdPnlUnwkdStnBeacon.add(generateLabeledSeparator(100,
				"Set the unworked penetrator Beacons (intervalled PM to unworked stations)"), 0, 0, 2, 1);
//        grdPnlShorts.add(lblEnableTRXMsgbyUCX, 0, 1);
//        grdPnlShorts.add(chkBxEnableTRXMsgbyUCX, 1, 1);

		VBox vbxUnwkdStnBeacon = new VBox();
		vbxUnwkdStnBeacon.setPadding(new Insets(10, 10, 10, 10));
		vbxUnwkdStnBeacon.getChildren().addAll(grdPnlUnwkdStnBeacon);

		/*************************************************************************************
		 * Internal database section / worked stations
		 *************************************************************************************/

		GridPane grdPnlInternalDBPane = new GridPane();
		grdPnlInternalDBPane.setPadding(new Insets(10, 10, 10, 10));
		grdPnlInternalDBPane.setVgap(5);
		grdPnlInternalDBPane.setHgap(5);

//        Label lblEnableTRXMsgbyUCX = new Label("Receive UCXLog network based UDP trx messages");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		grdPnlInternalDBPane.add(
				generateLabeledSeparator(100,
						"Change the settings of the internal database (worked stations, reset before a new contest!)"),
				0, 0, 2, 1);
//        grdPnlShorts.add(lblEnableTRXMsgbyUCX, 0, 1);
//        grdPnlShorts.add(chkBxEnableTRXMsgbyUCX, 1, 1);

		VBox vbxInternalDB = new VBox();
		vbxInternalDB.setPadding(new Insets(10, 10, 10, 10));
		vbxInternalDB.getChildren().addAll(grdPnlInternalDBPane);

		TableView<ChatMember> tblVw_worked = new TableView<ChatMember>();
		tblVw_worked = initWkdStnTable();
//		tblVw_worked.setItems(); TODO

		Button btn_wkdDB_reset = new Button("Reset worked-data");
		btn_wkdDB_reset.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				// TODO: get the way to the appcontroller, there should be a reset method which
				// drives the db and resets the 2 worked lists, also.

				int affectedLines;
				affectedLines = chatcontroller.getDbHandler().resetWorkedDataInDB();
				chatcontroller.resetWorkedInfoInGuiLists();
				
				
				if (affectedLines >= 0) {

					Alert a = new Alert(AlertType.INFORMATION);

					a.setTitle("Worked data");
					a.setHeaderText("All worked data had been resetted." + affectedLines
							+ " worked callsign entries resetted.");
//		        a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
					a.show();

				} else {
					Alert a = new Alert(AlertType.INFORMATION);

					a.setTitle("Worked data");
					a.setHeaderText("Something went wrong, DB have to be rebuilt or other error!");
//		        a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
					a.show();
				}
				
				

//				System.out.println("DB reset via DBHandler needs to be implemented");
			}
		});

		HBox hbxwkdShortBtnBox = new HBox();

		grdPnlInternalDBPane.add(hbxwkdShortBtnBox, 0, 2, 2, 1);
		hbxwkdShortBtnBox.getChildren().addAll(btn_wkdDB_reset);

		grdPnlInternalDBPane.add(tblVw_worked, 0, 1, 2, 1);

		/*************************************************************************************
		 * Internal database section / End
		 *************************************************************************************/

		/**
		 * Building the options tabpanel
		 */

		Tab tbStationSettings = new Tab("Station", vbxStation);
		Tab tbLogSynchSet = new Tab("Log synch", vbxLog);
		Tab tbTRXSynchSet = new Tab("TRX synch", vbxTRXSynch);
		Tab tbAirScoutSettings = new Tab("Airscout", vbxAirScout);
		Tab tbNotify = new Tab("Notification", vbxNotify);
		Tab tbShorts = new Tab("Shortcuts", vbxShorts);
//        Tab tbMacro = new Tab("Macros" , new Label("Set the right clickable Macros"));
		Tab tbBeacon = new Tab("Beacon", vbxBeacon);
		Tab tbUnwkd = new Tab("Unworkedstn requester", vbxUnwkdStnBeacon);
		Tab tbInternalDB = new Tab("Workedstn database", vbxInternalDB);

		tabPaneOptions.getTabs().addAll(tbStationSettings, tbLogSynchSet, tbTRXSynchSet, tbAirScoutSettings, tbNotify,
				tbShorts, tbBeacon, tbUnwkd, tbInternalDB);

		optionsPanel.setLeft(tabPaneOptions);

		HBox vbxButtons = new HBox();
		vbxButtons.setPadding(new Insets(20, 20, 20, 20));

		Button btnOptionsPnlApply = new Button("Apply/Close prefs");
		btnOptionsPnlApply.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				settingsStage.hide();
			}
		});

		Button btnOptionspnlDisconnect = new Button("Disconnect & close Chat");
		btnOptionspnlDisconnect.setDisable(false);

		btnOptionspnlDisconnect.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				closeWindowEvent(null);

			}
		});

		Button btnOptionspnlDisconnectOnly = new Button("Disconnect");
		btnOptionspnlDisconnectOnly.setDisable(true);

		btnOptionspnlDisconnect.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				closeWindowEvent(null);

			}
		});

		btnOptionspnlConnect = new Button("Connect to " + chatcontroller.getChatPreferences().getLoginChatCategory()
				.getChatCategoryName(choiceBxChatChategory.getSelectionModel().getSelectedItem().getCategoryNumber()));
		btnOptionspnlConnect.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				chatcontroller.getChatPreferences().setLoginCallSign(txtFldCallSign.getText());
				chatcontroller.getChatPreferences().setLoginPassword(txtFldPassword.getText());
				chatcontroller.getChatPreferences().setLoginLocator(txtFldLocator.getText());
				chatcontroller.getChatPreferences().setLoginName(txtFldName.getText());
				chatcontroller.getChatPreferences()
						.setLoginChatCategory(choiceBxChatChategory.getSelectionModel().getSelectedItem());

				// Todo: here is where all settings has to be written to the preferences
				// instance
//		    	ownChatMemberObject.setCallSign(txtFldCallSign.getText());
//		    	ownChatMemberObject.setPassword(txtFldPassword.getText());
//		    	ownChatMemberObject.setQra(txtFldLocator.getText());
//		    	ownChatMemberObject.setName(txtFldName.getText());
//		    	ownChatMemberObject.setChatCategory(choiceBxChatChategory.getSelectionModel().getSelectedItem());
//		    	chatcontroller.getChatPreferences().setLoginChatCategory(chatCategoryChoice);

				System.out.println("[Info] Main.java: connect clicked, using "
						+ chatcontroller.getChatPreferences().getLoginCallSign() + " / "
						+ chatcontroller.getChatPreferences().getLoginPassword() + " / "
						+ chatcontroller.getChatPreferences().getLoginName() + " / "
						+ chatcontroller.getChatPreferences().getLoginLocator() + " at category "
						+ choiceBxChatChategory.getSelectionModel().getSelectedItem());

				try {
					chatcontroller.execute(); // TODO:THAT IS THE MAIN POINT WHERE THE CHAT WILL BE STARTED...MUST CATCH
												// Passwordfailedexc in future
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					btnOptionspnlConnect.setDisable(false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					btnOptionspnlConnect.setDisable(false);
				}
				txtFldCallSign.setDisable(true);
				txtFldPassword.setDisable(true);
				txtFldName.setDisable(true);
				txtFldLocator.setDisable(true);
				choiceBxChatChategory.setDisable(true);
				btnOptionspnlConnect.setDisable(true);
				btnOptionspnlDisconnect.setDisable(false);

			}
		});

		Button btn_preferences_saveAsDefault = new Button("Save settings");
		btn_preferences_saveAsDefault.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				System.out.println("saved");

				chatcontroller.getChatPreferences().writePreferencesToXmlFile();
				Alert a = new Alert(AlertType.INFORMATION);

				a.setTitle("Info");
				a.setHeaderText("Settings are stored as default to the xml config file:");
				a.setContentText(chatcontroller.getChatPreferences().getStoreAndRestorePreferencesFileName());
				a.show();

//				ChatMessage sendMe = new ChatMessage();
//				sendMe.setMessageText(txt_chatMessageUserInput.getText());
//				sendMe.setMessageDirectedToServer(false);
//
//				chatcontroller.getMessageTXBus().add(sendMe);
//
//				txt_chatMessageUserInput.clear();

			}
		});

		vbxButtons.getChildren().addAll(btnOptionspnlConnect, btn_preferences_saveAsDefault, btnOptionsPnlApply,
				btnOptionspnlDisconnect, btnOptionspnlDisconnectOnly);

		AnchorPane anchorPaneOkAndSave = new AnchorPane();
		AnchorPane.setRightAnchor(vbxButtons, 10d);
		AnchorPane.setBottomAnchor(vbxButtons, 10d);

		anchorPaneOkAndSave.getChildren().addAll(vbxButtons);

		optionsPanel.setBottom(anchorPaneOkAndSave);
//        optionsPanel.setAlignment(vbxButtons, Pos.CENTER);;

//        VBox vBox = new VBox(tabPaneOptions);
		settingsStage.setScene(new Scene(optionsPanel, 720, 768));

//		settingsStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);

		settingsStage.show();

	}

	/**
	 * 
	 * @param width,           left and right of the label
	 * @param labelofSeperator Info text
	 * @return
	 */
	public HBox generateLabeledSeparator(int width, String labelofSeperator) {

		HBox labeledSeparator = new HBox();
		Label lblInfo = new Label(labelofSeperator);
		Separator leftSeparator = new Separator();
		leftSeparator.setPrefWidth(width);
		Separator rightSeparator = new Separator();
		rightSeparator.setPrefWidth(width);
		labeledSeparator.getChildren().add(leftSeparator);
		labeledSeparator.getChildren().add(lblInfo);
		labeledSeparator.getChildren().add(rightSeparator);
		labeledSeparator.setAlignment(Pos.CENTER);

		return labeledSeparator;
	}

	/**
	 * Handles the close action of the Chatwindow
	 * 
	 * @param event
	 */
	private void closeWindowEvent(WindowEvent event) {
		System.out.println("Window close request ...");

//        if(storageModel.dataSetChanged()) {  // if the dataset has changed, alert the user with a popup
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.getButtonTypes().remove(ButtonType.OK);
		alert.getButtonTypes().add(ButtonType.CANCEL);
		alert.getButtonTypes().add(ButtonType.YES);
		alert.setTitle("Quit application");
		alert.setContentText(String.format("Do you want to disconnect from the Chat?"));
//            alert.initOwner(primaryStage.getOwner());
		Optional<ButtonType> res = alert.showAndWait();

		if (res.isPresent()) {
			if (res.get().equals(ButtonType.CANCEL)) {
//				event.consume();
			} else {
				System.out.println("closewindowevent: Platform.exit");
				Platform.exit();
			}
		}
//        }
	}

	public static void main(String[] args) {
		launch(args);
	}

}
