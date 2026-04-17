package com.voronina.course;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.voronina.course.lastmessageapi.LastMessageApi;
import com.voronina.course.randomuserapi.RandomUserApi;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("Hello world!");
        LastMessageApi api = new LastMessageApi();
        try {
            api.fetchData();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}