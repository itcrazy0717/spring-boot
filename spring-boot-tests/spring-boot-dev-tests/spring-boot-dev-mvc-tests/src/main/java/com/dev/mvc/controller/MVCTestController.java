package com.dev.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: dengxin.chen
 * @date: 2019-12-06 14:33
 * @description:
 */
@RestController
public class MVCTestController {

	@GetMapping("/hello-mvc")
	public String hello() {
		return "Hello Spring Boot MVC";
	}
}
