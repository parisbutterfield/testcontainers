package com.parisbutterfield.app.controller;

import com.parisbutterfield.app.repository.UserRepository;
import com.parisbutterfield.app.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping(path="/")
public class MainController {
	Logger logger = LoggerFactory.getLogger(MainController.class);

	@Autowired
	private UserRepository userRepository;

	@RequestMapping(path="/addUser",  method = RequestMethod.POST)
	public @ResponseBody Integer addNewUser (@RequestBody User user) {
		return userRepository.save(user).getId();
	}

	@RequestMapping(path="/addUsers",  method = RequestMethod.POST)
	public @ResponseBody Integer addNewUsers (@RequestBody List<User> users) {
		AtomicInteger atomicInteger = new AtomicInteger(0);
		userRepository.saveAll(users).forEach(user -> atomicInteger.getAndIncrement());
		return atomicInteger.get();
	}

	@GetMapping(path="/all")
	public @ResponseBody Iterable<User> getAllUsers() {
		return userRepository.findAll();
	}

	@GetMapping(path="/count")
	public @ResponseBody Long countAllUsers() {
		return userRepository.count();
	}

	@GetMapping(path="/user/{userId}")
	public @ResponseBody User getUser(@PathVariable(value="userId") String id) {
		return userRepository.findById(Integer.valueOf(id)).get();
	}

}
