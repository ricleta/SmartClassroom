package main.java.SmartClassroom;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import SmartClassroom.TurmaJson;
import SmartClassroom.UserJson;
import SmartClassroom.models.SalaHorario;
import SmartClassroom.models.Turma;
import SmartClassroom.models.User;
import ckafka.data.Swap;
import main.java.ckafka.GroupDefiner;
import main.java.ckafka.GroupSelection;

/**
 * MyGroupDefiner is a class that implements the GroupSelection interface.
 * It is responsible for defining and managing groups based on various criteria.
 * This class interacts with user and turma (class) data to determine group assignments.
 * It also logs information about group assignments to a specified log file.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * MyGroupDefiner myGroupDefiner = new MyGroupDefiner();
 * Set<Integer> groups = myGroupDefiner.getNodesGroupByContext(contextInfo);
 * }
 * </pre>
 * 
 * <p>Groups managed by this class include:</p>
 * <ul>
 *   <li>6000 -> Professors</li>
 *   <li>6001 -> Students</li>
 *   <li>3000 -> INF1304 - 3WA</li>
 *   <li>3100 -> INF1748 - 3WA</li>
 *   <li>3101 -> INF1748 - 3WB</li>
 *   <li>16500 -> T01</li>
 *   <li>16501 -> LABGRAD</li>
 *   <li>16502 -> L420</li>
 *   <li>16503 -> L522</li>
 *   <li>16001 -> INF1304 - 3WA - PRESENT</li>
 *   <li>16002 -> INF1304 - 3WA - ABSENT</li>
 *   <li>16003 -> INF1748 - 3WA - PRESENT</li>
 *   <li>16004 -> INF1748 - 3WA - ABSENT</li>
 *   <li>16005 -> INF1748 - 3WB - PRESENT</li>
 *   <li>16006 -> INF1748 - 3WB - ABSENT</li>
 * </ul>
 * 
 * <p>Methods:</p>
 * <ul>
 *   <li>{@link #groupsIdentification()} - Returns a set of all group IDs managed by this class.</li>
 *   <li>{@link #getNodesGroupByContext(ObjectNode)} - Returns a set of group IDs related to the provided context information.</li>
 *   <li>{@link #logInfo(String, String, String, Set)} - Logs information about a student's group assignment.</li>
 *   <li>{@link #kafkaConsumerPrefix()} - Returns the Kafka consumer prefix.</li>
 *   <li>{@link #kafkaProducerPrefix()} - Returns the Kafka producer prefix.</li>
 * </ul>
 * 
 * <p>Dependencies:</p>
 * <ul>
 *   <li>{@link UserJson} - For reading and manipulating user data.</li>
 *   <li>{@link TurmaJson} - For reading and manipulating turma (class) data.</li>
 *   <li>{@link ObjectMapper} - For JSON processing.</li>
 *   <li>{@link Swap} - For swapping data.</li>
 *   <li>{@link GroupDefiner} - For defining groups.</li>
 * </ul>
 * 
 * <p>Logging:</p>
 * This class uses SLF4J for logging. Log entries are written to a file specified by {@code logFilePath}.
 * 
 * @see GroupSelection
 * @see UserJson
 * @see TurmaJson
 * @see ObjectMapper
 * @see Swap
 * @see GroupDefiner
 */
public class MyGroupDefiner implements GroupSelection {
    /** Logger */
    final Logger logger = LoggerFactory.getLogger(GroupDefiner.class);
    private final String logFilePath = "/groups_log.csv";
    
    /** objects to help read and manipulate user */
    private UserJson user_dto = new UserJson();
    
    /** objects to help read and manipulate turma */
    private TurmaJson turma_dto = new TurmaJson();

    public static void main(String[] args) {
        MyGroupDefiner MyGD = new MyGroupDefiner();
    }

    /**
     * Constructor
     */
    public MyGroupDefiner() {
        ObjectMapper objectMapper = new ObjectMapper();
        Swap swap = new Swap(objectMapper);
        new GroupDefiner(this, swap);
    }

    /**
     * Set with all the groups that this GroupDefiner controls.
     * @return set with all the groups that this GroupDefiner manages
     */
    public Set<Integer> groupsIdentification() {
        /**
         * 6000 -> Professors
         * 6001 -> Students
         * 3000 -> INF1304 - 3WA
         * 3100 -> INF1748 - 3WA
         * 3101 -> INF1748 - 3WB
         * 16500 -> T01
         * 16501 -> LABGRAD
         * 16502 -> L420
         * 16503 -> L522
         * 16001 -> INF1304 - 3WA - PRESENT
         * 16002 -> INF1304 - 3WA - ABSENT
         * 16003 -> INF1748 - 3WA - PRESENT
         * 16004 -> INF1748 - 3WA - ABSENT
         * 16005 -> INF1748 - 3WB - PRESENT
         * 16006 -> INF1748 - 3WB - ABSENT
         */
        Set<Integer> setOfGroups = new HashSet<Integer>();
     
        setOfGroups.add(6000);
        setOfGroups.add(6001);
        setOfGroups.add(3000);
        setOfGroups.add(3100);
        setOfGroups.add(3101);
        setOfGroups.add(16500);
        setOfGroups.add(16501);
        setOfGroups.add(16502);
        setOfGroups.add(16503);
        setOfGroups.add(16001);
        setOfGroups.add(16002);
        setOfGroups.add(16003);
        setOfGroups.add(16004);
        setOfGroups.add(16005);
        setOfGroups.add(16006);

        return setOfGroups;
    }

    /**
     * Function to get user group ID from location
     * @param location
     * @return group ID
     */
    private int getGroupIDFromLocation(String location)
    {
        System.out.println("Location: " + location);
        location = location.replace("\"", "");
        switch (location) {
            case "T01":
                return 16500;
            case "LABGRAD":
                return 16501;
            case "L420":
                return 16502;
            case "L522":
                return 16503;
            default:
                return -1;
        }
    }

    /**
     * Set with all the groups related to this contextInfo.
     * Only groups controlled by this GroupDefiner.
     * 
     * @param contextInfo context info
     * @return set with all the groups related to this contextInfo
     */
    public Set<Integer> getNodesGroupByContext(ObjectNode contextInfo) {
        // Initialize a set to store group IDs
        Set<Integer> setOfGroups = new HashSet<Integer>();
        System.out.println("#--------------# Receiving context #--------------#");
        
        // Extract the matricula (student ID) from contextInfo
        String matricula = String.valueOf(contextInfo.get("matricula"));
        matricula = matricula.replace("\"", ""); // Remove any quotes

        // Extract the location from contextInfo
        String local = String.valueOf(contextInfo.get("local"));
        local = local.replace("\"", ""); // Remove any quotes
        
        // Parse the date from contextInfo or use the current date if null
        LocalDate date;
        if (contextInfo.get("date").asText().equals("null")) {
            date = LocalDate.now(); // Use today's date if no date is provided
        } else {
            date = LocalDate.parse(contextInfo.get("date").asText()); // Parse the provided date
        }
        
        // Get the day of the week as an integer (1 = Monday, 7 = Sunday)
        int dia_da_semana = date.getDayOfWeek().getValue();
        
        // Extract the hour from contextInfo
        String hora = String.valueOf(contextInfo.get("hour"));
        hora = hora.replace("\"", ""); // Remove any quotes
        
        // Log the extracted information for debugging
        System.out.println("Matricula: " + matricula);
        System.out.println("Local: " + local);
        System.out.println("Data: " + date);
        System.out.println("Hora: " + hora);

        // Retrieve the User object based on matricula
        User user = this.user_dto.getUser(Integer.parseInt(matricula));
        System.out.println("Nome: " + user.nome); // Log the user's name

        // Loop through the classes (turmas) associated with the user
        for (String turma : user.turmas) {
            try {
                // Get the group ID associated with the current turma
                int groupId = this.turma_dto.getGroupIDFromTurma(turma);
                
                // Check if the group ID is valid and add it to the set
                if (groupId != -1) {
                    setOfGroups.add(groupId);
                } else {
                    logger.error("Invalid group ID for turma: " + turma);
                }
            } catch (Exception e) {
                // Log any exceptions that occur while retrieving the group ID
                logger.error("Exception occurred while getting group ID for turma: " + turma, e);
            }

            try {
                // Get the groups related to the user's attendance in the current turma
                Set<Integer> groups = this.turma_dto.getGroupsFromStudentAttendance(turma, dia_da_semana, hora, local);
                
                // If groups were found, add them to the set and log the information
                if (!groups.isEmpty()) {
                    setOfGroups.addAll(groups);
                    this.logInfo(matricula, date.toString(), hora, groups); // Log the attendance info
                }
            } catch (Exception e) {
                // Log any exceptions that occur while retrieving attendance groups
                logger.error("Exception occurred while getting groups from student attendance in turma: " + turma, e);
            }
        }
        
        try {
            // Get the group ID based on the user's location
            int groupId = this.getGroupIDFromLocation(local);
            
            // Check if the group ID is valid and add it to the set
            if (groupId != -1) {
                setOfGroups.add(groupId);
            } else {
                logger.error("Invalid group ID for local: " + local);
            }
        } catch (Exception e) {
            // Log any exceptions that occur while retrieving the group ID based on location
            logger.error("Exception occurred while getting group ID for turma: " + local, e);
        }

        // Log the final set of groups for debugging purposes
        System.out.println(setOfGroups);
        return setOfGroups; // Return the set of group IDs
}

    /**
     * Logs information about a student's group assignment.
     *
     * @param matricula The student's matricula (registration number).
     * @param data The date of the log entry.
     * @param hora The time of the log entry.
     * @param setOfGroups A set of group IDs the student is assigned to.
     * 
     * This method writes a log entry to a file specified by {@code logFilePath}.
     * The log entry includes the date, time, matricula, and a comma-separated list of group IDs.
     * If the set of groups is empty, "None" is logged instead.
     * 
     * @throws IOException If an I/O error occurs while writing to the log file.
     */
    private void logInfo(String matricula, String data, String hora, Set<Integer> setOfGroups) 
    {
        try (PrintWriter writer = new PrintWriter(new FileWriter(this.logFilePath, true))) 
        {   
            // Convert setOfGroups to a comma-separated string
            String groupsString = setOfGroups.isEmpty() ? "None" : setOfGroups.toString().replaceAll("[\\[\\]]", "");
        
            writer.println(data + "," + hora + "," + matricula + "," + groupsString);
        } catch (IOException e) {
            logger.error("Error writing to log file", e);
        }
    }


    public String kafkaConsumerPrefix() {
        return "gd.one.consumer";
    }

    public String kafkaProducerPrefix() {
        return "gd.one.producer";
    }
}
