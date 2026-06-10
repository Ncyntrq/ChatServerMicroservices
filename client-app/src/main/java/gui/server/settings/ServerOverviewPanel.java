package gui.server.settings;

import gui.components.FormField;
import gui.components.PrimaryButton;
import gui.theme.AppColors;
import network.ApiException;
import network.ServerApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ServerOverviewPanel extends JPanel {

    private final ServerApiClient serverApi = new ServerApiClient();
    private final long serverId;
    private final Runnable onChange;

    private String uploadedIconUrl;
    private final FormField nameField;
    private final JTextArea descArea;
    private final JLabel statusLabelOverview;

    public ServerOverviewPanel(long serverId, Runnable onChange) {
        this.serverId = serverId;
        this.onChange = onChange;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppColors.BG_PRIMARY);

        JLabel title = new JLabel("Server Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(AppColors.TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(20));

        nameField = new FormField("SERVER NAME", "Enter server name", false);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(nameField);
        add(Box.createVerticalStrut(15));

        JLabel descLabel = new JLabel("DESCRIPTION");
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        descLabel.setForeground(AppColors.TEXT_MUTED);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(descLabel);
        add(Box.createVerticalStrut(5));

        descArea = new JTextArea(3, 30);
        descArea.setBackground(AppColors.BG_TERTIARY);
        descArea.setForeground(AppColors.TEXT_NORMAL);
        descArea.setCaretColor(AppColors.TEXT_WHITE);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descScroll.setBorder(BorderFactory.createEmptyBorder());
        add(descScroll);
        add(Box.createVerticalStrut(15));

        statusLabelOverview = new JLabel(" ");
        statusLabelOverview.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabelOverview.setForeground(AppColors.TEXT_MUTED);
        statusLabelOverview.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton uploadBtn = new PrimaryButton("Upload Server Icon", e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (JPEG, PNG)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fc.getSelectedFile();
                statusLabelOverview.setForeground(AppColors.TEXT_MUTED);
                statusLabelOverview.setText("Uploading icon...");
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() {
                        return new network.FileApiClient().uploadAvatar(file);
                    }
                    @Override protected void done() {
                        try {
                            uploadedIconUrl = get();
                            statusLabelOverview.setForeground(AppColors.SUCCESS);
                            statusLabelOverview.setText("Icon uploaded successfully! Click Save Changes.");
                        } catch(Exception ex) {
                            statusLabelOverview.setForeground(AppColors.DANGER);
                            statusLabelOverview.setText("Upload error: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
        });
        uploadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(uploadBtn);
        add(Box.createVerticalStrut(15));

        PrimaryButton saveBtn = new PrimaryButton("Save Changes", e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                statusLabelOverview.setForeground(AppColors.DANGER);
                statusLabelOverview.setText("Server name cannot be empty");
                return;
            }
            String desc = descArea.getText().trim();
            statusLabelOverview.setForeground(AppColors.TEXT_MUTED);
            statusLabelOverview.setText("Saving...");

            new SwingWorker<Map<String, Object>, Void>() {
                @Override protected Map<String, Object> doInBackground() {
                    return serverApi.updateServer(serverId, name, desc.isEmpty() ? null : desc, uploadedIconUrl);
                }
                @Override protected void done() {
                    try {
                        get();
                        statusLabelOverview.setForeground(AppColors.SUCCESS);
                        statusLabelOverview.setText("Changes saved successfully!");
                        if (onChange != null) onChange.run();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() instanceof ApiException ? ex.getCause() : ex;
                        statusLabelOverview.setForeground(AppColors.DANGER);
                        statusLabelOverview.setText("Error: " + cause.getMessage());
                    }
                }
            }.execute();
        });
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(saveBtn);
        add(Box.createVerticalStrut(10));
        add(statusLabelOverview);

        loadServerDetails();
    }

    private void loadServerDetails() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() {
                return serverApi.getServerDetails(serverId);
            }
            @Override protected void done() {
                try {
                    Map<String, Object> response = get();
                    Map<String, Object> details = (Map<String, Object>) response.get("server");
                    if (details == null) details = response;

                    String loadedName = str(details.get("name"));
                    String loadedDesc = str(details.get("description"));
                    uploadedIconUrl = str(details.get("icon"));

                    if (loadedName != null) nameField.setText(loadedName);
                    if (loadedDesc != null) descArea.setText(loadedDesc);
                } catch (Exception ex) {
                    statusLabelOverview.setForeground(AppColors.WARNING);
                    statusLabelOverview.setText("Failed to load server details");
                }
            }
        }.execute();
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
