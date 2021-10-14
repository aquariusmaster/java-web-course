package com.bobocode.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

public class Json {

    @SneakyThrows
    public static void main(String[] args) {
        var mapper = new ObjectMapper();
        var json = "{\n" +
                "\"firstName\":          \"Andrii\",\n" +
                "  \"lastName\": \"Petrov\"      ,     \n" +
                "  \"email\" :\"apetrov@gmail.com\",\n" +
                "  \"age\": 19,\n" +
                "  \"address\": {\n" +
                "    \"line1\": \"Kiev\",\n" +
                "    \"line2\": \"Svitla\"\n" +
                "  },\n" +
                "  \"active\"      : true   \n" +
                "}";
        User user = mapper.readValue(json, User.class);
        System.out.println(user);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class User {
        private String firstName;
        private String lastName;
        private String email;
        private Integer age;
        private boolean active;
        private Address address;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Address {
        private String line1;
        private String line2;
    }

}
