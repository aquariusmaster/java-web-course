package com.bobocode.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
                .map(imgSrc -> Image.builder().initLocation(imgSrc).build())
                .collect(Collectors.toList());
        setLocation(images);
        setLength(images);

        Image maxImage = images.stream()
                .max(Comparator.comparing(Image::getSize))
                .orElseThrow();
        System.out.println(maxImage);
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
    private static void setLocation(List<Image> urls) {
        try (var socket = new Socket(URI.create(urls.get(0).initLocation).getHost(), 80);
             var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            urls.forEach(img ->
                    img.setLocation(headFor(writer, reader, URI.create(img.getInitLocation()), "Location"))
            );
        }
    }

    @SneakyThrows
    private static void setLength(List<Image> urls) {
        var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (var socket = (SSLSocket) factory.createSocket(URI.create(urls.get(0).location).getHost(), 443);
             var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            urls.forEach(img -> {
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
//        URI uri = URI.create("https://bobocode.herokuapp.com/nasa/pictures");
//        var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//        ObjectMapper mapper = new ObjectMapper();
//        ObjectWriter objectWriter = mapper.writerFor(request.getClass());
//        String jsonBody = objectWriter.writeValueAsString(request);
        String jsonBody = "{\"picture\":{\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00012/soas/rdr/ccam/CR0_398560983PRCLF0030004CCAM03012L1.PNG\",\"size\":660132},\"user\":{\"firstName\":\"Andrii\",\"lastName\":\"Bobrov\"}}";
        System.out.println(jsonBody);
        try (var socket = new Socket("bobocode.herokuapp.com", 80);
             var writer = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("GET /nasa/pictures HTTP/1.1");
            writer.println("Host: bobocode.herokuapp.com");
//            writer.println("Content-Length: " + jsonBody.length());
//            writer.println("Content-Length:2");
//            writer.println("Content-Type: application/json;charset=UTF-8");
//            writer.println("\r\n");
//            writer.println(jsonBody);
//            writer.println("{}");
            writer.println();
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
