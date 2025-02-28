package com.bobocode.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

public class DemoWebApp {

    @SneakyThrows
    public static void main(String[] args) {
        var photosUri = URI.create("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=12&api_key=DEMO_KEY");

        var photos = doWithinOpenedSocket(photosUri, (writer, reader) -> {
            writer.println("GET " + photosUri.getPath() + "?" + photosUri.getQuery() + " HTTP/1.0");
            writer.println("Host: " + photosUri.getHost());
            writer.println();
            writer.flush();
            return reader.lines().filter(s -> s.startsWith("{")).findFirst().orElseThrow();
        });

        var images = new ObjectMapper().readValue(photos, JsonNode.class)
                .findValuesAsText("img_src")
                .stream()
                .map(Image::new)
                .collect(Collectors.toList());

        doWithinOpenedSocket(URI.create(images.get(0).getInitLocation()), (writer, reader) -> {
            images.forEach(img ->
                    img.setLocation(headFor(writer, reader, URI.create(img.getInitLocation()), "Location"))
            );
            return null;
        });

        doWithinOpenedSocket(URI.create(images.get(0).getLocation()), (writer, reader) -> {
            images.forEach(img -> {
                        var lengthStr = headFor(writer, reader, URI.create(img.getLocation()), "Content-Length");
                        img.setSize(Long.parseLong(lengthStr));
                    }
            );
            return null;
        });

        var maxImage = images.stream().max(comparingLong(Image::getSize)).orElseThrow();

        var request = new BobocodeRequest(
                new Picture(maxImage.getInitLocation(), maxImage.getSize()),
                new User("Andrii", "Bobrov")
        );

        var bobocodeServer = URI.create("https://bobocode.herokuapp.com/nasa/pictures");
        var jsonBody = new ObjectMapper().writeValueAsString(request);

        var response = doWithinOpenedSocket(bobocodeServer, (writer, reader) -> {
            writer.println("POST " + bobocodeServer.getPath() + " HTTP/1.0\r");
            writer.println("Host: " + bobocodeServer.getHost() + "\r");
            writer.println("Content-Length: " + jsonBody.length() + "\r");
            writer.println("Content-Type: application/json;charset=UTF-8\r");
            writer.println("User-Agent: AnderbSocket/0.1\r");
            writer.println("\r");
            writer.println(jsonBody + "\r");
            writer.println("\r");
            writer.println("\r");
            writer.flush();
            System.out.println("Reading response:");
            return reader.lines().collect(Collectors.joining("\n"));
        });

        System.out.println(response);

    }

    private static String headFor(PrintWriter writer, BufferedReader reader, URI uri, String header) {
        writer.println("HEAD " + uri.getPath() + " HTTP/1.1");
        writer.println("Host: " + uri.getHost());
        writer.println();
        writer.flush();
        return reader.lines()
                .filter(line -> line.startsWith(header))
                .map(im -> im.substring(header.length() + 2))
                .findFirst().orElseThrow();
    }

    @SneakyThrows
    private static String doWithinOpenedSocket(URI uri, BiFunction<PrintWriter, BufferedReader, String> biFunction) {
        try (var socket = openSocketConnection(uri);
             var writer = new PrintWriter(socket.getOutputStream());
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            return biFunction.apply(writer, reader);
        }
    }

    private static Socket openSocketConnection(URI uri) throws IOException {
        if (uri.getScheme().equals("https")) {
            return SSLSocketFactory.getDefault().createSocket(uri.getHost(), 443);
        }
        return new Socket(uri.getHost(), 80);
    }

    @Data
    @Builder
    @AllArgsConstructor
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
    @AllArgsConstructor
    static class BobocodeRequest {
        private Picture picture;
        private User user;
    }

    @Data
    @Builder
    @AllArgsConstructor
    static class Picture {
        private String url;
        private long size;
    }

    @Data
    @Builder
    @AllArgsConstructor
    static class User {
        private String firstName;
        private String lastName;
    }
}
