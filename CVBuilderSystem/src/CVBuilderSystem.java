/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author MD. Maruf Siddiki
 */
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class CVBuilderSystem {

    private static final String CV_FILE = "cv_data.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /* ==================== MODEL ==================== */
    static class CV {
        String fullName, email, phone, address, photoPath;
        String degree, institute, passingYear, cgpa;
        String company, position, durationFrom, durationTo, responsibilities;
        double totalExperienceYears;
        List<String> skills = new ArrayList<>();
        List<Project> projects = new ArrayList<>();

        static class Project {
            String title, description, technologies;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }

    /* ==================== LOGIN FRAME ==================== */
    static class LoginFrame extends JFrame {
        LoginFrame() {
            setTitle("CV Builder System - Login");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(20, 20, 20, 20);

            JButton userBtn = new JButton("User Login");
            JButton adminBtn = new JButton("Admin Login");
            userBtn.setPreferredSize(new Dimension(220, 55));
            adminBtn.setPreferredSize(new Dimension(220, 55));

            userBtn.addActionListener(e -> { dispose(); new CVBuilderForm(null); });
            adminBtn.addActionListener(e -> {
                String pass = JOptionPane.showInputDialog(this, "Enter Admin Password:");
                if ("admin123".equals(pass)) {
                    dispose();
                    new AdminDashboard();
                } else if (pass != null) {
                    JOptionPane.showMessageDialog(this, "Wrong password!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            gbc.gridx = 0; gbc.gridy = 0; panel.add(userBtn, gbc);
            gbc.gridy = 1; panel.add(adminBtn, gbc);
            add(panel);
            setVisible(true);
        }
    }

    /* ==================== USER - CV BUILDER FORM ==================== */
    static class CVBuilderForm extends JFrame {
        private final CV cv;
        private JTextField tfName, tfEmail, tfPhone, tfAddress;
        private JTextField tfDegree, tfInstitute, tfYear, tfCgpa;
        private JTextField tfCompany, tfPosition, tfFrom, tfTo, tfResp;
        private DefaultListModel<String> skillModel = new DefaultListModel<>();
        private JList<String> skillList = new JList<>(skillModel);
        private JTextField tfSkill = new JTextField(20);
        private List<ProjectPanel> projectPanels = new ArrayList<>();
        //private JLabel lblPhoto = new JLabel("No photo", SwingConstants.CENTER);
        private String photoPath = null;

        // Fixed: Store reference to the container and button
        private JPanel projectsContainer;
        private JButton btnAddProject;

        CVBuilderForm(CV existingCV) {
            this.cv = (existingCV == null) ? new CV() : existingCV;
            setTitle("CV Builder - " + (existingCV == null ? "New CV" : cv.fullName));
            setSize(1000, 720);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();

            tabs.addTab("Personal", createPersonalPanel());
            tabs.addTab("Education", createEducationPanel());
            tabs.addTab("Experience", createExperiencePanel());
            tabs.addTab("Skills", createSkillsPanel());
            tabs.addTab("Projects", createProjectsPanel());  // Now returns JPanel (not JScrollPane)

            JPanel btnPanel = new JPanel();
            JButton btnSave = new JButton("Save CV");
            JButton btnPdf = new JButton("Generate PDF");
            JButton btnClear = new JButton("Clear");
            btnSave.addActionListener(e -> saveCV());
            btnPdf.addActionListener(e -> { saveCV(); CVExporter.generate(cv); });
            btnClear.addActionListener(e -> clearForm());

            btnPanel.add(btnSave); btnPanel.add(btnPdf); btnPanel.add(btnClear);
            btnPanel.add(new JButton("Logout") {{ addActionListener(e -> { dispose(); new LoginFrame(); }); }});

            add(tabs, BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);

            if (existingCV != null) loadCVToForm();

            setVisible(true);
        }

        private JPanel createPersonalPanel() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5,5,5,5); g.anchor = GridBagConstraints.WEST;

            tfName = new JTextField(30); tfEmail = new JTextField(30);
            tfPhone = new JTextField(30); tfAddress = new JTextField(30);

            addRow(p, g, "Full Name:", tfName, 0);
            addRow(p, g, "Email:", tfEmail, 1);
            addRow(p, g, "Phone:", tfPhone, 2);
            addRow(p, g, "Address:", tfAddress, 3);
         
            //JButton uploadBtn = new JButton("Upload Photo");
            //lblPhoto.setPreferredSize(new Dimension(150,150));
            //lblPhoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            //uploadBtn.addActionListener(e -> uploadPhoto());

            //JPanel photoBox = new JPanel();
            //photoBox.add(uploadBtn); photoBox.add(lblPhoto);
            //g.gridx = 0; g.gridy = 4; g.gridwidth = 2; p.add(photoBox, g);
            return p;
        }

        private JPanel createEducationPanel() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5,5,5,5); g.anchor = GridBagConstraints.WEST;

            tfDegree = new JTextField(30); tfInstitute = new JTextField(30);
            tfYear = new JTextField(10); tfCgpa = new JTextField(10);

            addRow(p, g, "Degree:", tfDegree, 0);
            addRow(p, g, "Institute:", tfInstitute, 1);
            addRow(p, g, "Passing Year:", tfYear, 2);
            addRow(p, g, "CGPA:", tfCgpa, 3);
            return p;
        }

        private JPanel createExperiencePanel() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5,5,5,5); g.anchor = GridBagConstraints.WEST;

            tfCompany = new JTextField(30); tfPosition = new JTextField(30);
            tfFrom = new JTextField(15); tfTo = new JTextField(15); tfResp = new JTextField(40);

            addRow(p, g, "Company:", tfCompany, 0);
            addRow(p, g, "Position:", tfPosition, 1);
            addRow(p, g, "From (yyyy-MM):", tfFrom, 2);
            addRow(p, g, "To (yyyy-MM or Present):", tfTo, 3);
            addRow(p, g, "Responsibilities:", tfResp, 4);
            return p;
        }

        private JPanel createSkillsPanel() {
            JPanel main = new JPanel(new BorderLayout());
            main.setBorder(BorderFactory.createTitledBorder("Skills"));

            JPanel top = new JPanel();
            JButton addBtn = new JButton("Add Skill");
            addBtn.addActionListener(e -> {
                String s = tfSkill.getText().trim();
                if (!s.isEmpty() && !skillModel.contains(s)) {
                    skillModel.addElement(s);
                    tfSkill.setText("");
                }
            });
            top.add(new JLabel("Skill:")); top.add(tfSkill); top.add(addBtn);
            main.add(top, BorderLayout.NORTH);
            main.add(new JScrollPane(skillList), BorderLayout.CENTER);
            return main;
        }

        // Fixed: Return JPanel with JScrollPane inside
        private JPanel createProjectsPanel() {
            projectsContainer = new JPanel();
            projectsContainer.setLayout(new BoxLayout(projectsContainer, BoxLayout.Y_AXIS));
            projectsContainer.setBorder(BorderFactory.createTitledBorder("Projects (at least 2)"));

            btnAddProject = new JButton("Add Another Project");
            btnAddProject.addActionListener(e -> addProjectPanel());

            projectsContainer.add(btnAddProject);
            addProjectPanel();
            addProjectPanel();

            JScrollPane scroll = new JScrollPane(projectsContainer);
            scroll.setPreferredSize(new Dimension(800, 300));

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(scroll, BorderLayout.CENTER);
            return wrapper;
        }

        private void addProjectPanel() {
            ProjectPanel pp = new ProjectPanel();
            projectPanels.add(pp);
            projectsContainer.add(pp, projectsContainer.getComponentCount() - 1);
            projectsContainer.revalidate();
            projectsContainer.repaint();
        }

        static class ProjectPanel extends JPanel {
            JTextField tfTitle = new JTextField(30);
            JTextArea taDesc = new JTextArea(3, 30);
            JTextField tfTech = new JTextField(30);

            ProjectPanel() {
                setLayout(new GridBagLayout());
                GridBagConstraints g = new GridBagConstraints();
                g.insets = new Insets(4,4,4,4); g.anchor = GridBagConstraints.WEST;
                addRow(this, g, "Title:", tfTitle, 0);
                addRow(this, g, "Description:", new JScrollPane(taDesc), 1);
                addRow(this, g, "Technologies:", tfTech, 2);
                setBorder(BorderFactory.createEtchedBorder());
            }
            private void addRow(JPanel p, GridBagConstraints g, String label, JComponent c, int y) {
                g.gridx=0; g.gridy=y; p.add(new JLabel(label), g);
                g.gridx=1; p.add(c, g);
            }
        }

        private void addRow(JPanel p, GridBagConstraints g, String label, JComponent c, int y) {
            g.gridx=0; g.gridy=y; p.add(new JLabel(label), g);
            g.gridx=1; p.add(c, g);
        }

        /*private void uploadPhoto() {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg","jpeg","png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                photoPath = fc.getSelectedFile().getAbsolutePath();
                try {
                    BufferedImage img = ImageIO.read(new File(photoPath));
                    Image scaled = img.getScaledInstance(150,150,Image.SCALE_SMOOTH);
                    lblPhoto.setIcon(new ImageIcon(scaled));
                    lblPhoto.setText("");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Cannot load image");
                }
            }
        }*/

        private void loadCVToForm() {
            tfName.setText(cv.fullName); tfEmail.setText(cv.email);
            tfPhone.setText(cv.phone); tfAddress.setText(cv.address);
            tfDegree.setText(cv.degree); tfInstitute.setText(cv.institute);
            tfYear.setText(cv.passingYear); tfCgpa.setText(cv.cgpa);
            tfCompany.setText(cv.company); tfPosition.setText(cv.position);
            tfFrom.setText(cv.durationFrom); tfTo.setText(cv.durationTo);
            tfResp.setText(cv.responsibilities);
            /*photoPath = cv.photoPath;
            if (photoPath != null && new File(photoPath).exists()) {
                try {
                    BufferedImage img = ImageIO.read(new File(photoPath));
                    Image scaled = img.getScaledInstance(150,150,Image.SCALE_SMOOTH);
                    lblPhoto.setIcon(new ImageIcon(scaled));
                    lblPhoto.setText("");
                } catch (Exception ignored) {}
            }*/

            skillModel.clear();
            cv.skills.forEach(skillModel::addElement);

            // Clear and reload projects safely
            projectsContainer.removeAll();
            projectPanels.clear();
            projectsContainer.add(btnAddProject);

            for (CV.Project pr : cv.projects) {
                ProjectPanel pp = new ProjectPanel();
                pp.tfTitle.setText(pr.title);
                pp.taDesc.setText(pr.description);
                pp.tfTech.setText(pr.technologies);
                projectPanels.add(pp);
                projectsContainer.add(pp, projectsContainer.getComponentCount() - 1);
            }

            projectsContainer.revalidate();
            projectsContainer.repaint();
        }

        private void saveCV() {
            if (!validateForm()) return;

            cv.fullName = tfName.getText().trim();
            cv.email = tfEmail.getText().trim();
            cv.phone = tfPhone.getText().trim();
            cv.address = tfAddress.getText().trim();
            //cv.photoPath = photoPath;

            cv.degree = tfDegree.getText().trim();
            cv.institute = tfInstitute.getText().trim();
            cv.passingYear = tfYear.getText().trim();
            cv.cgpa = tfCgpa.getText().trim();

            cv.company = tfCompany.getText().trim();
            cv.position = tfPosition.getText().trim();
            cv.durationFrom = tfFrom.getText().trim();
            cv.durationTo = tfTo.getText().trim();
            cv.responsibilities = tfResp.getText().trim();
            cv.totalExperienceYears = ExperienceCalculator.calculate(cv.durationFrom, cv.durationTo);

            cv.skills.clear();
            Enumeration<String> en = skillModel.elements();
            while (en.hasMoreElements()) cv.skills.add(en.nextElement());

            cv.projects.clear();
            for (ProjectPanel pp : projectPanels) {
                String title = pp.tfTitle.getText().trim();
                if (!title.isEmpty()) {
                    CV.Project p = new CV.Project();
                    p.title = title;
                    p.description = pp.taDesc.getText().trim();
                    p.technologies = pp.tfTech.getText().trim();
                    cv.projects.add(p);
                }
            }

            List<CV> list = JsonFileManager.loadCVs();
            list.removeIf(c -> c.fullName.equals(cv.fullName));
            list.add(cv);
            JsonFileManager.saveCVs(list);
            JOptionPane.showMessageDialog(this, "CV Saved Successfully!");
        }

        private boolean validateForm() {
            if (tfName.getText().trim().isEmpty() || tfEmail.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Email required!");
                return false;
            }
            if (!Validator.isValidEmail(tfEmail.getText().trim())) {
                JOptionPane.showMessageDialog(this, "Invalid email!");
                return false;
            }
            return true;
        }

        private void clearForm() {
            if (JOptionPane.showConfirmDialog(this, "Clear all fields?") == JOptionPane.YES_OPTION) {
                dispose();
                new CVBuilderForm(null);
            }
        }
    }

    /* ==================== ADMIN DASHBOARD ==================== */
    static class AdminDashboard extends JFrame {
        private DefaultTableModel tableModel;
        private JTable table;
        // New: Tracking current sort field
        private CVSorter.SortField currentSortField = CVSorter.SortField.NAME;
        // FIX: Member variable for the CV Count Label
        private JLabel lblTotalCVs;

        AdminDashboard() {
            setTitle("Admin Dashboard");
            setSize(1100, 650);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            String[] cols = {"Name", "Email", "CGPA", "Exp (yrs)", "Skills", "Projects"};
            tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; }};
            table = new JTable(tableModel);
            // Allows interactive table header sorting, works alongside our custom sort
            table.setAutoCreateRowSorter(true); 

            JPanel top = new JPanel();
            
            // FIX: Initialize the member variable
            lblTotalCVs = new JLabel("Total CVs: 0"); 
            top.add(lblTotalCVs);
            
            // New: Sort ComboBox
            JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Name (A-Z)", "Experience (High-Low)", "CGPA (High-Low)"});
            sortCombo.addActionListener(e -> {
                String selected = (String) sortCombo.getSelectedItem();
                if ("Experience (High-Low)".equals(selected)) {
                    currentSortField = CVSorter.SortField.EXPERIENCE;
                } else if ("CGPA (High-Low)".equals(selected)) {
                    currentSortField = CVSorter.SortField.CGPA;
                } else {
                    currentSortField = CVSorter.SortField.NAME;
                }
                loadTable(); // Reload table with new sorting
            });
            top.add(new JLabel("Sort by:"));
            top.add(sortCombo);
            
            JButton btnOpen = new JButton("Open CV");
            JButton btnPdf = new JButton("Export PDF");
            btnOpen.addActionListener(e -> openSelectedCV());
            btnPdf.addActionListener(e -> exportSelectedPDF());

            top.add(btnOpen); top.add(btnPdf);
            top.add(new JButton("Logout") {{ addActionListener(e -> { dispose(); new LoginFrame(); }); }});
            
            loadTable(); // Load the table data and update the label for the first time

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            setVisible(true);
        }

        private void loadTable() {
            // Load CVs and sort them using the currently selected field
            List<CV> allCVs = JsonFileManager.loadCVs();
            List<CV> sortedCVs = CVSorter.sort(allCVs, currentSortField);

            tableModel.setRowCount(0);
            for (CV cv : sortedCVs) {
                tableModel.addRow(new Object[]{
                    cv.fullName,
                    cv.email,
                    safeCGPA(cv.cgpa),
                    String.format("%.1f", cv.totalExperienceYears),
                    cv.skills.size(),
                    cv.projects.size()
                });
            }
            
            // FIX: Update the JLabel directly after loading data
            lblTotalCVs.setText("Total CVs: " + tableModel.getRowCount());
        }
        
        // Helper to format CGPA for display
        private String safeCGPA(String cgpa) {
            if (cgpa == null || cgpa.trim().isEmpty()) return "-";
            return cgpa;
        }

        private void openSelectedCV() {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Please select a CV"); return; }
            // Convert view row index to model row index if the table is sorted by the JTable RowSorter
            int modelRow = table.convertRowIndexToModel(viewRow);

            String name = (String) tableModel.getValueAt(modelRow, 0);
            CV cv = JsonFileManager.loadCVs().stream()
                    .filter(c -> c.fullName.equals(name))
                    .findFirst().orElse(null);
            if (cv != null) new CVBuilderForm(cv);
        }

        private void exportSelectedPDF() {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) return;
            // Convert view row index to model row index if the table is sorted by the JTable RowSorter
            int modelRow = table.convertRowIndexToModel(viewRow);

            String name = (String) tableModel.getValueAt(modelRow, 0);
            CV cv = JsonFileManager.loadCVs().stream()
                    .filter(c -> c.fullName.equals(name))
                    .findFirst().orElse(null);
            if (cv != null) CVExporter.generate(cv);
        }
    }

    /* ==================== UTILS ==================== */
    static class JsonFileManager {
        static List<CV> loadCVs() {
            try {
                if (!Files.exists(Paths.get(CV_FILE))) return new ArrayList<>();
                String json = Files.readString(Paths.get(CV_FILE));
                return gson.fromJson(json, new TypeToken<List<CV>>(){}.getType());
            } catch (Exception e) { return new ArrayList<>(); }
        }

        static void saveCVs(List<CV> list) {
            try {
                Files.writeString(Paths.get(CV_FILE), gson.toJson(list));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    static class Validator {
        static boolean isValidEmail(String email) {
            return Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$").matcher(email).matches();
        }
    }

    static class ExperienceCalculator {
        static double calculate(String from, String to) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                Date d1 = sdf.parse(from);
                Date d2 = "Present".equalsIgnoreCase(to.trim()) ? new Date() : sdf.parse(to);
                return (d2.getTime() - d1.getTime()) / (1000.0 * 60 * 60 * 24 * 365.25);
            } catch (Exception e) { return 0.0; }
        }
    }

    // New: Class for sorting logic
    static class CVSorter {
        public enum SortField {
            NAME, EXPERIENCE, CGPA
        }

        public static List<CV> sort(List<CV> cvList, SortField field) {
            List<CV> sortedList = new ArrayList<>(cvList);

            Comparator<CV> comparator = switch (field) {
                case NAME -> Comparator.comparing(cv -> cv.fullName);
                case EXPERIENCE -> Comparator.comparingDouble(cv -> cv.totalExperienceYears);
                case CGPA -> (cv1, cv2) -> {
                    double cgpa1 = safeParseDouble(cv1.cgpa);
                    double cgpa2 = safeParseDouble(cv2.cgpa);
                    // Compare numerically (reversed later)
                    return Double.compare(cgpa1, cgpa2); 
                };
            };

            // Sort in descending order for numerical fields (Exp and CGPA)
            if (field == SortField.EXPERIENCE || field == SortField.CGPA) {
                sortedList.sort(comparator.reversed());
            } else {
                // Sort ascending for Name
                sortedList.sort(comparator);
            }

            return sortedList;
        }

        private static double safeParseDouble(String value) {
            if (value == null || value.trim().isEmpty()) return 0.0;
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
    }
    // End of CVSorter

    /* ==================== PDF EXPORTER ==================== */
    static class CVExporter {
        static void generate(CV cv) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(cv.fullName + "_CV.pdf"));
            if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            try {
                PdfWriter.getInstance(doc, new FileOutputStream(fc.getSelectedFile()));
                doc.open();

                /*if (cv.photoPath != null && new File(cv.photoPath).exists()) {
                    Image img = Image.getInstance(cv.photoPath);
                    img.scaleToFit(110, 110);
                    img.setAlignment(Element.ALIGN_LEFT);
                    doc.add(img);
                }*/

                com.lowagie.text.Font title = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 26, com.lowagie.text.Font.BOLD);
                com.lowagie.text.Font heading = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
                com.lowagie.text.Font normal = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11);

                doc.add(new Paragraph("Personal Information: ", title));
                doc.add(new Paragraph("Name: "+cv.fullName , normal));
                doc.add(new Paragraph("Email: " + cv.email + " | Phone: " + cv.phone, normal));
                doc.add(new Paragraph("Address: " + cv.address, normal));
                doc.add(Chunk.NEWLINE);

                doc.add(new Paragraph("EDUCATION", heading));
                doc.add(new Paragraph(cv.degree + " - " + cv.institute, normal));
                doc.add(new Paragraph("Year: " + cv.passingYear + " | CGPA: " + cv.cgpa, normal));
                doc.add(Chunk.NEWLINE);

                doc.add(new Paragraph("EXPERIENCE", heading));
                doc.add(new Paragraph(cv.position + " at " + cv.company, normal));
                doc.add(new Paragraph("Duration: " + cv.durationFrom + " to " + cv.durationTo, normal));
                doc.add(new Paragraph("Total: " + String.format("%.1f years", cv.totalExperienceYears), normal));
                doc.add(Chunk.NEWLINE);

                doc.add(new Paragraph("SKILLS", heading));
                doc.add(new Paragraph(String.join(" • ", cv.skills), normal));
                doc.add(Chunk.NEWLINE);

                doc.add(new Paragraph("PROJECTS", heading));
                for (CV.Project p : cv.projects) {
                    doc.add(new Paragraph(p.title, new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD)));
                    doc.add(new Paragraph("Description: " + p.description, normal));
                    doc.add(new Paragraph("Technologies: " + p.technologies, normal));
                    doc.add(Chunk.NEWLINE);
                }

                doc.close();
                JOptionPane.showMessageDialog(null, "PDF Generated Successfully!");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to generate PDF");
            }
        }
    }
}