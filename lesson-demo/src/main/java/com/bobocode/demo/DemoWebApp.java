package com.bobocode.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DemoWebApp {

    @SneakyThrows
    public static void main(String[] args) {
        String photos = getForBody("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=12&api_key=DEMO_KEY");
        var mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readValue(photos, JsonNode.class);
        var photosArray = jsonNode.get("photos");
        List<Image> images = StreamSupport.stream(photosArray.spliterator(), false)
                .map(node -> node.get("img_src"))
                .map(JsonNode::asText)
                .map(Image::new)
                .collect(Collectors.toList());
        fetchLocation(images);
        fetchLength(images);

        Image maxImage = images.stream()
                .max(Comparator.comparing(Image::getSize))
                .orElseThrow();
        System.out.println(maxImage);
        post(BobocodeRequest.builder().
                picture(
                        Picture.builder()
                                .url(maxImage.getInitLocation())
                                .size(maxImage.getSize())
                                .build()
                )
                .user(User.builder().firstName("Andrii").lastName("Bobrov").build())
                .build());
    }

    @SneakyThrows
    private static String getForBody(String url) {
        URI uri = URI.create(url);
        var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (var socket = (SSLSocket) factory.createSocket(uri.getHost(), 443);
             var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.println("GET " + uri.getPath() + "?" + uri.getQuery() + " HTTP/1.0");
            writer.println("Host: " + uri.getHost());
            writer.println();
            writer.flush();
            return reader.lines().filter(s -> s.startsWith("{")).findFirst().orElseThrow();
        }
    }

    @SneakyThrows
    private static void fetchLocation(List<Image> images) {
        try (var socket = new Socket(URI.create(images.get(0).initLocation).getHost(), 80);
             var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            images.forEach(img ->
                    img.setLocation(headFor(writer, reader, URI.create(img.getInitLocation()), "Location"))
            );
        }
    }

    @SneakyThrows
    private static void fetchLength(List<Image> images) {
        var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (var socket = (SSLSocket) factory.createSocket(URI.create(images.get(0).location).getHost(), 443);
             var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            images.forEach(img -> {
                        String lengthStr = headFor(writer, reader, URI.create(img.location), "Content-Length");
                        img.setSize(Long.parseLong(lengthStr));
                    }
            );
        }
    }

    @SneakyThrows
    private static String headFor(PrintWriter writer, BufferedReader reader, URI uri, String header) {
        writer.println("HEAD " + uri.getPath() + " HTTP/1.1");
        writer.println("Host: " + uri.getHost());
        writer.println();
        writer.flush();
        return reader.lines()
                .filter(line -> line.startsWith(header))
                .map(im -> im.substring(header.length() + 2))
                .findFirst()
                .orElseThrow();
    }

    @SneakyThrows
    private static void post(BobocodeRequest request) {
        URI uri = URI.create("https://bobocode.herokuapp.com/nasa/pictures");
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter objectWriter = mapper.writerFor(request.getClass());
        String jsonBody = objectWriter.writeValueAsString(request);
        System.out.println(jsonBody);
        try (var socket = new Socket(uri.getHost(), 80);
             var writer = new PrintWriter(socket.getOutputStream(), false);
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("POST " + uri.getPath() + " HTTP/1.1\r");
            writer.println("Host: " + uri.getHost() + "\r");
            writer.println("Content-Length: " + jsonBody.length() + "\r");
            writer.println("Content-Type: application/json;charset=UTF-8\r");
            writer.println("User-Agent: anderb_socket/0.1\r");
            writer.println("\r");
            writer.println(jsonBody + "\r");
            writer.println("\r");
            writer.println("\r");
            writer.flush();
            System.out.println("Reading response:");
            reader.lines().forEach(System.out::println);
        }
    }

    @Data
    @Builder
    static class Image {
        private String initLocation;
        private String location;
        private long size;

        public Image(String initLocation) {
            this.initLocation = initLocation;
        }
    }

    @Data
    @Builder
    static class BobocodeRequest {
        private Picture picture;
        private User user;
    }

    @Data
    @Builder
    static class Picture {
        private String url;
        private long size;
    }

    @Data
    @Builder
    static class User {
        private String firstName;
        private String lastName;
    }
}
