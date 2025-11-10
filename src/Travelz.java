import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

public class Travelz {
    static final String DB_URL = "jdbc:mysql://localhost:3306/travel";
    static final String USER = "root"; // your MySQL username
    static final String PASS = "root"; // your MySQL password

    static Scanner sc = new Scanner(System.in);
    static int loggedInUserId = -1;
    static String loggedInUserEmail = "";
    static boolean isAdmin = false;
    static final int MAX_SEATS_PER_ROUTE = 40;
    static final int MAX_SEATS_PER_BOOKING = 5;

    // ANSI color codes
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String PURPLE = "\u001B[35m";
    public static final String WHITE = "\u001B[37m";

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.out.println(BLUE + "╔════════════════════════════════════════════╗" + RESET);
            System.out.println(BLUE + "║   Welcome to Travelz Ticket Booking! 🚌✈️🚆║" + RESET);
            System.out.println(BLUE + "╚════════════════════════════════════════════╝" + RESET);
            Thread.sleep(1000);

            System.out.println("\n" + CYAN + "1. Register 👤" + RESET);
            System.out.println(CYAN + "2. Login 🔑" + RESET);
            System.out.println(CYAN + "3. Exit 🚪" + RESET);
            System.out.print(YELLOW + "Choose option: " + RESET);
            String choiceStr = sc.nextLine();
            int choice = parseInt(choiceStr);

            switch (choice) {
                case 1:
                    register();
                    break;
                case 2:
                    if (login()) {
                        if (isAdmin) {
                            adminMenu();
                        } else {
                            userMenu();
                        }
                    }
                    break;
                case 3:
                    System.out.println(GREEN + "Thank you for using Travelz! Have a nice day! 👋😊" + RESET);
                    System.exit(0);
                default:
                    System.out.println(RED + "Invalid option. ❌" + RESET);
            }
        }
    }

    static void register() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println(PURPLE + "\n--- Register 👤 ---" + RESET);
            System.out.print("Enter Name: ");
            String name = sc.nextLine().trim();

            String phone;
            while (true) {
                System.out.print("Enter Phone (10 digits): ");
                phone = sc.nextLine().trim();
                if (phone.matches("\\d{10}")) break;
                System.out.println(RED + "Invalid phone number. ❌" + RESET);
            }

            String email;
            while (true) {
                System.out.print("Enter Email: ");
                email = sc.nextLine().trim();
                if (email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) break;
                System.out.println(RED + "Invalid email format. ❌" + RESET);
            }

            System.out.print("Enter Password: ");
            String password = sc.nextLine();

            String hashedPassword = hashPassword(password);

            String sql = "INSERT INTO users (name, phone, email, password) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setString(4, hashedPassword);

            pstmt.executeUpdate();
            System.out.println(GREEN + "Register Successful! 🎉" + RESET);
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println(RED + "Phone or Email already registered! ❌" + RESET);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean login() {
        isAdmin = false;
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println(PURPLE + "\n--- Login 🔑 ---" + RESET);
            System.out.print("Enter Phone or Email: ");
            String user = sc.nextLine().trim();
            System.out.print("Enter Password: ");
            String pass = sc.nextLine();

            String hashedPassword = hashPassword(pass);

            String sql = "SELECT id, email FROM users WHERE (phone=? OR email=?) AND password=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user);
            pstmt.setString(2, user);
            pstmt.setString(3, hashedPassword);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                loggedInUserId = rs.getInt("id");
                loggedInUserEmail = rs.getString("email");
                if (loggedInUserEmail.equalsIgnoreCase("admin@travelz.com")) {
                    isAdmin = true;
                }
                System.out.println(GREEN + "Login Successful! ✅" + RESET);
                return true;
            } else {
                System.out.println(RED + "Invalid user! ❌" + RESET);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static void userMenu() {
        while (true) {
            System.out.println(BLUE + "\n--- User Menu ---" + RESET);
            System.out.println(CYAN + "1. Book Ticket 🏷️" + RESET);
            System.out.println(CYAN + "2. View Bookings 📄" + RESET);
            System.out.println(CYAN + "3. Update Profile 📝" + RESET);
            System.out.println(CYAN + "4. Logout 🚪" + RESET);
            System.out.print(YELLOW + "Choose option: " + RESET);
            String choiceStr = sc.nextLine();
            int choice = parseInt(choiceStr);

            switch (choice) {
                case 1:
                    bookTicket();
                    break;
                case 2:
                    viewBookings(false);
                    break;
                case 3:
                    updateProfile();
                    break;
                case 4:
                    loggedInUserId = -1;
                    loggedInUserEmail = "";
                    System.out.println(GREEN + "Logged out! 👋" + RESET);
                    return;
                default:
                    System.out.println(RED + "Invalid option. ❌" + RESET);
            }
        }
    }

    static void adminMenu() {
        while (true) {
            System.out.println(BLUE + "\n--- Admin Menu 🛡️ ---" + RESET);
            System.out.println(CYAN + "1. View All Users 👥" + RESET);
            System.out.println(CYAN + "2. View All Bookings 📄" + RESET);
            System.out.println(CYAN + "3. Delete User 🗑️" + RESET);
            System.out.println(CYAN + "4. Delete Booking 🗑️" + RESET);
            System.out.println(CYAN + "5. Logout 🚪" + RESET);
            System.out.print(YELLOW + "Choose option: " + RESET);
            String choiceStr = sc.nextLine();
            int choice = parseInt(choiceStr);

            switch (choice) {
                case 1:
                    viewAllUsers();
                    break;
                case 2:
                    viewBookings(true);
                    break;
                case 3:
                    deleteUser();
                    break;
                case 4:
                    deleteBooking();
                    break;
                case 5:
                    loggedInUserId = -1;
                    loggedInUserEmail = "";
                    isAdmin = false;
                    System.out.println(GREEN + "Admin logged out! 👋" + RESET);
                    return;
                default:
                    System.out.println(RED + "Invalid option. ❌" + RESET);
            }
        }
    }

    static void bookTicket() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println(PURPLE + "\n--- Book Ticket 🏷️ ---" + RESET);
            System.out.println(CYAN + "Choose travel type: 1. Bus 🚌  2. Train 🚆  3. Flight ✈️" + RESET);
            System.out.print(YELLOW + "Enter option: " + RESET);
            String typeChoiceStr = sc.nextLine();
            int typeChoice = parseInt(typeChoiceStr);
            String type = (typeChoice == 1) ? "Bus" : (typeChoice == 2) ? "Train" : "Flight";
            String typeEmoji = (type.equals("Bus")) ? "🚌" : (type.equals("Train")) ? "🚆" : "✈️";

            System.out.print("From: ");
            String from = sc.nextLine().trim();
            System.out.print("To: ");
            String to = sc.nextLine().trim();

            String date;
            while (true) {
                System.out.print("Date (YYYY-MM-DD): ");
                date = sc.nextLine().trim();
                if (isValidDate(date)) break;
                System.out.println(RED + "Invalid date format. ❌" + RESET);
            }

            int availableSeats = getAvailableSeats(conn, type, from, to, date);
            System.out.println(BLUE + "Available seats: " + (MAX_SEATS_PER_ROUTE - availableSeats) + RESET);

            int seats;
            while (true) {
                System.out.print("How many seats to book? (max " + MAX_SEATS_PER_BOOKING + "): ");
                String seatsStr = sc.nextLine();
                seats = parseInt(seatsStr);
                if (seats >= 1 && seats <= MAX_SEATS_PER_BOOKING) break;
                System.out.println(RED + "You can book minimum 1 and maximum " + MAX_SEATS_PER_BOOKING + " seats per booking. ❌" + RESET);
            }

            if (availableSeats + seats > MAX_SEATS_PER_ROUTE) {
                System.out.println(RED + "Not enough seats available! Only " + (MAX_SEATS_PER_ROUTE - availableSeats) + " left. ❌" + RESET);
                return;
            }

            System.out.print(YELLOW + "Confirm booking? (yes/no): " + RESET);
            String confirm = sc.nextLine();
            if (confirm.equalsIgnoreCase("yes")) {
                String bookingRef = "TRZ" + System.currentTimeMillis();
                String bookSql = "INSERT INTO bookings (user_id, type, from_place, to_place, date, seats, booking_ref) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement bookPstmt = conn.prepareStatement(bookSql);
                bookPstmt.setInt(1, loggedInUserId);
                bookPstmt.setString(2, type);
                bookPstmt.setString(3, from);
                bookPstmt.setString(4, to);
                bookPstmt.setString(5, date);
                bookPstmt.setInt(6, seats);
                bookPstmt.setString(7, bookingRef);
                bookPstmt.executeUpdate();

                System.out.println(GREEN + "Successfully booked! " + typeEmoji + " Your reference number: " + bookingRef + " 🎫" + RESET);
            } else {
                System.out.println(YELLOW + "Booking cancelled. 🗑️" + RESET);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static int getAvailableSeats(Connection conn, String type, String from, String to, String date) throws SQLException {
        String sql = "SELECT SUM(seats) as total FROM bookings WHERE type=? AND from_place=? AND to_place=? AND date=?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, type);
        pstmt.setString(2, from);
        pstmt.setString(3, to);
        pstmt.setString(4, date);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    static void viewBookings(boolean isAdminView) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql;
            if (isAdminView) {
                sql = "SELECT b.*, u.name FROM bookings b JOIN users u ON b.user_id = u.id ORDER BY b.booking_date DESC";
            } else {
                sql = "SELECT * FROM bookings WHERE user_id=? ORDER BY booking_date DESC";
            }
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if (!isAdminView) {
                pstmt.setInt(1, loggedInUserId);
            }
            ResultSet rs = pstmt.executeQuery();

            System.out.println(PURPLE + "\n--- Bookings 📄 ---" + RESET);
            boolean hasBookings = false;
            while (rs.next()) {
                hasBookings = true;
                String type = rs.getString("type");
                String typeEmoji = (type.equals("Bus")) ? "🚌" : (type.equals("Train")) ? "🚆" : "✈️";
                System.out.println(BLUE + "Booking ID: " + rs.getInt("id") + RESET);
                if (isAdminView) System.out.println("User: " + rs.getString("name"));
                System.out.println("Ref: " + rs.getString("booking_ref") + " 🏷️");
                System.out.println("Type: " + type + " " + typeEmoji);
                System.out.println("From: " + rs.getString("from_place"));
                System.out.println("To: " + rs.getString("to_place"));
                System.out.println("Date: " + rs.getString("date") + " 📅");
                System.out.println("Seats: " + rs.getInt("seats") + " 💺");
                System.out.println("Booked on: " + rs.getString("booking_date"));
                System.out.println(CYAN + "-------------------------------" + RESET);
            }
            if (!hasBookings) {
                System.out.println(YELLOW + "No bookings found. ℹ️" + RESET);
            }

            // Search Option (only for user, not admin)
            if (!isAdminView) {
                System.out.print("Do you want to search bookings? (yes/no): ");
                String search = sc.nextLine();
                if (search.equalsIgnoreCase("yes")) {
                    System.out.println("Search by: 1. Date 📅  2. Type 🚌/🚆/✈️  3. From 🏁  4. To 🏁");
                    System.out.print("Choose option: ");
                    int searchOpt = parseInt(sc.nextLine());
                    String searchSql = "SELECT * FROM bookings WHERE user_id=? ";
                    String param = "";
                    switch (searchOpt) {
                        case 1:
                            System.out.print("Enter date (YYYY-MM-DD): ");
                            param = sc.nextLine();
                            searchSql += "AND date=?";
                            break;
                        case 2:
                            System.out.print("Enter type (Bus/Train/Flight): ");
                            param = sc.nextLine();
                            searchSql += "AND type=?";
                            break;
                        case 3:
                            System.out.print("Enter from place: ");
                            param = sc.nextLine();
                            searchSql += "AND from_place=?";
                            break;
                        case 4:
                            System.out.print("Enter to place: ");
                            param = sc.nextLine();
                            searchSql += "AND to_place=?";
                            break;
                        default:
                            System.out.println(RED + "Invalid option. ❌" + RESET);
                            return;
                    }
                    try (PreparedStatement searchPstmt = conn.prepareStatement(searchSql)) {
                        searchPstmt.setInt(1, loggedInUserId);
                        searchPstmt.setString(2, param);
                        ResultSet searchRs = searchPstmt.executeQuery();
                        boolean found = false;
                        while (searchRs.next()) {
                            found = true;
                            String type = searchRs.getString("type");
                            String typeEmoji = (type.equals("Bus")) ? "🚌" : (type.equals("Train")) ? "🚆" : "✈️";
                            System.out.println(BLUE + "Booking ID: " + searchRs.getInt("id") + RESET);
                            System.out.println("Ref: " + searchRs.getString("booking_ref") + " 🏷️");
                            System.out.println("Type: " + type + " " + typeEmoji);
                            System.out.println("From: " + searchRs.getString("from_place"));
                            System.out.println("To: " + searchRs.getString("to_place"));
                            System.out.println("Date: " + searchRs.getString("date") + " 📅");
                            System.out.println("Seats: " + searchRs.getInt("seats") + " 💺");
                            System.out.println("Booked on: " + searchRs.getString("booking_date"));
                            System.out.println(CYAN + "-------------------------------" + RESET);
                        }
                        if (!found) System.out.println(YELLOW + "No bookings found for your search. ℹ️" + RESET);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                // Export Option
                System.out.print("Do you want to export your bookings? (yes/no): ");
                String export = sc.nextLine();
                if (export.equalsIgnoreCase("yes")) {
                    String sqlExport = "SELECT * FROM bookings WHERE user_id=? ORDER BY booking_date DESC";
                    try (PreparedStatement pstmtExport = conn.prepareStatement(sqlExport)) {
                        pstmtExport.setInt(1, loggedInUserId);
                        ResultSet rsExport = pstmtExport.executeQuery();

                        String fileName = "my_bookings_" + loggedInUserId + ".txt";
                        try (java.io.PrintWriter writer = new java.io.PrintWriter(fileName)) {
                            while (rsExport.next()) {
                                String type = rsExport.getString("type");
                                String typeEmoji = (type.equals("Bus")) ? "🚌" : (type.equals("Train")) ? "🚆" : "✈️";
                                writer.println("Booking ID: " + rsExport.getInt("id"));
                                writer.println("Ref: " + rsExport.getString("booking_ref") + " 🏷️");
                                writer.println("Type: " + type + " " + typeEmoji);
                                writer.println("From: " + rsExport.getString("from_place"));
                                writer.println("To: " + rsExport.getString("to_place"));
                                writer.println("Date: " + rsExport.getString("date") + " 📅");
                                writer.println("Seats: " + rsExport.getInt("seats") + " 💺");
                                writer.println("Booked on: " + rsExport.getString("booking_date"));
                                writer.println("-------------------------------");
                            }
                        }
                        System.out.println(GREEN + "Bookings exported to file: " + fileName + " 📤" + RESET);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Cancel Option
                System.out.print("Do you want to cancel any booking? (yes/no): ");
                String cancel = sc.nextLine();
                if (cancel.equalsIgnoreCase("yes")) {
                    System.out.print("Enter Booking ID to cancel: ");
                    String bookingIdStr = sc.nextLine();
                    int bookingId = parseInt(bookingIdStr);

                    String delSql = "DELETE FROM bookings WHERE id=? AND user_id=?";
                    PreparedStatement delPstmt = conn.prepareStatement(delSql);
                    delPstmt.setInt(1, bookingId);
                    delPstmt.setInt(2, loggedInUserId);
                    int rows = delPstmt.executeUpdate();
                    if (rows > 0) {
                        System.out.println(GREEN + "Booking cancelled successfully! 🗑️" + RESET);
                    } else {
                        System.out.println(RED + "Invalid Booking ID. ❌" + RESET);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void updateProfile() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println(PURPLE + "\n--- Update Profile 📝 ---" + RESET);
            System.out.println(CYAN + "1. Change Name" + RESET);
            System.out.println(CYAN + "2. Change Phone" + RESET);
            System.out.println(CYAN + "3. Change Email" + RESET);
            System.out.println(CYAN + "4. Change Password" + RESET);
            System.out.println(CYAN + "5. Back" + RESET);
            System.out.print(YELLOW + "Choose option: " + RESET);
            String choiceStr = sc.nextLine();
            int choice = parseInt(choiceStr);

            String sql = "";
            String value = "";
            switch (choice) {
                case 1:
                    System.out.print("Enter new name: ");
                    value = sc.nextLine().trim();
                    sql = "UPDATE users SET name=? WHERE id=?";
                    break;
                case 2:
                    while (true) {
                        System.out.print("Enter new phone (10 digits): ");
                        value = sc.nextLine().trim();
                        if (value.matches("\\d{10}")) break;
                        System.out.println(RED + "Invalid phone number. ❌" + RESET);
                    }
                    sql = "UPDATE users SET phone=? WHERE id=?";
                    break;
                case 3:
                    while (true) {
                        System.out.print("Enter new email: ");
                        value = sc.nextLine().trim();
                        if (value.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) break;
                        System.out.println(RED + "Invalid email format. ❌" + RESET);
                    }
                    sql = "UPDATE users SET email=? WHERE id=?";
                    break;
                case 4:
                    System.out.print("Enter new password: ");
                    value = hashPassword(sc.nextLine());
                    sql = "UPDATE users SET password=? WHERE id=?";
                    break;
                case 5:
                    return;
                default:
                    System.out.println(RED + "Invalid option. ❌" + RESET);
                    return;
            }
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            pstmt.setInt(2, loggedInUserId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "Profile updated successfully! ✅" + RESET);
            } else {
                System.out.println(RED + "Update failed. ❌" + RESET);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println(RED + "Phone or Email already registered! ❌" + RESET);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void viewAllUsers() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT id, name, phone, email FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println(PURPLE + "\n--- Users 👥 ---" + RESET);
            while (rs.next()) {
                System.out.println(BLUE + "ID: " + rs.getInt("id") + RESET +
                        ", Name: " + rs.getString("name") +
                        ", Phone: " + rs.getString("phone") +
                        ", Email: " + rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void deleteUser() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.print("Enter User ID to delete: ");
            String userIdStr = sc.nextLine();
            int userId = parseInt(userIdStr);

            if (userId == loggedInUserId) {
                System.out.println(RED + "Admin cannot delete self! ❌" + RESET);
                return;
            }

            String delBookings = "DELETE FROM bookings WHERE user_id=?";
            PreparedStatement delBookPstmt = conn.prepareStatement(delBookings);
            delBookPstmt.setInt(1, userId);
            delBookPstmt.executeUpdate();

            String delUser = "DELETE FROM users WHERE id=?";
            PreparedStatement delUserPstmt = conn.prepareStatement(delUser);
            delUserPstmt.setInt(1, userId);
            int rows = delUserPstmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "User deleted successfully! 🗑️" + RESET);
            } else {
                System.out.println(RED + "Invalid User ID. ❌" + RESET);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void deleteBooking() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.print("Enter Booking ID to delete: ");
            String bookingIdStr = sc.nextLine();
            int bookingId = parseInt(bookingIdStr);

            String delSql = "DELETE FROM bookings WHERE id=?";
            PreparedStatement delPstmt = conn.prepareStatement(delSql);
            delPstmt.setInt(1, bookingId);
            int rows = delPstmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "Booking deleted successfully! 🗑️" + RESET);
            } else {
                System.out.println(RED + "Invalid Booking ID. ❌" + RESET);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Utility methods

    static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // fallback (not secure)
        }
    }

    static boolean isValidDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}