package com.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
		@GetMapping("/users/status")
	    public String status() {
	        return "User service is up!";
}
}