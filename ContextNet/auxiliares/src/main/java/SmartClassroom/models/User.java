package SmartClassroom.models;

/**
 * Class to store the information of a user
 * including the registration number, the name, and the classes which the user is enrolled in.
 */
public class User {

    /**
     * The registration number of the user.
     */
    public int matricula;

    /**
     * The name of the user.
     */
    public String nome;

    /**
     * The classes which the user is enrolled in.
     */
    public String[] turmas;

    /**
     * Default constructor.
     */
    public User() {

    }
}