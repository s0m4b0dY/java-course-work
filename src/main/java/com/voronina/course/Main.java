package com.voronina.course;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.voronina.course.lastmessageapi.LastMessage;
import com.voronina.course.randomuserapi.RandomUser;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("Hello world!");
        BufferedReader br = new BufferedReader(new FileReader("input2.json"));
        String everything;
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            everything = sb.toString();
        } finally {
            br.close();
        }
        System.out.println(everything);
        Gson gson = new Gson();
        LastMessage user = gson.fromJson(everything, LastMessage.class);
        System.out.println(user.getReactions());
    }
}