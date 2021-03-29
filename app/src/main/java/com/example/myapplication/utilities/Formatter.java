package com.example.myapplication.utilities;

public class Formatter {
    public static String formatName(String name) {
        return name.trim().replaceAll(" +", " ");
    }
}
