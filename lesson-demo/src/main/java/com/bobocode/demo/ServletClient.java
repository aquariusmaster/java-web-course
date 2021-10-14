package com.bobocode.demo;

import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServletClient {

    public static void main(String[] args) {

        try (var socket = new Socket("93.175.204.87", 8080);
             var writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

//            String json = "{\"firstName\": \"Andrii\",\n\"lastName\": \"Bobrov\"}";
//            writer.println("POST /ping HTTP/1.1");
//            writer.println("Host: 93.175.204.87");
//            writer.println("Content-Type: application/json;charset=utf-8");
//            writer.println("Content-Length: " + json.length());
//            writer.println();
//            writer.println("{\"firstName\": \"Andrii\",\n\"lastName\": \"Bobrov\"}");
            writer.println("GET /ping/current HTTP/1.1");
            writer.println("Host: 93.175.204.87");
            writer.println("Cookie: JSESSIONID=82412006A57A67AE5DF71CAA1FE5719E");
//            writer.println("Content-Type: application/json;charset=utf-8");
//            writer.println("Content-Length: " + json.length());
            writer.println();
//            writer.println("{\"firstName\": \"Andrii\",\n\"lastName\": \"Bobrov\"}");
            writer.flush();

            reader.lines().forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
