import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

public class ImageCompressionTool extends JFrame {
    private JTextField resolutionField;
    private JTextField qualityField;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JLabel previewLabel;
    private File selectedFile;
    private File outputFolder;
    private long originalFileSize = 0;

    public ImageCompressionTool() {
        setTitle("smallJPG");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        Color bgColor = new Color(30, 30, 30);
        Color panelColor = new Color(45, 45, 45);
        Color textColor = Color.WHITE;
        Color accentColor = new Color(0, 122, 255);
        Font font = new Font("Segoe UI", Font.PLAIN, 18);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ---------- Side Panel ----------
        JPanel sidePanel = new JPanel();
        sidePanel.setBackground(panelColor);
        sidePanel.setLayout(new BorderLayout(20, 20));
        sidePanel.setPreferredSize(new Dimension(350, getHeight()));

        // Top Panel for Logo + Inputs
        JPanel topPanel = new JPanel();
        topPanel.setBackground(panelColor);
        topPanel.setLayout(new BorderLayout(10, 10));

        // Logo Panel
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(panelColor);

        try {
            BufferedImage logoImg = ImageIO.read(new File("resources/logo.png"));
            Image scaledLogo = logoImg.getScaledInstance(300, -1, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoPanel.add(logoLabel);
        } catch (IOException e) {
            JLabel fallback = new JLabel("Logo Missing", SwingConstants.CENTER);
            fallback.setForeground(Color.RED);
            fallback.setFont(font);
            logoPanel.add(fallback);
        }

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBackground(panelColor);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel resLabel = new JLabel("Max Pixels:");
        resLabel.setForeground(textColor);
        resLabel.setFont(font);

        resolutionField = new JTextField("800");
        resolutionField.setFont(font);

        JLabel qualityLabel = new JLabel("Quality (0-1):");
        qualityLabel.setForeground(textColor);
        qualityLabel.setFont(font);

        qualityField = new JTextField("0.7");
        qualityField.setFont(font);

        KeyAdapter liveUpdateListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateEstimatedSize();
            }
        };
        resolutionField.addKeyListener(liveUpdateListener);
        qualityField.addKeyListener(liveUpdateListener);

        inputPanel.add(resLabel);
        inputPanel.add(resolutionField);
        inputPanel.add(qualityLabel);
        inputPanel.add(qualityField);

        // Add logo + inputs to top panel
        topPanel.add(logoPanel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonPanel.setBackground(panelColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        FlatButton selectButton = new FlatButton("Select Image", accentColor, font);
        selectButton.addActionListener(e -> selectImage());

        FlatButton outputFolderButton = new FlatButton("Select Output Folder", accentColor, font);
        outputFolderButton.addActionListener(e -> selectOutputFolder());

        FlatButton compressButton = new FlatButton("Compress", accentColor, font);
        compressButton.addActionListener(e -> compressImage());

        buttonPanel.add(selectButton);
        buttonPanel.add(outputFolderButton);
        buttonPanel.add(compressButton);

        // Bottom Info Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(panelColor);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        statusLabel = new JLabel("No file selected", SwingConstants.CENTER);
        statusLabel.setForeground(textColor);
        statusLabel.setFont(font);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setFont(font);
        progressBar.setForeground(accentColor);

        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        // Add sections to side panel
        sidePanel.add(topPanel, BorderLayout.NORTH);
        sidePanel.add(buttonPanel, BorderLayout.CENTER);
        sidePanel.add(bottomPanel, BorderLayout.SOUTH);

        // ---------- Preview ----------
        previewLabel = new JLabel("Preview will appear here", SwingConstants.CENTER);
        previewLabel.setForeground(Color.LIGHT_GRAY);
        previewLabel.setFont(new Font("Segoe UI", Font.ITALIC, 22));
        previewLabel.setBackground(Color.BLACK);
        previewLabel.setOpaque(true);

        JScrollPane previewScroll = new JScrollPane(previewLabel);
        previewScroll.setBackground(Color.BLACK);
        previewScroll.getViewport().setBackground(Color.BLACK);

        mainPanel.add(sidePanel, BorderLayout.WEST);
        mainPanel.add(previewScroll, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            originalFileSize = selectedFile.length();
            statusLabel.setText("Selected: " + selectedFile.getName() +
                    " | Original Size: " + formatFileSize(originalFileSize));
            updateEstimatedSize();
            updatePreview();
        }
    }

    private void updatePreview() {
        if (selectedFile != null) {
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                Image scaled = img.getScaledInstance(previewLabel.getWidth(), -1, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
                previewLabel.setText(null);
            } catch (IOException e) {
                previewLabel.setText("Preview not available");
            }
        }
    }

    private void selectOutputFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (selectedFile != null) {
            folderChooser.setCurrentDirectory(selectedFile.getParentFile());
        } else {
            folderChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        int option = folderChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            outputFolder = folderChooser.getSelectedFile();
            statusLabel.setText("Output Folder: " + outputFolder.getAbsolutePath());
        }
    }

    private void updateEstimatedSize() {
        if (selectedFile == null) return;
        try {
            int maxDim = Integer.parseInt(resolutionField.getText());
            float quality = Float.parseFloat(qualityField.getText());
            BufferedImage originalImage = ImageIO.read(selectedFile);

            int newWidth = originalImage.getWidth();
            int newHeight = originalImage.getHeight();
            if (newWidth > newHeight && newWidth > maxDim) {
                newHeight = (maxDim * newHeight) / newWidth;
                newWidth = maxDim;
            } else if (newHeight >= newWidth && newHeight > maxDim) {
                newWidth = (maxDim * newWidth) / newHeight;
                newHeight = maxDim;
            }

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
            long estimatedSize = estimateCompressedSize(resizedImage, quality);
            statusLabel.setText("Original: " + formatFileSize(originalFileSize) +
                    " | Estimated: " + formatFileSize(estimatedSize));
        } catch (Exception ignored) {}
    }

    private void compressImage() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select an image first.");
            return;
        }

        try {
            int maxDim = Integer.parseInt(resolutionField.getText());
            float quality = Float.parseFloat(qualityField.getText());

            BufferedImage originalImage = ImageIO.read(selectedFile);

            int newWidth = originalImage.getWidth();
            int newHeight = originalImage.getHeight();

            if (newWidth > newHeight && newWidth > maxDim) {
                newHeight = (maxDim * newHeight) / newWidth;
                newWidth = maxDim;
            } else if (newHeight >= newWidth && newHeight > maxDim) {
                newWidth = (maxDim * newWidth) / newHeight;
                newHeight = maxDim;
            }

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            long estimatedSize = estimateCompressedSize(resizedImage, quality);
            statusLabel.setText("Original: " + formatFileSize(originalFileSize) +
                    " | Estimated: " + formatFileSize(estimatedSize));

            progressBar.setValue(30);

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegParams.setCompressionQuality(quality);

            File saveDir = (outputFolder != null) ? outputFolder : selectedFile.getParentFile();
            File compressedFile = new File(saveDir,
                    selectedFile.getName().replaceFirst("(\\.[^.]+)?$", "_compressed.jpg"));

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(compressedFile)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resizedImage, null, null), jpegParams);
                writer.dispose();
            }

            progressBar.setValue(100);
            statusLabel.setText("Compression complete: " + compressedFile.getName() +
                    " | Size: " + formatFileSize(compressedFile.length()));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.2f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z) + "");
    }

    private long estimateCompressedSize(BufferedImage image, float quality) {
        int bytesPerPixel = 3;
        long rawSize = (long) image.getWidth() * image.getHeight() * bytesPerPixel;
        return (long) (rawSize * quality * 0.25);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageCompressionTool::new);
    }
}

/**
 * Custom flat button with rounded corners, hover effect, and drop shadow
 */
class FlatButton extends JButton {
    private Color baseColor;
    private Color hoverColor;
    private boolean hovering = false;

    public FlatButton(String text, Color color, Font font) {
        super(text);
        this.baseColor = color;
        this.hoverColor = color.brighter();
        setFont(font);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setMargin(new Insets(10, 20, 10, 20));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 20, 20);

        // Button background
        g2.setColor(hovering ? hoverColor : baseColor);
        g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 20, 20);

        // Text
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
