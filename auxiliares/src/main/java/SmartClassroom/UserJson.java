package SmartClassroom;


import com.fasterxml.jackson.databind.ObjectMapper;

import SmartClassroom.models.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import org.apache.commons.io.*;

/**
 * Class to read the users from a JSON file
 */
public class UserJson {
    /** Variable that stores all users read from the JSON file */
    private User[] user_list = null;

    /**
     * Load the users from the JSON file
     * @param filePath path to the JSON file
     * @return array with all users read from the JSON file
     */
    private User[] loadUsersFromFile(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        User[] users = null;

        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String text = IOUtils.toString(inputStream, "UTF-8");
            // System.out.println(text);
            
            // Read the JSON array directly into a User array
            users = objectMapper.readValue(text, User[].class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Constructor;
     * Loads the users from the JSON file, the path to the JSON file is hardcoded 
     * and has to match the path in the container
     */
    public UserJson() {
        // Path to the JSON file
        String jsonFilePath = "/users.json";

        // Load the users from the JSON file
        this.user_list = loadUsersFromFile(jsonFilePath);
    }

    /** 
     * Get the user from the user matricula
     * @param matricula user matricula
     * @return the user with the given matricula
     * @return null if the user is not found
     */
    public User getUser(Integer matricula) {
        for (User user : this.user_list) {
            if (user.matricula == matricula) {
                return user;
            }
        }
        return null;
    }

    /**
     * Get the user list
     * @return the user list
     */
    public User[] getUserList() {
        return this.user_list;
    }
}