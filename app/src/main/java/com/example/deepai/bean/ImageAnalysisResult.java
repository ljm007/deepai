// ImageAnalysis.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

package com.example.deepai.bean;

import java.util.List;


public class ImageAnalysisResult {
    public String analysis_type;
    public String page_type;
    public double confidence;
    public ContextData context_data;
    public List<Elements> elements;

    @Override
    public String toString() {
        return "ImageAnalysis{" +
                "analysis_type='" + analysis_type + '\'' +
                ", page_type='" + page_type + '\'' +
                ", confidence=" + confidence +
                ", context_data=" + context_data +
                ", elements=" + elements +
                '}';
    }

    public static class ContextData {
        public String interface_type;
        public List<String> core_actions;

        @Override
        public String toString() {
            return "ContextData{" +
                    "interface_type='" + interface_type + '\'' +
                    ", core_actions=" + core_actions +
                    '}';
        }
    }

    public static class Elements {

        public String element_type;
        public List<List<Integer>> coordinates;
        public String button_text;
        public String function;
        public String gesture_relation;

        @Override
        public String toString() {
            return "Elements{" +
                    "element_type='" + element_type + '\'' +
                    ", coordinates=" + coordinates +
                    ", button_text='" + button_text + '\'' +
                    ", function='" + function + '\'' +
                    ", gesture_relation='" + gesture_relation + '\'' +
                    '}';
        }
    }
}