import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import oracle.jdbc.driver.*;

public class BMS {
    private static OracleConnection conn;
    private static final String username = "\"23087921d\"";
    private static final String password = "nugrfvun";
    private static final String url = "jdbc:oracle:thin:@studora.comp.polyu.edu.hk:1521:dbms";
    public static Statement statement;
    public static Scanner scanner;
    public static boolean mainfag;
    public static String constantID;

    public static void main(String[] args) throws SQLException {
        connectToServer(); // Connect to the database
        scanner = new Scanner(System.in);
        int fchoice;
        mainfag = false;


        do {
            System.out.println("Welcome to the Banquet System!");
            System.out.println("Enter '1' to register or '2' to log in or '3' to exit the system: ");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            fchoice = scanner.nextInt();
            scanner.nextLine();

            switch (fchoice) {
                case 1:
                    registerUser();
                    mainfag = true;
                    break;
                case 2:
                    loginUser();
                    break;
                case 3:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (fchoice != 3 && !mainfag);

        if (getUserType(constantID).equals("Attendee")) {
            AttendeePage();
        } else {
            AdministratorDashboard();
        }
        //closeApp(); // Clean up resources
    }

    public static String getUserType(String email) throws SQLException {
        String userType="";
        try (PreparedStatement pstmt = conn.prepareStatement(SQLQueries.GET_USER_TYPE)) {
            pstmt.setString(1, email);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) {
                    userType = resultSet.getString("UserType");
                }
            }
        }
        return userType;
    }


    // Connect to the database server
    private static void connectToServer() throws SQLException {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        conn = (OracleConnection) DriverManager.getConnection(url, username, password);
        statement = getStmt(conn);
        System.out.println("Connected to the database.");
    }

    // Close the database connection
    private static void closeApp() throws SQLException {
        if (conn != null) {
            conn.close();
            System.out.println("Database connection closed.");
        }
        if (scanner != null) {
            scanner.close();
        }
    }

    // SQL Queries Class
    public static class SQLQueries {
        public static final String CREATE_USER_TABLE =
                "CREATE TABLE AU (FirstName VARCHAR2(50), LastName VARCHAR2(50), Address VARCHAR2(100), AttendeeType VARCHAR2(50), Email VARCHAR2(100) UNIQUE AS IDENTITY PRIMARY KEY, Password VARCHAR2(100), UserType VARCHAR2(50), MobileNumber VARCHAR2(15), AffiliatedOrganization VARCHAR2(50))";
        public static final String CHECK_USERTABLE_EXIST = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'AU'";
        public static final String INSERT_USER =
                "INSERT INTO AU (FirstName, LastName, Address, AttendeeType, Email, Password, UserType, MobileNumber, AffiliatedOrganization) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        public static final String VALIDATE_USER = "SELECT COUNT(*) FROM AU WHERE Email = ? AND Password = ?";
        public static final String GET_USER_TYPE = "SELECT UserType FROM AU WHERE Email = ?";
        public static final String INSERT_BANQUET_TABLE = "INSERT INTO Banquet (BinNum, banquetName, date1, time1, address, location, firstName, lastName, availability, quota, mealID1, mealID2, mealID3, mealID4) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    // Check if the user table exists
    public static boolean checkUserTableExists() throws SQLException {
        try (Statement stmt = getStmt(conn);
             ResultSet rs = stmt.executeQuery(SQLQueries.CHECK_USERTABLE_EXIST)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Create user table if it doesn't exist
    public static void createUserTable() throws SQLException {
        statement.executeUpdate(SQLQueries.CREATE_USER_TABLE);
        System.out.println("User table created successfully.");
    }

    // Register user method
    public static void registerUser() throws SQLException {
        String firstName = promptUserInput("Enter your first name: ", "^[A-Za-z]+$", "Invalid first name. Please use letters only.");
        String lastName = promptUserInput("Enter your last name: ", "^[A-Za-z]+$", "Invalid last name. Please use letters only.");
        String address = promptUserInput("Enter your address: ");
        String attendeeType = promptUserInput("Enter attendee type (staff, student, alumni, guest, not available): ", "^(staff|student|alumni|guest|not available)$", "Invalid attendee type.");
        String email = promptUserInput("Enter your email: ", ".+@.+\\..+", "Invalid email format.");
        String password = promptUserInput("Enter your password: ");
        String userType = promptUserInput("Enter user type (Attendee, Administrator): ", "^(Attendee|Administrator)$", "Invalid user type.");
        String mobileNumber = promptUserInput("Enter your mobile number (8 digits): ", "^[0-9]{8}$", "Invalid mobile number. It must be exactly 8 digits.");
        String affiliatedOrganization = promptUserInput("Enter your affiliated organization (PolyU, SPEED, HKCC, Others, not available): ", "^(PolyU|SPEED|HKCC|Others|not available)$", "Invalid affiliated organization.");

        PreparedStatement pstmt = conn.prepareStatement(SQLQueries.INSERT_USER);
        pstmt.setString(1, firstName);
        pstmt.setString(2, lastName);
        pstmt.setString(3, address);
        pstmt.setString(4, attendeeType);
        pstmt.setString(5, email);
        pstmt.setString(6, password);
        pstmt.setString(7, userType);
        pstmt.setString(8, mobileNumber);
        pstmt.setString(9, affiliatedOrganization);
        pstmt.executeUpdate();
        System.out.println("User registered successfully! Welcome to the system.");
    }


    // Prompt user input with validation
    private static String promptUserInput(String message, String regex, String errorMessage) {
        String input;
        while (true) {
            System.out.print(message);
            input = scanner.nextLine();
            if (input.matches(regex)) {
                return input;
            } else {
                System.out.println(errorMessage);
            }
        }
    }

    // Overloaded method for inputs without regex
    private static String promptUserInput(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    // Login user method
    public static void loginUser() throws SQLException {
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        if (validateUser(email, password)) {
            mainfag = true;
            System.out.println("Login successful! Welcome to the system.");
            // Navigate based on user type, if applicable
        } else {
            mainfag = false;
            System.out.println("Invalid email or password. Please try again.");
        }
    }

    // Validate user credentials
    public static boolean validateUser(String email, String password) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQLQueries.VALIDATE_USER)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password); // Ensure to hash passwords in production
            try (ResultSet resultSet = pstmt.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0; // Returns true if user exists
            }
        }
    }

    // Get statement from connection
    public static Statement getStmt(OracleConnection conn) throws SQLException {
        return conn.createStatement();
    }

    // Set the current Statement for use
    public static void setStmt(Statement stmt) {
        statement = stmt;
    }

    /***************************************************************************************/
    public static void AttendeePage() throws SQLException {

        String userEmail = "";
        ResultSet rset = statement.executeQuery("SELECT FirstName, LastName, Email, Address, AttendeeType FROM AU WHERE Email = '" + constantID + "'");
        if (rset.next()) {
            userEmail = rset.getString("Email");
        }

        while (true) {
            System.out.println();
            System.out.println("---------- Welcome to Banquet Management System ----------");
            System.out.println();
            System.out.println("[1] View Banquet Information");
            System.out.println("[2] Register Banquet");
            System.out.println("[3] List Registered Banquet");
            System.out.println("[4] Update Registered Banquet Information");    //new
            System.out.println("[5] Search Banquet");
            System.out.println("[6] View Profile");
            System.out.println("[7] Update Profile");
            System.out.println("[8] Log Out");
            System.out.println();
            System.out.print("Please enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    displayBanquets();     // just display available banquet information
                    break;
                case 2:
                    System.out.println("Please register according to the availability. (1=open for registration || 0=full) ");
                    registerBanquets(userEmail);    // user register banquet
                    break;
                case 3:
                    System.out.println("All the registered banquets.");
                    System.out.println();
                    listAttendeeBanquets(userEmail);    // display user registered banquets
                    break;
                case 4:
                    updateRegistration(userEmail);      // update registered banquet information
                    break;
                case 5:
                    searchAttendeeBanquets(userEmail);  // search registered banquets
                    break;
                case 6:
                    System.out.println("Individual Profile");
                    displayProfile(userEmail);  // display user profile
                    break;
                case 7:
                    updateUserProfile(userEmail);
                    break;
                case 8:
                    System.out.println("Thank you for your visiting.");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please enter a valid choice.");
            }
        }
    }


    public static boolean registerBanquets(String email) throws SQLException {
        //List<Banquet> banquets = availableBanquets(connection);
        displayBanquets();          //1. display all banquets details
        int banquetId = selectBanquet();     //2. let user to select a banquet
        return handleAttendeeRegistration(banquetId, email); //3. add attendee registration information to registration table
    }

    //1 updated   changed!!!!!  can run!!!!!!  function for command 1 and 2!!!!!
    private static void displayBanquets() throws SQLException {
        //ResultSet resultSet = statement.executeQuery("SELECT BinNum, banquetName, date1, time1, address, location, firstName, lastName, availability, quota " +
        //"FROM Banquet WHERE quota > 0 ");
        String as = "SELECT BinNum, banquetName, date1, time1, address, location, firstName, lastName, availability, quota " +
                "FROM Banquet WHERE quota > 0";
        PreparedStatement pas = conn.prepareStatement(as);
        ResultSet resultSet = pas.executeQuery();

        System.out.println("Available Banquet Information:");
        System.out.printf("%-10s %-30s %-10s %-10s %-50s %-30s %-20s %-20s %-15s %-10s%n",
                "BinNum", "Banquet Name", "Date", "Time", "Address", "Location",
                "First Name", "Last Name", "Availability", "Quota");
        System.out.println("------------------------------------------------------------------------------------------------");

        while (resultSet.next()) {
            int BIN = resultSet.getInt("BinNum");
            String banquetName = resultSet.getString("banquetName");
            String date1 = resultSet.getString("date1");
            String time1 = resultSet.getString("time1");
            String address = resultSet.getString("address");
            String location = resultSet.getString("location");
            String firstName = resultSet.getString("firstName");
            String lastName = resultSet.getString("lastName");
            int availability = resultSet.getInt("availability");
            int quota = resultSet.getInt("quota");

            System.out.printf("%-10d %-30s %-10s %-10s %-50s %-30s %-20s %-20s %-15s %-10d%n",
                    BIN, banquetName, date1, time1, address, location,
                    firstName, lastName, availability, quota);
        }

    }

    //2 updated   have checked!!!!!   can run!!!!!
    private static int selectBanquet() throws SQLException {
        System.out.print("Select a banquet by BinNum to register: ");
        int banquetId = -1; // Initialize with an invalid ID

        while (true) {
            while (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a valid banquet ID.");
                scanner.next(); // Clear invalid input
            }
            banquetId = scanner.nextInt(); // Enter BIN
            scanner.nextLine(); // Clear the buffer

            // Validate the banquet ID
            if (isValidBanquetID(banquetId)) {
                break; // Exit the loop if the ID is valid
            } else {
                System.out.println("Invalid banquet ID. Please enter a valid banquet ID.");
            }
        }
        return banquetId; // Return selected banquet's BIN
    }

    // have checked  can run!!!!!!
    private static boolean isValidBanquetID(int banquetId) throws SQLException {
        String sql33 = "SELECT COUNT(*) AS count FROM Banquet WHERE BinNum = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql33);
        pstmt.setInt(1, banquetId); // Set the banquet ID to validate
        ResultSet resultSet = pstmt.executeQuery();
        if (resultSet.next()) {
            int count = resultSet.getInt("count");
            return count > 0; // Return true if the banquet ID exists in the Registration table
        }
        return false; // Return false if there was an error or no match was found
    }
    // checked  can run!!!!!!!

    public static boolean handleAttendeeRegistration(int BIN, String email) throws SQLException {
        String sql = "SELECT mealID1, mealID2, mealID3, mealID4 FROM Banquet WHERE BinNum = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, BIN);
        ResultSet rset = pstmt.executeQuery();
        if (rset.next()) {
            String mealID1 = rset.getString("mealID1");
            String mealID2 = rset.getString("mealID2");
            String mealID3 = rset.getString("mealID3");
            String mealID4 = rset.getString("mealID4");

            String query = "SELECT mealID, type, dishName, price, specialCuisine FROM Meal WHERE mealID IN (?, ?, ?, ?)";
            PreparedStatement pstmt1 = conn.prepareStatement(query);
            pstmt1.setString(1, mealID1);
            pstmt1.setString(2, mealID2);
            pstmt1.setString(3, mealID3);
            pstmt1.setString(4, mealID4);
            ResultSet rset2 = pstmt1.executeQuery();
            System.out.println("Meals available for banquet ID: " + BIN);
            boolean hasMeals = false;
            while (rset2.next()) {
                hasMeals = true;
                System.out.println("Meal ID: " + rset2.getString("mealID"));
                System.out.println("Type: " + rset2.getString("type"));
                System.out.println("Dish Name: " + rset2.getString("dishName"));
                System.out.println("Price: $" + rset2.getInt("price"));
                System.out.println("Special Cuisine: " + rset2.getString("specialCuisine"));
                System.out.println("-------------------------");
            }
            if (!hasMeals) {
                System.out.println("No meals found for banquet ID: " + BIN);
            }
            // get user input for meal selection
            System.out.print("Choose your meal (Please enter respective mealID): ");
            String selectedMealID = scanner.nextLine().trim();
            if (!isValidMealIDForBanquet(BIN, selectedMealID)) {
                System.out.println("Invalid mealID for the specified banquet. Please enter a valid type.");
                return false; // Exit if the meal type is invalid
            }

            System.out.print("Enter your drink choice: ");
            String drinkChoice = scanner.nextLine().trim();

            System.out.print("Enter your seating preference: ");
            String seatPreference = scanner.nextLine().trim();

            //Update registration table
            String a = "INSERT INTO Registration (Email, BinNum, mealId, drink, seatPreference) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement p1 = conn.prepareStatement(a);
            p1.setString(1, email);  // Attendee's email
            p1.setInt(2, BIN);        // Banquet BIN
            p1.setString(3, selectedMealID); // Selected Meal ID
            p1.setString(4, drinkChoice);    // Drink preference
            p1.setString(5, seatPreference); // Seat preference

            // Execute the insertion newwwwwwwww
            //int rowsAffected = statement.executeUpdate(a);      //????????????????????????????????
            int rowsAffected = p1.executeUpdate();

            // Check if the insertion was successful
            if (rowsAffected > 0) {
                if (decreaseBanquetQuota(BIN)) {    //Decrease the quota of the specified banquet
                    System.out.println("Registration successful!");
                } else {
                    System.out.println("Failed to save registration to database.");
                }
                return true;
            } else {
                System.out.println("Registration failed. Please check the details and try again.");
            }

        } else {
            System.out.println("No meals found for banquet ID " + BIN);
        }
        return false;   // need to think!!!!!!!!!!!!
    }

    //new method  checked!!!!!  can run!!!!!
    public static boolean isValidMealIDForBanquet(int BIN, String mealID) throws SQLException {
        String sql = "SELECT COUNT(*) AS count " +
                "FROM Banquet " +
                "JOIN Meal ON Meal.mealID IN (Banquet.mealID1, Banquet.mealID2, Banquet.mealID3, Banquet.mealID4) " +
                "WHERE Banquet.BinNum = ? AND Meal.mealID = ?";
        PreparedStatement p = conn.prepareStatement(sql);
        p.setInt(1, BIN); // Set the banquet BIN
        p.setString(2, mealID); // Set the meal type to validate
        ResultSet resultSet = p.executeQuery();

        if (resultSet.next()) {
            int count = resultSet.getInt("count");
            return count > 0; // Return true if there's a valid meal type for the banquet
        }
        return false; // Return false if there was an error or no match was found
    }

    //new 2  checked!!!!  can run!!!!!!
    private static boolean decreaseBanquetQuota(int BIN) throws SQLException {
        String sql = "UPDATE Banquet SET quota = quota - 1 WHERE BinNum = ? AND quota > 0";
        PreparedStatement p = conn.prepareStatement(sql);
        p.setInt(1, BIN);
        int rowsUpdated = p.executeUpdate();
        return rowsUpdated > 0;
    }

    public static void listAttendeeBanquets(String email) throws SQLException {
        String query = "SELECT b.BinNum, b.banquetName, r.mealId, r.drink, r.seatPreference, m.type AS mealType " +
                "FROM Banquet b " +
                "JOIN Registration r ON b.BinNum = r.BinNum " +
                "LEFT JOIN Meal m ON r.mealId = m.mealID " +
                "WHERE r.Email = ?";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, email);
        ResultSet rset = stmt.executeQuery();

        if (!rset.next()) {
            System.out.println("No registered banquet is found.");
            return;
        }
        int i = 1;       //i: index for each banquet for each attendee
        System.out.println("List of banquets registered:");
        do {
            int BIN = rset.getInt("BinNum");
            String banquetName = rset.getString("banquetName");
            String mealType = rset.getString("mealType");
            String drink = rset.getString("drink");
            String seatPreference = rset.getString("seatPreference");

            // Display banquet details
            System.out.println("Banquet " + i);
            System.out.println("Banquet Identification Number (BIN): " + BIN);
            System.out.println("Banquet Name: " + banquetName);
            System.out.println("Meal Type: " + mealType);
            System.out.println("Drink Preference: " + drink);
            System.out.println("Seat Preference: " + seatPreference);
            System.out.println();
            i++;
        } while (rset.next());
    }

    public static void updateRegistration(String email) throws SQLException {
        boolean continueUpdating = true;

        while (continueUpdating) {
            listAttendeeBanquets(email);

            System.out.print("Enter the BIN of the banquet you want to update (or -1 to go back): ");
            int selectedBin = scanner.nextInt();
            scanner.nextLine();

            if (selectedBin == -1) {        //exit the loop to update
                continueUpdating = false;
                System.out.println("Exiting update banquet registration process.");
                continue;
            }

            if (!isValidBin(email, selectedBin)) {      //validate BIN
                System.out.println("Invalid BIN. Please choose a valid banquet.");
                continue;
            }

            boolean continueRegisteredBanquetUpdating = true;
            while (continueRegisteredBanquetUpdating) {
                System.out.println("Select the banquet detail you want to update:");
                System.out.println("1. Meal Type");
                System.out.println("2. Drink");
                System.out.println("3. Seat Preference");
                System.out.println("4. Finish updating banquet details");

                int banquetChoice = scanner.nextInt();
                scanner.nextLine();

                switch (banquetChoice) {
                    case 1: //Meal Type
                        updateMealType(email, selectedBin);
                        break;

                    case 2: //Drink
                        System.out.print("Enter new Drink Choice: ");
                        String newDrink = scanner.nextLine().trim();
                        if (!newDrink.isEmpty()) {
                            updateRegistrationDetail(email, selectedBin, "drink", newDrink);
                        } else {
                            System.out.println("Drink choice cannot be empty.");
                        }
                        break;

                    case 3: //Seat Preference
                        System.out.print("Enter new Seat Preference: ");
                        String newSeatPreference = scanner.nextLine().trim();
                        if (!newSeatPreference.isEmpty()) {
                            updateRegistrationDetail(email, selectedBin, "seatPreference", newSeatPreference);
                        } else {
                            System.out.println("Seat preference cannot be empty.");
                        }
                        break;

                    case 4: //Finish updating registered information
                        continueRegisteredBanquetUpdating = false;
                        break;

                    default:
                        System.out.println("Invalid choice. Please select again.");
                        break;
                }
            }

            // Display updated registration details for confirmation
            System.out.println("Updated Registration Details:");
            displayRegistration(email, selectedBin);
        }
    }

    //Validate if the selected BIN is valid (match with the registered one) can run!!!
    public static boolean isValidBin(String email, int bin) throws SQLException {
        String query = "SELECT COUNT(*) FROM Registration WHERE Email = ? AND BinNum = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, email);
        pstmt.setInt(2, bin);
        ResultSet rset = pstmt.executeQuery();
        if (rset.next()) {
            return rset.getInt(1) > 0; //true if have at least one registration for the BIN
        }
        return false;
    }

    //Update meal type can run!!!!
    public static void updateMealType(String email, int selectedBin) throws SQLException {
        List<String> availableMealIDs = new ArrayList<>();

        String mealQuery = "SELECT m.mealID, m.type, m.dishName, m.price, m.specialCuisine FROM Meal m " +
                "WHERE m.mealID IN (SELECT mealId FROM Registration WHERE Email = ? AND BinNum = ?)";

        PreparedStatement mealStatement = conn.prepareStatement(mealQuery);
        mealStatement.setString(1, email);
        mealStatement.setInt(2, selectedBin);
        ResultSet mealSet = mealStatement.executeQuery();

        System.out.println("Available Meals:");
        while (mealSet.next()) {
            String mealID = mealSet.getString("mealID");
            String type = mealSet.getString("type");
            String dishName = mealSet.getString("dishName");
            int price = mealSet.getInt("price");
            String specialCuisine = mealSet.getString("specialCuisine");

            availableMealIDs.add(mealID);
            System.out.printf("Meal ID: %s, Type: %s, Dish Name: %s, Price: $%d, Special Cuisine: %s%n",
                    mealID, type, dishName, price, specialCuisine);
        }

        while (true) {
            System.out.print("Enter new Meal ID you want to have: ");
            String newMealID = scanner.nextLine().trim();

            if (!newMealID.isEmpty() && availableMealIDs.contains(newMealID)) {     //validate inputted mealID
                updateRegistrationDetail(email, selectedBin, "mealId", newMealID);
                System.out.println("Meal ID updated successfully.");
                break; //exit loop on valid input
            } else {
                System.out.println("Invalid Meal ID. Please choose an existing meal ID.");
            }
        }
    }

    //Update registration details can run!!!!
    public static void updateRegistrationDetail(String originalEmail, int bin, String field, String newValue) throws SQLException {
        String query = "UPDATE Registration SET " + field + " = ? WHERE email = ? AND BinNum = ?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1, newValue);
        preparedStatement.setString(2, originalEmail);
        preparedStatement.setInt(3, bin);
        preparedStatement.executeUpdate();
        System.out.println(field + " is updated successfully!");
    }

    //Display the all information of the specified banquet (attendee update info) can run!!!!
    public static void displayRegistration(String email, int bin) throws SQLException {
        String query = "SELECT b.BinNum, b.banquetName, r.mealId, r.drink, r.seatPreference, m.type AS mealType " +
                "FROM Banquet b " +
                "JOIN Registration r ON b.BinNum = r.BinNum " +
                "LEFT JOIN Meal m ON r.mealId = m.mealID " +
                "WHERE r.Email = ? AND b.BinNum = ?";

        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, email);
        pstmt.setInt(2, bin);
        ResultSet rset = pstmt.executeQuery();

        if (!rset.next()) {
            System.out.println("No registration details found for the specified banquet.");
            return;
        }

        // Display registration details for the specified banquet
        System.out.println("Updated Registration Details for Banquet (BIN: " + bin + ": ");
        String banquetName = rset.getString("banquetName");
        String mealType = rset.getString("mealType");
        String drink = rset.getString("drink");
        String seatPreference = rset.getString("seatPreference");

        System.out.println("Banquet Name: " + banquetName);
        System.out.println("Meal Type: " + mealType);
        System.out.println("Drink Preference: " + drink);
        System.out.println("Seat Preference: " + seatPreference);
    }



    public static void displayProfile(String email) throws SQLException {
        String query = "SELECT * FROM AU WHERE Email = ?";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1, email);
        ResultSet rset = preparedStatement.executeQuery();
        if (rset.next()) {
            System.out.println("First Name: " + rset.getString("FirstName"));
            System.out.println("Last Name: " + rset.getString("LastName"));
            System.out.println("Address: " + rset.getString("Address"));
            System.out.println("Attendee Type: " + rset.getString("AttendeeType"));
            System.out.println("Email: " + rset.getString("Email"));
            System.out.println("Mobile Number: " + rset.getString("MobileNumber"));
            System.out.println("Affiliated Organization: " + rset.getString("AffiliatedOrganization"));
        }else {
            System.out.println("No profile found for the retrieving the provided email.");
        }
    }

    public static void updateUserProfile(String originalEmail) throws SQLException {
        boolean continueUpdating = true;

        while (continueUpdating) {
            System.out.println();
            System.out.println("Select the item you want to update:");
            System.out.println("[1] First Name");
            System.out.println("[2] Last Name");
            System.out.println("[3] Address");
            System.out.println("[4] Attendee Type (e.g. staff, student, alumni, guest)");
            System.out.println("[5] E-mail Address");
            System.out.println("[6] Password");
            System.out.println("[7] Mobile Number");
            System.out.println("[8] Affiliated Organization (e.g. PolyU, SPEED, HKCC, Others)");
            System.out.println("[9] Finish updating");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: //First Name
                    System.out.print("Enter new First Name: ");
                    String firstName = scanner.nextLine().trim();
                    if (!isValidEnglishName(firstName)) {
                        System.out.println("Invalid name. Only English characters (1-50 characters) are allowed.");
                        continue;
                    }
                    updateUserDetail(originalEmail, "FirstName", firstName);
                    break;

                case 2: //Last Name
                    System.out.print("Enter new Last Name: ");
                    String lastName = scanner.nextLine().trim();
                    if (!isValidEnglishName(lastName)) {
                        System.out.println("Invalid name. Only English characters (1-50 characters) are allowed.");
                        continue;
                    }
                    updateUserDetail(originalEmail, "LastName", lastName);
                    break;

                case 3: //Address
                    System.out.print("Enter new Address: ");
                    String address = scanner.nextLine().trim();
                    updateUserDetail(originalEmail, "Address", address);
                    break;

                case 4: //Attendee Type
                    System.out.print("Enter new Attendee Type (e.g. staff, student, alumni, guest): ");
                    String attendeeType = scanner.nextLine().trim();
                    updateUserDetail(originalEmail, "AttendeeType", attendeeType);
                    break;

                case 5: //E-mail Address
                    System.out.print("Enter new E-mail Address: ");
                    String newEmail = scanner.nextLine().trim();
                    if (!isValidEmail(newEmail)) {
                        System.out.println("Invalid email format. There should be an '@'. ");
                        continue;
                    }
                    updateUserDetail(originalEmail, "Email", newEmail);
                    break;

                case 6: //Password
                    boolean passwordVerified = false;
                    for (int attempts = 0; attempts < 3; attempts++) {
                        System.out.print("Enter your current password: ");
                        String currentPassword = scanner.nextLine().trim();
                        if (validateUser(originalEmail, currentPassword)) {
                            passwordVerified = true;
                        } else {    // Validate the user's current password
                            System.out.println("Incorrect password. You have " + (2 - attempts) + " attempt(s) left.");
                        }
                    }

                    if (!passwordVerified) {
                        System.out.println("Max attempts reached. Please try later. ");
                        continue;
                    }

                    System.out.print("Enter new password: ");
                    String newPassword = scanner.nextLine().trim();
                    if (newPassword.isEmpty()) {
                        System.out.println("Password should not be null.");
                        continue;
                    }
                    updateUserDetail(originalEmail, "Password", newPassword);
                    break;

                case 7: //Mobile Number
                    System.out.print("Enter new Mobile Number (8-digit): ");
                    String mobileNumber = scanner.nextLine().trim();
                    if (!isValidMobileNumber(mobileNumber)) {
                        System.out.println("Invalid mobile number. It must be an 8-digit number. ");
                        continue;
                    }
                    updateUserDetail(originalEmail, "MobileNumber", mobileNumber);
                    break;

                case 8: //Affiliated Organization
                    System.out.print("Enter new Affiliated Organization (e.g. PolyU, SPEED, HKCC, Others) :");
                    String organization = scanner.nextLine().trim();
                    updateUserDetail(originalEmail, "AffiliatedOrganization", organization);
                    break;

                case 9: //Finish updating
                    continueUpdating = false;
                    break;

                default:
                    System.out.println("Invalid choice. Please select again.");
                    break;
            }

            // Display updated profile for confirmation
            System.out.println("Updated Profile:");
            displayProfile(originalEmail);
        }
    }

    //Update user detail in the database based on the field
    public static void updateUserDetail(String originalEmail, String field, String newValue) throws SQLException {
        String query = "UPDATE AU SET " + field + " = ? WHERE Email = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setString(1, newValue);
            preparedStatement.setString(2, originalEmail);
            int rowsUpdated = preparedStatement.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(field + " is updated successfully!");
            } else {
                System.out.println("No user found with the provided email.");
            }
        } catch (SQLException e) {
            System.out.println("Error updating " + field + ": " + e.getMessage());
            throw e; // Rethrow the exception if you want to handle it elsewhere
        }
    }

    //Validation methods
    public static boolean isValidEmail(String email) {
        return email.contains("@"); // Check if @ is included
    }

    public static boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber.matches("\\d{8}"); // Check for exactly 8 digits
    }

    public static boolean isValidEnglishName(String name) {
        return name.matches("[a-zA-Z]{1,50}"); // Check for only English characters
    }

    //Attendee function 5: Search Banquet (by specified keyword)  checked !!!!! cannot
    public static void searchAttendeeBanquets(String email) throws SQLException {
        System.out.println("Enter keyword to search ");
        System.out.println("(can be substring of banquet name, BinNum, date(YYYY-MM-DD), time(HH-MM), location, first name or last name of contact staff)");
        System.out.print("> ");
        String keyword = scanner.next();

        String query = "SELECT b.* FROM Banquet b " +
                "JOIN Registration r ON b.BinNum = r.BinNum " +
                "WHERE r.Email = ? AND (" +
                "b.BinNum = ? OR " +
                "b.date1 = ? OR " +
                "b.time1 = ? OR " +
                "b.location LIKE ? OR " +
                "b.banquetName LIKE ? OR " +
                "b.firstName Like ? OR " +
                "b.lastName Like ?)";

        PreparedStatement preparedStatement = conn.prepareStatement(query);
        preparedStatement.setString(1, email);

        Integer bin = null;
        try {
            bin = Integer.parseInt(keyword);
            preparedStatement.setInt(2, bin);
        } catch (NumberFormatException e) {
            preparedStatement.setNull(2, java.sql.Types.INTEGER); // Set null if not a valid BIN
        }

        preparedStatement.setString(3, keyword);
        preparedStatement.setString(4, keyword);
        preparedStatement.setString(5, keyword);
        preparedStatement.setString(6, "%" + keyword + "%");
        preparedStatement.setString(7,  keyword);
        preparedStatement.setString(8,  keyword);


        ResultSet rset = preparedStatement.executeQuery();

        if (!rset.next()) {
            System.out.println("No matching banquet is found.");
            return;
        }

        int i = 1;       //i: index for each banquet successfully searched for each attendee
        System.out.println("Matching banquets:");
        do {
            int BIN = rset.getInt("BinNum");
            String banquetName = rset.getString("banquetName");
            String banquetDate = rset.getString("date1");
            String banquetTime = rset.getString("time1");
            String location = rset.getString("location");
            String address = rset.getString("address");
            String firstName = rset.getString("firstName");
            String lastName = rset.getString("lastName");

            System.out.println("Matching result " + i + ": ");
            System.out.println("BIN: " + BIN);
            System.out.println("Banquet Name: " + banquetName);
            System.out.println("Date: " + banquetDate);
            System.out.println("Time: " + banquetTime);
            System.out.println("Location: " + location);
            System.out.println("Address: " + address);
            System.out.println("Contact Staff: " + firstName + " " + lastName);
            System.out.println();
            i++;
        }while (rset.next());

    }

    /********************************************************************************************/

    public static void AdministratorDashboard() throws SQLException {

        while (true) {
            System.out.println("Welcome to Banquet Management System.");
            System.out.println("You can now choose to perform the following actions.");
            System.out.println("Please type the number in the bracket. [] ");
            System.out.println("[1] Create a new banquet.");
            System.out.println("[2] Update the banquet.");
            System.out.println("[3] Get attendee information. ");
            System.out.println("[4] Update attendee registration data. ");
            System.out.println("[5] View analysis report.");
            System.out.println("[6] Quit. ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    if (addMeal()) {
                        System.out.println("Meal input successfully.");
                        addBanquet();
                        System.out.println("Banquet created successfully!");
                    } else {
                        System.out.println("Failed to create banquet.");
                    }
                    System.out.println();
                    break;

                case "2":
                    if (updateBanquet()) {
                        System.out.println("Banquet updated successfully.");
                    } else {
                        System.out.println("Failed to update.");
                    }
                    System.out.println();
                    break;

                case "3":
                    if (getAttendeeInfo()) {
                        System.out.println("Meal input successfully.");
                    } else {
                        System.out.println("Meals failed to input.");
                    }
                    System.out.println();
                    break;

                case "4":
                    if (editAttendeeInfo()) {
                        System.out.println("Update attendee information successfully.");
                    } else {
                        System.out.println("Failed to update. ");
                    }
                    System.out.println();
                    break;

            case "5":
                chooseReportType();
                break;

                case "6":
                    System.out.println("Thank you for your visiting.");
                    System.exit(0);

                default:
                    System.out.println("Invalid input.");

            }
        }
    }

    public static boolean addBanquet() throws SQLException {
        String banquetName, address, location, firstName, lastName;
        String date1;
        String time1;
        String mealID1, mealID2, mealID3, mealID4;
        int BinNum, quota, price;
        int availability;

        while (true) {
            System.out.print("Enter BIN: ");
            while (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a valid integer for BIN.");
                scanner.next(); // Clear the invalid input
            }
            BinNum = scanner.nextInt(); // Read the integer value
            scanner.nextLine(); // Consume the remaining newline character

            // Check if BIN already exists
            String checkSQL = "SELECT COUNT(*) FROM Banquet WHERE BinNum = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                checkStmt.setInt(1, BinNum);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("This BIN already exists. Please enter a different BIN.");
                } else {
                    System.out.println("BIN accepted: " + BinNum);
                    break; // Exit the loop if the BIN is unique
                }
            }
        }

        while (true) {
            System.out.print("Enter banquet name: ");
            banquetName = scanner.nextLine();

            // Check if the banquet name already exists
            String checkSQL = "SELECT COUNT(*) FROM Banquet WHERE banquetName = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                checkStmt.setString(1, banquetName);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("This banquet name already exists. Please enter a different name.");
                } else {
                    System.out.println("Banquet name added: " + banquetName);
                    break; // Exit the loop if the name is unique
                }
            }
        }

        System.out.print("Enter Date (YYYY-MM-DD): ");
        date1 = scanner.nextLine();

        System.out.print("Enter Time (HH:MM): ");
        time1 = scanner.nextLine();

        System.out.println("Enter address: ");
        address = scanner.nextLine();

        System.out.println("Enter location: ");
        location = scanner.nextLine();

        System.out.println("Enter first name: ");
        firstName = scanner.nextLine();

        System.out.println("Enter last name: ");
        lastName = scanner.nextLine();

        do {
            System.out.print("Is the banquet available? (1 for true, 0 for false): ");
            while (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter 1 or 0.");
                scanner.next(); // Clear the invalid input
            }
            availability = scanner.nextInt();
        } while (availability != 0 && availability != 1);
        scanner.nextLine(); // Consume the remaining newline


        do {
            System.out.print("Enter Quota: ");
            while (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a valid integer for quota.");
                scanner.next(); // Clear invalid input
            }
            quota = scanner.nextInt();
        } while (quota <= 0); // Ensure quota is positive
        scanner.nextLine(); // Consume the newline character

        System.out.println("Enter Banquet Name+_mealID_+1: ");
        mealID1 = scanner.nextLine();

        System.out.println("Enter Banquet Name+_mealID_+2: ");
        mealID2 = scanner.nextLine();

        System.out.println("Enter Banquet Name+_mealID_+3: ");
        mealID3 = scanner.nextLine();

        System.out.println("Enter Banquet Name+_mealID_+4: ");
        mealID4 = scanner.nextLine();

        PreparedStatement pstmt = conn.prepareStatement(SQLQueries.INSERT_BANQUET_TABLE);
        pstmt.setInt(1, BinNum);
        pstmt.setString(2, banquetName);
        pstmt.setString(3, date1);
        pstmt.setString(4, time1);
        pstmt.setString(5, address);
        pstmt.setString(6, location);
        pstmt.setString(7, firstName);
        pstmt.setString(8, lastName);
        pstmt.setInt(9, availability);
        pstmt.setInt(10, quota);
        pstmt.setString(11, mealID1);
        pstmt.setString(12, mealID2);
        pstmt.setString(13, mealID3);
        pstmt.setString(14, mealID4);

        pstmt.executeUpdate();

        return true;
    }

    public static boolean addMeal() throws SQLException {
        String mealID, type, dishName, specialCuisine;
        int price;

        for (int i = 1; i <= 4; i++) {
            System.out.println("Enter mealID");
            mealID = scanner.nextLine();

            System.out.println("Enter meal type (Fish, Chicken, Beef, Vegetarian): ");
            type = scanner.next();

            System.out.println("Enter dish name: ");
            scanner.nextLine();
            dishName = scanner.nextLine();

            System.out.println("Enter meal price: ");
            while (true) {
                if (scanner.hasNextInt()) {
                    price = scanner.nextInt();
                    break; // Valid input; exit the loop
                } else {
                    System.out.println("Invalid input. Please enter a valid integer for the price.");
                    scanner.next(); // Consume the invalid input
                }
            }

            System.out.println("Enter today's special cuisine: ");
            scanner.nextLine();
            specialCuisine = scanner.nextLine();

            insertMealDetails(mealID, type, price, dishName, specialCuisine);
        }
        return true; // Indicate success
    }


    public static boolean updateBanquet() throws SQLException {
        String banquetName, address, location, date, time, firstName, lastName, BIN;
        int quota;
        boolean availability;


        do {
            System.out.println("Enter Banquet Name: ");
            banquetName = scanner.next();
            if (banquetName.equals("-1")) {
                return false;
            }
            PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM Banquet WHERE banquetName = ?");
            statement.setString(1, banquetName);
            ResultSet rset = statement.executeQuery();

            if (rset.next() && rset.getInt(1) > 0) {
                break; // Valid BIN found
            } else {
                System.out.println("Banquet name does not exist. Please enter a valid banquet name.");
            }
        } while (true);


        System.out.println("Please choose what you want to update. Enter the number.");
        System.out.println("[1] Date");
        System.out.println("[2] Time");
        System.out.println("[3] Address");
        System.out.println("[4] Location");
        System.out.println("[5] First Name");
        System.out.println("[6] Last Name");
        System.out.println("[7] Availability");
        System.out.println("[8] Quota");

        int input = scanner.nextInt();
        scanner.nextLine();

        String newinput = "";
        boolean validInput = false;

        switch (input) {
            case 1:
                newinput = "date1";
                validInput = true;
                break;
            case 2:
                newinput = "time1";
                validInput = true;
                break;
            case 3:
                newinput = "address";
                validInput = true;
                break;
            case 4:
                newinput = "location";
                validInput = true;
                break;
            case 5:
                newinput = "firstName";
                validInput = true;
                break;
            case 6:
                newinput = "lastName";
                validInput = true;
                break;
            case 7:
                newinput = "availability";
                validInput = true;
                break;
            case 8:
                newinput = "quota";
                validInput = true;
                break;
            default:
                System.out.println("Invalid input.");
                return false;
        }

        System.out.println("Enter new value for " + newinput + ": ");
        String newValue = scanner.nextLine(); // Adjust type if necessary

        // Prepare and execute the update statement
        String sql = "UPDATE Banquet SET " + newinput + " = ? WHERE banquetName = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, newValue); // Set the new value
        pstmt.setString(2, banquetName); // Set the banquet name for the WHERE clause

        int rowsUpdated = pstmt.executeUpdate();
        return rowsUpdated > 0; // Return true if the update was successful
    }

    public static boolean getAttendeeInfo() throws SQLException {
        String input, email;
        do {
            System.out.println("Enter Attendee's email address to get his/her information.");
            email = scanner.nextLine();
            PreparedStatement statement2 = conn.prepareStatement("SELECT COUNT(*) FROM AU WHERE email = ?");
            statement2.setString(1, email);
            ResultSet rset = statement2.executeQuery();
            if (rset.next() && rset.getInt(1) > 0) {
                break;
            } else {
                System.out.println("Invalid email address.");
            }
        } while (true);
        PreparedStatement statement2 = conn.prepareStatement("SELECT * FROM AU WHERE email = ?");
        statement2.setString(1, email);
        ResultSet rset = statement2.executeQuery();
        if (rset.next()) {
            String firstName = rset.getString("FirstName");
            String lastName = rset.getString("LastName");
            String address = rset.getString("Address");
            String AttendeeType = rset.getString("AttendeeType");
            String MobileNumber = rset.getString("MobileNumber");
            String AffiliatedOrganization = rset.getString("AffiliatedOrganization");
            System.out.println();
            System.out.println("First Name: " + firstName);
            System.out.println("Last Name: " + lastName);
            System.out.println("Address: " + address);
            System.out.println("AttendeeType: " + AttendeeType);
            System.out.println("Mobile Number: " + MobileNumber);
            System.out.println("Affiliated Organization: " + AffiliatedOrganization);
        }
        return true;
    }

    public static boolean editAttendeeInfo() throws SQLException {
        String Email;

        do {
            System.out.println("Enter Attendee's email address to update his/her information.");
            Email = scanner.next();
            PreparedStatement statement2 = conn.prepareStatement("SELECT COUNT(*) FROM Registration WHERE Email = ?");
            statement2.setString(1, Email);
            ResultSet rset = statement2.executeQuery();
            if (rset.next() && rset.getInt(1) > 0) {
                break;
            } else {
                System.out.println("Invalid email address.");
            }
        } while (true);

        System.out.println("Please choose what you want to update. Enter the number.");
        System.out.println("[1] Drink");
        System.out.println("[2] Seat Preference.");

        int input = scanner.nextInt();
        scanner.nextLine();

        String newinput = "";
        boolean validInput = false;

        switch (input) {
            case 1:
                newinput = "drink";
                validInput = true;
                break;
            case 2:
                newinput = "seatPreference";
                validInput = true;
                break;
            default:
                System.out.println("Invalid input.");
                return false;
        }

        System.out.println("Enter new value for " + newinput + ": ");
        String newValue = scanner.nextLine();

        String sql = "UPDATE Registration SET " + newinput + " = ? WHERE Email = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, newValue); // Set the new value
        pstmt.setString(2, Email); // Set the email for the WHERE clause

        int rowsUpdated = pstmt.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("Attendee information updated successfully.");
            return true; // Return true if the update was successful
        } else {
            System.out.println("No information was updated. Please check your input.");
            return false;
        }
    }

    public static void insertMealDetails(String meal_ID, String type, int price, String dishName, String specialCuisine) throws SQLException {
        String insertMealSQL = "INSERT INTO Meal (mealID, type, price, dishName, specialCuisine) VALUES (?, ?, ?, ?, ?)";
        // Prepare and execute the insert statement
        PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO Meal (mealID, type, dishName, price, specialCuisine) VALUES (?, ?, ?, ?, ?)");
        insertStmt.setString(1, meal_ID);
        insertStmt.setString(2, type);
        insertStmt.setString(3, dishName);
        insertStmt.setInt(4, price);
        insertStmt.setString(5, specialCuisine); // Convert from string to int


        insertStmt.executeUpdate();
    }

    public static void chooseReportType() {
        while (true) {
            System.out.println();
            System.out.println("---------- Banquet Management Report Type ----------");
            System.out.println();
            System.out.println("[1] Banquet Report");
            System.out.println("[2] Promotion Report");     // back to administrator page
            System.out.println("[3] Log Out");
            System.out.println();
            System.out.println("Please enter your choice: ");


            try {
                int choice = scanner.nextInt();
                scanner.nextLine();


                switch (choice) {
                    case 1:
                        BanquetReportPage();
                        break;
                    case 2:
                        generatePromotionReport();
                        break;
                    case 3:
                        System.out.println("Log out of the system.");
                        return;     // exit the loop
                    default:
                        System.out.println("Invalid choice. Please enter a valid choice.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter a valid number.");
                scanner.nextLine();
            }
        }
    }

        public static void BanquetReportPage() {
            while (true) {
                System.out.println();
                System.out.println("---------- Banquet Report ----------");
                System.out.println();
                System.out.println("[1] Popular Banquets");
                System.out.println("[2] Popular Meals");
                System.out.println();
                System.out.print("Please enter your choice: ");


                try {
                    int choice = scanner.nextInt();
                    scanner.nextLine();


                    switch (choice) {
                        case 1:
                            generatePopularBanquetReport();
                            break;
                        case 2:
                            generatePopularBanquetMeal();
                            break;
                        case 3:
                            chooseReportType();     // back to the banquet management report type page
                            break;
                        default:
                            System.out.println("Invalid choice. Please enter a valid choice");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Please enter a valid number.");
                    scanner.nextLine();
                }
            }
        }

        //1. Popular Banquet Report
        private static void generatePopularBanquetReport () {
            String sql = "SELECT B.BinNum, B.banquetName, COUNT(R.registrationID) AS totalRegistrations "
                    + "FROM Banquet B "
                    + "LEFT JOIN Registration R ON B.BinNum = R.BinNum "
                    + "GROUP BY B.BinNum, B.banquetName "
                    + "ORDER BY totalRegistrations DESC";
            StringBuilder reportContent = new StringBuilder();


            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                System.out.println("Popular Banquet Report: ");
                System.out.println("BIN\tBanquet Name\tTotal Registrations");
                System.out.println("--------------------------------------------------");


                while (resultSet.next()) {
                    int bin = resultSet.getInt("BinNum");
                    String banquetName = resultSet.getString("banquetName");
                    int totalRegistrations = resultSet.getInt("totalRegistrations");
                    System.out.printf("%d\t%s\t%d%n", bin, banquetName, totalRegistrations);
                    reportContent.append(bin).append("- ").append(banquetName).append(": ").append(totalRegistrations).append(" attendees\n");
                }
                promptToSaveReport(reportContent.toString(), "Popular_Banquets_Report.txt");
            } catch (SQLException e) {
                System.out.println("Error retrieving popular banquets: " + e.getMessage());
            }
        }

        //2. Popular Meal Report
        private static void generatePopularBanquetMeal() {
            String sql = "SELECT M.dishName, COUNT(R.mealId) AS totalChoices "
                    + "FROM Registration R "
                    + "JOIN Meal M ON R.mealId = M.mealID "
                    + "GROUP BY M.dishName "
                    + "ORDER BY totalChoices DESC";
            StringBuilder reportContent = new StringBuilder();
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                System.out.println("Popular Meals Report:");
                System.out.println("Dish Name\tNumber of Selections");
                System.out.println("--------------------------------------------------");


                while (resultSet.next()) {
                    String dishName = resultSet.getString("dishName");
                    int totalChoices = resultSet.getInt("totalChoices");
                    System.out.printf("%s\t%d%n", dishName, totalChoices);
                    reportContent.append(dishName).append(": ").append(totalChoices).append(" selections\n");
                }
                promptToSaveReport(reportContent.toString(), "Popular_Meals_Report.txt");
            } catch (SQLException e) {
                System.out.println("Error retrieving popular meals: " + e.getMessage());
            }
        }

        //3. Promotion Report about the last 30 days
        private static void generatePromotionReport () {
            String sql = "SELECT B.BinNum, B.banquetName, COUNT(R.registrationId) AS totalAttendees "
                    + "FROM Banquet B "
                    + "JOIN Registration R ON B.BinNum = R.BinNum "
                    + "WHERE TO_DATE(B.date1, 'YYYY-MM-DD') >= SYSDATE - INTERVAL '30' DAY "
                    + "GROUP BY B.BinNum, B.banquetName "
                    + "ORDER BY totalAttendees DESC";
            StringBuilder reportContent = new StringBuilder();
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                System.out.println("Promotion Report (Last 30 days): ");
                System.out.println("BIN\tBanquet Name\tTotal Attendees");
                System.out.println("--------------------------------------------------");


                while (resultSet.next()) {
                    int bin = resultSet.getInt("BinNum");
                    String banquetName = resultSet.getString("banquetName");
                    int totalAttendees = resultSet.getInt("totalAttendees");
                    System.out.printf("%d\t%s\t%d%n", bin, banquetName, totalAttendees);


                    reportContent.append(bin).append("- ").append(banquetName).append(": ").append(totalAttendees).append(" attendees\n");
                }
                promptToSaveReport(reportContent.toString(), "Promotion_Report.txt");
            } catch (SQLException e) {
                System.out.println("Error retrieving promotion report: " + e.getMessage());
            }
        }

        // administrator decide whether save report
        private static void promptToSaveReport(String reportContent, String fileName){
            System.out.print("Do you want to save this report to a file? (yes / no): ");
            String saveChoice = scanner.nextLine();
            while (!saveChoice.equalsIgnoreCase("yes") && !saveChoice.equalsIgnoreCase("no")) {
                System.out.print("Invalid choice. Please enter 'yes' or 'no': ");
                saveChoice = scanner.nextLine().trim();
            }

            if (saveChoice.equalsIgnoreCase("yes")) {
                saveReportToFile(reportContent, fileName);
                System.out.println("Report saved successfully.");
            } else {
                System.out.println("Report not saved.");
            }
        }

        // save report to file
        private static void saveReportToFile (String reportContent, String fileName){
            String directoryPath = "BMS_Report";
            String filePath = directoryPath + "/" + fileName;
            // create a directory if it doesn't exist
            java.io.File directory = new java.io.File(directoryPath);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Directory created: " + directoryPath);
                } else {
                    System.out.println("Failed to create directory: " + directoryPath);
                    return; // Exit if the directory creation fails
                }
            }
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(reportContent);
                System.out.println("Report saved to " + filePath);
            } catch (IOException e) {
                System.out.println("Error saving report to file: " + e.getMessage());
            }
        }
}


        

