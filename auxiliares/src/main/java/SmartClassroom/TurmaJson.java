package SmartClassroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import SmartClassroom.models.SalaHorario;
import SmartClassroom.models.Turma;

/**
 * Class to read the JSON file with the class information
 */
public class TurmaJson {
    /** Variable that stores all classes read from the JSON file */
    private Turma[] turma_list = null;

    /**
     * Load the classes from the JSON file
     * @param filePath path to the JSON file
     * @return array with all classes read from the JSON file
     */
    private Turma[] loadTurmasFromFile(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Turma[] turmas = null;

        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            String text = new String(inputStream.readAllBytes(), "UTF-8");
            
            // Read the JSON array directly into a Turma array
            turmas = objectMapper.readValue(text, Turma[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return turmas;
    }

    /**
     * Constructor;
     * Loads the classes from the JSON file, the path to the JSON file is hardcoded 
     * and has to match the path in the container
     */
    public TurmaJson() {
        // Path to the JSON file
        String jsonFilePath = "/turmas.json";

        // Load the classes from the JSON file
        this.turma_list = loadTurmasFromFile(jsonFilePath);

        for (Turma turma : this.turma_list) {
            for (SalaHorario salaHorario : turma.salas_horarios) {
                salaHorario.set_dados_horario(turma.duracao);
            }
        }
    }

    /** 
     * Get the class from the class name
     * @param disciplina_turma class name e.g. "inf1304 3WA"
     * @return the class with the given name, or null if the class is not found
     */
    public Turma getTurma(String disciplina_turma) {
        String[] parts = disciplina_turma.split(" ");
        String id_disciplina = parts[0];
        String id_turma = parts[1];

        for (Turma turma : this.turma_list) {
            if (turma.id_turma.equals(id_turma) && turma.disciplina.equals(id_disciplina)) {
                return turma;
            }
        }

        return null;
    }

    /**
     * Get the class by group ID
     * @param groupID group ID
     * @return the class with the given group ID, or null if the class is not found
     */
    public Turma getTurma(int groupID) {
        for (Turma turma : this.turma_list) {
            if (turma.group == groupID) {
                return turma;
            }
        }
        return null;
    }

    /**
     * Get all classes
     * @return array of all classes
     */
    public Turma[] getTurmas() {
        return this.turma_list;
    }

    /**
     * Get the group ID from the class name
     * @param disciplina_turma class name
     * @return the group ID, or -1 if the class is not found
     */
    public int getGroupIDFromTurma(String disciplina_turma) {
        String[] parts = disciplina_turma.split(" ");
        String id_disciplina = parts[0];
        String id_turma = parts[1];

        for (Turma turma : this.turma_list) {
            if (turma.id_turma.equals(id_turma) && turma.disciplina.equals(id_disciplina)) {
                return turma.group;
            }
        }

        return -1;
    }

    public Turma getCurrentTurmaAtClassroom(LocalDate date, LocalTime currentTime, String location) {
        for (Turma turma : this.turma_list) {
            // System.out.println("##################################");
            // System.out.println("Turma: " + turma.disciplina + " " + turma.id_turma);
            for (SalaHorario salaHorario : turma.salas_horarios) {
                // System.out.println("Sala: " + salaHorario.sala);
                // System.out.println("Dia da semana salaHorario: " + salaHorario.getDayOfWeek());
                // System.out.println("Hora de começo: " + salaHorario.getHoraComeco());
                // System.out.println("Hora de fim: " + salaHorario.getHoraFim());
                // System.out.println("Is class time: " + salaHorario.isClassTime(currentTime));
                // System.out.println("(currentTime.equals(this.hora_comeco): " + (currentTime.equals(salaHorario.getHoraComeco())));
                // System.out.println("(currentTime.isAfter(this.hora_comeco): " + (currentTime.isAfter(salaHorario.getHoraComeco())));
                // System.out.println("&&");
                // System.out.println("(currentTime.isBefore(this.hora_fim): " + currentTime.isBefore(salaHorario.getHoraFim()));
                // System.out.println("(currentTime.equals(this.hora_fim): " + currentTime.equals(salaHorario.getHoraFim()));
                
                if (salaHorario.getDayOfWeek() == date.getDayOfWeek().getValue()) {
                    if (salaHorario.isClassTime(currentTime)) {
                        if (salaHorario.sala.equals(location)) {
                            return turma;
                        }
                    }
                }
            }
        }

        return null;
    }

    public ArrayList<String> getCurrentTurmasLocations(LocalDate date, LocalTime currentTime){
        ArrayList<String> currentTurmasLocations = new ArrayList<String>();

        for (Turma turma : this.turma_list) {
            for (SalaHorario salaHorario : turma.salas_horarios) {
                if (salaHorario.getDayOfWeek() == date.getDayOfWeek().getValue()) {
                    if (salaHorario.isClassTime(currentTime)) {
                        currentTurmasLocations.add(salaHorario.sala);
                    }
                }
            }
        }
        return currentTurmasLocations;
    }

    public ArrayList<Integer> getAllCurrentClassGroups(LocalDate date, LocalTime currentTime)
    {
        ArrayList<Integer> groups = new ArrayList<Integer>();

        for (Turma turma : this.turma_list) {
            for (SalaHorario salaHorario : turma.salas_horarios) {
                if (salaHorario.getDayOfWeek() == date.getDayOfWeek().getValue()) {
                    if (salaHorario.isClassTime(currentTime)) {
                        groups.add(turma.group);
                    }
                }
            }
        }
        
        return groups;
    }

    public ArrayList<Turma> getClassesJustEnded(LocalDate date, LocalTime currentTime) {
        ArrayList<Turma> classesJustEnded = new ArrayList<Turma>();

        for (Turma turma : this.turma_list) {
            for (SalaHorario salaHorario : turma.salas_horarios) {
                if (salaHorario.getDayOfWeek() == date.getDayOfWeek().getValue()) {
                    if (salaHorario.getHoraFim().isBefore(currentTime)) {
                        classesJustEnded.add(turma);
                    }
                }
            }
        }

        return classesJustEnded;
    }
}
