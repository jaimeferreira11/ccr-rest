package py.com.jaimeferreira.ccr.commons.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Component;

/**
 * @author Luis Fernando Capdevila Avalos
 *
 */
@Component
public class FechaUtil {
    private final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final String DATE_FORMAT_2 = "dd/MM/yyyy";
    private final String DATE_FORMAT_3 = "yyyy-MM-dd";
    private final String TIME_FORMAT = "HH:mm:ss";



    public Date formatoFechaHora(Date fechaHora) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        String dateString = format.format(fechaHora);
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return date;
    }
    
    public Date stringToDateParser(String dateString) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT);
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public Date stringToDateParser2(String dateString) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT_2);
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return date;
    }
    
    public Date stringToDateParser3(String dateString) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT_3);
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return date;
    }

    public String dateToString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        return format.format(date.getTime());
    }
    
    
    public String dateToStringForma3(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_3);
        return format.format(date.getTime());
    }
    
    public Date getDateWithoutTimeUsingCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
     
        return calendar.getTime();
    }
    
    public String formatearFechaSinGuionConGuion(String fechaSinGuion) {
        //dd-MM-yyyy
        StringBuilder resultado = new StringBuilder();
        // Día
        resultado.append(fechaSinGuion.substring(0, 2));
        resultado.append("/");
        // Mes
        resultado.append(fechaSinGuion.substring(2, 4));
        resultado.append("/");
        // Año
        resultado.append(fechaSinGuion.substring(4));
        String fechaFormateada = resultado.toString();
        //dd/MM/yyyy
        return fechaFormateada;
    }
    
    
    public String cambiarInglesEspanolMesdeFechaddMMyyyy(String fechaMesEnIngles) {
        String fechaMesEnEspanol = "";
        if(fechaMesEnIngles!=null) {
            if(fechaMesEnIngles.contains("Jan")) {
                fechaMesEnEspanol = fechaMesEnIngles.replace("Jan", "Ene");
                return fechaMesEnEspanol;
            }
            if(fechaMesEnIngles.contains("Apr")) {
                fechaMesEnEspanol = fechaMesEnIngles.replace("Apr", "Abr");
                return fechaMesEnEspanol;
            }
            if(fechaMesEnIngles.contains("Aug")) {
                fechaMesEnEspanol = fechaMesEnIngles.replace("Aug", "Ago");
                return fechaMesEnEspanol;
            }
            if(fechaMesEnIngles.contains("Dec")) {
                fechaMesEnEspanol = fechaMesEnIngles.replace("Dec", "Dic");
                return fechaMesEnEspanol;
            }
        }
        return fechaMesEnIngles;
    }
    
    public String localDateTimeToString(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        String fechaFormateada = localDateTime.format(formatter);
        return fechaFormateada;
    }
    
    public LocalDateTime stringToLocalDateTime(String stringLocalDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        LocalDateTime fechaLocalDateTime = LocalDateTime.parse(stringLocalDateTime, formatter);
        return fechaLocalDateTime;
    }
    
    public String horaStringFromDate(Date fecha) {
        SimpleDateFormat timeFormatter = new SimpleDateFormat(TIME_FORMAT);
        return timeFormatter.format(fecha.getTime());
    }

    

}
