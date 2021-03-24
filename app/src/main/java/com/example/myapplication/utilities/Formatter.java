package com.example.myapplication.utilities;

public class Formatter {
    public static String formatName(String fullName) {
        return fullName.trim().replaceAll(" +", " ");
    }
}
