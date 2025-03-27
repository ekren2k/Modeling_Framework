
import core.Controller;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ModellingFrameworkUI extends JFrame {

private JList<String> modelList;
private JList<String> dataList;
private JTable dataTable;
private DefaultTableModel tableModel;
private Controller controller;
public Path res = Paths.get(System.getProperty("user.dir"), "src/main/resources");

public ModellingFrameworkUI() {
    setTitle("Modelling Framework Sample");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());

    modelList = new JList<>(new Reflections("models", new SubTypesScanner(false))
            .getSubTypesOf(Object.class)
            .stream().map(Class::getSimpleName)
            .toArray(String[]::new));
    modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    leftPanel.add(new JScrollPane(modelList), BorderLayout.WEST);


    File folder = res.toFile();
    File[] listOfFiles = folder.listFiles(file -> file.getName().endsWith(".txt"));
    String[] dataFiles = listOfFiles != null ? new String[listOfFiles.length] : null;

    for (int i = 0; i < listOfFiles.length; i++)
        dataFiles[i] = listOfFiles[i].getName();

    dataList = new JList<>(dataFiles);
    dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    leftPanel.add(new JScrollPane(dataList), BorderLayout.CENTER);

    JButton runModelButton = new JButton("Run model");
    leftPanel.add(runModelButton, BorderLayout.SOUTH);

    add(leftPanel, BorderLayout.WEST);

    tableModel = new DefaultTableModel();
    dataTable = new JTable(tableModel);

    JScrollPane tableScrollPane = new JScrollPane(dataTable);
    add(tableScrollPane, BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

    JButton runScriptFileButton = new JButton("Run script from file");
    JButton runAdhocScriptButton = new JButton("Create and run ad-hoc script");

    bottomPanel.add(runScriptFileButton);
    bottomPanel.add(runAdhocScriptButton);

    add(bottomPanel, BorderLayout.SOUTH);

    runModelButton.addActionListener(e -> runModelAction());

    runScriptFileButton.addActionListener(e -> runScriptFromFileAction());

    runAdhocScriptButton.addActionListener(e -> createAndRunAdhocScript());

    setSize(800, 600);
    setLocationRelativeTo(null);
    setVisible(true);
}

private void runModelAction() {
    String selectedModel = modelList.getSelectedValue();
    String selectedData = dataList.getSelectedValue();

    if (selectedModel != null && selectedData != null) {
        try {
            controller = new Controller("models."+selectedModel);
            controller.readDataFrom(res+"/"+selectedData).runModel();
            updateTable(controller.getResultAsTSV());
        } catch (ClassNotFoundException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException | IOException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select both a model and a data file.");
    }
}

private void runScriptFromFileAction() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select Script File");

    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

    fileChooser.setFileFilter(new FileNameExtensionFilter("Groovy Scripts", "groovy"));

    int result = fileChooser.showOpenDialog(null);

    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        try {
            controller.runScriptFromFile(selectedFile.getAbsolutePath());
            updateTable(controller.getResultAsTSV());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    } else {
        System.out.println("File selection was canceled.");
    }
}

private void createAndRunAdhocScript() {
    if (controller != null) {
        JTextArea scriptArea = new JTextArea(10, 40);
        int result = JOptionPane.showConfirmDialog(this,
                new JScrollPane(scriptArea),
                "Enter Ad-hoc Script",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String script = scriptArea.getText();
            try {
                controller.runScript(script);
                updateTable(controller.getResultAsTSV());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error executing script: " + e.getMessage());
            }
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a model and input data before running a script.");
    }
}

private void updateTable(String tsvData) {
    tableModel.setRowCount(0);

    String[] rows = tsvData.split("\n");
    String[] headers = rows[0].split("\t");

    tableModel.setColumnIdentifiers(headers);

    for (int i = 1; i < rows.length; i++) {
        String[] data = rows[i].split("\t");
        String[] formattedData = formatData(data);

        tableModel.addRow(formattedData);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();

        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col = 1; col < dataTable.getColumnCount(); col++) {
            dataTable.getColumnModel().getColumn(col).setCellRenderer(rightRenderer);
        }
    }
}

private String[] formatData(String[] data) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pl"));

    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator(' ');

    DecimalFormat bigNumberFormat = new DecimalFormat("#,##0.0", symbols);
    DecimalFormat smallNumberFormat = new DecimalFormat("#0.00", symbols);
    DecimalFormat verySmallNumberFormat = new DecimalFormat("0.000", symbols);
    DecimalFormat integerFormat = new DecimalFormat("#,##0", symbols);

    String digitPattern = "^([\\d]+,\\d{2})0$";

    bigNumberFormat.setGroupingUsed(true);
    String[] formattedData = new String[data.length];
    for (int i = 0; i < data.length; i++) {
        if (i == 0) formattedData[i] = data[i];
        else {
            double num = Double.parseDouble(data[i]);
            if (num < 1) {
                formattedData[i] = verySmallNumberFormat.format(num);
                formattedData[i] = formattedData[i].replaceAll(digitPattern, "$1");
            } else if (num < 10) formattedData[i] = smallNumberFormat.format(num);
            else {
                formattedData[i] = bigNumberFormat.format(num);
                if (formattedData[i].endsWith(",0")) {
                    formattedData[i] = integerFormat.format(num);
                }
            }
        }
    }
    return formattedData;
}

public static void main(String[] args) {
    SwingUtilities.invokeLater(ModellingFrameworkUI::new);
    }
}

