module praktiKST {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.web;
	requires jdk.xml.dom;
	requires java.sql;
    requires javafx.media;
    requires jdk.jsobject;
    requires java.net.http;
    requires java.desktop;
    exports kst4contest.controller.interfaces;
    exports kst4contest.controller;
	exports kst4contest.locatorUtils;
	exports kst4contest.model;
	exports kst4contest.view;

	opens kst4contest.view.map to javafx.web;
}