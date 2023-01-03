package com.io.file.model;

import lombok.Data;

import java.util.List;

@Data
public class UserAddressDTO {
    private String version;
    private List<User> users;
    private List<Address> addresses;

}
