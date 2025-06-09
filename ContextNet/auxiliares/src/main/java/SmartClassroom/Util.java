package SmartClassroom;


import java.util.Date;

public class Util {
    public static boolean isWithinRange(Date testDate, Date startDate, Date endDate) {
        return !(testDate.before(startDate) || testDate.after(endDate));
    }
}