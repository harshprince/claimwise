package com.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

	  @GetMapping("/auth/status")
	    public String status() {
	        return "Hi Auth Service is UP and working!";
	    }
	  @PostMapping("/auth/login")
	    public ResponseEntity<String> login() {
	        return ResponseEntity.ok("Login successful");
	    }
}
