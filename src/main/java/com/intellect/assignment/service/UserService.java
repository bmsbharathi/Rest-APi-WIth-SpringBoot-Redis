package com.intellect.assignment.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellect.assignment.dao.RedisDao;
import com.intellect.assignment.domain.Response;
import com.intellect.assignment.domain.User;
import com.intellect.assignment.domain.ValidationErrors;
import com.intellect.assignment.validator.UserValidator;

@Service
public class UserService {

	@Autowired
	private RedisDao redisDao;
	@Autowired
	private UserValidator userValidator;
	private static List<User> userList = new ArrayList<>();
	private static ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = Logger.getLogger(UserService.class);

	public Response createUser(User user) {

		logger.info("Inside createUser() - UserService");
		Response resp = new Response();
		List<ValidationErrors> valErrors = new ArrayList<>();
		valErrors = userValidator.validateUser(user);
		Iterator<ValidationErrors> iter = valErrors.iterator();
		logger.info("After Validation- " + valErrors);

		boolean isEmailExists = redisDao.checkUserEmail(user.getEmail());
		logger.info("Checking MailId");
		if (isEmailExists) {

			resp.setResMsg("Active User exists with same Mail ID");
			resp.setUserId(user.getId());
			ValidationErrors error = new ValidationErrors();
			error.setCode("-1");
			error.setField("Email");
			error.setMessage("Email Id already exists");
			valErrors.add(error);
			resp.setValErrors(valErrors);

			return resp;
		}

		if (iter.hasNext()) {

			resp.setResMsg("User has Validation errors");
			resp.setUserId(user.getId());
			resp.setValErrors(valErrors);

			return resp;
		}

		redisDao.addUserToRedis(user);
		resp.setResMsg("User has been created");
		resp.setUserId(user.getId());
		resp.setValErrors(valErrors);

		return resp;
	}

	public String getUsers() throws JsonProcessingException, ParseException {

		userList = redisDao.getAllUsers();
		logger.info("List of users:\n" + mapper.writeValueAsString(userList));
		return mapper.writeValueAsString(userList);
	}

	public ResponseEntity<Response> deleteUser(String id) {

		Response resp = new Response();
		Boolean result = redisDao.deActivateUser(id);
		if (result) {

			resp.setResMsg("Successfully Deactivated");
			resp.setUserId(id);

			return new ResponseEntity<Response>(resp, HttpStatus.OK);
		}

		resp.setResMsg("Could not delete, User with Id: " + id + " Not found!");
		resp.setUserId(id);

		return new ResponseEntity<Response>(resp, HttpStatus.BAD_REQUEST);
	}

	public Response getUserById(String id) throws ParseException {

		logger.info("ID: " + id);
		Response resp = redisDao.getUserFromRedis(id);

		return resp;
	}

	public ResponseEntity<Response> updateUser(User user) {

		logger.info("Inside UserService - updateUser()");
		Response response = new Response();
		boolean result = redisDao.updateUser(user);

		if (result) {

			response.setResMsg("User pincode and birthDate Successfully updated!");
			response.setUserId(user.getId());

			return new ResponseEntity<Response>(response, HttpStatus.OK);
		}

		response.setResMsg("Failed to Update, User doesn't exist");
		response.setUserId("User id not found");

		return new ResponseEntity<Response>(response, HttpStatus.BAD_REQUEST);
	}
}
