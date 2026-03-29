import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FileHandler {

    static String BOOK_FILE = "data/books.txt";
    static String MEMBER_FILE = "data/members.txt";
    static String ISSUE_FILE = "data/issued.txt";

    // 🔹 Read file safely
    static List<String> read(String path) throws Exception {
        File f = new File(path);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }

        List<String> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;

        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty())
                list.add(line);
        }
        br.close();
        return list;
    }

    // 🔹 Safe write (overwrite)
    static void write(String path, List<String> data) throws Exception {
        FileWriter fw = new FileWriter(path);
        for (String s : data) {
            fw.write(s + "\n");
        }
        fw.close();
    }

    // 🔹 Generate next ID (no duplicates)
    static int getNextId(List<String> list) {
        int max = 0;
        for (String s : list) {
            String[] p = s.split(",");
            int id = Integer.parseInt(p[0]);
            if (id > max) max = id;
        }
        return max + 1;
    }

    // 🔹 Add Book
    public static String addBook(String title, String author) {
        try {
            List<String> books = read(BOOK_FILE);
            int id = getNextId(books);

            FileWriter fw = new FileWriter(BOOK_FILE, true);
            fw.write(id + "," + title + "," + author + ",true\n");
            fw.close();

            return "Book added successfully (ID: " + id + ")";
        } catch (Exception e) {
            return "Error adding book!";
        }
    }

    // 🔹 Add Member
    public static String addMember(String name) {
        try {
            List<String> members = read(MEMBER_FILE);
            int id = getNextId(members);

            FileWriter fw = new FileWriter(MEMBER_FILE, true);
            fw.write(id + "," + name + ",0\n");
            fw.close();

            return "Member added (ID: " + id + ")";
        } catch (Exception e) {
            return "Error adding member!";
        }
    }

    // 🔹 Check if member exists
    static boolean memberExists(int memberId) throws Exception {
        List<String> members = read(MEMBER_FILE);
        for (String m : members) {
            if (Integer.parseInt(m.split(",")[0]) == memberId)
                return true;
        }
        return false;
    }

    // 🔹 Issue Book
    public static String issueBook(int bookId, int memberId) {
        try {
            if (!memberExists(memberId))
                return "Member does not exist!";

            List<String> books = read(BOOK_FILE);
            List<String> updated = new ArrayList<>();
            boolean found = false;

            for (String b : books) {
                String[] p = b.split(",");

                if (Integer.parseInt(p[0]) == bookId) {
                    found = true;

                    if (p[3].equals("false"))
                        return "Book already issued!";

                    p[3] = "false";

                    FileWriter fw = new FileWriter(ISSUE_FILE, true);
                    LocalDate issueDate = LocalDate.now();
                    LocalDate dueDate = issueDate.plusDays(14);
                    fw.write(bookId + "," + memberId + "," + issueDate + "," + dueDate + "\n");
                    fw.close();
                }
                updated.add(String.join(",", p));
            }

            if (!found) return "Book not found!";

            // Increment member's total_borrowed tally
            List<String> members = read(MEMBER_FILE);
            List<String> updatedMembers = new ArrayList<>();
            for (String m : members) {
                String[] mp = m.split(",");
                if (Integer.parseInt(mp[0]) == memberId) {
                    int count = mp.length >= 3 ? Integer.parseInt(mp[2]) : 0;
                    count++;
                    if (mp.length >= 3) {
                        mp[2] = String.valueOf(count);
                        updatedMembers.add(String.join(",", mp));
                    } else {
                        updatedMembers.add(m + "," + count);
                    }
                } else {
                    if (mp.length < 3) updatedMembers.add(m + ",0");
                    else updatedMembers.add(m);
                }
            }
            write(MEMBER_FILE, updatedMembers);

            write(BOOK_FILE, updated);
            return "Book issued successfully!";

        } catch (Exception e) {
            return "Error issuing book!";
        }
    }

    // 🔹 Return Book
    public static String returnBook(int bookId) {
        try {
            List<String> books = read(BOOK_FILE);
            List<String> updated = new ArrayList<>();
            List<String> issues = read(ISSUE_FILE);
            List<String> newIssues = new ArrayList<>();

            boolean found = false;

            for (String b : books) {
                String[] p = b.split(",");

                if (Integer.parseInt(p[0]) == bookId) {
                    found = true;

                    if (p[3].equals("true"))
                        return "Book is already available!";

                    p[3] = "true";
                }
                updated.add(String.join(",", p));
            }

            // remove from issued list
            for (String i : issues) {
                if (!i.startsWith(bookId + ","))
                    newIssues.add(i);
            }

            if (!found) return "Book not found!";

            write(BOOK_FILE, updated);
            write(ISSUE_FILE, newIssues);

            return "Book returned successfully!";

        } catch (Exception e) {
            return "Error returning book!";
        }
    }

    // 🔹 Get all books as JSON
    public static String getBooksJson() {
        try {
            List<String> books = read(BOOK_FILE);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < books.size(); i++) {
                String[] p = books.get(i).split(",");
                if (p.length >= 4) {
                    sb.append("{\"id\":").append(p[0])
                      .append(",\"title\":\"").append(p[1].replace("\"", "\\\""))
                      .append("\",\"author\":\"").append(p[2].replace("\"", "\\\""))
                      .append("\",\"available\":").append(p[3]).append("}");
                    if (i < books.size() - 1) sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    // 🔹 Get all members as JSON
    public static String getMembersJson() {
        try {
            List<String> members = read(MEMBER_FILE);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < members.size(); i++) {
                String[] p = members.get(i).split(",");
                if (p.length >= 2) {
                    int totalBorrowed = p.length >= 3 ? Integer.parseInt(p[2]) : 0;
                    sb.append("{\"id\":").append(p[0])
                      .append(",\"name\":\"").append(p[1].replace("\"", "\\\""))
                      .append("\",\"totalBorrowed\":").append(totalBorrowed)
                      .append("}");
                    if (i < members.size() - 1) sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    // 🔹 Get all issued records as JSON
    public static String getIssuedJson() {
        try {
            List<String> issued = read(ISSUE_FILE);
            StringBuilder sb = new StringBuilder("[");
            LocalDate today = LocalDate.now();
            for (int i = 0; i < issued.size(); i++) {
                String[] p = issued.get(i).split(",");
                if (p.length >= 2) {
                    String issueDateStr = p.length > 2 ? p[2] : today.toString();
                    String dueDateStr = p.length > 3 ? p[3] : today.plusDays(14).toString();
                    LocalDate dueDateStrDate = LocalDate.parse(dueDateStr);
                    long daysOverdue = ChronoUnit.DAYS.between(dueDateStrDate, today);
                    long fine = 0;
                    boolean missed = false;
                    if (daysOverdue > 14) {
                        fine = 500;
                        missed = true;
                    } else if (daysOverdue > 0) {
                        fine = daysOverdue * 10;
                    }

                    sb.append("{\"bookId\":").append(p[0])
                      .append(",\"memberId\":").append(p[1])
                      .append(",\"issueDate\":\"").append(issueDateStr).append("\"")
                      .append(",\"dueDate\":\"").append(dueDateStr).append("\"")
                      .append(",\"daysOverdue\":").append(daysOverdue)
                      .append(",\"fine\":").append(fine)
                      .append(",\"missed\":").append(missed).append("}");
                    if (i < issued.size() - 1) sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[]";
        }
    }
}