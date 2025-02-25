package SmartClassroom.models;

/**
 * Represents a class (Turma) with details such as its ID, course ID, professor,
 * duration, group information, and associated room schedules.
 */
public class Turma {
    /**
     * The unique identifier for the class.
     */
    public String id_turma;

    /**
     * The discipline or subject of the class.
     */
    public String disciplina;

    /**
     * The professor teaching the class.
     */
    public String professor;

    /**
     * The duration of the class in minutes.
     */
    public int duracao;

    /**
     * The group number associated with the class.
     */
    public int group;

    /**
     * The number of students attending the class.
     */
    public int group_attending;

    /**
     * The number of students absent from the class.
     */
    public int group_absent;
    
    /**
     * An array of room schedules associated with the class.
     */
    public SalaHorario[] salas_horarios;

    /**
     * Default constructor for creating an instance of Turma.
     */
    public Turma() {
    }
}