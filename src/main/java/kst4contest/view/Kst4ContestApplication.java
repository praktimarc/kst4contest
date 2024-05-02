package kst4contest.view;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import kst4contest.ApplicationConstants;
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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.model.*;

import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;


public class Kst4ContestApplication extends Application {
//	private static final Kst4ContestApplication dbcontroller = new DBController();



	String chatState;
	ChatController chatcontroller;
	Button MYQRGButton; // TODO: clean code? Got the myqrg button out of the factory method to modify
						// the text later
	Button MYCALLSetQRGButton;

	Timer timer_buildWindowTitle;
	Timer timer_chatMemberTableSortTimer; // need that because javafx bug, it´s the only way to actualize the table...
	Timer timer_updatePrivatemessageTable; // same here
	VBox selectedCallSignFurtherInfoPane = new VBox();

	public static Node createArrow(double deg) {
		// Convert degrees to radians
		double rad = Math.toRadians(90-180 - deg);

		// Length of the arrow line
		double arrowLength = 6;

		// Coordinates of the arrow tip
		double tipX = arrowLength * Math.cos(rad);
		double tipY = arrowLength * Math.sin(rad);

		// Draw the arrow line
		Line arrowLine = new Line(0, 0, tipX, -tipY);
		arrowLine.setStroke(Color.LIGHTGREEN);

		// Calculate coordinates for the arrowhead
		double arrowheadAngle = Math.toRadians(20); // Angle of arrowhead
		double arrowheadLength = 15; // Length of arrowhead
		double arrowheadX1 = tipX + arrowheadLength * Math.cos(rad - arrowheadAngle);
		double arrowheadY1 = tipY + arrowheadLength * Math.sin(rad - arrowheadAngle);
		double arrowheadX2 = tipX + arrowheadLength * Math.cos(rad + arrowheadAngle);
		double arrowheadY2 = tipY + arrowheadLength * Math.sin(rad + arrowheadAngle);

		// Draw the arrowhead
		Polygon arrowhead = new Polygon(
				0, 0,  // tip
				arrowheadX1, -arrowheadY1, // left corner
				arrowheadX2, -arrowheadY2 // right corner
		);
		arrowhead.setFill(Color.GREEN);

		// Return the arrow element (line + polygon)
		return new javafx.scene.Group(arrowLine, arrowhead);
	}


	/**
	 * This method generates a BoderPane which shows some additional information about a callsign which had been
	 * selected either: <br/>
	 * - at the userlist <br/>
	 * - at the CQ message list (senders callsign)<br/>
	 * - at the PM message list (senders callsign)<br/><br/>
	 * Method gets its information source out of the original chatmember object of the userlist, not a copy
	 *
	 * @param selectedCallSignInfoStageChatMember
	 * @return
	 */
	private BorderPane generateFurtherInfoAbtSelectedCallsignBP(ChatMember selectedCallSignInfoStageChatMember) {


		selectedCallSignInfoBorderPane = new BorderPane();

		SplitPane selectedCallSignSplitPane = new SplitPane();
		selectedCallSignSplitPane.setOrientation(Orientation.VERTICAL);
		selectedCallSignSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUIselectedCallSignSplitPane_dividerposition());


		TableView<ChatMessage> initFurtherInfoAbtCallsignMSGTable = initFurtherInfoAbtCallsignMSGTable();

		Label selectedCallSignInfoLblQTFInfo = new Label("QTF:" + selectedCallSignInfoStageChatMember.getQTFdirection() + " deg");

		Label selectedCallSignInfoLblQRBInfo = new Label("QRB: " + selectedCallSignInfoStageChatMember.getQrb() + " km");



		GridPane selectedCallSignDownerSiteGridPane = new GridPane();
		selectedCallSignDownerSiteGridPane.setHgap(10);
		selectedCallSignDownerSiteGridPane.setVgap(2);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignInfoLblQTFInfo, 0,0,1,1);
		selectedCallSignDownerSiteGridPane.add(selectedCallSignInfoLblQRBInfo, 0,1,1,1);
		selectedCallSignDownerSiteGridPane.add(new Label("Last activity: " + new Utils4KST().time_convertEpochToReadable(selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch()+"")), 0,2,1,1);
		selectedCallSignDownerSiteGridPane.add(new Label(("(" + Utils4KST.time_getSecondsBetweenEpochAndNow(selectedCallSignInfoStageChatMember.getActivityTimeLastInEpoch()+"") /60%60) +" min ago)"), 0,3,1,1);

		/**
		 * users qrv info setting will follow here
		 */

		CheckBox furtherInfoPnl_chkbx_notQRV144 = new CheckBox("tag not qrv 144");
		furtherInfoPnl_chkbx_notQRV144.setSelected(!selectedCallSignInfoStageChatMember.isQrv144());
		furtherInfoPnl_chkbx_notQRV144.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv144(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv144(false);
				}
				try {

				chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);

//				double[] deviderPos = selectedCallSignSplitPane.getDividerPositions();
//				for (int i = 0; i<deviderPos.length;i++) {
//					System.out.println("<<<<<<<<<<<<<<<DEVIDER " + deviderPos[i]);
//				}

				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV432 = new CheckBox("tag not qrv 432");
		furtherInfoPnl_chkbx_notQRV432.setSelected(!selectedCallSignInfoStageChatMember.isQrv432());
		furtherInfoPnl_chkbx_notQRV432.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv432(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv432(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV23 = new CheckBox("tag not qrv 23cm");
		furtherInfoPnl_chkbx_notQRV23.setSelected(!selectedCallSignInfoStageChatMember.isQrv1240());
		furtherInfoPnl_chkbx_notQRV23.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv1240(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv1240(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV13 = new CheckBox("tag not qrv 13cm");
		furtherInfoPnl_chkbx_notQRV13.setSelected(!selectedCallSignInfoStageChatMember.isQrv2300());
		furtherInfoPnl_chkbx_notQRV13.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv2300(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv2300(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV9 = new CheckBox("tag not qrv 9cm");
		furtherInfoPnl_chkbx_notQRV9.setSelected(!selectedCallSignInfoStageChatMember.isQrv3400());
		furtherInfoPnl_chkbx_notQRV9.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv3400(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv3400(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV6 = new CheckBox("tag not qrv 6cm");
		furtherInfoPnl_chkbx_notQRV6.setSelected(!selectedCallSignInfoStageChatMember.isQrv5600());
		furtherInfoPnl_chkbx_notQRV6.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv5600(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv5600(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRV3 = new CheckBox("tag not qrv 3cm");
		furtherInfoPnl_chkbx_notQRV3.setSelected(!selectedCallSignInfoStageChatMember.isQrv10G());
		furtherInfoPnl_chkbx_notQRV3.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv10G(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv10G(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		CheckBox furtherInfoPnl_chkbx_notQRVall = new CheckBox("tag not qrv all");
		furtherInfoPnl_chkbx_notQRVall.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue) {
					selectedCallSignInfoStageChatMember.setQrv144(true);
					selectedCallSignInfoStageChatMember.setQrv432(true);
					selectedCallSignInfoStageChatMember.setQrv1240(true);
					selectedCallSignInfoStageChatMember.setQrv2300(true);
					selectedCallSignInfoStageChatMember.setQrv3400(true);
					selectedCallSignInfoStageChatMember.setQrv5600(true);
					selectedCallSignInfoStageChatMember.setQrv10G(true);
				} else {
					selectedCallSignInfoStageChatMember.setQrv144(false);
					selectedCallSignInfoStageChatMember.setQrv432(false);
					selectedCallSignInfoStageChatMember.setQrv1240(false);
					selectedCallSignInfoStageChatMember.setQrv2300(false);
					selectedCallSignInfoStageChatMember.setQrv3400(false);
					selectedCallSignInfoStageChatMember.setQrv5600(false);
					selectedCallSignInfoStageChatMember.setQrv10G(false);
				}

				try {

					chatcontroller.getDbHandler().updateNotQRVInfoOnChatMember(selectedCallSignInfoStageChatMember);
				} catch (Exception e) {
					//do nothing, upodate was not possible
				}
			}
		});

		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV144, 2,0,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV432, 2,1,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV23, 2,2,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV13, 2,3,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV9, 3,0,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV6, 3,1,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRV3, 3,2,1,1);
		selectedCallSignDownerSiteGridPane.add(furtherInfoPnl_chkbx_notQRVall, 3,3,1,1);

		if (!chatcontroller.getChatPreferences().isStn_bandActive144()) {
			furtherInfoPnl_chkbx_notQRV144.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive432()) {
			furtherInfoPnl_chkbx_notQRV432.setVisible(false);
		}

		if (!chatcontroller.getChatPreferences().isStn_bandActive1240()) {
			furtherInfoPnl_chkbx_notQRV23.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive2300()) {
			furtherInfoPnl_chkbx_notQRV13.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive3400()) {
			furtherInfoPnl_chkbx_notQRV9.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive5600()) {
			furtherInfoPnl_chkbx_notQRV6.setVisible(false);
		}
		if (!chatcontroller.getChatPreferences().isStn_bandActive10G()) {
			furtherInfoPnl_chkbx_notQRV3.setVisible(false);
		}




		/**
		 * users qrv info setting ending
		 */

		Button selectedCallSignShowAsPathBtn = new Button("Show path in AS");
		selectedCallSignShowAsPathBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				chatcontroller.airScout_SendAsShowPathPacket(selectedCallSignInfoStageChatMember);
			}
		});

		selectedCallSignShowAsPathBtn.setGraphic(createArrow(selectedCallSignInfoStageChatMember.getQTFdirection()));

		selectedCallSignDownerSiteGridPane.add(selectedCallSignShowAsPathBtn, 1,0,1,2);



//		selectedCallSignDownerSiteGridPane.add(new Label("publicmsgCount"), 3,0,1,1);
//		selectedCallSignDownerSiteGridPane.add(new Label("toMeMsgCount"), 3,1,1,1);
//		selectedCallSignDownerSiteGridPane.add(new Label("fromMeMSGCount"), 3,2,1,1);
//		HBox selectedCallSignDownerSiteHBox = new HBox();
//		selectedCallSignDownerSiteHBox.getChildren().add(selectedCallSignInfoLblQRBInfo);
//		selectedCallSignDownerSiteHBox.getChildren().add(selectedCallSignInfoLblQTFInfo);

		selectedCallSignSplitPane.getItems().add(initFurtherInfoAbtCallsignMSGTable);
		selectedCallSignSplitPane.getItems().add(selectedCallSignDownerSiteGridPane);

		//first initialize how much divider positions we need...
//		chatcontroller.getChatPreferences().setGUIselectedCallSignSplitPane_dividerposition(new double[selectedCallSignSplitPane.getDividers().size()]);
		/**
		 * Then add change listeners to the dividers to save their state
		 */
		for (SplitPane.Divider divider : selectedCallSignSplitPane.getDividers()) {
			divider.positionProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
//					System.out.println("<<<<<<<<<<<<<<<<<<< devider " + selectedCallSignSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  selectedCallSignSplitPane.getDividers().size());
					chatcontroller.getChatPreferences().getGUIselectedCallSignSplitPane_dividerposition()[selectedCallSignSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
				}
			});

		}


		selectedCallSignInfoBorderPane.setCenter(selectedCallSignSplitPane);

		HBox selectedCallSignInfoBottomControlsBox = new HBox();
		selectedCallSignInfoBottomControlsBox.setSpacing(10);
//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Always on top"));

		ToggleGroup selectedCallSignInfoFilterMessagesRadioGrp = new ToggleGroup();
		RadioButton selectedCallSignFilterToMeMsgRB = new RadioButton("pm to me ");
//		selectedCallSignFilterToMeMsgRB.setSelected(true);
		selectedCallSignFilterToMeMsgRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignFilterMsgToOtherRB = new RadioButton("pm to other");
		selectedCallSignFilterMsgToOtherRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignFilterMsgpublic = new RadioButton("public msgs");
		selectedCallSignFilterMsgpublic.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);
		RadioButton selectedCallSignNoFilterRB = new RadioButton("nothing");
		selectedCallSignNoFilterRB.setToggleGroup(selectedCallSignInfoFilterMessagesRadioGrp);


		selectedCallSignInfoFilterMessagesRadioGrp.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle t1) {

				RadioButton radioButton = (RadioButton) selectedCallSignInfoFilterMessagesRadioGrp.getSelectedToggle();

				if (radioButton.equals(selectedCallSignFilterToMeMsgRB)) {
					chatcontroller.getLst_selectedCallSignInfofilteredMessageList().setPredicate(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if (((chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign())) || (chatMessage.getSender().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign()))
								) && ((chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) || (chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())))) {
									return true;
								}

								else return false;
							} catch (Exception exception) {
								System.out.println("KST4ContestApp <<<catched error>>> " + exception.getMessage());
							}

							return true;
						}
					});

					System.out.println(t1 + " filter to me was selected <<<<<<<<<<<<<<<<<<<");
				} else if (radioButton.equals(selectedCallSignFilterMsgToOtherRB)) {

					chatcontroller.getLst_selectedCallSignInfofilteredMessageList().setPredicate(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) && (!chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign()))) {
									return true;
								} else if ((chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (!chatMessage.getReceiver().getCallSign().equals("ALL")) && (!chatMessage.getReceiver().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign()))) {
									return true;
								} else return false;
							} catch (NullPointerException SenderNull) {
								System.out.println("KST4ContestApp, <<<catched error>>>: Sender/receiver of the message is unknown, categorizing is impossible: " + SenderNull.getMessage());

								return false;
							}

						}
					});

					System.out.println(t1 + " filter to other was selected <<<<<<<<<<<<<<<<<<<");
				} else if (radioButton.equals(selectedCallSignFilterMsgpublic)) {

					chatcontroller.getLst_selectedCallSignInfofilteredMessageList().setPredicate(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) && (chatMessage.getReceiver().getCallSign().equals("ALL"))) {
									return true;
								}
									else return false;

							} catch (NullPointerException SenderNull) {
								System.out.println("KST4ContestApp, <<<catched error>>>: Sender of the message is unknown, categorizing is impossible");

								return false;
							}

						}
					});


					System.out.println(t1 + " filter to public was selected <<<<<<<<<<<<<<<<<<<");
				} else {
					System.out.println(t1 + " no filter was selected <<<<<<<<<<<<<<<<<<<");
					chatcontroller.getLst_selectedCallSignInfofilteredMessageList().setPredicate(new Predicate<ChatMessage>() {
						@Override
						public boolean test(ChatMessage chatMessage) {

							try {

								if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) ||
										chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) {
									return true;
								}

								else return false;
							} catch (NullPointerException SenderNull) {
								System.out.println("KST4ContestApp, <<<catched error>>>: Sender/receiver of the message is unknown, categorizing is impossible");

								return false;
							}
						}
					});
				}
			}
		});

		selectedCallSignInfoBottomControlsBox.getChildren().add(new Label("Messages of " + selectedCallSignInfoStageChatMember.getCallSign() + " -> Filter:  "));
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignNoFilterRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterToMeMsgRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterMsgToOtherRB);
		selectedCallSignInfoBottomControlsBox.getChildren().add(selectedCallSignFilterMsgpublic);

//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Filter messages to me"));
//		selectedCallSignInfoBottomControlsBox.getChildren().add(new CheckBox("Filter messages to Other"));
		selectedCallSignInfoBorderPane.setTop(selectedCallSignInfoBottomControlsBox);

		chatcontroller.getLst_selectedCallSignInfofilteredMessageList().setPredicate(new Predicate<ChatMessage>() {
			/**
			 * This is the filter "nothing" option. It will get all communication of a callsign to all directions
			 *
			 * @param chatMessage the input argument
			 * @return
			 */
			@Override
			public boolean test(ChatMessage chatMessage) {

				try {
					if ((chatMessage.getSender().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) ||
							chatMessage.getReceiver().getCallSign().equals(selectedCallSignInfoStageChatMember.getCallSign())) {
						return true;
					} else return false;

				} catch (Exception exception) {
					System.out.println("KST4ContestApplication <<<catched ERROR>>>>: cant get sender infos due to sender is not known yet" + exception.getMessage());
				 return false;
				}
			}
		});

		selectedCallSignNoFilterRB.setSelected(true); //TODO: that behavior as default selection could be made preferencable
		return selectedCallSignInfoBorderPane;

	}

	private TableView<ChatMember> initChatMemberTable() {

		TableView<ChatMember> tbl_chatMemberTable = new TableView<ChatMember>();
		tbl_chatMemberTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});

		tbl_chatMemberTable.setTooltip(new Tooltip(
				"Stations available \n\nUse right click to a station to select predefined texts\nor hit <strg> + <1> ... <9> to write textsnippet to selected station\n\nHit <enter> to send"));

		TableColumn<ChatMember, String> callSignCol = new TableColumn<ChatMember, String>("Callsign");
		callSignCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty callsgn = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getState() == 1) {
					callsgn.setValue("(" + cellDataFeatures.getValue().getCallSign() + ")"); //away user
				} else {

					callsgn.setValue(cellDataFeatures.getValue().getCallSign());
				}

//				System.out.println(cellDataFeatures.getValue().getCallSign() + " / " + cellDataFeatures.getValue().getState()+ " <<<<<<<<<<<<<<<<<< state ");

				return callsgn;
			}
		});

//		asd hier weiter machen, für bold state
		callSignCol.setCellFactory(new Callback<TableColumn<ChatMember, String>, TableCell<ChatMember, String>>() {
			public TableCell call(TableColumn param) {

//				param.getProperties().
				return new TableCell<ChatMember, String>() {


					@Override
					public void updateItem(String item, boolean empty) {

						super.updateItem(item, empty);

						int currentIndex = indexProperty().getValue() < 0 ? 0 : indexProperty().getValue();
//						System.out.println(">>>>>>>>>>>>>>>> INDEXPROPERTY  =  " + indexProperty().getValue()  + " " + getIndex() + " / " + item);


						if (item != null) {

							ChatMember chatMember = (ChatMember) param.getTableView().getItems().get(currentIndex);
//							System.out.println(chatMember.getCallSign() + " / " + chatMember.getState() + " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<state ");

							if (chatMember.getState() == 3 ) { //login in last 5 min
								this.setStyle("-fx-font-weight: bold");
//								System.out.println("FAAAAAAAAAAAAAAAAAAAAAAAT: " + chatMember.getCallSign());
							} else if (chatMember.getState() == 0 ) { //here
								this.setStyle("-fx-font-weight: normal");
							} else if (chatMember.getState() == 2 ) { //here and relogin
								this.setStyle("-fx-font-weight: normal");
							} else if (chatMember.getState() == 1 ) { //away
//								this.setStyle("-fx-font-weight: thin");
							}

							//TODO: Experimental, isinangleandrange function

							if (chatMember.isInAngleAndRange()) {
								this.setTextFill(Color.GREEN);
								this.setStyle("-fx-font-weight: bold");
							}
//							else if (chatMember.getState() != 3){ //TODO: this double handling should be improved as may there can be new markers. Neccessarry to reset the colour to black
//								this.setTextFill(Color.BLACK);
//								this.setStyle("-fx-font-weight: normal");
//							} else {
//								this.setTextFill(Color.BLACK);
//							}

//							if ((Utils4KST.time_getSecondsBetweenEpochAndNow(chatMember.getActivityTimeLastInEpoch()+"") /60%60) < 2) {
//								this.setTextFill(Color.ORANGE);
//							}
						}


//						if (!isEmpty()) {
//							this.setTextFill(Color.BLACK);
//							// Get fancy and change color based on data
//
//							if (item.contains("5")) {
//								this.setTextFill(Color.BLUEVIOLET);
//							} else if (item.contains("7") ) {
//								this.setTextFill(Color.RED);
//							} else if (item.contains("0") ) {
//								this.setTextFill(Color.ORANGE);
//							}
////
							setText(item);
//						}
					}
				};
			}
		});

		callSignCol.setSortType(TableColumn.SortType.ASCENDING);
		tbl_chatMemberTable.getSortOrder().add(callSignCol);

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

		TableColumn<ChatMember, String> qtfCol = new TableColumn<ChatMember, String>("QTF");
		qtfCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty qra = new SimpleStringProperty();

				qra.setValue(cellDataFeatures.getValue().getQTFdirection()+"°");

				return qra;
			}
		});
		qtfCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(15));

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


		TableColumn<ChatMember, String> lastActCol = new TableColumn<ChatMember, String>("Act");
		lastActCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty lastActEpoch = new SimpleStringProperty();

//				lastActEpoch.setValue(cellDataFeatures.getValue().getActivityTimeLastInEpoch()+"");

				lastActEpoch.setValue((Utils4KST.time_getSecondsBetweenEpochAndNow(cellDataFeatures.getValue().getActivityTimeLastInEpoch()+"") /60%60) +"");

				return lastActEpoch;
			}
		});
		lastActCol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(32));

/**
 * section of worked flag in chatmember table
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

		TableColumn<ChatMember, String> wkdAny_subcol = new TableColumn<ChatMember, String>("wkdany");
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
		wkdAny_subcol.prefWidthProperty().bind(tbl_chatMemberTable.widthProperty().divide(14));

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

		/**
		 * section of NOT-QRV flag in chatmember table
		 */

		TableColumn<ChatMember, String> notQRVCol = new TableColumn<ChatMember, String>("NOT QRV @");
		notQRVCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMember, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMember, String> cellDataFeatures) {
				SimpleStringProperty wkd = new SimpleStringProperty();

				wkd.setValue("");

				if (!cellDataFeatures.getValue().isQrv144()) {
					wkd.setValue(wkd.getValue() + "144 ");
				} else {
					wkd.setValue(wkd.getValue().replace("144 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv432()) {
					wkd.setValue(wkd.getValue() + "70 ");
				} else {
					wkd.setValue(wkd.getValue().replace("70 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv1240()) {
					wkd.setValue(wkd.getValue() + "SHF23 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHFcm ",""));
				}

				if (!cellDataFeatures.getValue().isQrv2300()) {
					wkd.setValue(wkd.getValue() + "SHF13 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF13 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv3400()) {
					wkd.setValue(wkd.getValue() + "SHF9 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF9 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv5600()) {
					wkd.setValue(wkd.getValue() + "SHF6 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF6 ",""));
				}

				if (!cellDataFeatures.getValue().isQrv10G()) {
					wkd.setValue(wkd.getValue() + "SHF3 ");
				} else {
					wkd.setValue(wkd.getValue().replace("SHF3 ",""));
				}


				return wkd;
			}
		});




		/**
		 * add now only cols which affects the used band of my station
		 */

		if (chatcontroller.getChatPreferences().isStn_bandActive144()) {
			workedCol.getColumns().add(vhfCol_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive432()) {
			workedCol.getColumns().add(uhfCol_subcol);
		}

		if (chatcontroller.getChatPreferences().isStn_bandActive1240()) {
			workedCol.getColumns().add(shf23_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive2300()) {
			workedCol.getColumns().add(shf13_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive3400()) {
			workedCol.getColumns().add(shf9_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive5600()) {
			workedCol.getColumns().add(shf6_subcol);
		}
		if (chatcontroller.getChatPreferences().isStn_bandActive10G()) {
			workedCol.getColumns().add(shf3_subcol);
		}

		/**
		 * The worked any col makes sense in all cases
		 */
			workedCol.getColumns().add(wkdAny_subcol);




		tbl_chatMemberTable.getColumns().addAll(callSignCol, nameCol, qraCol, qtfCol, qrgCol, lastActCol, airScoutCol, workedCol, notQRVCol);

//		tbl_chatMemberTable.setItems(chatcontroller.getLst_chatMemberListFiltered());

		tbl_chatMemberTable.setItems(chatcontroller.getLst_chatMemberSortedFilteredList());
		chatcontroller.getLst_chatMemberSortedFilteredList().comparatorProperty().bind(tbl_chatMemberTable.comparatorProperty());
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

//		initializeCommunicationOverMyHeadVizalizationStage(new ChatMember());

		/**
		 * timer_chatMemberTableSortTimer -->
		 * This part fixes a javafx bug. The update of the Chatmember fields is (for any
		 * reason) not visible in the ui. Its neccessarry to (now no more sort!) but refresh
		 * the table in intervals to keep the table up to date.
		 */

		timer_chatMemberTableSortTimer = new Timer();

		timer_chatMemberTableSortTimer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				Thread.currentThread().setName("chatMemberTableSortTimer");

				System.out.println("[KST4CApp, Info:] Chatmemberlist-Filterlist predicates size: " + chatcontroller.getLst_chatMemberListFilterPredicates().size());
//				System.out.println("[KST4CApp, Info:] Deviderpos: " + spl);
//				for (int i = 0; i < chatcontroller.getLst_chatMemberListFilterPredicates().size(); i++) {
//
//					Predicate test = chatcontroller.getLst_chatMemberListFilterPredicates().get(i);
//					test.so
//				}


				Platform.runLater(() -> {

					try {
						
//						tbl_chatMemberTable.sort();

					} catch (Exception e) {
						System.out.println("[Main.java, Warning:] Table sorting (actualizing) failed this time.");
					}

					tbl_chatMemberTable.refresh();

				});
			}
		}, new Date(), 5000);

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
	 *
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

//	private Stage initializeCommunicationOverMyHeadVizalizationStage(ChatMember selectedChatMember) {
//		Stage stage_CommunicationOverMyHeadVizalizationStage = new Stage();
//		stage_CommunicationOverMyHeadVizalizationStage.setAlwaysOnTop(true);
//
//		MaidenheadLocatorMapPane locatorMapPane = new MaidenheadLocatorMapPane();
//		locatorMapPane.addLocator("JO51IJ", Color.RED);
//		locatorMapPane.addLocator("JN39OC", Color.BLUE);
//		locatorMapPane.addLocator("JN49GL", Color.GREEN);
//		locatorMapPane.connectLocators("JO51IJ", "JN49GL");
//
//		try {
//
//
//		BorderPane bp_CommunicationOverMyHeadVizalizationStage = new BorderPane();
//
//			stage_CommunicationOverMyHeadVizalizationStage.setTitle("Further info on "+ selectedChatMember.getCallSign());
//
//			stage_CommunicationOverMyHeadVizalizationStage.setScene(new Scene(locatorMapPane));
//			stage_CommunicationOverMyHeadVizalizationStage.show();
//
//			return stage_CommunicationOverMyHeadVizalizationStage;
//		} catch (Exception exception){
//
//		}
//		return stage_CommunicationOverMyHeadVizalizationStage;
//	}

	/**
	 * Initializes the right click contextmenu for the chatmember-table, sets the
	 * clickhandler for the contextmenu out of a string array (each menuitam will be
	 * created out of exact one array-entry). These are initialized by the
	 * chatpreferences object out of the config-xml
	 *
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

	private TableView<ChatMessage> initFurtherInfoAbtCallsignMSGTable() {

		TableView<ChatMessage> tbl_furtherInfoAbtCallsignMSGTable = new TableView<ChatMessage>();
		tbl_furtherInfoAbtCallsignMSGTable.setTooltip(new Tooltip("Messages of selected station are shown here"));

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
		msgCol.prefWidthProperty().bind(tbl_furtherInfoAbtCallsignMSGTable.widthProperty().divide(2));

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

		tbl_furtherInfoAbtCallsignMSGTable.getColumns().addAll(timeCol, callSignTRCVCol, callSignRCVRCol,
				  msgCol);

		ObservableList<ChatMessage> toOtherMSGList = chatcontroller.getLst_toOtherMessageList();
		tbl_furtherInfoAbtCallsignMSGTable.setItems(chatcontroller.getLst_selectedCallSignInfofilteredMessageList());

		return tbl_furtherInfoAbtCallsignMSGTable;
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

		tbl_generalMSGTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});

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
			
			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty airPlaneInfo = new SimpleStringProperty();

				try {
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
				} catch (NullPointerException thereIsNoApReflectionInfo) {
					//e.g. in case of mycall it´s not possible to set!
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

							try {
								if (item.contains("100%")) {
									this.setTextFill(Color.BLUEVIOLET);
								} else if (item.contains("75%") && !item.contains("100%")) {
									this.setTextFill(Color.RED);
								} else if (item.contains("50%") && ((!item.contains("100%")) || (!item.contains("75%")))) {
									this.setTextFill(Color.ORANGE);
								}

							} catch (Exception exc) {
								//TODO: on nullpointerexcteption dont to that
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

		TableColumn<ChatMessage, String> qrbCol = new TableColumn<ChatMessage, String>("QRB");
		qrbCol.setCellValueFactory(new Callback<CellDataFeatures<ChatMessage, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<ChatMessage, String> cellDataFeatures) {
				SimpleStringProperty qrb = new SimpleStringProperty();

				if (cellDataFeatures.getValue().getSender() != null && !cellDataFeatures.getValue().getSender().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign())) {
					//do not calc for your own callsign as this will be NaN

					if (!cellDataFeatures.getValue().getSender().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign())) {

						try {
	//						System.out.println(cellDataFeatures.getValue().getSender().getQrb()+"      QRB");
							qrb.setValue(cellDataFeatures.getValue().getSender().getQrb().intValue() +" km (" + cellDataFeatures.getValue().getSender().getQTFdirection().intValue() + ")°"); //make int for less space
						} catch (Exception nullOrFormatExc) {
							System.out.println("KST4ContestApp: <<<catched error>>>: qrb was faulty" + nullOrFormatExc.getMessage() + " / " + nullOrFormatExc.getStackTrace());
						}
					}

//					qrb.setValue("");

				} else {

					qrb.setValue("");//Prevents a bug of not setting all values as a default
				}
				return qrb;
			}
		});


		tbl_privateMSGTable.getColumns().addAll(timeCol, callSignCol, nameCol, qraCol, qrbCol, msgCol, qrgCol, airScoutCol);

		ObservableList<ChatMessage> privateMSGList = chatcontroller.getLst_toMeMessageList();
		tbl_privateMSGTable.setItems(privateMSGList);

		tbl_privateMSGTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {

				//we need to overdrive the Enter pressed as it should (in the whole scene) send the text!
				if (event.getCode() == KeyCode.ENTER) {

					event.consume();
					sendButton.fire();
				}

			}
		});


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
		menuItemFileDisconnect = new MenuItem("Disconnect");
		menuItemFileDisconnect.setDisable(true);

		if (chatcontroller.isConnectedAndLoggedIn() || chatcontroller.isConnectedAndNOTLoggedIn()) {
			menuItemFileDisconnect.setDisable(false);
		} if (chatcontroller.isDisconnected()) {
			menuItemFileDisconnect.setDisable(true);
		}
		menuItemFileDisconnect.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
				menuItemFileDisconnect.setDisable(true);
			}
		});


		MenuItem m10 = new MenuItem("Exit + disconnect");
		m10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				closeWindowEvent(null);
			}
		});

		// add menu items to menu
		fileMenu.getItems().add(menuItemFileDisconnect);
		fileMenu.getItems().add(m10);

		Menu optionsMenu = new Menu("Options");
		menuItemOptionsSetFrequencyAsName = new MenuItem("Set QRG as name in Chat");
		menuItemOptionsSetFrequencyAsName.setDisable(true);
		menuItemOptionsSetFrequencyAsName.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				ChatMessage sendMe = new ChatMessage();
				sendMe.setMessageDirectedToServer(false);
				sendMe.setMessageText("/SETNAME " + chatcontroller.getChatPreferences().getMYQRG().getValue());

				chatcontroller.getMessageTXBus().add(sendMe);

			}
		});


		menuItemOptionsAwayBack = new MenuItem("Show me as away in chat");


		MenuItem options10 = new MenuItem("Show options");

		menuItemOptionsAwayBack.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				ChatMessage sendMe = new ChatMessage();
				sendMe.setMessageDirectedToServer(false);

				if (chatcontroller.getChatPreferences().isLoginAFKState()) {

					menuItemOptionsAwayBack.setText("Show me as AWAY FROM chat!");
					chatcontroller.getChatPreferences().setLoginAFKState(false);
					sendMe.setMessageText("/BACK");

				} else {

					menuItemOptionsAwayBack.setText("Show me as ACTIVE in chat!");
					chatcontroller.getChatPreferences().setLoginAFKState(true);
					sendMe.setMessageText("/AWAY");
				}

				chatcontroller.getMessageTXBus().add(sendMe);

			}
		});
		options10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				if (settingsStage.isShowing()) {
					settingsStage.hide();
				} else {
					settingsStage.show();
				}
			}
		});

		optionsMenu.getItems().addAll(menuItemOptionsSetFrequencyAsName, menuItemOptionsAwayBack, options10);

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
		MenuItem help2 = new MenuItem("Donate for kst4Contest development via PayPal");
		MenuItem help3 = new MenuItem("_______________________");
		help3.setDisable(true);
		MenuItem help4 = new MenuItem("Visit DARC X08-Homepage");
		MenuItem menuItmDonateON4KST = new MenuItem("Donate for ON4KST Chatservers with PayPal to on4kst@skynet.be");
		MenuItem menuItmDonateOV3T = new MenuItem("Donate for OV3T´s plane feed service");
//		help5.setDisable(true);
		MenuItem help6 = new MenuItem("Contact the author using default mail app");
		MenuItem help8 = new MenuItem("Join kst4Contest newsgroup");
//		MenuItem help9 = new MenuItem("Download the changelog / roadmap");

		// Changelog
		// https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV

		help2.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://www.paypal.com/paypalme/do5amf");


			}
		});

		help4.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("http://www.x08.de");

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

//		menuItmDonateON4KST.setOnAction(new EventHandler<ActionEvent>() {
//			public void handle(ActionEvent event) {
//
//				getHostServices().showDocument("https://www.paypal.com");
//
//
//			}
//		});

		menuItmDonateOV3T.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				getHostServices().showDocument("https://www.paypal.me/ov3t");


			}
		});

//		help9.setOnAction(new EventHandler<ActionEvent>() {
//			public void handle(ActionEvent event) {
//
//				getHostServices()
//						.showDocument("https://e.pcloud.link/publink/show?code=XZwAoWZIap9DYqDlhhwncqAxLbU6STOh2PV");
//
//			}
//		});

		MenuItem help10 = new MenuItem("About...");
		help10.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {

				Alert a = new Alert(AlertType.INFORMATION);

				a.setTitle("About kst4contest");
				a.setHeaderText("kst4Contest " + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER + ": ON4KST Chatclient by DO5AMF");
				a.setContentText(chatcontroller.getChatPreferences().getProgramVersion());
				a.show();
			}
		});

//		helpMenu.getItems().add(help1);
		helpMenu.getItems().addAll(help2, help3, help4, menuItmDonateOV3T, menuItmDonateON4KST, help6, help8, help10);

//		helpMenu.getItems().add(help2);
//		helpMenu.getItems().add(help4);
//		
//		helpMenu.getItems().add(help10);

		MenuBar menubar = new MenuBar();
		menubar.getMenus().addAll(fileMenu, optionsMenu, windowMenu, helpMenu); // macromenu deleted

		return menubar;

	}

//	SimpleStringProperty messageBusOfChatCtrl = messageBus;
	MenuItem menuItemFileDisconnect;
	MenuItem menuItemOptionsAwayBack;

	MenuItem menuItemOptionsSetFrequencyAsName;
	TextField txt_chatMessageUserInput = new TextField();
	Button sendButton;
	TextField txt_ownqrg = new TextField();
	TextField txt_myQTF = new TextField();
	Button btnOptionspnlConnect;
	ContextMenu chatMessageContextMenu; // public due need to update it on modify
	ContextMenu chatMemberContextMenu;// public due need to update it on modify
	HBox chatMemberTableFilterQTFAndQRBHbox;

	FlowPane flwPane_textSnippets;

	Stage clusterAndQSOMonStage;
//	Stage stage_selectedCallSignInfoStage;
	ChatMember selectedCallSignInfoStageChatMember;
	BorderPane selectedCallSignInfoBorderPane;


	Stage stage_updateStage;

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
	 *
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

	private Queue<Media> musicList = new LinkedList<Media>();
	private MediaPlayer mediaPlayer ;

	private void playCWLauncher(String playThisChars) {

		char[] playThisInCW = playThisChars.toUpperCase().toCharArray();

		for (char letterToPlay: playThisInCW){
			switch (letterToPlay){
				case 'A':
					musicList.add(new Media(new File ("LTTRA.mp3").toURI().toString()));
					break;
				case 'B':
					musicList.add(new Media(new File ("LTTRB.mp3").toURI().toString()));
					break;
				case 'C':
					musicList.add(new Media(new File ("LTTRC.mp3").toURI().toString()));
					break;
				case 'D':
					musicList.add(new Media(new File ("LTTRD.mp3").toURI().toString()));
					break;
				case 'E':
					musicList.add(new Media(new File ("LTTRE.mp3").toURI().toString()));
					break;
				case 'F':
					musicList.add(new Media(new File ("LTTRF.mp3").toURI().toString()));
					break;
				case 'G':
					musicList.add(new Media(new File ("LTTRG.mp3").toURI().toString()));
					break;
				case 'H':
					musicList.add(new Media(new File ("LTTRH.mp3").toURI().toString()));
					break;
				case 'I':
					musicList.add(new Media(new File ("LTTRI.mp3").toURI().toString()));
					break;
				case 'J':
					musicList.add(new Media(new File ("LTTRJ.mp3").toURI().toString()));
					break;
				case 'K':
					musicList.add(new Media(new File ("LTTRK.mp3").toURI().toString()));
					break;
				case 'L':
					musicList.add(new Media(new File ("LTTRL.mp3").toURI().toString()));
					break;
				case 'M':
					musicList.add(new Media(new File ("LTTRM.mp3").toURI().toString()));
					break;
				case 'N':
					musicList.add(new Media(new File ("LTTRN.mp3").toURI().toString()));
					break;
				case 'O':
					musicList.add(new Media(new File ("LTTRO.mp3").toURI().toString()));
					break;
				case 'P':
					musicList.add(new Media(new File ("LTTRP.mp3").toURI().toString()));
					break;
				case 'Q':
					musicList.add(new Media(new File ("LTTRQ.mp3").toURI().toString()));
					break;
				case 'R':
					musicList.add(new Media(new File ("LTTRR.mp3").toURI().toString()));
					break;
				case 'S':
					musicList.add(new Media(new File ("LTTRS.mp3").toURI().toString()));
					break;
				case 'T':
					musicList.add(new Media(new File ("LTTRT.mp3").toURI().toString()));
					break;
				case 'U':
					musicList.add(new Media(new File ("LTTRU.mp3").toURI().toString()));
					break;
				case 'V':
					musicList.add(new Media(new File ("LTTRV.mp3").toURI().toString()));
					break;
				case 'W':
					musicList.add(new Media(new File ("LTTRW.mp3").toURI().toString()));
					break;
				case 'X':
					musicList.add(new Media(new File ("LTTRX.mp3").toURI().toString()));
					break;
				case 'Y':
					musicList.add(new Media(new File ("LTTRY.mp3").toURI().toString()));
					break;
				case 'Z':
					musicList.add(new Media(new File ("LTTRZ.mp3").toURI().toString()));
					break;
				case '1':
					musicList.add(new Media(new File ("LTTR1.mp3").toURI().toString()));
					break;
				case '2':
					musicList.add(new Media(new File ("LTTR2.mp3").toURI().toString()));
					break;
				case '3':
					musicList.add(new Media(new File ("LTTR3.mp3").toURI().toString()));
					break;
				case '4':
					musicList.add(new Media(new File ("LTTR4.mp3").toURI().toString()));
					break;
				case '5':
					musicList.add(new Media(new File ("LTTR5.mp3").toURI().toString()));
					break;
				case '6':
					musicList.add(new Media(new File ("LTTR6.mp3").toURI().toString()));
					break;
				case '7':
					musicList.add(new Media(new File ("LTTR7.mp3").toURI().toString()));
					break;
				case '8':
					musicList.add(new Media(new File ("LTTR8.mp3").toURI().toString()));
					break;
				case '9':
					musicList.add(new Media(new File ("LTTR9.mp3").toURI().toString()));
					break;
				case '0':
					musicList.add(new Media(new File ("LTTR0.mp3").toURI().toString()));
					break;
				case '/':
					musicList.add(new Media(new File ("LTTRSTROKE.mp3").toURI().toString()));
					break;
				case ' ':
					musicList.add(new Media(new File ("LTTRSPACE.mp3").toURI().toString()));
					break;
				default:
					System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
			}
		}
		playMusic();
//		mediaPlayer.dispose();

	}

	/**
	 * Plays a voice file for each char in the string (only EN alphabetic and numbers) except some specials: <br/><br/>
	 *
	 * 	case '!': BELL<br/>
	 *  case '?': YOUGOTMAIL<br/>
	 * 	case '#': HELLO<br/>
	 * 	case '*': 73 bye<br/>
	 * 	case '$': STROKEPORTABLE<br/>
	 * @param playThisChars
	 */
	private void playVoiceLauncher(String playThisChars) {

		char[] playThisInCW = playThisChars.toUpperCase().toCharArray();

		for (char letterToPlay: playThisInCW){
			switch (letterToPlay){
				case '!':
					musicList.add(new Media(new File ("VOICEBELL.mp3").toURI().toString()));
					break;
				case '?':
					musicList.add(new Media(new File ("VOICEYOUGOTMAIL.mp3").toURI().toString()));
					break;
				case '#':
					musicList.add(new Media(new File ("VOICEHELLO.mp3").toURI().toString()));
					break;
				case '*':
					musicList.add(new Media(new File ("VOICE73.mp3").toURI().toString()));
					break;
				case '$':
					musicList.add(new Media(new File ("VOICESTROKEPORTABLE.mp3").toURI().toString()));
					break;
				case 'A':
					musicList.add(new Media(new File ("VOICEA.mp3").toURI().toString()));
					break;
				case 'B':
					musicList.add(new Media(new File ("VOICEB.mp3").toURI().toString()));
					break;
				case 'C':
					musicList.add(new Media(new File ("VOICEC.mp3").toURI().toString()));
					break;
				case 'D':
					musicList.add(new Media(new File ("VOICED.mp3").toURI().toString()));
					break;
				case 'E':
					musicList.add(new Media(new File ("VOICEE.mp3").toURI().toString()));
					break;
				case 'F':
					musicList.add(new Media(new File ("VOICEF.mp3").toURI().toString()));
					break;
				case 'G':
					musicList.add(new Media(new File ("VOICEG.mp3").toURI().toString()));
					break;
				case 'H':
					musicList.add(new Media(new File ("VOICEH.mp3").toURI().toString()));
					break;
				case 'I':
					musicList.add(new Media(new File ("VOICEI.mp3").toURI().toString()));
					break;
				case 'J':
					musicList.add(new Media(new File ("VOICEJ.mp3").toURI().toString()));
					break;
				case 'K':
					musicList.add(new Media(new File ("VOICEK.mp3").toURI().toString()));
					break;
				case 'L':
					musicList.add(new Media(new File ("VOICEL.mp3").toURI().toString()));
					break;
				case 'M':
					musicList.add(new Media(new File ("VOICEM.mp3").toURI().toString()));
					break;
				case 'N':
					musicList.add(new Media(new File ("VOICEN.mp3").toURI().toString()));
					break;
				case 'O':
					musicList.add(new Media(new File ("VOICEO.mp3").toURI().toString()));
					break;
				case 'P':
					musicList.add(new Media(new File ("VOICEP.mp3").toURI().toString()));
					break;
				case 'Q':
					musicList.add(new Media(new File ("VOICEQ.mp3").toURI().toString()));
					break;
				case 'R':
					musicList.add(new Media(new File ("VOICER.mp3").toURI().toString()));
					break;
				case 'S':
					musicList.add(new Media(new File ("VOICES.mp3").toURI().toString()));
					break;
				case 'T':
					musicList.add(new Media(new File ("VOICET.mp3").toURI().toString()));
					break;
				case 'U':
					musicList.add(new Media(new File ("VOICEU.mp3").toURI().toString()));
					break;
				case 'V':
					musicList.add(new Media(new File ("VOICEV.mp3").toURI().toString()));
					break;
				case 'W':
					musicList.add(new Media(new File ("VOICEW.mp3").toURI().toString()));
					break;
				case 'X':
					musicList.add(new Media(new File ("VOICEX.mp3").toURI().toString()));
					break;
				case 'Y':
					musicList.add(new Media(new File ("VOICEY.mp3").toURI().toString()));
					break;
				case 'Z':
					musicList.add(new Media(new File ("VOICEZ.mp3").toURI().toString()));
					break;
				case '1':
					musicList.add(new Media(new File ("VOICE1.mp3").toURI().toString()));
					break;
				case '2':
					musicList.add(new Media(new File ("VOICE2.mp3").toURI().toString()));
					break;
				case '3':
					musicList.add(new Media(new File ("VOICE3.mp3").toURI().toString()));
					break;
				case '4':
					musicList.add(new Media(new File ("VOICE4.mp3").toURI().toString()));
					break;
				case '5':
					musicList.add(new Media(new File ("VOICE5.mp3").toURI().toString()));
					break;
				case '6':
					musicList.add(new Media(new File ("VOICE6.mp3").toURI().toString()));
					break;
				case '7':
					musicList.add(new Media(new File ("VOICE7.mp3").toURI().toString()));
					break;
				case '8':
					musicList.add(new Media(new File ("VOICE8.mp3").toURI().toString()));
					break;
				case '9':
					musicList.add(new Media(new File ("VOICE9.mp3").toURI().toString()));
					break;
				case '0':
					musicList.add(new Media(new File ("VOICE0.mp3").toURI().toString()));
					break;
				case '/':
					musicList.add(new Media(new File ("VOICESTROKE.mp3").toURI().toString()));
					break;
//				case ' ':
//					musicList.add(new Media(new File ("VOICESPACE.mp3").toURI().toString()));
//					break;
				default:
					System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
			}
		}
		playMusic();
//		mediaPlayer.dispose();

	}

//	protected static final int SAMPLE_RATE = 16 * 1024;




	private void playMusic() {


//		System.out.println("Kst4ContestApplication.playMusic");
		if(musicList.peek() == null)
		{
			return;
		}
		mediaPlayer = new MediaPlayer(musicList.poll());
		mediaPlayer.setRate(1.0);

		mediaPlayer.setOnReady(() -> {
			mediaPlayer.play();
			mediaPlayer.setOnEndOfMedia(() -> {
//				mediaPlayer.dispose();
				playMusic();
				if (musicList.isEmpty()) {
//					mediaPlayer.dispose();
				}
			});
		});

	}

	@Override
	public void start(Stage primaryStage) throws InterruptedException, IOException, URISyntaxException {

//		this.primaryStage = primaryStage;
//		ChatCategory category = new ChatCategory(0); //TODO: get the Category out of the preferences-object

		ChatMember ownChatMemberObject = new ChatMember();

		chatcontroller = new ChatController(ownChatMemberObject); // instantiate the Chatcontroller with the user object

//		this.chatcontroller.getPlayAudioUtils().playNoiseLauncher('!');

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
						try {
						System.out.println(
								"[Main.java, Info]: Set the MYQTF property by hand to: " + txt_myQTF.getText());
						chatcontroller.getChatPreferences().getActualQTF().set(Integer.parseInt(txt_myQTF.getText()));}
						catch (Exception exception) {
							System.out.println("bullshit entered in myqtf");
							txt_myQTF.setText("0");
						}
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

			Scene scn_ChatwindowMainScene = new Scene(bPaneChatWindow, chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[1], chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[0]);

			//add listeners for size changes to restore after startup
			scn_ChatwindowMainScene.widthProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newWidthValue) {
					chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[1] = newWidthValue.doubleValue();
				}
			});

			scn_ChatwindowMainScene.heightProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newHeightValue) {
					chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW()[0] = newHeightValue.doubleValue();
				}
			});

			scn_ChatwindowMainScene.setOnKeyPressed(new EventHandler<KeyEvent>() {
				KeyCombination keyComboSTRGplus1 = new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus2 = new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus3 = new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus4 = new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus5 = new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus6 = new KeyCodeCombination(KeyCode.DIGIT6, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus7 = new KeyCodeCombination(KeyCode.DIGIT7, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus8 = new KeyCodeCombination(KeyCode.DIGIT8, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus9 = new KeyCodeCombination(KeyCode.DIGIT9, KeyCombination.CONTROL_DOWN);
				KeyCombination keyComboSTRGplus0 = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);


				@Override
				public void handle(KeyEvent keyEvent) {
					try {

//					System.out.println(keyEvent.getCode());

						/**
						 * if a macro is set by hitting strg+Nr, it should be possible to send the message by hit the enter key
						 */
					if (keyEvent.getCode() == KeyCode.ENTER) {

						sendButton.fire();

					} else if (keyEvent.getCode() == KeyCode.ESCAPE) {
						txt_chatMessageUserInput.clear();
					} else

						if (selectedCallSignInfoStageChatMember.getCallSign() != null) {

							if (keyComboSTRGplus1.match(keyEvent)) {

								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(0));

							} else if (keyComboSTRGplus2.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(1));

							} else if (keyComboSTRGplus3.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(2));

							} else if (keyComboSTRGplus4.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(3));

							} else if (keyComboSTRGplus5.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(4));

							} else if (keyComboSTRGplus6.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(5));

							} else if (keyComboSTRGplus7.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(6));

							} else if (keyComboSTRGplus8.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(7));

							} else if (keyComboSTRGplus9.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(8));

							} else if (keyComboSTRGplus0.match(keyEvent)) {
								txt_chatMessageUserInput.setText("/cq " + selectedCallSignInfoStageChatMember.getCallSign() + " " + chatcontroller.getChatPreferences().getLst_txtSnipList().get(9));

							}

						}
					} catch (Exception nullPointerExc) {
						System.out.println("There are no predifined textsnippets for this keycombo! -> " + nullPointerExc.getMessage());
					}
				}
			});


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

			sendButton = new Button("send");
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

					boolean noAirplaneHere = false;

					if (txt_chatMessageUserInput.getText().contains("FIRSTAP")) {

						if (selectedCallSignInfoStageChatMember != null) {

							if (selectedCallSignInfoStageChatMember.getCallSign() != chatcontroller.getChatPreferences().getLoginCallSign()) {

								if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo() != null) {

									if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes() != null) {

										if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes().size() != 0) {
											noAirplaneHere = false;
											AirPlane airPlane = selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes().get(0);
											txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("FIRSTAP", "a " + airPlane.getPotencialDescriptionAsWord() +
													" in " + airPlane.getArrivingDurationMinutes() + " min"));
										}  else noAirplaneHere = true;
									} else noAirplaneHere = true;
								}
								else noAirplaneHere = true;
							}
						}

						if (noAirplaneHere) {
							txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("FIRSTAP",
									"no ap available"));
						}
					}

					if (txt_chatMessageUserInput.getText().contains("SECONDAP")) {

						if (selectedCallSignInfoStageChatMember != null) {

							if (selectedCallSignInfoStageChatMember.getCallSign() != chatcontroller.getChatPreferences().getLoginCallSign()) {

								if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo() != null) {

									if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes() != null) {

										if (selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes().size() >= 2) {
											System.out.println("RISINGAP : " + selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes().size());
											AirPlane airPlane = selectedCallSignInfoStageChatMember.getAirPlaneReflectInfo().getRisingAirplanes().get(1);

											if (!airPlane.getPotencialDescriptionAsWord().isEmpty()) {
											noAirplaneHere = false;
											txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("SECONDAP", "Next " + airPlane.getPotencialDescriptionAsWord() +
													" in " + airPlane.getArrivingDurationMinutes() + " min"));

											} else noAirplaneHere = true;

										}  else noAirplaneHere = true;
									} else noAirplaneHere = true;
								}
								else noAirplaneHere = true;
							}
						}

						if (noAirplaneHere) {
							txt_chatMessageUserInput.setText(txt_chatMessageUserInput.getText().replaceAll("SECONDAP",
									""));
						}
					}

					if (txt_chatMessageUserInput.getText().startsWith("/cq " + chatcontroller.getChatPreferences().getLoginCallSign())) {
						txt_chatMessageUserInput.setText(" "); //prevent user sends a message to himself, that will cause errors
					}
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
									+ chatcontroller.getLst_chatMemberList().size() + " users online, "
									+ chatcontroller.getLst_chatMemberSortedFilteredList().size() + " shown), "
									+ (chatcontroller.getLst_globalChatMessageList().size())
									+ " messages total.";
							chatcontroller.getChatPreferences().setChatState(chatState);
						}

						else {
							chatState = "DISCONNECTED!";
							chatcontroller.getChatPreferences().setChatState(chatState);
						}
						if (chatcontroller.isDisconnected()) {
							chatState = "DISCONNECTED!";
							chatcontroller.getChatPreferences().setChatState(chatState);
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

			ObservableList<ChatMessage> selectedChatMessageList = privateChatselectionModelChatMessage
					.getSelectedItems();
			selectedChatMessageList.addListener(new ListChangeListener<ChatMessage>() {
				@Override
				public void onChanged(Change<? extends ChatMessage> selectedChatMemberPrivateChat) {
					if (privateChatselectionModelChatMessage.getSelectedItems().isEmpty()) {
						// do nothing, that was a deselection-event!
					} else {

						/**
						 * We need a special trick here. Since the private message list is a messagelist only for my own callsign, it´s not useful to show a sender and receiver.
						 * But if you choose a line with a message which you sent do another station, the default mechanism will type "/cq MYOWNCALL" to the textfield and if you are sleepy,
						 * you wouldnt remark that you sent a message to yourself. Thatswhy the rx-callsign (in brackets) will be extracted out of your sended message and added to the sendmessage-field.
						 * Thats what happening in line with //here1
						 * Your own sent texts will look like this:
						 *
						 * (>ON4KST) Hi team! Nice to meet you
						 *
						 */

						if (selectedChatMemberPrivateChat.getList().get(0).getSender().getCallSign().equals(chatcontroller.getChatPreferences().getLoginCallSign()) ) {
							System.out.println("////////////////////////////// rx in orginal message: " + selectedChatMemberPrivateChat.getList().get(0).getReceiver().getCallSign());
							System.out.println("privChat selected ChatMember: was own object...!" + "rx was: " + selectedChatMemberPrivateChat.getList().get(0).getMessageText().substring(2,(selectedChatMemberPrivateChat.getList().get(0).getMessageText().indexOf(")"))));

							txt_chatMessageUserInput.clear();
							txt_chatMessageUserInput.setText("/cq "
									+ selectedChatMemberPrivateChat.getList().get(0).getMessageText().substring(2,(selectedChatMemberPrivateChat.getList().get(0).getMessageText().indexOf(")"))) + " "); //here1

						} else {

							txt_chatMessageUserInput.clear();
							txt_chatMessageUserInput.setText("/cq "
									+ selectedChatMemberPrivateChat.getList().get(0).getSender().getCallSign() + " ");

							try {
								selectedCallSignFurtherInfoPane.getChildren().clear();
								selectedCallSignInfoStageChatMember = selectedChatMemberPrivateChat.getList().get(0).getSender();
								selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
							} catch (Exception exception) {
								System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
							}

							System.out.println("privChat selected ChatMember: "
									+ selectedChatMemberPrivateChat.getList().get(0).getSender());
							// selectedChatMemberList.clear();
//						selectionModelChatMember.clearSelection(0);
						}
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


			TableView<ChatMessage> tbl_generalMessageTable = new TableView<ChatMessage>();
			tbl_generalMessageTable = initChatGeneralMSGTable();

			tbl_generalMessageTable.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent t) {
					if (t.getButton() == MouseButton.SECONDARY) {
						chatMemberContextMenu.show(primaryStage, t.getScreenX(), t.getScreenY());

					}
				}
			});

			TableViewSelectionModel<ChatMessage> generalChatselectionModelChatMessage = tbl_generalMessageTable
					.getSelectionModel();
			privateChatselectionModelChatMessage.setSelectionMode(SelectionMode.SINGLE);

			ObservableList<ChatMessage> selectedChatMessageListGeneralChat = generalChatselectionModelChatMessage
					.getSelectedItems();
			selectedChatMessageListGeneralChat.addListener(new ListChangeListener<ChatMessage>() {
				@Override
				public void onChanged(Change<? extends ChatMessage> selectedChatMemberGeneralChat) {
					if (generalChatselectionModelChatMessage.getSelectedItems().isEmpty()) {
						// do nothing, that was a deselection-event!
					} else {

						txt_chatMessageUserInput.clear();
						txt_chatMessageUserInput.setText("/cq "
								+ selectedChatMemberGeneralChat.getList().get(0).getSender().getCallSign() + " ");
						System.out.println("privChat selected ChatMember: "
								+ selectedChatMemberGeneralChat.getList().get(0).getSender());

						try {
							selectedCallSignFurtherInfoPane.getChildren().clear();
							selectedCallSignInfoStageChatMember = selectedChatMemberGeneralChat.getList().get(0).getSender();
							selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));
						} catch (Exception exception) {
							System.out.println("KST4CApp, <<<catched error>>>>: message sender is not in the userlist any more!");
						}
						// selectedChatMemberList.clear();
//						selectionModelChatMember.clearSelection(0);
					}
				}
			});



			messageSectionSplitpane.getItems().addAll(privateMessageTable, flwPane_textSnippets, textInputFlowPane,
					tbl_generalMessageTable);
			messageSectionSplitpane.setDividerPositions(chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition());

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImessageSectionSplitpane_dividerposition(chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition());
			/**
			 * Then add change listeners to the dividers to save their state
			 */
			for (SplitPane.Divider divider : messageSectionSplitpane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<< devider>>>>>> " + messageSectionSplitpane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  messageSectionSplitpane.getDividers().size());
						chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition()[messageSectionSplitpane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
					}
				});

			}

			//Changed to add contextmenu to cq message table
//			messageSectionSplitpane.getItems().addAll(privateMessageTable, flwPane_textSnippets, textInputFlowPane,
//					initChatGeneralMSGTable());

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
					try{

						if (selectionModelChatMember.getSelectedItems().isEmpty()) {
							// do nothing, that was a deselection-event!
						} else {


//							selectedCallSignInfoStageChatMember = selectedChatMember.getList().get(0); //TODO: may need reference to original chatmember object
							selectedCallSignInfoStageChatMember = chatcontroller.getLst_chatMemberList()
									.get(chatcontroller.checkListForChatMemberIndexByCallSign(
											selectedChatMember.getList().get(0)));

							selectedCallSignFurtherInfoPane.getChildren().clear();
							selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));

							txt_chatMessageUserInput.clear();
							txt_chatMessageUserInput
									.setText("/cq " + selectedChatMember.getList().get(0).getCallSign() + " ");
//							System.out.println(
//									"##################selected ChatMember: " + selectedChatMember.getList().get(0));
							// selectedChatMemberList.clear();
	//						selectionModelChatMember.clearSelection(0);
						}
					} catch (Exception exception) {
						selectedCallSignFurtherInfoPane.getChildren().clear();
						txt_chatMessageUserInput.clear();
						System.out.println("KST4ContestApp <<<catched ERROR>>>, selected user left chat!");
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

			SplitPane mainWindowRightSplitPane = new SplitPane();
			mainWindowRightSplitPane.setOrientation(Orientation.VERTICAL);
			mainWindowRightSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition());

			BorderPane chatMemberTableBorderPane = new BorderPane();
			chatMemberTableBorderPane.setCenter(tbl_chatMember);

			chatMemberTableFilterQTFAndQRBHbox = new HBox();
			chatMemberTableFilterQTFAndQRBHbox.setSpacing(10);

//			chatMemberTableFilterQTFAndQRBHbox.set

			VBox chatMemberTableFilterVBoxForAllFilters= new VBox();
			chatMemberTableFilterVBoxForAllFilters.setSpacing(1);
			chatMemberTableFilterVBoxForAllFilters.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgreen;");

//			HBox chatMemberTableFilterQRBHBox  = new HBox();
			FlowPane chatMemberTableFilterQRBHBox  = new FlowPane();
			chatMemberTableFilterQRBHBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterQRBHBox.setHgap(2);
			chatMemberTableFilterQRBHBox.setPrefWidth(210);

			TextField chatMemberTableFilterMaxQrbTF = new TextField(chatcontroller.getChatPreferences().getStn_maxQRBDefault() + "");
			ToggleButton tglBtnQRBEnable = new ToggleButton("Show only QRB [km] <= ");
			tglBtnQRBEnable.selectedProperty().addListener(new ChangeListener<Boolean>() {
				Predicate<ChatMember> maxQrbPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {
						if (chatMember.getQrb() < Double.parseDouble(chatMemberTableFilterMaxQrbTF.getText())) {
							return true;
						} else return false;
					}
				};
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					if (tglBtnQRBEnable.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(maxQrbPredicate);
					} else chatcontroller.getLst_chatMemberListFilterPredicates().remove(maxQrbPredicate);
				}
			});

			chatMemberTableFilterQRBHBox.getChildren().add(tglBtnQRBEnable);
			chatMemberTableFilterMaxQrbTF.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
					if (!newValue.matches("\\d*")) {
						chatMemberTableFilterMaxQrbTF.setText(newValue.replaceAll("[^\\d]", ""));
					}
				}
			});
			chatMemberTableFilterMaxQrbTF.setPrefSize(50,0);

			chatMemberTableFilterQRBHBox.getChildren().add(chatMemberTableFilterMaxQrbTF);
			chatMemberTableFilterQRBHBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");

			chatMemberTableFilterQTFAndQRBHbox.setFillHeight(true);
			chatMemberTableFilterQTFAndQRBHbox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterQTFAndQRBHbox.getChildren().add(chatMemberTableFilterQRBHBox);


//			HBox chatMemberTableFilterQTFHBox  = new HBox();
			FlowPane chatMemberTableFilterQTFHBox  = new FlowPane();
			chatMemberTableFilterQTFHBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterQTFHBox.setPrefWidth(490);
			chatMemberTableFilterQTFHBox.setHgap(2);

			CheckBox chatMemberTableFilterQtfEnableChkbx = new CheckBox("Show only QTF:");
			TextField chatMemberTableFilterQtfTF = new TextField(chatcontroller.getChatPreferences().getStn_qtfDefault()+"");
			chatMemberTableFilterQtfTF.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
					if (newValue.equals("")) {
						chatMemberTableFilterQtfTF.setText("0");
					}
					if (!newValue.matches("\\d*")) {
						chatMemberTableFilterQtfTF.setText(newValue.replaceAll("[^\\d]", ""));
					}
					chatMemberTableFilterQtfEnableChkbx.setSelected(false);
					chatMemberTableFilterQtfEnableChkbx.setSelected(true);
				}
			});
			chatMemberTableFilterQtfEnableChkbx.selectedProperty().addListener(new ChangeListener<Boolean>() {

				Predicate<ChatMember> qtfCheckPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {

//						System.out.println(chatMemberTableFilterQtfTF.getText() + " stn have " + chatMember.getQTFdirection());

//						double myQTF = );

						return DirectionUtils.isAngleInRange(chatMember.getQTFdirection(),Double.parseDouble(chatMemberTableFilterQtfTF.getText()), chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg());

					}
				};
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					if (chatMemberTableFilterQtfEnableChkbx.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(qtfCheckPredicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(qtfCheckPredicate);
					}
				}
			});
			chatMemberTableFilterQTFHBox.getChildren().add(chatMemberTableFilterQtfEnableChkbx);




			chatMemberTableFilterQtfTF.setPrefSize(50,0);
//			chatMemberTableFilterQTFHBox.getChildren().add(chatMemberTableFilterQtfTF);
			chatMemberTableFilterQTFHBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");



			Button qtfNorth = new Button("N");
			qtfNorth.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("0");
				}
			});
			Button qtfNorthEast = new Button("NE");
			qtfNorthEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("45");
				}
			});
			Button qtfEast = new Button("E");
			qtfEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("90");
				}
			});
			Button qtfSouthEast = new Button("SE");
			qtfSouthEast.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("135");
				}
			});
			Button qtfSouth = new Button("S");
			qtfSouth.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("180");
				}
			});
			Button qtfSouthWest = new Button("SW");
			qtfSouthWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("225");
				}
			});
			Button qtfWest = new Button("W");
			qtfWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("270");
				}
			});
			Button qtfNorthWest = new Button("NW");
			qtfNorthWest.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					chatMemberTableFilterQtfTF.textProperty().set("315");
				}
			});

//			chatMemberTableFilterQTFHBox.setSpacing(5);
			chatMemberTableFilterQTFHBox.getChildren().addAll(chatMemberTableFilterQtfTF, new Label("deg +/- " + chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg() + ""), qtfNorth, qtfNorthEast, qtfEast, qtfSouthEast, qtfSouth, qtfSouthWest, qtfWest, qtfNorthWest);
			chatMemberTableFilterQTFAndQRBHbox.getChildren().add(chatMemberTableFilterQTFHBox);

			chatMemberTableFilterVBoxForAllFilters.getChildren().add(chatMemberTableFilterQTFAndQRBHbox);

			HBox chatMemberTableFilterTextFieldBox = new HBox();
			chatMemberTableFilterTextFieldBox.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterTextFieldBox.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");


			chatcontroller.getLst_chatMemberListFiltered().predicateProperty().bind(Bindings.createObjectBinding(() -> chatcontroller.getLst_chatMemberListFilterPredicates().stream().reduce(x -> true, Predicate::and), chatcontroller.getLst_chatMemberListFilterPredicates()));




			TextField chatMemberTableFilterTextField = new TextField("Find...");
			chatMemberTableFilterTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					if (chatMemberTableFilterTextField.focusedProperty().getValue()) {

						chatMemberTableFilterTextField.clear();
					} else {
						if (!chatMemberTableFilterTextField.focusedProperty().getValue() && chatMemberTableFilterTextField.textProperty().equals("")) {

						chatMemberTableFilterTextField.setText("Find...");
						}
					}
//					System.out.println(chatMemberTableFilterTextField.focusedProperty().getValue());
				}
			});
			chatMemberTableFilterTextField.textProperty().addListener(new ChangeListener<String>() {

				Predicate<ChatMember> searchTextPredicate = new Predicate<ChatMember>() {
					@Override
					public boolean test(ChatMember chatMember) {
						if (chatMember.getCallSign().toUpperCase().contains(chatMemberTableFilterTextField.getText().toUpperCase()) ||
								chatMember.getCallSign().toUpperCase().contains(chatMemberTableFilterTextField.getText().toLowerCase())) {
							return true;
						} else

							return false;
					}

				};

				@Override
				public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {

					if (chatMemberTableFilterTextField.textProperty().getValue().equals("") && !chatMemberTableFilterTextField.focusedProperty().getValue()) {
						chatMemberTableFilterTextField.setText("Find...");
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(searchTextPredicate);
					}
					else {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(searchTextPredicate);
					}

					System.out.println(chatMemberTableFilterTextField.textProperty().getValue().equals("") + " / " + !chatMemberTableFilterTextField.focusedProperty().getValue());
				}
			});

			HBox chatMemberTableFilterWorkedBandFiltersHbx = new HBox();

			ToggleButton btnTglwkd = new ToggleButton("wkd");

			Predicate<ChatMember> wkdPredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkdPredicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkdPredicate);
					}
				}
			});

			ToggleButton btnTglwkd144 = new ToggleButton("144");

			Predicate<ChatMember> wkd144Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked144() || !chatMember.isQrv144()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd144.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd144.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd144Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd144Predicate);
					}
				}
			});
//			btnTglwkd144.setVisible(chatcontroller.getChatPreferences().isStn_bandActive144());

			ToggleButton btnTglwkd432 = new ToggleButton("432");

			Predicate<ChatMember> wkd432Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked432() || !chatMember.isQrv432()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd432.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd432.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd432Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd432Predicate);
					}
				}
			});
//			btnTglwkd432.setVisible(chatcontroller.getChatPreferences().isStn_bandActive432());


			ToggleButton btnTglwkd23 = new ToggleButton("23");

			Predicate<ChatMember> wkd23Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked1240() || !chatMember.isQrv1240()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd23.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd23.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd23Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd23Predicate);
					}
				}
			});

			ToggleButton btnTglwkd13 = new ToggleButton("13");

			Predicate<ChatMember> wkd13Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked2300() || !chatMember.isQrv2300()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd13.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd13.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd13Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd13Predicate);
					}
				}
			});

			ToggleButton btnTglwkd9 = new ToggleButton("9");

			Predicate<ChatMember> wkd9Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked3400() || !chatMember.isQrv3400()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd9.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd9.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd9Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd9Predicate);
					}
				}
			});


			ToggleButton btnTglwkd6 = new ToggleButton("6");

			Predicate<ChatMember> wkd6Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked5600() || !chatMember.isQrv5600()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd6.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd6.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd6Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd6Predicate);
					}
				}
			});


			ToggleButton btnTglwkd3 = new ToggleButton("3");

			Predicate<ChatMember> wkd3Predicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {

					if (chatMember.isWorked10G() || !chatMember.isQrv10G()) {
						return false;
					}
					else return true;
				}
			};
			btnTglwkd3.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglwkd3.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(wkd3Predicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(wkd3Predicate);
					}
				}
			});

			ToggleButton btnTglInactive = new ToggleButton("Inactive stations");

			Predicate<ChatMember> inactivePredicate = new Predicate<ChatMember>() {
				@Override
				public boolean test(ChatMember chatMember) {


					if ((Utils4KST.time_getSecondsBetweenEpochAndNow(chatMember.getActivityTimeLastInEpoch()+"") /60%60) > 20) {
						return false;
					}
					else return true;
				}
			};
			btnTglInactive.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					if (btnTglInactive.isSelected()) {
						chatcontroller.getLst_chatMemberListFilterPredicates().add(inactivePredicate);
					} else {
						chatcontroller.getLst_chatMemberListFilterPredicates().remove(inactivePredicate);
					}
				}
			});

			btnTglInactive.setTooltip(new Tooltip("Hide inactive stations"));

			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(new Label("Hide worked:\nHide un-QRV: "));
			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd);

			/**
			 * add only filter buttons at the callsigntable which affects used bands
			 */
			if (chatcontroller.getChatPreferences().isStn_bandActive144()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd144);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive432()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd432);
			}

			if (chatcontroller.getChatPreferences().isStn_bandActive1240()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd23);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive2300()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd13);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive3400()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd9);
			}
			if (chatcontroller.getChatPreferences().isStn_bandActive5600()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd6);

			}
			if (chatcontroller.getChatPreferences().isStn_bandActive10G()) {
				chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglwkd3);

			}

			chatMemberTableFilterWorkedBandFiltersHbx.getChildren().add(btnTglInactive);
			chatMemberTableFilterWorkedBandFiltersHbx.setAlignment(Pos.CENTER_LEFT);
			chatMemberTableFilterWorkedBandFiltersHbx.setSpacing(5);
			chatMemberTableFilterWorkedBandFiltersHbx.setStyle("-fx-padding: 1;" +
					"-fx-border-style: solid inside;" +
					"-fx-border-width: 1;" +
					"-fx-border-insets: 1;" +
					"-fx-border-radius: 1;" +
					"-fx-border-color: lightgrey;");

//			chatMemberTableFilterWorkedBandFilters


			chatMemberTableFilterTextFieldBox.getChildren().addAll(chatMemberTableFilterTextField);

//			HBox chatMemberTableFilterTextFieldAndWorkedBandsHbx = new HBox();
			FlowPane chatMemberTableFilterTextFieldAndWorkedBandsHbx = new FlowPane();
			chatMemberTableFilterTextFieldAndWorkedBandsHbx.getChildren().addAll(chatMemberTableFilterTextFieldBox, chatMemberTableFilterWorkedBandFiltersHbx);
//			chatMemberTableFilterTextFieldAndWorkedBandsHbx.setSpacing(5);
			chatMemberTableFilterTextFieldAndWorkedBandsHbx.setHgap(2);

			chatMemberTableFilterVBoxForAllFilters.getChildren().add(chatMemberTableFilterTextFieldAndWorkedBandsHbx);

//			Tooltip filterPanelTooltip = new Tooltip("Set the station-visible-filters here");
//			Tooltip.install(chatMemberTableFilterVBoxForAllFilters,filterPanelTooltip);

			Tooltip filterTextBoxTooltip = new Tooltip("Free text search");
			Tooltip.install(chatMemberTableFilterTextField,filterTextBoxTooltip);

			chatMemberTableBorderPane.setTop(chatMemberTableFilterVBoxForAllFilters);


			mainWindowRightSplitPane.getItems().add(chatMemberTableBorderPane);


			mainWindowLeftSplitPane.getItems().addAll(messageSectionSplitpane, mainWindowRightSplitPane);
			mainWindowLeftSplitPane.setDividerPositions(chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition());

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImainWindowLeftSplitPane_dividerposition(new double[mainWindowLeftSplitPane.getDividers().size()]);
//			chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition()[0] = 0.2;
			/**
			 * here will follow the Splitpane divider listener to save the user made UI changes, should been made at the very end of all splitpane operations
			 */
			for (SplitPane.Divider divider : mainWindowLeftSplitPane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<< mainWindowLeftSplitPanedevider " + mainWindowLeftSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  mainWindowLeftSplitPane.getDividers().size());
						chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition()[mainWindowLeftSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
					}
				});

			}

/**
 * initializing the furter infos of a callsign part of the right splitpane
 */






//			selectedCallSignFurtherInfoPane.getChildren().add(generateFurtherInfoAbtSelectedCallsignBP(selectedCallSignInfoStageChatMember));


//			selectedCallSignInfoPane.getChildren().add(selectedCallSignInfoBorderPane);



	/**
	 * end of initializing the furter infos of a callsign part of the right splitpane
	 */

			mainWindowRightSplitPane.getItems().add(selectedCallSignFurtherInfoPane);

			//first initialize how much divider positions we need...
//			chatcontroller.getChatPreferences().setGUImainWindowRightSplitPane_dividerposition(new double[mainWindowRightSplitPane.getDividers().size()]);

			/**
			 * here will follow the Splitpane divider listener to save the user made UI changes, should been made at the very end of all splitpane operations
			 */

			for (SplitPane.Divider divider : mainWindowRightSplitPane.getDividers()) {
				divider.positionProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
						System.out.println("<<<<<<<<<<<<<<<<<<<>>>>>> devider mainwindowRIGHTsplitpane " + mainWindowRightSplitPane.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  mainWindowRightSplitPane.getDividers().size());
						chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition()[mainWindowRightSplitPane.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
					}
				});

			}

			primaryStage.setScene(scn_ChatwindowMainScene);

			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * Window selected callsign information
		 * Works with a ChatMember variable, initialized by a selected-listener of the Chatmemberlist
		 */



		/**
		 * end Window selected callsign information
		 */

		/**
		 * Window Cluster & qso of the other
		 */
		clusterAndQSOMonStage = new Stage();
//		clusterAndQSOMonStage.initStyle(StageStyle.UTILITY);
		clusterAndQSOMonStage.setTitle("Cluster & QSO of the other");
		SplitPane pnl_directedMSGWin = new SplitPane();
		pnl_directedMSGWin.setOrientation(Orientation.VERTICAL);
		pnl_directedMSGWin.setDividerPositions(chatcontroller.getChatPreferences().getGUIpnl_directedMSGWin_dividerpositionDefault());
		pnl_directedMSGWin.getItems().addAll(initDXClusterTable(), initChatToOtherMSGTable());



		//first initialize how much divider positions we need...
//		chatcontroller.getChatPreferences().setGUIpnl_directedMSGWin_dividerpositionDefault(new double[pnl_directedMSGWin.getDividers().size()]);

		/**
		 * here will follow the Splitpane divider listener to save the user made UI changes, should been made at the very end of all splitpane operations
		 */

		for (SplitPane.Divider divider : pnl_directedMSGWin.getDividers()) {
			divider.positionProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number oldDividerPos, Number newDividerPosition) {
					System.out.println("<<<<<<<<<<<<<<<<<<<|||||||||||||||||||| devider " + pnl_directedMSGWin.getDividers().indexOf(divider)  + " position change, new position: " + newDividerPosition + " // size dev: " +  pnl_directedMSGWin.getDividers().size());
					chatcontroller.getChatPreferences().getGUIpnl_directedMSGWin_dividerpositionDefault()[pnl_directedMSGWin.getDividers().indexOf(divider)] = newDividerPosition.doubleValue();
				}
			});

		}


		Scene clusterAndQSOMonScene = new Scene(pnl_directedMSGWin, chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0], chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1]);

		clusterAndQSOMonScene.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newHeightValue) {
				chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1] = newHeightValue.doubleValue();
			}
		});

		clusterAndQSOMonScene.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number newWidthValue) {
				chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0] = newWidthValue.doubleValue();
			}
		});

		clusterAndQSOMonStage.setScene(clusterAndQSOMonScene);


		clusterAndQSOMonStage.show();

		/**
		 * end Window Cluster & qso of the other
		 */


		/**
		 * Window updates
		 */
		stage_updateStage = new Stage();
//		clusterAndQSOMonStage.initStyle(StageStyle.UTILITY);
		stage_updateStage.setTitle("Update information");
//		SplitPane pnl_directedMSGWin = new SplitPane();
//		apnl_directedMSGWin.setOrientation(Orientation.VERTICAL);

//		pnl_directedMSGWin.getItems().addAll(initDXClusterTable(), initChatToOtherMSGTable());

		try {


		stage_updateStage.setAlwaysOnTop(true);

		Label lblUpdateInfo = new Label("Update aviable!");
		Label lblUpdateInfo2 = new Label("Your Software version: ");
		Label lblUpdateInfo3 = new Label("Newest Software version: ");
		Label lblUpdateInfoChanges = new Label("Major Changes: ");
		Label lblUpdateInfoAdminMessage = new Label("Admin Message: ");
		Label lblUpdateInfoDownload = new Label("Downloadable here: " );


		TreeView treeView = new TreeView();

		GridPane upd_gridPaneUpd = new GridPane();

		upd_gridPaneUpd.setPadding(new Insets(10, 10, 10, 10));
		upd_gridPaneUpd.setVgap(5);
		upd_gridPaneUpd.setHgap(5);


		VBox vbxUpdateWindow = new VBox();
		vbxUpdateWindow.setSpacing(30);


		vbxUpdateWindow.getChildren().add(upd_gridPaneUpd);
		upd_gridPaneUpd.add(lblUpdateInfo, 0,0,1,1);
		upd_gridPaneUpd.add(lblUpdateInfo2, 0,1,1,1);
		upd_gridPaneUpd.add(new Label("kst4Contest " + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER+""), 1,1,1,1);
		upd_gridPaneUpd.add(lblUpdateInfo3, 0,2,1,1);
		upd_gridPaneUpd.add(new Label("kst4Contest " + chatcontroller.getUpdateInformation().getLatestVersionNumberOnServer()+""), 1,2,1,1);
		upd_gridPaneUpd.add(lblUpdateInfoChanges, 0,3,1,1);
		upd_gridPaneUpd.add(new Label(chatcontroller.getUpdateInformation().getMajorChanges()), 1,3,1,1);
		upd_gridPaneUpd.add(lblUpdateInfoAdminMessage, 0,4,1,1);
		upd_gridPaneUpd.add(new Label(chatcontroller.getUpdateInformation().getAdminMessage()), 1,4,1,1);
		upd_gridPaneUpd.add(lblUpdateInfoDownload, 0,5,1,1);

		Hyperlink link = new Hyperlink("Download here");
		link.setOnAction(e -> {
			getHostServices().showDocument(chatcontroller.getUpdateInformation().getLatestVersionPathOnWebserver());
//			System.out.println("The Hyperlink was clicked!");
		});

//		TextField upd_txtfldUpdateDownloadLink = new TextField(chatcontroller.getUpdateInformation().getLatestVersionPathOnWebserver());
//		upd_txtfldUpdateDownloadLink.setEditable(false);

		upd_gridPaneUpd.add(link, 1,5,1,1);



//		vbxUpdateWindow.getChildren().addAll(lblUpdateInfo, lblUpdateInfo2, lblUpdateInfo3, lblUpdateInfoChanges, lblUpdateInfoAdminMessage, lblUpdateInfoDownload);
		vbxUpdateWindow.getChildren().add(treeView);

		TreeItem rootItem = new TreeItem(ApplicationConstants.APPLICATION_NAME);
		TreeItem changeLog = new TreeItem<>("ChangeLog");

		ArrayList<String[]> changeLogArrayWith7Fiels = chatcontroller.getUpdateInformation().getChangeLog();

		for (String[] aSubversionArray : changeLogArrayWith7Fiels) {
			TreeItem aSubversionEntry = new TreeItem(aSubversionArray[0]);

			for (int i = 1; i < aSubversionArray.length; i++) {
				aSubversionEntry.getChildren().add(new TreeItem<>(aSubversionArray[i]));
			}

			changeLog.getChildren().add(aSubversionEntry);
		}

		rootItem.getChildren().add(changeLog);

		TreeItem knownBugs = new TreeItem<>("Known bugs");

		ArrayList<String[]> BugArrayWith2Fiels = chatcontroller.getUpdateInformation().getBugList();

		for (String[] aBugArray : BugArrayWith2Fiels) {
			TreeItem aBugEntry = new TreeItem(aBugArray[0]);

			for (int i = 1; i < aBugArray.length; i++) {
				aBugEntry.getChildren().add(new TreeItem<>(aBugArray[i]));
			}

			knownBugs.getChildren().add(aBugEntry);
		}

		rootItem.getChildren().add(knownBugs);



		treeView.setRoot(rootItem);
		treeView.setShowRoot(false);

		System.out.println("SRVR Version: " + chatcontroller.getUpdateInformation().getLatestVersionNumberOnServer() + " // installed versioasdn " + ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER);

		stage_updateStage.setScene(new Scene(vbxUpdateWindow, chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[0], chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[1]));

		if (chatcontroller.getUpdateInformation().getLatestVersionNumberOnServer() > ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER) {
			stage_updateStage.show();
		} else {
			//nothing to do
		}
		} catch (Exception excOnUpdateFileProcessing) {
			System.out.println("[KST4ContestApp, ERROR]: Problem on Updateservice! " + excOnUpdateFileProcessing.getMessage());
			excOnUpdateFileProcessing.printStackTrace();
		}
		/**
		 * end Window Update
		 */


		/*****************************************************************************
		 * 
		 * Settings Scene
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
				txtFldCallSign.setText(txtFldCallSign.getText().toUpperCase());
				System.out.println("[Main.java, Info]: Setted the Login Callsign: " + txtFldCallSign.getText().toUpperCase());
				chatcontroller.getChatPreferences().setLoginCallSign(txtFldCallSign.getText().toUpperCase());
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

		TextField txtFldstn_antennaBeamWidthDeg = new TextField(this.chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg() + "");
		txtFldstn_antennaBeamWidthDeg.setTooltip(new Tooltip("Your antenna beamwidth in DEG\n\nEnter correct values here due it´s used for path suggestions!!!"));
		txtFldstn_antennaBeamWidthDeg.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				if (newString.equals("")) {
					txtFldstn_antennaBeamWidthDeg.setText("0");
				}

				if (!newString.matches("\\d*")) {
					txtFldstn_antennaBeamWidthDeg.setText(newString.replaceAll("[^\\d]", ""));
				}

				System.out.println("[Main.java, Info]: Setted the beam: " + txtFldstn_antennaBeamWidthDeg.getText());
				chatcontroller.getChatPreferences().setStn_antennaBeamWidthDeg(Double.parseDouble(txtFldstn_antennaBeamWidthDeg.getText()));
			}
		});

		TextField txtFldstn_maxQRBDefault = new TextField(this.chatcontroller.getChatPreferences().getStn_maxQRBDefault() + "");

		txtFldstn_maxQRBDefault.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				if (newString.equals("")) {
					txtFldstn_maxQRBDefault.setText("0");
				}

				if (!newString.matches("\\d*")) {
					txtFldstn_maxQRBDefault.setText(newString.replaceAll("[^\\d]", ""));
				}

				System.out.println("[Main.java, Info]: Setted the QRB: " + txtFldstn_maxQRBDefault.getText());
				chatcontroller.getChatPreferences().setStn_maxQRBDefault(Double.parseDouble(txtFldstn_maxQRBDefault.getText()));
			}
		});

		TextField txtFldstn_qtfDefault = new TextField(this.chatcontroller.getChatPreferences().getStn_qtfDefault() + "");

		txtFldstn_qtfDefault.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observed, String oldString, String newString) {

				if (newString.equals("")) {
					txtFldstn_qtfDefault.setText("0");
				}

				if (!newString.matches("\\d*")) {
					txtFldstn_qtfDefault.setText(newString.replaceAll("[^\\d]", ""));
				}

				System.out.println("[Main.java, Info]: Setted the QRB: " + txtFldstn_qtfDefault.getText());
				chatcontroller.getChatPreferences().setStn_antennaBeamWidthDeg(Double.parseDouble(txtFldstn_qtfDefault.getText()));
//				chatMemberTableFilterQTFHBox.getChildren().addAll(chatMemberTableFilterQtfTF, new Label("deg, " + chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg() + " beamwidth"), qtfNorth, qtfNorthEast, qtfEast, qtfSouthEast, qtfSouth, qtfSouthWest, qtfWest, qtfNorthWest);
//				chatMemberTableFilterQTFHBox.getChildren().addAll(chatMemberTableFilterQtfTF, new Label("deg, " + chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg() + " beamwidth"), qtfNorth, qtfNorthEast, qtfEast, qtfSouthEast, qtfSouth, qtfSouthWest, qtfWest, qtfNorthWest);
			}

		});


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
		grdPnlStation.add(new Label("Antenna beamwidth:"), 0, 5);
		grdPnlStation.add(txtFldstn_antennaBeamWidthDeg, 1, 5);
		grdPnlStation.add(new Label("Default maximum QRB:"), 0, 6);
		grdPnlStation.add(txtFldstn_maxQRBDefault, 1, 6);
		grdPnlStation.add(new Label("Default filter QTF:"), 0, 7);
		grdPnlStation.add(txtFldstn_qtfDefault, 1, 7);

		VBox vbxStation = new VBox();
		vbxStation.setPadding(new Insets(10, 10, 10, 10));
		vbxStation.getChildren().addAll(
				generateLabeledSeparator(100, "Set your Login Credentials and Station Parameters here"), grdPnlStation);
		vbxStation.getChildren().addAll(generateLabeledSeparator(50,
				"! ! ! ! Don´t forget to reset the worked stations information before starting a new contest ! ! ! !"));


		CheckBox settings_chkbx_QRV144 = new CheckBox("My station uses 2m band");
		settings_chkbx_QRV144.setSelected(chatcontroller.getChatPreferences().isStn_bandActive144());
		settings_chkbx_QRV144.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive144(
						settings_chkbx_QRV144.isSelected());
				System.out.println("[Main.java, Info]: setted my 144 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive144());
				chatMemberTableFilterQTFAndQRBHbox.setVisible(false);
				chatMemberTableFilterQTFAndQRBHbox.setVisible(true);
			}
		});

		CheckBox settings_chkbx_QRV432 = new CheckBox("My station uses 70cm band");
		settings_chkbx_QRV432.setSelected(chatcontroller.getChatPreferences().isStn_bandActive432());
		settings_chkbx_QRV432.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive432(
						settings_chkbx_QRV432.isSelected());
				System.out.println("[Main.java, Info]: setted my 432 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive432());
			}
		});

		CheckBox settings_chkbx_QRV1240 = new CheckBox("My station uses 23cm band");
		settings_chkbx_QRV1240.setSelected(chatcontroller.getChatPreferences().isStn_bandActive1240());
		settings_chkbx_QRV1240.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive1240(
						settings_chkbx_QRV1240.isSelected());
				System.out.println("[Main.java, Info]: setted my 1240 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive1240());
			}
		});

		CheckBox settings_chkbx_QRV2300 = new CheckBox("My station uses 13cm band");
		settings_chkbx_QRV2300.setSelected(chatcontroller.getChatPreferences().isStn_bandActive2300());
		settings_chkbx_QRV2300.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive2300(
						settings_chkbx_QRV2300.isSelected());
				System.out.println("[Main.java, Info]: setted my 2300 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive2300());
			}
		});

		CheckBox settings_chkbx_QRV3400 = new CheckBox("My station uses 9cm band");
		settings_chkbx_QRV3400.setSelected(chatcontroller.getChatPreferences().isStn_bandActive3400());
		settings_chkbx_QRV3400.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive3400(
						settings_chkbx_QRV3400.isSelected());
				System.out.println("[Main.java, Info]: setted my 3400 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive3400());
			}
		});

		CheckBox settings_chkbx_QRV5600 = new CheckBox("My station uses 6cm band");
		settings_chkbx_QRV5600.setSelected(chatcontroller.getChatPreferences().isStn_bandActive5600());
		settings_chkbx_QRV5600.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive5600(
						settings_chkbx_QRV5600.isSelected());
				System.out.println("[Main.java, Info]: setted my 5600 qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive5600());
			}
		});

		CheckBox settings_chkbx_QRV10G = new CheckBox("My station uses 3cm band");
		settings_chkbx_QRV10G.setSelected(chatcontroller.getChatPreferences().isStn_bandActive10G());
		settings_chkbx_QRV10G.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				chatcontroller.getChatPreferences().setStn_bandActive10G(
						settings_chkbx_QRV10G.isSelected());
				System.out.println("[Main.java, Info]: setted my 10G qrv setting to: "
						+ chatcontroller.getChatPreferences().isStn_bandActive10G());
			}
		});


		GridPane grdPnlStation_bands = new GridPane();
		grdPnlStation_bands.setPadding(new Insets(10, 10, 10, 10));
		grdPnlStation_bands.setVgap(5);
		grdPnlStation_bands.setHgap(5);

		grdPnlStation_bands.add(new Label("Define on which bands you will be qrv today (changes UI a bit ... click save, then restart!)"), 0, 0, 3,1);
		grdPnlStation_bands.add(settings_chkbx_QRV144, 0, 1);
		grdPnlStation_bands.add(settings_chkbx_QRV432, 1, 1);
		grdPnlStation_bands.add(settings_chkbx_QRV1240, 2, 1);
		grdPnlStation_bands.add(settings_chkbx_QRV2300, 0, 2);
		grdPnlStation_bands.add(settings_chkbx_QRV3400, 1, 2);
		grdPnlStation_bands.add(settings_chkbx_QRV5600, 2, 2);
		grdPnlStation_bands.add(settings_chkbx_QRV10G, 0, 3);
		grdPnlStation_bands.setMaxWidth(555.0);

		grdPnlStation_bands.setStyle("   -fx-border-color: lightgray;\n" +
				"    -fx-vgap: 5;\n" +
				"    -fx-hgap: 5;\n" +
				"    -fx-padding: 5;");

		vbxStation.getChildren().add(new Label("    ")); //need some space there
		vbxStation.getChildren().add(grdPnlStation_bands);

//		vbxStation.getChildren().add(settings_chkbx_QRV144);
//		vbxStation.getChildren().add(settings_chkbx_QRV432);
//		vbxStation.getChildren().add(settings_chkbx_qRV1240);
//		vbxStation.getChildren().add(settings_chkbx_QRV2300);
//		vbxStation.getChildren().add(settings_chkbx_QRV3400);
//		vbxStation.getChildren().add(settings_chkbx_QRV5600);
//		vbxStation.getChildren().add(settings_chkbx_QRV10G);

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
		grdPnlLog.add(generateLabeledSeparator(100, "N1MM/QARTEST/UCXLog/DXLog.net Network-Listener"), 0, 3, 2, 1);
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
		grdPnlNotify.add(generateLabeledSeparator(100, "Notification settings"), 0, 0, 2, 1);

//		Label lblNitificationInfo = new Label(
//				"Switch bands, prefix worked by others alert, direction notifications, notification pattern matchers");
//        CheckBox chkBxEnableTRXMsgbyUCX = new CheckBox();

		Label lblNotifyEnableSimpleSounds = new Label("Enable simple audio notifications at: new personal message, new sked in ur dir, other");
		Label lblNotifyEnableCWSounds = new Label("Enable CW callsign spelling for new personal messages");
		Label lblNotifyEnableVoiceSounds = new Label("Enable phonetic callsign spelling for new personal messages");


		CheckBox chkBxEnableNotifySimpleSounds = new CheckBox();
		chkBxEnableNotifySimpleSounds.setSelected(this.chatcontroller.getChatPreferences().isNotify_playSimpleSounds());

		chkBxEnableNotifySimpleSounds.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				chatcontroller.getChatPreferences()
						.setNotify_playSimpleSounds(chkBxEnableNotifySimpleSounds.isSelected());
				System.out.println("[Main.java, Info]: Notification simplesounds enabled: " + newValue);
			}
		});


		CheckBox chkBxEnableNotifyCWSounds = new CheckBox();
		chkBxEnableNotifyCWSounds.setSelected(this.chatcontroller.getChatPreferences().isNotify_playCWCallsignsOnRxedPMs());

		chkBxEnableNotifyCWSounds.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				chatcontroller.getChatPreferences()
						.setNotify_playCWCallsignsOnRxedPMs(chkBxEnableNotifyCWSounds.isSelected());
				System.out.println("[Main.java, Info]: Notification CW Callsigns enabled: " + newValue);
			}
		});

		CheckBox chkBxEnableNotifyVoiceSounds = new CheckBox();
		chkBxEnableNotifyVoiceSounds.setSelected(this.chatcontroller.getChatPreferences().isNotify_playVoiceCallsignsOnRxedPMs());

		chkBxEnableNotifyVoiceSounds.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				chatcontroller.getChatPreferences()
						.setNotify_playVoiceCallsignsOnRxedPMs(chkBxEnableNotifyVoiceSounds.isSelected());
				System.out.println("[Main.java, Info]: Notification Voice Callsigns enabled: " + newValue);
			}
		});


		grdPnlNotify.add(lblNotifyEnableSimpleSounds, 0, 1);
		grdPnlNotify.add(chkBxEnableNotifySimpleSounds, 1, 1);

		grdPnlNotify.add(lblNotifyEnableCWSounds, 0, 2);
		grdPnlNotify.add(chkBxEnableNotifyCWSounds, 1, 2);

		grdPnlNotify.add(lblNotifyEnableVoiceSounds, 0, 3);
		grdPnlNotify.add(chkBxEnableNotifyVoiceSounds, 1, 3);


//		grdPnlNotify.add(lblNitificationInfo, 0, 1);
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
		grdPnlShorts.add(generateLabeledSeparator(100, "Set the Text-snippets (First 10 are accessible by pressing <strg> + <nr>!)"), 0,
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

		Button btn_wkdDB_reset = new Button("Reset worked-tags and NOT-QRV-tags");
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
		btnOptionspnlDisconnect.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				closeWindowEvent(null);

//				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);

			}
		});


		Button btnOptionspnlDisconnectOnly = new Button("Disconnect");
		btnOptionspnlDisconnectOnly.setDisable(true);
		menuItemFileDisconnect.setDisable(true);
		menuItemOptionsAwayBack.setDisable(true);

		if (chatcontroller.isDisconnected()) {

			btnOptionspnlDisconnectOnly.setDisable(true);
			menuItemFileDisconnect.setDisable(true);
			menuItemOptionsAwayBack.setDisable(true);

		} else if (chatcontroller.isConnectedAndNOTLoggedIn()) {
			btnOptionspnlDisconnectOnly.setDisable(true);
			menuItemFileDisconnect.setDisable(true);
			menuItemOptionsAwayBack.setDisable(true);
		}

		else if (chatcontroller.isConnectedAndLoggedIn()) {
			btnOptionspnlDisconnectOnly.setDisable(false);
			menuItemFileDisconnect.setDisable(false);
			menuItemOptionsAwayBack.setDisable(false);
		}

		btnOptionspnlDisconnectOnly.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
//				closeWindowEvent(null);
//				System.out.println("UI: disc requested");
				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);

				txtFldCallSign.setDisable(false);
				txtFldPassword.setDisable(false);
				txtFldName.setDisable(false);
				txtFldLocator.setDisable(false);
				choiceBxChatChategory.setDisable(false);
				btnOptionspnlConnect.setDisable(false);
				btnOptionspnlDisconnect.setDisable(false);
				btnOptionspnlDisconnectOnly.setDisable(true);
				txtFldstn_antennaBeamWidthDeg.setDisable(false);
				txtFldstn_qtfDefault.setDisable(false);
				txtFldstn_maxQRBDefault.setDisable(false);
				menuItemOptionsSetFrequencyAsName.setDisable(true);
				menuItemOptionsAwayBack.setDisable(true);
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

					btnOptionspnlDisconnectOnly.setDisable(false);
					menuItemFileDisconnect.setDisable(false);
					menuItemOptionsAwayBack.setDisable(false);
					menuItemOptionsSetFrequencyAsName.setDisable(false);

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
				txtFldstn_antennaBeamWidthDeg.setDisable(true);
				txtFldstn_qtfDefault.setDisable(true);
				txtFldstn_maxQRBDefault.setDisable(true);
				btnOptionspnlConnect.setDisable(true);
				btnOptionspnlDisconnect.setDisable(false);
				chatcontroller.setConnectedAndLoggedIn(true);
				chatcontroller.setDisconnected(false);
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
		settingsStage.setScene(new Scene(optionsPanel, chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[0], chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[1]));

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

	/**
	 * Informs a user about a warning, shows given String in simple alertwindow
	 *
	 */
	public static void alertWindowEvent(String warning) {
		System.out.println("Alert due to ... " + warning);

//        if(storageModel.dataSetChanged()) {  // if the dataset has changed, alert the user with a popup
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.getButtonTypes().remove(ButtonType.OK);
//		alert.getButtonTypes().add(ButtonType.CANCEL);
//		alert.getButtonTypes().add(ButtonType.YES);
		alert.setTitle("WARNING");
		alert.setContentText(String.format(warning));

	}


	public static void main(String[] args) {
		launch(args);
	}

//	public class MaidenheadLocatorMapPane extends Pane {
//
//		private static final double MAP_WIDTH = 800;
//		private static final double MAP_HEIGHT = 600;
//		private static final double CIRCLE_RADIUS = 5;
//		private static final double TEXT_OFFSET_X = 10;
//		private static final double TEXT_OFFSET_Y = -10;
//
//		public MaidenheadLocatorMapPane() {
//			setPrefSize(MAP_WIDTH, MAP_HEIGHT);
//		}
//
//		public void addLocator(String locator, Color color) {
//			double[] coords = locatorToCoordinates(locator);
//			Circle circle = new Circle(coords[0], coords[1], CIRCLE_RADIUS, color);
//			Text text = new Text(coords[0] + TEXT_OFFSET_X, coords[1] + TEXT_OFFSET_Y, locator);
//			getChildren().addAll(circle, text);
//		}
//
//		public void connectLocators(String locator1, String locator2) {
//			double[] coords1 = locatorToCoordinates(locator1);
//			double[] coords2 = locatorToCoordinates(locator2);
//			Line line = new Line(coords1[0], coords1[1], coords2[0], coords2[1]);
//			getChildren().add(line);
//
//			// Calculate distance between locators
//			double distance = calculateDistance(coords1, coords2);
//
//			// Calculate direction in degrees from locator1 to locator2
//			double direction = calculateDirection(coords1, coords2);
//
//			// Format distance to display only two decimal places
//			DecimalFormat df = new DecimalFormat("#.##");
//
//			// Create text for displaying distance and direction
//			Text distanceText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2, "Distance: " + df.format(distance) + " km");
//			Text directionText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2 + 20, "Direction: " + df.format(direction) + "°");
//			getChildren().addAll(distanceText, directionText);
//		}
//
//		// Helper method to convert Maidenhead locator string to coordinates
//		private double[] locatorToCoordinates(String locator) {
//			double lon = (locator.charAt(0) - 'A') * 20 - 180;
//			double lat = (locator.charAt(1) - 'A') * 10 - 90;
//			lon += (locator.charAt(2) - '0') * 2;
//			lat += (locator.charAt(3) - '0');
//			lon += (locator.charAt(4) - 'A') * 5.0 / 60;
//			lat += (locator.charAt(5) - 'A') * 2.5 / 60;
//
//			// Convert coordinates to map coordinates
//			double x = (lon + 180) / 360 * MAP_WIDTH;
//			double y = MAP_HEIGHT - (lat + 90) / 180 * MAP_HEIGHT;
//			return new double[]{x, y};
//		}
//
//		// Helper method to calculate distance between two coordinates (in km)
//		private double calculateDistance(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dlon = lon2 - lon1;
//			double dlat = lat2 - lat1;
//
//			double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
//			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//			// Earth radius in km
//			double radius = 6371;
//
//			return radius * c;
//		}
//
//		// Helper method to calculate direction in degrees from coords1 to coords2
//		private double calculateDirection(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dLon = lon2 - lon1;
//
//			double y = Math.sin(dLon) * Math.cos(lat2);
//			double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
//
//			double direction = Math.atan2(y, x);
//			direction = Math.toDegrees(direction);
//			direction = (direction + 360) % 360;
//
//			return direction;
//		}
//	}

}

/**
 * This cell type is used to declare buttons which can be placed in the tableview
 *
 * // source: https://stackoverflow.com/questions/76248808/how-do-i-add-a-button-into-a-jfx-tableview
 */
class ActionButtonTableCell<S, T> extends TableCell<S, T> {
	private final ToggleButton actionButton;

	public ActionButtonTableCell(String label, Consumer<S> function) {
		this.getStyleClass().add("action-button-table-cell");
		this.actionButton = new ToggleButton(label);
		this.actionButton.setOnAction(e -> function.accept(getCurrentItem()));
		this.actionButton.setMaxWidth(Double.MAX_VALUE);
	}
	public S getCurrentItem() {
		// No need for a cast here:
		System.out.println("<<<<<<<<<<<<<<<<<<<<TV Actionbutton pressed");
		return getTableView().getItems().get(getIndex());
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(String label, Consumer<S> function) {
		return param -> new ActionButtonTableCell<>(label, function);
	}
	@Override
	public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			setGraphic(actionButton);
		}
	}
}

/**
 * This cell type is used to declare buttons which can be placed in the tableview
 *
 * // source: https://stackoverflow.com/questions/76248808/how-do-i-add-a-button-into-a-jfx-tableview
 */
class CheckBoxTableCell<S, T> extends TableCell<S, T> {
	private final CheckBox actionCheckBox;

	public CheckBoxTableCell(String label, Consumer<S> function) {
		this.getStyleClass().add("action-button-table-cell");
		this.actionCheckBox = new CheckBox(label);
		this.actionCheckBox.setOnAction(e -> function.accept(getCurrentItem()));
//		this.actionCheckBox.setMaxWidth(Double.MAX_VALUE);
	}
	public S getCurrentItem() {
		// No need for a cast here:
		System.out.println("<<<<<<<<<<<<<<<<<<<<TV Actionbutton pressed");
		return getTableView().getItems().get(getIndex());
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(String label, Consumer<S> function) {
		return param -> new CheckBoxTableCell<>(label, function);
	}
	@Override
	public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			setGraphic(actionCheckBox);
		}
	}
}
