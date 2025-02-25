package SmartClassroom.models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Class to store the room and the time of a class
 */
public class SalaHorario {    
    public String sala;
    public String horario;
    private int diaDaSemana;
    private LocalTime hora_comeco;
    private LocalTime hora_fim;

    public SalaHorario(){}

    /**
     * Sets the start and end time of the class based on the duration.
     * @param duracao Duration of the class in hours.
     */
    public void set_dados_horario(int duracao) {
        this.diaDaSemana = this.getDiaDaSemana();
        this.hora_comeco = LocalTime.parse(this.getTimeString(), DateTimeFormatter.ofPattern("HH:mm"));
        this.hora_fim = hora_comeco.plusHours(duracao);
    }

    /**
     * Gets the day of the week.
     * @return Day of the week as an integer.
     */
    public int getDayOfWeek() {
        return diaDaSemana;
    }

    /**
     * Gets the start time of the class.
     * @return Start time as a LocalTime object.
     */
    public LocalTime getHoraComeco() {
        return hora_comeco;
    }

    /**
     * Gets the end time of the class.
     * @return End time as a LocalTime object.
     */
    public LocalTime getHoraFim() {
        return hora_fim;
    }

    /**
     * Checks if the current time is within the class time.
     * @param currentTime The current time to check.
     * @return True if the current time is within the class time, false otherwise.
     */
    public boolean isClassTime(LocalTime currentTime) {
        return (currentTime.equals(this.hora_comeco) || currentTime.isAfter(this.hora_comeco)) && (currentTime.isBefore(this.hora_fim) || currentTime.equals(this.hora_fim));
    }
    
    /**
     * Extracts and formats the time string from the horario attribute.
     * @return Formatted time string in "HH:mm" format.
     */
    public String getTimeString() {
        String parts[] = this.horario.split(" ");
        String time = parts[1].strip();

        // Ensure the time is in "HH:mm" format
        String[] timeParts = time.split(":");

        if (timeParts.length == 2) {
            String hour = timeParts[0].strip();
            String minute = timeParts[1].strip();

            // Add leading zero to hour if necessary
            if (hour.length() == 1) {
                hour = "0" + hour;
            }

            if (minute.length() == 1) {
                minute = "0" + minute;
            }

            return hour + ":" + minute;
        } else {
            // Handle invalid time format
            System.out.println("Invalid time format: " + time);
            return null;
        }
    }

    /**
     * Extracts and converts the day of the week from the horario attribute.
     * @return Day of the week as an integer.
     */
    private int getDiaDaSemana() {
        String parts[] = this.horario.split(" ");
        String day = parts[0].strip();

        int dayOfWeek;
        switch (day.toLowerCase()) {
            case "segunda":
                dayOfWeek = 1;
                break;
            case "terca":
                dayOfWeek = 2;
                break;
            case "quarta":
                dayOfWeek = 3;
                break;
            case "quinta":
                dayOfWeek = 4;
                break;
            case "sexta":
                dayOfWeek = 5;
                break;
            case "sabado":
                dayOfWeek = 6;
                break;
            case "domingo":
                dayOfWeek = 7;
                break;
            default:
                throw new IllegalArgumentException("Invalid day: " + day);
        }

        return dayOfWeek;
    }
}