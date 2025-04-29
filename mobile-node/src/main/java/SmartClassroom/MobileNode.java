package SmartClassroom;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ckafka.data.SwapData;
import lac.cnclib.net.NodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;
import main.java.ckafka.mobile.CKMobileNode;
import main.java.ckafka.mobile.tasks.SendLocationTask;

/**
 * Mobile Node
 * <br>
 * Implements a mobile node that contains a student's information to check their attendance
 */
public class MobileNode extends CKMobileNode {
    // Valid user input options
    private static final String OPTION_GROUPCAST = "G";
    private static final String OPTION_UNICAST = "I";
    private static final String OPTION_PN = "P";
    private static final String OPTION_REGISTER = "R";
    private static final String OPTION_EXIT = "Z";

    private ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    // The variable cannot be local because it is being used in a lambda function
    // Control the infinite loop until it ends
    private boolean fim = false;
    private ArrayList<Integer> studentIDs = new ArrayList<>();
    private String local = "INVALIDO";

    /**
     * main
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        MobileNode mn = new MobileNode();
        mn.runMN(keyboard);

        // Calls close() to properly close MN method after shut down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            close();
        }));
    }

    /**
     * Executes the Mobile Node.
     * <br>
     * Read user option from keyboard (unicast or groupcast message)<br>
     * Read destination receipt from keyboard (UUID or Group)<br>
     * Read message from keyboard<br>
     * Send message<br>
     */
    private void runMN(Scanner keyboard) {
        Map<String, Consumer<Scanner>> optionsMap = new HashMap<>();

        // Maps options to corresponding functions
        optionsMap.put(OPTION_UNICAST, this::sendUnicastMessage);
        optionsMap.put(OPTION_PN, this::enterMessageToPN);
        optionsMap.put(OPTION_GROUPCAST, this::sendGroupcastMessage);
        optionsMap.put(OPTION_EXIT, scanner -> fim = true);
        optionsMap.put(OPTION_REGISTER, this::registerClass);

        Properties properties = new Properties();
        try {
            properties.loadFromXML(new FileInputStream("./properties.xml"));
            this.local = properties.getProperty("placeTag");
            System.out.println("local = " + local);          
        } catch (IOException e) {
            System.out.println("Error reading properties file: " + e.getMessage());
            return;
        }

        // get matriculas from csv in the UserX folder
        File file = new File("./matriculas.csv");

        // Main loop that continues until the 'fim' variable is true
        while (!fim) {
            studentIDs = updateStudentIDs(file);

            // Requests the user's option
            System.out.print("(T) Change location | (R) Register class | (Z) to finish)? ");
            String linha = keyboard.nextLine().trim().toUpperCase();
            System.out.printf("Your option was %s. ", linha);

            // Checks if the option is valid and executes the corresponding function
            if (optionsMap.containsKey(linha))
                optionsMap.get(linha).accept(keyboard);
            else
                System.out.println("Invalid option");
        }

        // Closes the scanner and ends the program
        keyboard.close();
        System.out.println("END!");
        System.exit(0);
    }

    private ArrayList<Integer> updateStudentIDs(File ids_file) {
        ArrayList<Integer> ids = new ArrayList<>();           
        // Read the file and store the matriculas in the studentIDs list
        studentIDs.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(ids_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Assuming the matricula is the first column in the CSV
                String[] values = line.split(",");
                if (values.length > 0) {
                    for (String value : values) {
                        if (!value.trim().isEmpty()) {
                            studentIDs.add(Integer.parseInt(value.trim()));
                        }
                    }
                }
            }
        } 
        catch (IOException e) {
            System.out.println("Error reading matriculas file: " + e.getMessage());
            return null;
        }

        return studentIDs;
    }

    /**
     * When connected, send location at each instant
     */
    @Override
    public void connected(NodeConnection nodeConnection) {
        try {
            logger.debug("Connected");
            final SendLocationTask sendlocationtask = new SendLocationTask(this);
            this.scheduledFutureLocationTask = this.threadPool.scheduleWithFixedDelay(
                    sendlocationtask, 5000, 60000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Error scheduling SendLocationTask", e);
        }
    }

    /**
     * Reads the message via command line from the user.
     * <br>
     * Sends a unicast message
     */
    private void sendUnicastMessage(Scanner keyboard) {
        System.out.println("Unicast message. Enter the individual's UUID: ");
        String uuid = keyboard.nextLine();
        System.out.print("Enter the message: ");
        String messageText = keyboard.nextLine();
        System.out.println(String.format("Message from |%s| to %s.", messageText, uuid));
        // Create and send the message
        SwapData privateData = new SwapData();
        privateData.setMessage(messageText.getBytes(StandardCharsets.UTF_8));
        privateData.setTopic("PrivateMessageTopic");
        privateData.setRecipient(uuid);
        ApplicationMessage message = createDefaultApplicationMessage();
        message.setContentObject(privateData);
        sendMessageToGateway(message);
    }

    /**
     * Receives messages
     */
    @Override
    public void newMessageReceived(NodeConnection nodeConnection, Message message) {
        try {
            SwapData swp = fromMessageToSwapData(message);
            System.out.println("Topic: " + swp.getTopic());
            if (swp.getTopic().equals("Ping")) {
                message.setSenderID(this.mnID);
                sendMessageToGateway(message);
            }
            if (swp.getTopic().equals("StudentAttendanceCheck")) {
                String str = new String(swp.getMessage(), StandardCharsets.UTF_8);
                System.out.println("Attendance check received. Message: " + str);
                returnAttendanceCheck(str);
            } else {
                String str = new String(swp.getMessage(), StandardCharsets.UTF_8);
                logger.info("Message: " + str);
            }
        } catch (Exception e) {
            logger.error("Error reading new message received");
        }
    }

    /**
     * Sends message to the stationary Processing Node
     */
    private void enterMessageToPN(Scanner keyboard) {
        System.out.print("Enter the message: ");
        String messageText = keyboard.nextLine();

        this.sendMessageToPN(messageText, "AppModel");
    }

    /**
     * Sends message to the Processing Node
     * @param messageText
     * @param topic
     * 
     * OBS: For some reason, the message is only sent when the topic is "AppModel"
     */
    private void sendMessageToPN(String messageText, String topic) {
        ApplicationMessage message = createDefaultApplicationMessage();
        SwapData data = new SwapData();
        data.setMessage(messageText.getBytes(StandardCharsets.UTF_8));
        data.setTopic("AppModel");
        message.setContentObject(data);
        sendMessageToGateway(message);
    }

    private void returnAttendanceCheck(String group) {
        LocalDate currentDate = LocalDate.now(this.zoneId);
        LocalTime currentHour = LocalTime.now(this.zoneId).withSecond(0).withNano(0);

        // String messageText = String.format("LOG %s %s %s %s", currentDate.toString(), currentHour.toString(), this.matricula, group);
        String messageText = String.format("LOG %s %s %s ", currentDate.toString(), currentHour.toString(), group);

        for (Integer id : this.studentIDs) {
            messageText = messageText.concat(String.valueOf(id).concat(","));
        }
        System.out.println("Sending attendance check reply: " + messageText);
        this.sendMessageToPN(messageText, "StudentAttendanceCheck");
    }

    private void registerClass(Scanner keyboard) {
        System.out.print("Enter subject: ");
        String subjectText = keyboard.nextLine();

        System.out.print("Enter class: ");
        String classText = keyboard.nextLine();

        System.out.println("Enter class date (yyyy-mm-dd): ");
        String dateText = keyboard.nextLine();

        System.out.print("Enter threshold: ");
        String thresholdText = keyboard.nextLine();

        String command = "Register ";
        command = command.concat(subjectText).concat(" ").concat(classText).concat(" ").concat(dateText).concat(" ").concat(thresholdText);

        this.sendMessageToPN(command, "AppModel");
    }

    @Override
    public void internalException(NodeConnection arg0, Exception arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void unsentMessages(NodeConnection arg0, List<Message> arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void disconnected(NodeConnection arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * Send groupcast message
     * 
     * @param keyboard
     */
    private void sendGroupcastMessage(Scanner keyboard) {
        String group;
        System.out.print("Groupcast message. Enter the group number: ");
        group = keyboard.nextLine();
        System.out.print("Enter the message: ");
        String messageText = keyboard.nextLine();
        System.out.println(String.format("Message from |%s| to group %s.", messageText, group));
        // Create and send the message
        SwapData groupData = new SwapData();
        groupData.setMessage(messageText.getBytes(StandardCharsets.UTF_8));
        groupData.setTopic("GroupMessageTopic");
        groupData.setRecipient(group);
        ApplicationMessage message = createDefaultApplicationMessage();
        message.setContentObject(groupData);
        sendMessageToGateway(message);
    }

    /**
     * Updates user groups by sending updated location to group definer
     * @param messageCount
     * @return context with updated location
     */
    @Override
    public SwapData newLocation(Integer messageCount) {
        ObjectMapper objMapper = new ObjectMapper();
        ObjectNode contextObj = objMapper.createObjectNode();

        LocalDate currentDate = LocalDate.now(this.zoneId);
        LocalTime currentHour = LocalTime.now(this.zoneId).withSecond(0).withNano(0);

        System.out.println("matriculas = " + this.studentIDs.toString());
        contextObj.put("matricula", this.studentIDs.toString().replace("[", "").replace("]", ""));
        // contextObj.set("matricula", objMapper.valueToTree(this.studentIDs));
        contextObj.put("local", this.local);
        contextObj.put("date", currentDate.toString());
        contextObj.put("hour", currentHour.toString()); 

        try {
            SwapData ctxData = new SwapData();
            ctxData.setContext(contextObj);
            ctxData.setDuration(60);
            return ctxData;
        } catch (Exception e) {
            logger.error("Failed to send context");
            return null;
        }
    }
}
