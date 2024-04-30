package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
// MS75 Added Import 4-29-24
import java.io.File;
import java.io.IOException;
// MS75 Added Import 4-29-24

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
// MS75 Added Import 4-29-24
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
// MS75 Added Import 4-29-24
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.ClientUtils;
import Project.Client.ICardControls;

public class ChatPanel extends JPanel {
    private static Logger logger = Logger.getLogger(ChatPanel.class.getName());
    private JPanel chatArea = null;
    private UserListPanel userListPanel;

    public ChatPanel(ICardControls controls) {
        super(new BorderLayout(10, 10));
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        // wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // no need to add content specifically because scroll wraps it
        wrapper.add(scroll);
        this.add(wrapper, BorderLayout.CENTER);

        JPanel input = new JPanel();
        input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
        JTextField textValue = new JTextField();
        input.add(textValue);
        JButton button = new JButton("Send");
    // MS75 4-29-24
    // New export button to export chat history using Java File chooser and java printwriter
    // as well as java simple date format to have unique filename
    JButton export = new JButton("Export Chat");
    input.add(export);
    export.addActionListener((event) -> {
    JFileChooser select = new JFileChooser();
    int x = select.showSaveDialog(this);
    if (x == JFileChooser.APPROVE_OPTION) {
        File file = select.getSelectedFile();
        
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_hhmm a");
        date.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String timeStamp = date.format(new Date());
        String fileNameWithTimestamp = "chatHistory_" + timeStamp;
        File fileWithTimestamp = new File(file.getParent(), fileNameWithTimestamp);
            try (PrintWriter print = new PrintWriter(fileWithTimestamp)) {
                for (Component component : chatArea.getComponents()) {
                    if (component instanceof JEditorPane) {
                        JEditorPane pane = (JEditorPane) component;
                        String text = pane.getText();
                        print.println(text.trim());
                        print.println("\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });


        // lets us submit with the enter key instead of just the button click
        textValue.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    button.doClick();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }

        });
        button.addActionListener((event) -> {
            try {
                String text = textValue.getText().trim();
                if (text.length() > 0) {
                    Client.INSTANCE.sendMessage(text);
                    textValue.setText("");// clear the original text

                    // debugging
                    logger.log(Level.FINEST, "Content: " + content.getSize());
                    logger.log(Level.FINEST, "Parent: " + this.getSize());

                }
            } catch (NullPointerException e) {
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        chatArea = content;
        input.add(button);
        userListPanel = new UserListPanel();
        this.add(userListPanel, BorderLayout.EAST);
        this.add(input, BorderLayout.SOUTH);
        this.setName(CardView.CHAT.name());
        controls.addPanel(CardView.CHAT.name(), this);
        chatArea.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {

                if (chatArea.isVisible()) {
                    chatArea.revalidate();
                    chatArea.repaint();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (chatArea.isVisible()) {
                    chatArea.revalidate();
                    chatArea.repaint();
                }
            }

        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // System.out.println("Resized to " + e.getComponent().getSize());
                // rough concepts for handling resize
                // set the dimensions based on the frame size
                Dimension frameSize = wrapper.getParent().getParent().getSize();
                int w = (int) Math.ceil(frameSize.getWidth() * .3f);

                userListPanel.setPreferredSize(new Dimension(w, (int) frameSize.getHeight()));
                userListPanel.revalidate();
                userListPanel.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // System.out.println("Moved to " + e.getComponent().getLocation());
            }
        });
    }
    // MS75 4-29-24 Added lastMessageHighlight method that takes the clientID paramaeter
    // and calls the highlightUser method in the userListPanel.java file
    public void lastMessageHighlight(long clientId){
        userListPanel.highlightUser(clientId);
    }
    public void addUserListItem(long clientId, String clientName) {
        userListPanel.addUserListItem(clientId, clientName);
    }

    public void removeUserListItem(long clientId) {
        userListPanel.removeUserListItem(clientId);
    }

    public void clearUserList() {
        userListPanel.clearUserList();
    }
// New Code Milestone 3 MS75 4-27-24 
// Custom Formatting Feature 
    private String messageProcessor(String message) {
        message = message.replaceAll("\\*(.*?)\\*", "<b>$1</b>");

        message = message.replaceAll("\\-(.*?)\\-", "<i>$1</i>");

        message = message.replaceAll("\\_(.*?)\\_", "<u>$1</u>");

        message = message.replaceAll("\\#r(.*?)\\#", "<font color='red'>$1</font>");
        message = message.replaceAll("\\#b(.*?)\\#", "<font color='blue'>$1</font>");
        message = message.replaceAll("\\#g(.*?)\\#", "<font color='green'>$1</font>");

        message = message.replaceAll("\\*\\-(.*?)\\-\\*", "<b><i><u>$1</u></i></b>");
        message = message.replaceAll("\\-\\*(.*?)\\*\\-", "<i><u><font color='red'>$1</font></u></i>");

        return message;
    }
    public void addText(String text) {
        JPanel content = chatArea;
        // New Code Milestone 3 MS75 4-27-24 
        // Custom Formatting Feature 
        text = messageProcessor(text);
        // add message
        // New Code Milestone 3 MS75 4-27-24 
        // Custom Formatting Feature 
        JEditorPane textContainer = new JEditorPane("text/html", "<html>" + text + "</html>");

        // sizes the panel to attempt to take up the width of the container
        // and expand in height based on word wrapping
        textContainer.setLayout(null);
        textContainer.setPreferredSize(
                new Dimension(content.getWidth(), ClientUtils.calcHeightForText(this, text, content.getWidth())));
        textContainer.setMaximumSize(textContainer.getPreferredSize());
        textContainer.setEditable(false);
        ClientUtils.clearBackground(textContainer);
        // add to container and tell the layout to revalidate
        content.add(textContainer);
        // scroll down on new message
        JScrollBar vertical = ((JScrollPane) chatArea.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
}

