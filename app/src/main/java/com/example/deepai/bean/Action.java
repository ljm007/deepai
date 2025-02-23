package com.example.deepai.bean;

public class Action {
    public static final String CLICK = "click";
    public static final String DOUBLE_CLICK = "double_click";
    public static final String LONG_CLICK = "long_click";
    public static final String SWIPE = "swipe";
    public static final String INPUT = "input";
    public static final String LAUNCH_APP = "launch_app";
    public static final String BACK = "back";
    public static final String HOME = "home";
    public static final String RECENT_APPS = "recent_apps";
    public static final String SCROLL = "scroll";
    public static final String SCREENSHOT_REQUEST = "screenshot_request";
    public static final String END = "end";
    public static final String ERROR = "error";
    public static final String CONFIRM_ACTION = "confirm_action";
    public static final String PARSE = "parse";

    private String action;
    private ActionParameters parameters;

    // Getters & Setters
    public String getAction() {
        return action;
    }

    public ActionParameters getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "Action{" +
                "action='" + action + '\'' +
                ", parameters=" + parameters +
                '}';
    }

}