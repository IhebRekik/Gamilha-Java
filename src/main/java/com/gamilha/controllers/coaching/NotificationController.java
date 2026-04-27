package com.gamilha.controllers.coaching;

import com.gamilha.MainFX;
import com.gamilha.entity.AppNotification;
import com.gamilha.entity.User;
import com.gamilha.services.NotificationService;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationController {

    @FXML private VBox notificationsBox;
    @FXML private Label lblUnreadInfo;

    private final NotificationService notificationService = new NotificationService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        Integer userId = getCurrentUserId();
        int unreadCount = notificationService.countUnread(userId);
        lblUnreadInfo.setText(unreadCount + " notification(s) non lue(s)");

        List<AppNotification> notifications = notificationService.getRecentNotifications(userId, 30);
        notificationsBox.getChildren().clear();

        if (notifications.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label icon = new Label("🔔");
            icon.setStyle("-fx-font-size:38;-fx-text-fill:#38bdf8;");
            Label message = new Label("Aucune notification recente.");
            message.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            empty.getChildren().addAll(icon, message);
            notificationsBox.getChildren().add(empty);
        } else {
            for (AppNotification notification : notifications) {
                notificationsBox.getChildren().add(createNotificationCard(notification));
            }
        }

        notificationService.markAllAsRead(userId);
        MainFX.refreshNavigationBadges();
    }

    private VBox createNotificationCard(AppNotification notification) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color:" + (notification.isRead() ? "#1e293b" : "#172554") + ";"
                        + "-fx-background-radius:12;-fx-border-color:#334155;-fx-border-radius:12;"
        );

        Label title = new Label(notification.isRead() ? "Notification recente" : "Nouvelle notification");
        title.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:12;-fx-font-weight:bold;");

        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill:white;-fx-font-size:14;");

        Label date = new Label(
                notification.getCreatedAt() != null
                        ? formatter.format(notification.getCreatedAt())
                        : "-"
        );
        date.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");

        card.getChildren().addAll(title, message, date);
        return card;
    }

    private Integer getCurrentUserId() {
        User user = SessionContext.getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
