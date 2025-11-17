package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import scissors.ScissorsSelectionModel;
import selector.SelectionModel.SelectionState;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Add status bar
        statusLabel = new JLabel("NO_SELECTION");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        frame.add(statusLabel, BorderLayout.SOUTH);

        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();

        /// Giving the image scroll bars with suggested parameters
        JScrollPane scrollPane = new JScrollPane(imgPanel);
        scrollPane.setPreferredSize(new Dimension(400, 700));

        frame.add(scrollPane, BorderLayout.CENTER);

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        frame.add(makeControlPanel(), BorderLayout.EAST);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel controlPanel = new JPanel();
        GridLayout controlLayout = new GridLayout(0, 1);
        controlPanel.setLayout(controlLayout);
        cancelButton = new JButton("Cancel");
        controlPanel.add(cancelButton);
        undoButton = new JButton("Undo");
        controlPanel.add(undoButton);
        resetButton = new JButton("Reset");
        controlPanel.add(resetButton);
        finishButton = new JButton("Finish");
        controlPanel.add(finishButton);

        // Controller: Attach control panel item listeners
        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());

        String[] select = {"Point-to-point", "Intelligent scissors: gray", "Intelligent scissors: color detail",
                "Intelligent scissors: luminance"};
        JComboBox<String> box = new JComboBox<>(select);
        box.setSelectedIndex(0);
        controlPanel.add(box);
        box.addActionListener(e -> {
            if (model != null && box.getSelectedIndex() == 0) {
                SelectionModel copy = new PointToPointSelectionModel(getSelectionModel());
                setSelectionModel(copy);
            } else if (model != null && box.getSelectedIndex() == 1) {
                SelectionModel scissors = new ScissorsSelectionModel("CrossGradMono"
                        , getSelectionModel());
                setSelectionModel(scissors);
            } else if (model != null && box.getSelectedIndex() == 2) {
                SelectionModel colorWeigher = new ScissorsSelectionModel("ColorWeigher",
                        getSelectionModel());
                setSelectionModel(colorWeigher);
            } else if (model != null && box.getSelectedIndex() == 3) {
                SelectionModel luminanceWeigher = new ScissorsSelectionModel("LuminanceWeigher",
                        getSelectionModel());
                setSelectionModel(luminanceWeigher);
            }
        });
        return controlPanel;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include: * "state":
     * Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            reflectSelectionState(model.state());
            if (model.state() == PROCESSING) {
                processingProgress.setIndeterminate(true);
            } else {
                processingProgress.setIndeterminate(false);
                processingProgress.setValue(0);
            }
        }

        if ("progress".equals(evt.getPropertyName())) {
            Integer newProgress = (Integer) evt.getNewValue();
            processingProgress.setIndeterminate(false);
            processingProgress.setValue(newProgress);
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());
        cancelButton.setEnabled(state == PROCESSING);
        undoButton.setEnabled(state != NO_SELECTION);
        resetButton.setEnabled(state != NO_SELECTION);
        finishButton.setEnabled(state == SELECTING);
        saveItem.setEnabled(state == SELECTED);
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);
        // New in A6: Listen for "progress" events
        model.addPropertyChangeListener("progress", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());

    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(
                new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));

        BufferedImage image = null;
        while (image == null) {
            /// Implementing open file functionality
            int returnVal = chooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String imageDir =
                        chooser.getCurrentDirectory() + "\\" + chooser.getSelectedFile().getName();
                try {
                    image = ImageIO.read(chooser.getSelectedFile());
                    if (image == null) {
                        throw new IOException();
                    }
                    this.setImage(image);
                } catch (IOException e) {
                    /// Draw an error message if the file is unable to be read.
                    JOptionPane.showMessageDialog(frame,
                            "Unable to read image located at " + imageDir,
                            "Unsupported image format", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                return;
            }
        }
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog. Show
     * an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        // Start browsing in current directory filter for png files.
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        // Re-show the save dialog if the user does not intend to over-write a file.
        boolean repeat = true;
        while (repeat) {

            int returnVal = chooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    // Add .png if name does not end in it
                    String fileName = file.getCanonicalPath();
                    if (!fileName.endsWith(".png")) {
                        file = new File(fileName + ".png");
                    }
                    // Confirm if user would like to override file
                    if (file.exists()) {
                        int choice = JOptionPane.showConfirmDialog(frame,
                                "File already exists. Do you want to overwrite it?",
                                "Overwrite Confirmation", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (choice == 0) {
                            OutputStream out = new FileOutputStream(file);
                            model.saveSelection(out);
                            out.close();
                            repeat = false;
                        }
                    } else {
                        OutputStream out = new FileOutputStream(file);
                        model.saveSelection(out);
                        out.close();
                        repeat = false;
                    }
                } catch (IOException e) {
                    /// M: Draw an error message if the file is unable to be saved or read.
                    JOptionPane.showMessageDialog(frame, "Error writing file: " + e.getMessage(),
                            e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                repeat = false;
            }
        }
    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}