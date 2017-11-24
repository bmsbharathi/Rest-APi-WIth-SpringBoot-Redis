package com.intellect.assignment.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.intellect.assignment.config.Constants;
import com.intellect.assignment.domain.Response;
import com.intellect.assignment.domain.User;
import com.intellect.assignment.domain.ValidationErrors;

import redis.clients.jedis.Jedis;

@Component
public class RedisDao {

	private Jedis jedis;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy");
	private static String VARIABLE_PREFIX = "USER_";
	private Logger logger = Logger.getLogger(RedisDao.class);

	@PostConstruct
	public void createRedisConnection() {

		jedis = new Jedis(Constants.REDIS_HOST, Constants.REDIS_PORT);

	}

	public Response getUserFromRedis(String id) throws ParseException {

		String varName = VARIABLE_PREFIX.concat(id);
		Response resp = new Response();
		boolean isExists = jedis.exists(varName);
		if (isExists) {

			User user = new User();
			user.setId(jedis.hget(varName, "id"));
			user.setlName(jedis.hget(varName, "lName"));
			user.setfName(jedis.hget(varName, "fName"));
			user.setEmail(jedis.hget(varName, "email"));
			user.setPinCode(Long.parseLong(jedis.hget(varName, "pinCode")));

			logger.info("BIRTH_DATE ---" + jedis.hget(varName, "birthDate"));
			user.setBirthDate(dateFormat.parse(jedis.hget(varName, "birthDate")));
			logger.info("Status: --" + jedis.hget(varName, "isActive"));
			logger.info("Status: in Boolean: " + Boolean.parseBoolean(jedis.hget(varName, "isActive")));
			user.setIsActive(Boolean.parseBoolean(jedis.hget(varName, "isActive")));

			resp.setResMsg("user with id: " + user.getId() + " found! " + user);
			resp.setUserId(user.getId());
			resp.setValErrors(new ArrayList<ValidationErrors>());

			return resp;
		}

		resp.setResMsg("User does not exist!");
		resp.setUserId(id);
		List<ValidationErrors> valErrors = new ArrayList<ValidationErrors>();
		ValidationErrors error = new ValidationErrors();
		error.setCode(String.valueOf(-5));
		error.setField("UserId");
		error.setMessage("User not Found!s");
		resp.setValErrors(valErrors);

		return resp;
	}

	public boolean checkUserEmail(String email) {

		long noOfUsers = jedis.llen("userList");
		List<String> userList = jedis.lrange("userList", 0, noOfUsers);
		System.out.println(userList);
		for (String user : userList) {

			logger.info("Variable Name " + user);
			String userEmail = jedis.hget(user, "email");
			String status = jedis.hget(user, "isActive");
			System.out.println("Email exists " + userEmail + " and its status " + status);
			if (userEmail.equals(email) && status.equals("true")) {

				return true;
			}
		}

		return false;
	}

	public void addUserToRedis(User user) {

		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "id", user.getId());
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "email", user.getEmail());
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "fName", user.getfName());
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "lName", user.getlName());
		logger.info("Date before Insertion: " + dateFormat.format(user.getBirthDate()));
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "birthDate", dateFormat.format(user.getBirthDate()));
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "pinCode", String.valueOf(user.getPinCode()));
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "isActive", String.valueOf(user.getIsActive()));

		jedis.lpush("userList", "USER_".concat(user.getId()));
		logger.info("added");
	}

	public List<User> getAllUsers() throws ParseException {

		List<User> listOfUsers = new ArrayList<>();
		Long noOfUsers = jedis.llen("userList");

		List<String> userName = jedis.lrange("userList", 0, noOfUsers);

		for (String name : userName) {

			User user = new User();

			user.setId(jedis.hget(name, "id"));
			user.setlName(jedis.hget(name, "lName"));
			user.setfName(jedis.hget(name, "fName"));
			user.setEmail(jedis.hget(name, "email"));
			user.setPinCode(Long.parseLong(jedis.hget(name, "pinCode")));
			logger.info("1)DOB of User:- " + jedis.hget(name, "birthDate"));
			user.setBirthDate(dateFormat.parse(jedis.hget(name, "birthDate")));
			user.setIsActive(Boolean.getBoolean(jedis.hget(name, "isActive")));
			logger.info("Status of User:- " + jedis.hget(name, "isActive"));
			listOfUsers.add(user);
		}

		return listOfUsers;
	}

	public boolean deActivateUser(String userid) {

		boolean isExists = jedis.exists(VARIABLE_PREFIX.concat(userid));

		if (isExists) {

			jedis.hset(VARIABLE_PREFIX.concat(userid), "isActive", "false");
			return true;
		}

		return false;
	}

	public boolean updateUser(User user) {

		boolean isExists = jedis.exists(VARIABLE_PREFIX.concat(user.getId()));
		logger.info("User to be updated " + user);

		if (!isExists)
			return false;

		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "pinCode", String.valueOf(user.getPinCode()));
		jedis.hset(VARIABLE_PREFIX.concat(user.getId()), "birthDate", dateFormat.format(user.getBirthDate()));

		return true;
	}

	@PreDestroy
	public void closeRedisConnection() {

		jedis.close();
	}
}
