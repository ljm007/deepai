package com.example.deepai.bean;

import java.util.List;

public class ActionParameters {
    public String resource_id;
    public String target_text;
    public List<List<Integer>> coordinates;
    public String input_text;
    public String package_name;
    public String direction;
    public String prompt;
    public String message;
    public String action_after_loc;
    public Action pending_action;


    // Getters & Setters
    public String getResource_id() {
        return resource_id;
    }

    public String getTarget_text() {
        return target_text;
    }

    public List<List<Integer>> getCoordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return "ActionParameters{" +
                "resource_id='" + resource_id + '\'' +
                ", target_text='" + target_text + '\'' +
                ", coordinates=" + coordinates +
                ", input_text='" + input_text + '\'' +
                ", package_name='" + package_name + '\'' +
                ", direction='" + direction + '\'' +
                ", prompt='" + prompt + '\'' +
                ", message='" + message + '\'' +
                ", action_after_loc='" + action_after_loc + '\'' +
                ", pending_action=" + pending_action +
                '}';
    }
}