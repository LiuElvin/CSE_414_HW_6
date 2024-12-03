package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    // TODO
    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            greetingsText();
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            greetingsText();
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);

        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        }
        catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
        
        greetingsText();
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    // TODO
    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            greetingsText();
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            greetingsText();
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;

        try {
            patient = new Patient.PatientGetter(username, password).get();
        }
        catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }

        if (patient == null) {
            System.out.println("Login patient failed");
        }
        else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }

        greetingsText();
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    // TODO
    private static void searchCaregiverSchedule(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Please try again");
            greetingsText();
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            greetingsText();
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String sCaregivers = "SELECT Username FROM Availabilities WHERE Time = ?";
            String sVaccines = "SELECT * FROM Vaccines";

            String date = tokens[1];

            Date d1 = Date.valueOf(date);

            PreparedStatement statement1 = con.prepareStatement(sCaregivers);

            statement1.setDate(1, d1);

            ResultSet rs1 = statement1.executeQuery();

            if (rs1.isBeforeFirst() == false) {
                System.out.println("No caregiver is available");
            }
            else {
                while (rs1.next()) {
                    System.out.println(rs1.getString("Username"));
                }
            }

            PreparedStatement statement2 = con.prepareStatement(sVaccines);

            ResultSet rs2 = statement2.executeQuery();

            if (rs2.isBeforeFirst() == false) {
                System.out.println("No vaccines available");
            }
            else {
                while (rs2.next()) {
                    System.out.println(rs2.getString("Name") + " " + 
                                       rs2.getInt("Doses"));
                }
            }
        }
        catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        }
        catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
        finally {
            cm.closeConnection();
        }

        greetingsText();
    }

    // TODO
    private static void reserve(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please try again");
            greetingsText();
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            greetingsText();
            return;
        }

        if (currentCaregiver != null && currentPatient == null) {
            System.out.println("Please login as a patient");
            greetingsText();
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String date = tokens[1];
            String vaccine = tokens[2];

            Date d1 = Date.valueOf(date);
            
            String iAppointments = "INSERT INTO Appointments VALUES(?, ?, ?, ?)";
            String sVaccines = "SELECT * FROM Vaccines WHERE Doses > 0 AND Name = ?";
            String sCaregivers = "SELECT * FROM Availabilities WHERE Time = ? ORDER BY Username";
            String sAppointments = "SELECT * FROM Appointments WHERE Time = ? AND c_username = ?";
            
            PreparedStatement statement1 = con.prepareStatement(sCaregivers);

            statement1.setDate(1, d1);

            ResultSet rs1 = statement1.executeQuery();

            String name = "";

            if (!rs1.isBeforeFirst()) {
                System.out.println("No caregiver is available");
                cm.closeConnection();
                greetingsText();
                return;
            } 
            else {
                rs1.next();
                name = rs1.getString("Username");

                PreparedStatement statement2 = con.prepareStatement(sAppointments);

                statement2.setDate(1, d1);
                statement2.setString(2, name);

                ResultSet rs2 = statement2.executeQuery();

                if (rs2.isBeforeFirst()) {
                    System.out.println("No caregiver is available");
                    cm.closeConnection();
                    greetingsText();
                    return;
                }
            }

            PreparedStatement statement3 = con.prepareStatement(sVaccines);

            statement3.setString(1, vaccine);

            ResultSet rs3 = statement3.executeQuery();

            if (rs3.isBeforeFirst() == false) {
                System.out.println("Not enough available doses");
                greetingsText();
                cm.closeConnection();
                return;
            }
            else {
                Vaccine vaccination = new Vaccine.VaccineGetter(vaccine).get();
                vaccination.decreaseAvailableDoses(1);
            }

            PreparedStatement statement4 = con.prepareStatement(iAppointments);

            statement4.setDate(1, d1);
            statement4.setString(2, name);
            statement4.setString(3, currentPatient.getUsername());
            statement4.setString(4, vaccine);
            statement4.executeUpdate();

            String seAppointments = "SELECT * FROM Appointments ORDER BY Appointment_ID DESC";

            PreparedStatement statement5 = con.prepareStatement(seAppointments);

            ResultSet rs5 = statement5.executeQuery();

            rs5.next();

            System.out.println("Appointment ID " + rs5.getInt("Appointment_ID") +
                               ", Caregiver username " + rs5.getString("c_username"));

            String dAvailabilities = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";

            PreparedStatement statement6 = con.prepareStatement(dAvailabilities);

            statement6.setString(1, rs5.getString("c_username"));
            statement6.setDate(2, d1);
            statement6.executeUpdate();
        }
        catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        }
        catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
        finally {
            cm.closeConnection();
        }

        greetingsText();
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // No thanks!
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    // TODO
    private static void showAppointments(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again");
            greetingsText();
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            greetingsText();
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String username = "";
        String pcusername = "p_username";
        String sAppointments = "SELECT Appointment_ID, v_name, Time, p_username FROM Appointments WHERE c_username = ?";

        if (currentPatient != null) {
            username = currentPatient.getUsername();
            pcusername = "c_username";
            sAppointments = "SELECT Appointment_ID, v_name, Time, c_username FROM Appointments WHERE p_username = ?";
        }
        else {
            username = currentCaregiver.getUsername();
        }

        try {
            PreparedStatement statement1 = con.prepareStatement(sAppointments);

            statement1.setString(1, username);

            ResultSet rs1 = statement1.executeQuery();

            if (rs1.isBeforeFirst() == false) {
                System.out.println("No appointments");
            }
            else {
                while (rs1.next()) {
                    System.out.println(rs1.getInt("Appointment_ID") +  " " + 
                                       rs1.getString("v_name") + " " + 
                                       rs1.getDate("Time") +  " " + 
                                       rs1.getString(pcusername));
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
        finally {
            cm.closeConnection();
        }

        greetingsText();
    }

    // TODO
    private static void logout(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again");
            greetingsText();
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            greetingsText();
            return;
        }

        currentCaregiver = null;
        currentPatient = null;

        System.out.println("Successfully logged out");

        greetingsText();
    }
}
