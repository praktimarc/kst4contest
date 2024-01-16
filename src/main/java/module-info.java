module praktiKST {
	requires javafx.controls;
	requires jdk.xml.dom;
	requires java.sql;
    requires javafx.media;
    exports kst4contest.controller;
	exports kst4contest.locatorUtils;
	exports kst4contest.model;
	exports kst4contest.view;
}