package main.java.SmartClassroom;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays; 
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

// import Util;
import SmartClassroom.TurmaJson;
import SmartClassroom.UserJson;
import SmartClassroom.models.SalaHorario;
import SmartClassroom.models.Turma;
import SmartClassroom.models.User;
import ckafka.data.Swap;
import ckafka.data.SwapData;
import main.java.application.ModelApplication;

public class ProcessingNode extends ModelApplication{
    private Swap swap;
    private ObjectMapper objectMapper;
    //private static final Logger logger = LoggerFactory.getLogger(ProcessingNode.class);
    private static final ZoneId zoneId = ZoneId.of("America/Sao_Paulo");
    private static final String GROUPS_LOG_FILE_PATH = "/groups_log.csv";
    private static final String ATTENDANCE_LOG_FILE_PATH = "/attendance_log.csv";
    private static final String ATTENDANCE_TABLE_PATH = "/attendance_table.csv";

    // Valid commands
    private static final String COMMAND_REGISTER_ATTENDANCE = "Register";
    private static final String COMMAND_LOG_ATTENDANCE = "LOG";

    // The variable cannot be local because it is being used in a lambda function
    // Control of the eternal loop until it ends
    private UserJson user_dto = new UserJson();
    private TurmaJson turma_dto = new TurmaJson();
    private boolean fim = false;
    Map<String, Consumer<String[]>> commandMap = new HashMap<>();

    /**
     * Constructor
     */
    public ProcessingNode() {
        this.objectMapper = new ObjectMapper();
        this.swap = new Swap(objectMapper);
    }

    /**
     * Main
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        ProcessingNode pn = new ProcessingNode();
        pn.runPN(keyboard);
    }

    /**
     * TODO
     */
    public void runPN(Scanner keyboard) {
        // Map commands to corresponding functions
        this.commandMap.put(COMMAND_REGISTER_ATTENDANCE, params -> registerAttendance(params));
        this.commandMap.put(COMMAND_LOG_ATTENDANCE, params -> writeGroupLogs(params));

        while(!fim) {
            this.start_scheduling();
            // this.logAttendance(this.countTimeInClass());
            // this.sendGroupcastMessage("FML", "69");
            this.checkStudentsAttendance();
            
            try{
                Thread.sleep(60000);
            }
            catch(InterruptedException e){
                System.out.println("Error: " + e);
            }
        }
        keyboard.close();
        System.out.println("END!");
        System.exit(0);
    }

    public void start_scheduling(){
        try {
            Date now = new Date();
            Timer timer = new Timer();
            
            timer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() 
                    {
                        // get all turmas
                        Turma[] turmas = turma_dto.getTurmas();
                        for (Turma turma : turmas) 
                        {
                            alertClassTime(turma);
                        }
                    }
                }, now, 60000);
        
            } catch (Exception e) {
            logger.error("Error scheduling SendLocationTask", e);
        }
        return;
    }
    
    public void alertClassTime(Turma turma) {
        LocalDate currentDate = LocalDate.now(zoneId);
        LocalTime currentTime = LocalTime.now(zoneId).withSecond(0).withNano(0);
    
        int dayOfWeek = currentDate.getDayOfWeek().getValue();
    
        for (SalaHorario salaHorario : turma.salas_horarios) {
            try {
                int scheduledDay = salaHorario.getDayOfWeek();
                LocalTime scheduledStartTime = LocalTime.parse(salaHorario.getTimeString(), DateTimeFormatter.ofPattern("HH:mm")).minusMinutes(10);
                
                // since we already subtracted 10 minutes from scheduledStartTime, 
                // we only need to add the hours to get 10 min before the end time
                LocalTime scheduledEndTime = scheduledStartTime.plusHours((turma.duracao));
                
                if (scheduledDay == dayOfWeek) 
                {
                    if (scheduledStartTime.equals(currentTime))
                    {
                        String message = String.format("Class for %s starts soon in room %s", turma.id_turma, salaHorario.sala);
                        System.out.println(message);
                        
                        // Send message to the group
                        try {
                            sendRecord(createRecord("GroupMessageTopic", String.valueOf(turma.group), swap.SwapDataSerialization(createSwapData(message))));
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("Error SendGroupCastMessage", e);
                        }
                    }

                    if (scheduledEndTime.equals(currentTime))
                    {
                        String message = String.format("Class for %s ends soon in room %s", turma.id_turma, salaHorario.sala);
                        System.out.println(message);
                        
                        // Send message to the group
                        try 
                        {
                            sendRecord(createRecord("GroupMessageTopic", String.valueOf(turma.group), swap.SwapDataSerialization(createSwapData(message))));
                        } catch (Exception e) 
                        {
                            e.printStackTrace();
                            logger.error("Error SendGroupCastMessage", e);
                        }
                    }
                }

            } catch (NumberFormatException | DateTimeParseException e) {
                System.out.println("Error parsing schedule: " + salaHorario + " - " + e.getMessage());
            }
        }
    }

    /**
     * Registers the attendance of students in a class based on the provided parameters.
     *
     * @param params An array of strings containing the following parameters:
     *               - params[0]: The class identifier part 1.
     *               - params[1]: The class identifier part 2.
     *               - params[2]: The date of the class.
     *               - params[3]: The threshold for attendance as a float (e.g., 0.8 for 80%).
     *
     * This method retrieves the class information using the provided identifiers,
     * calculates the time each student spent in the class, and writes the attendance
     * information to a file. A student is marked as "PRESENTE" if their attendance
     * time meets or exceeds the threshold percentage of the class duration; otherwise,
     * they are marked as "FALTA".
     *
     * The attendance information is written to a file specified by the constant
     * ATTENDANCE_TABLE_PATH. Each line in the file contains the class date, class
     * identifier, student matricula, and attendance status.
     *
     * @throws IOException If an I/O error occurs while writing to the attendance table file.
     */
    public void registerAttendance(String[] params) {
        // Retrieve the Turma object based on the provided class identifiers
        Turma turmaObj = this.turma_dto.getTurma(String.format("%s %s", params[0], params[1]));
        
        // Extract the class date and threshold from the parameters
        String classDate = params[2];
        Float threshold = Float.parseFloat(params[3]);
        
        // Count the time each user is in different classes
        Map<String, Map<String, Map<String,Integer>>> userGroupCount = this.countTimeInClass();
        
        // Try-with-resources to automatically close the PrintWriter
        try (PrintWriter writer = new PrintWriter(new FileWriter(ATTENDANCE_TABLE_PATH, true))) 
        {
            // Iterate through each student ID in the userGroupCount map
            for (String matricula : userGroupCount.keySet()) 
            {
            // Get the map for the specific student's attendance records
            Map<String, Map<String, Integer>> dateMap = userGroupCount.get(matricula);
            
            // Get the map of group attendance counts for the specified class date
            Map<String, Integer> groupCounts = dateMap.get(classDate);
            
            // Iterate through each group and its attendance count
            for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) 
            {
                String groupId = entry.getKey(); // Get the group ID
                int count = entry.getValue(); // Get the attendance count
                
                try
                {
                // Retrieve the Turma object based on the group ID
                Turma turma = turma_dto.getTurma(Integer.parseInt(groupId));
                
                // Check if the group matches the specified class group
                if (turma.group == turmaObj.group) {    
                    int duration = turma.duracao * 60; // Assume duration is in minutes
                    System.out.println("PRESENTE ->  " + (threshold * duration));
                    // Check if the attendance count meets or exceeds the threshold
                    if (count >= threshold * duration) {
                    // Log as PRESENTE if attendance is sufficient
                    writer.println(classDate + "," + turma.disciplina + " " + turma.id_turma + "," + matricula + ",PRESENTE");
                    } else {
                    // Log as FALTA if attendance is insufficient
                    writer.println(classDate + "," + turma.disciplina + " " + turma.id_turma + "," + matricula + ",FALTA");
                    }
                }
                } catch (Exception e) {
                // Log an error if there is an issue retrieving the Turma
                System.err.println("Error getting turma by attending group: " + e.getMessage());
                }
            }
            }
        } catch (IOException e) {
            // Log an error if there is an issue writing to the attendance table
            System.err.println("Error writing to attendance_table: " + e.getMessage());
        }
    }

    private void executeCommand(String fullCommand) {
        String[] args = fullCommand.split(" ");
        String command = args[0];
        String[] params = Arrays.copyOfRange(args, 1, args.length);
        if (this.commandMap.containsKey(command)) {
            this.commandMap.get(command).accept(params);
        } else {
            System.out.println("Invalid command");
        }
    }

    /**
     * 
     */
    @Override
    public void recordReceived(ConsumerRecord record) {
        System.out.println(String.format("Command received from %s", record.key()));        
        try {
            SwapData data = swap.SwapDataDeserialization((byte[]) record.value());
            String text = new String(data.getMessage(), StandardCharsets.UTF_8);
            User[] user_list = this.user_dto.getUserList();
            System.out.println("Message received: " + text);
            executeCommand(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param keyboard
     */
    private void sendUnicastMessage(Scanner keyboard) {
        System.out.println("UUID:\nHHHHHHHH-HHHH-HHHH-HHHH-HHHHHHHHHHHH");
        String uuid = keyboard.nextLine();
        System.out.print("Message: ");
        String messageText = keyboard.nextLine();
        System.out.println(String.format("Sending |%s| to %s.", messageText, uuid));
    
        try {
            sendRecord(createRecord("PrivateMessageTopic", uuid, swap.SwapDataSerialization(createSwapData(messageText))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * UPDATE THIS JAVA DOC
     * Send groupcast message
     * @param keyboard
     */
    private void sendGroupcastMessage(String messageText, Integer group_num, String topic) {
        String group = String.format("%d", group_num);

        System.out.println(String.format("Sending message %s to group %s.",
                                         messageText, group));
        try {
            // sendRecord(createRecord("GroupMessageTopic", group, swap.SwapDataSerialization(createSwapData(messageText))));

            SwapData data = createSwapData(messageText);
            data.setMessage(messageText.getBytes());
            data.setTopic(topic);

            sendRecord(createRecord("GroupMessageTopic", group, swap.SwapDataSerialization(data)));

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error SendGroupCastMessage", e);
        }
    }

    /**
     * Check students attendance
     */
    private void checkStudentsAttendance() {
        LocalDate currentDate = LocalDate.now(zoneId);
        LocalTime currentTime = LocalTime.now(zoneId).withSecond(0).withNano(0);
        ArrayList<Integer> groups = turma_dto.getAllCurrentClassGroups(currentDate, currentTime);
        if (groups.isEmpty()) {
            System.out.println("No classes at this time.");
            return;
        }
        String topic = "StudentAttendanceCheck";
        
        for (Integer group : groups) {
            String message = String.format("%d", group);
            System.out.println(String.format("Checking attendance for group %d", group));
            this.sendGroupcastMessage(message, group, topic);
        }
    }

    private void writeGroupLogs(String[] params)
    {
        String data = params[0];
        String hora = params[1];
        // String [] matriculas = params[2];
        String groupsString = params[2];

        try (PrintWriter writer = new PrintWriter(new FileWriter(GROUPS_LOG_FILE_PATH, true))) 
        {
            // writer.println(data + "," + hora + "," + matriculas + ","+ groupsString);   
            writer.println(data + "," + hora + "," + groupsString);
        } catch (IOException e) {
            logger.error("Error writing to log file", e);
        }
    }

    /**
     * Counts the time each user is in different classes based on log data.
     * 
     * This method reads a log file specified by the constant GROUPS_LOG_FILE_PATH,
     * where each line contains a date, time, user matricula, and a list of group IDs.
     * It processes this data to count the number of times a user attends each group
     * on a given date.
     * 
     * @return A nested map where the first key is the user's matricula, the second key
     *         is the date, and the third key is the group ID. The value is the count of
     *         times the user attended that group on that date.
     */
    public Map<String, Map<String, Map<String, Integer>>> countTimeInClass() {
        Map<String, Map<String, Map<String, Integer>>> userGroupCount = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(GROUPS_LOG_FILE_PATH))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String date = parts[0];
                String hora = parts[1];
                String matricula = parts[2];
                String groupsString = parts[3];

                // Split the group string into individual group IDs
                String[] groups = groupsString.equals("None") ? new String[0] : groupsString.split(", ");

                // Initialize nested maps
                userGroupCount.putIfAbsent(matricula, new HashMap<>());
                userGroupCount.get(matricula).putIfAbsent(date, new HashMap<>());

                for (String group : groups) {
                    try {
                        Turma turma = turma_dto.getTurma(Integer.parseInt(group));
                        String turmaGroup = String.valueOf(turma.group);

                        userGroupCount.get(matricula).get(date).putIfAbsent(turmaGroup, 0);

                        // Update the userGroupCount using turma.group as the key
                        userGroupCount.get(matricula).get(date).put(turmaGroup, 
                            userGroupCount.get(matricula).get(date).get(turmaGroup) + 1);
                    } catch (Exception e) {
                        System.err.println("Error getting turma by group: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }

        return userGroupCount;
    }

    /**
     * Logs the attendance of users based on their group counts.
     *
     * @param userGroupCount A nested map where the first key is the user's matricula (ID), 
     *                       the second key is the date, and the third key is the group ID 
     *                       with the corresponding attendance count.
     *                       
     * This method writes the attendance information to a log file specified by 
     * ATTENDANCE_LOG_FILE_PATH. For each user, it iterates through the dates and groups 
     * to determine if the user was present or absent based on the attendance count. 
     * A user is considered present if their attendance count is at least 80% of the 
     * group's duration.
     *
     * The log entry format is: date, discipline group_id, matricula, status
     * where status is either "Present" or "Absent".
     *
     * If an error occurs while retrieving the group information or writing to the log file, 
     * an error message is printed to the standard error stream.
     *
     * @throws IOException If an I/O error occurs while writing to the log file.
     */
    public void logAttendance(Map<String, Map<String, Map<String, Integer>>> userGroupCount) 
    {
      // Try-with-resources to automatically close the PrintWriter
      try (PrintWriter writer = new PrintWriter(new FileWriter(ATTENDANCE_LOG_FILE_PATH, true))) 
      {  
        // Iterate through each student ID in the userGroupCount map
        for (String matricula : userGroupCount.keySet()) 
        {
            // Get the map for the specific student's attendance records
            Map<String, Map<String, Integer>> dateMap = userGroupCount.get(matricula);
            
            // Iterate through each date associated with the student
            for (String date : dateMap.keySet()) 
            {
                // Get the map of group attendance counts for that date
                Map<String, Integer> groupCounts = dateMap.get(date);
                
                // Iterate through each group and its attendance count
                for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) 
                {
                    String groupId = entry.getKey(); // Get the group ID
                    int count = entry.getValue(); // Get the attendance count
                    
                    try {
                        // Retrieve the Turma object based on the group ID
                        Turma turma = turma_dto.getTurma(Integer.parseInt(groupId));
                        int duration = turma.duracao * 60; // Assume duration is in minutes

                        // Check if the attendance count is at least 80% of the class duration
                        if (count >= 0.8 * duration) {
                            // Log as Present if attendance is sufficient
                            writer.println(date + "," + turma.disciplina + " " + turma.id_turma + "," + matricula + ",Present");
                        } else {
                            // Log as Absent if attendance is insufficient
                            writer.println(date + "," + turma.disciplina + " " + turma.id_turma + "," + matricula + ",Absent");
                        }
                    } catch (Exception e) {
                        // Log an error if there is an issue retrieving the Turma
                        System.err.println("Error getting turma by attending group: " + e.getMessage());
                    }
                }
            }
        }
        } catch (IOException e) 
        {
            // Log an error if there is an issue writing to the attendance log
            System.err.println("Error writing to attendance log: " + e.getMessage());
        }
    }
}
