import javax.swing.*;
import java.awt.*;

public class MainGUI {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Library System");
        frame.setSize(400, 350);
        frame.setLayout(new GridLayout(5, 1, 10, 10));

        JButton addBook = new JButton("Add Book");
        JButton issueBook = new JButton("Issue Book");
        JButton returnBook = new JButton("Return Book");
        JButton addMember = new JButton("Add Member");

        frame.add(addBook);
        frame.add(issueBook);
        frame.add(returnBook);
        frame.add(addMember);

        // 🔹 Add Book
        addBook.addActionListener(e -> {
            JTextField title = new JTextField();
            JTextField author = new JTextField();

            Object[] fields = {"Title:", title, "Author:", author};

            int opt = JOptionPane.showConfirmDialog(null, fields, "Add Book", JOptionPane.OK_CANCEL_OPTION);

            if (opt == JOptionPane.OK_OPTION) {
                String msg = FileHandler.addBook(title.getText(), author.getText());
                JOptionPane.showMessageDialog(null, msg);
            }
        });

        // 🔹 Add Member
        addMember.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter Member Name:");
            if (name != null && !name.trim().isEmpty()) {
                String msg = FileHandler.addMember(name);
                JOptionPane.showMessageDialog(null, msg);
            }
        });

        // 🔹 Issue Book
        issueBook.addActionListener(e -> {
            JTextField b = new JTextField();
            JTextField m = new JTextField();

            Object[] fields = {"Book ID:", b, "Member ID:", m};

            int opt = JOptionPane.showConfirmDialog(null, fields, "Issue Book", JOptionPane.OK_CANCEL_OPTION);

            if (opt == JOptionPane.OK_OPTION) {
                try {
                    int bookId = Integer.parseInt(b.getText());
                    int memberId = Integer.parseInt(m.getText());

                    String msg = FileHandler.issueBook(bookId, memberId);
                    JOptionPane.showMessageDialog(null, msg);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid input!");
                }
            }
        });

        // 🔹 Return Book
        returnBook.addActionListener(e -> {
            String input = JOptionPane.showInputDialog("Enter Book ID:");

            try {
                int id = Integer.parseInt(input);
                String msg = FileHandler.returnBook(id);
                JOptionPane.showMessageDialog(null, msg);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Invalid ID!");
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}