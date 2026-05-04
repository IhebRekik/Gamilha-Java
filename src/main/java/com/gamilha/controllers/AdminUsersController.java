package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminUsersController implements Initializable {

    @FXML private TableView<User>        usersTable;
    @FXML private TableColumn<User, String>  colAvatar;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRoles;
    @FXML private TableColumn<User, Boolean> colActive;
    @FXML private TableColumn<User, String>  colPresence;
    @FXML private TableColumn<User, Void>    colActions;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private Label  paginationInfo;
    @FXML private HBox   pageNumbers;
    @FXML private Button btnFirst, btnPrev, btnNext, btnLast;

    private static final int PAGE_SIZE = 5;
    private int currentPage = 1;

    private final UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User>   filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();

        roleFilter.getItems().addAll("Tous les rôles", "Utilisateur", "Administrateur");
        roleFilter.setValue("Tous les rôles");
        statusFilter.getItems().addAll("Tous les statuts", "Actif", "Inactif");
        statusFilter.setValue("Tous les statuts");
        if (sortFilter != null) {
            sortFilter.getItems().addAll("Tri par défaut", "Nom A→Z", "Nom Z→A");
            sortFilter.setValue("Tri par défaut");
        }

        loadUsers();

        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; applyFilters(); });
        roleFilter.valueProperty().addListener((obs, o, n)  -> { currentPage = 1; applyFilters(); });
        statusFilter.valueProperty().addListener((obs, o, n) -> { currentPage = 1; applyFilters(); });
        if (sortFilter != null) {
            sortFilter.valueProperty().addListener((obs, o, n) -> { currentPage = 1; updateTablePage(); });
        }
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));
        colAvatar.setCellValueFactory(new PropertyValueFactory<>("profileImage"));

        // ── Avatar ──────────────────────────────────────────────────
        colAvatar.setCellFactory(tc -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(38); iv.setFitHeight(38); }
            @Override protected void updateItem(String imgPath, boolean empty) {
                super.updateItem(imgPath, empty);
                if (empty) { setGraphic(null); return; }
                try {
                    Image image = null;
                    if (imgPath != null && !imgPath.isBlank()) {
                        Path p = ImageStorage.resolveToPath(imgPath);
                        if (p != null) image = new Image(p.toUri().toString(), 38, 38, true, true);
                    }
                    if (image == null) {
                        var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                        if (is != null) image = new Image(is, 38, 38, true, true);
                    }
                    if (image != null) {
                        iv.setImage(image);
                        iv.setClip(new Circle(19, 19, 19));
                        setGraphic(iv);
                    } else {
                        setGraphic(new Label("👤"));
                    }
                } catch (Exception e) {
                    setGraphic(new Label("👤"));
                }
            }
        });

        // ── Rôle ────────────────────────────────────────────────────
        colRoles.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String roles, boolean empty) {
                super.updateItem(roles, empty);
                if (empty || roles == null) { setGraphic(null); setText(null); return; }
                boolean isAdmin = roles.contains("ADMIN");
                Label badge = new Label(isAdmin ? "ADMIN" : "USER");
                badge.setStyle(isAdmin
                        ? "-fx-background-color:#eab308;-fx-text-fill:black;-fx-padding:4 12;-fx-background-radius:20;-fx-font-weight:bold;"
                        : "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-padding:4 12;-fx-background-radius:20;");
                setText(null); setGraphic(badge);
            }
        });

        // ── Statut ──────────────────────────────────────────────────
        colActive.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) { setGraphic(null); return; }
                User u = getTableRow() != null ? getTableRow().getItem() : null;
                boolean isBanned = u != null && u.getBanUntil() != null && !u.getBanUntil().isEmpty();
                Label lbl;
                if (isBanned) {
                    lbl = new Label("🚫 Banni");
                    lbl.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-padding:4 10;-fx-background-radius:20;");
                } else {
                    lbl = new Label(active ? "✅ Actif" : "⛔ Inactif");
                    lbl.setStyle(active
                            ? "-fx-background-color:#22c55e;-fx-text-fill:white;-fx-padding:4 10;-fx-background-radius:20;"
                            : "-fx-background-color:#ef4444;-fx-text-fill:white;-fx-padding:4 10;-fx-background-radius:20;");
                }
                setGraphic(lbl);
            }
        });

        // ── Présence (en ligne / hors ligne) ────────────────────────
        if (colPresence != null) {
            colPresence.setCellValueFactory(new PropertyValueFactory<>("presenceLabel"));
            colPresence.setCellFactory(tc -> new TableCell<>() {
                @Override protected void updateItem(String label, boolean empty) {
                    super.updateItem(label, empty);
                    if (empty || label == null) { setGraphic(null); return; }
                    Label l = new Label(label);
                    if (label.contains("En ligne")) {
                        l.setStyle("-fx-text-fill:#22c55e;-fx-font-weight:bold;-fx-font-size:12px;");
                    } else if (label.contains("Vu il y a")) {
                        l.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;");
                    } else {
                        l.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");
                    }
                    setGraphic(l);
                }
            });
        }

        // ── Actions ─────────────────────────────────────────────────
        colActions.setCellFactory(createActions());
    }

    // ── ACTIONS COLUMN ──────────────────────────────────────────────
    private Callback<TableColumn<User, Void>, TableCell<User, Void>> createActions() {
        return col -> new TableCell<>() {
            private final Button btnView   = styledBtn("👁",  "#0891b2", "#0e7490");
            private final Button btnEdit   = styledBtn("✏",  "#d97706", "#b45309");
            private final Button btnToggle = styledBtn("⊙",  "#4b5563", "#374151");
            private final Button btnBan    = styledBtn("🚫", "#9333ea", "#7c3aed");
            private final Button btnDelete = styledBtn("🗑",  "#dc2626", "#b91c1c");

            {
                btnView.setTooltip(new Tooltip("Voir profil"));
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnToggle.setTooltip(new Tooltip("Activer / Désactiver"));
                btnBan.setTooltip(new Tooltip("Bannir"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                btnView.setOnAction(e   -> showDetail(getTableRow().getItem()));
                btnEdit.setOnAction(e   -> editUser(getTableRow().getItem()));
                btnToggle.setOnAction(e -> confirmToggle(getTableRow().getItem()));
                btnBan.setOnAction(e    -> showBanDialog(getTableRow().getItem()));
                btnDelete.setOnAction(e -> confirmDelete(getTableRow().getItem()));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox hbox = new HBox(4, btnView, btnEdit, btnToggle, btnBan, btnDelete);
                hbox.setAlignment(Pos.CENTER);
                setGraphic(hbox);
            }
        };
    }

    private Button styledBtn(String icon, String bg, String hover) {
        Button b = new Button(icon);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                "-fx-background-radius:8;-fx-padding:5 8;-fx-cursor:hand;-fx-font-size:13px;";
        String hov  = "-fx-background-color:" + hover + ";-fx-text-fill:white;" +
                "-fx-background-radius:8;-fx-padding:5 8;-fx-cursor:hand;-fx-font-size:13px;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hov));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    // ── TOGGLE ACTIVE CONFIRM ────────────────────────────────────────
    private void confirmToggle(User user) {
        if (user == null) return;
        boolean willDeactivate = user.isActive();
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(willDeactivate ? "Désactiver le compte" : "Activer le compte");

        VBox root = new VBox(18);
        root.setPadding(new Insets(32)); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#1a1e2e;");

        Label ico = new Label(willDeactivate ? "🔕" : "✅");
        ico.setFont(Font.font(52));

        Label msg = new Label("Êtes-vous sûr de vouloir " + (willDeactivate ? "désactiver" : "activer") +
                " le compte de\n" + user.getName() + " ?");
        msg.setTextFill(Color.WHITE); msg.setFont(Font.font("System", FontWeight.BOLD, 15));
        msg.setWrapText(true);

        Label warn = new Label(willDeactivate
                ? "ℹ L'utilisateur ne pourra plus se connecter après désactivation."
                : "ℹ L'utilisateur pourra de nouveau se connecter.");
        warn.setTextFill(Color.web("#fbbf24")); warn.setWrapText(true);
        warn.setStyle("-fx-background-color:#1f1800;-fx-padding:12;-fx-background-radius:8;");

        HBox btns = new HBox(12); btns.setAlignment(Pos.CENTER);
        Button cancel  = modalBtn("✕ Annuler", "#374151");
        Button confirm = modalBtn(willDeactivate ? "🔕 Confirmer la désactivation" : "✅ Confirmer l'activation",
                willDeactivate ? "#b45309" : "#15803d");
        cancel.setOnAction(e -> dialog.close());
        confirm.setOnAction(e -> {
            userService.toggleActive(user.getId(), !willDeactivate);
            dialog.close(); loadUsers();
        });
        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(ico, msg, warn, btns);
        dialog.setScene(new Scene(root, 460, 310)); dialog.show();
    }

    // ── BAN DIALOG ──────────────────────────────────────────────────
    private void showBanDialog(User user) {
        if (user == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Bannir " + user.getName());

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#1a1e2e;-fx-border-color:#dc2626;-fx-border-width:2;" +
                "-fx-border-radius:12;-fx-background-radius:12;");

        Label title = new Label("🚫 Bannir " + user.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#ef4444"));

        Label sub = new Label("△  Choisissez la durée du bannissement pour " + user.getName());
        sub.setTextFill(Color.web("#94a3b8"));

        Label durLabel = new Label("Durée prédéfinie");
        durLabel.setTextFill(Color.WHITE);
        durLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        final String[] selectedBan = {null};
        VBox presetBox = new VBox(8);

        String[] labels = {"⏱ 1 jour", "⏱ 2 jours", "⏱ 3 jours", "⏱ 7 jours", "⏱ 30 jours", "🚫 Permanent"};
        int[]    ddays  = {1, 2, 3, 7, 30, -1};

        for (int i = 0; i < labels.length; i++) {
            final int dd = ddays[i]; final String lbl = labels[i];
            Button btn = new Button(lbl);
            btn.setPrefWidth(320);
            String normalStyle = "-fx-background-color:#0d1117;-fx-text-fill:#fbbf24;-fx-background-radius:8;" +
                    "-fx-padding:10;-fx-cursor:hand;-fx-border-color:#374151;-fx-border-radius:8;-fx-font-size:13px;";
            String selStyle = "-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-background-radius:8;" +
                    "-fx-padding:10;-fx-cursor:hand;-fx-border-color:#7c3aed;-fx-border-radius:8;" +
                    "-fx-font-size:13px;-fx-font-weight:bold;";
            btn.setStyle(normalStyle);
            btn.setOnAction(e -> {
                if (dd == -1) selectedBan[0] = "9999-12-31 23:59:59";
                else selectedBan[0] = LocalDateTime.now().plusDays(dd)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                presetBox.getChildren().forEach(n -> ((Button) n).setStyle(normalStyle));
                btn.setStyle(selStyle);
            });
            presetBox.getChildren().add(btn);
        }

        Label orLbl = new Label("— OU —");
        orLbl.setTextFill(Color.web("#94a3b8"));

        Label customLbl = new Label("📅 Date personnalisée");
        customLbl.setTextFill(Color.WHITE);
        customLbl.setFont(Font.font("System", FontWeight.BOLD, 13));

        DatePicker dp = new DatePicker();
        dp.setPromptText("jj/mm/aaaa"); dp.setPrefWidth(320);
        dp.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectedBan[0] = n.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                presetBox.getChildren().forEach(nd -> {
                    String ns = "-fx-background-color:#0d1117;-fx-text-fill:#fbbf24;-fx-background-radius:8;" +
                            "-fx-padding:10;-fx-cursor:hand;-fx-border-color:#374151;-fx-border-radius:8;-fx-font-size:13px;";
                    ((Button) nd).setStyle(ns);
                });
            }
        });
        Label dpInfo = new Label("Bannir jusqu'à une date précise");
        dpInfo.setTextFill(Color.web("#64748b")); dpInfo.setFont(Font.font(12));

        HBox btns = new HBox(12); btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel  = modalBtn("✕ Annuler", "#374151");
        Button confirm = modalBtn("🚫 Confirmer le ban", "#dc2626");
        cancel.setOnAction(e -> dialog.close());
        confirm.setOnAction(e -> {
            if (selectedBan[0] == null) { return; }
            userService.banUser(user.getId(), selectedBan[0]);
            dialog.close(); loadUsers();
        });
        btns.getChildren().addAll(cancel, confirm);

        root.getChildren().addAll(title, sub, durLabel, presetBox, orLbl, customLbl, dp, dpInfo, btns);
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        dialog.setScene(new Scene(sp, 400, 580)); dialog.show();
    }

    // ── DELETE CONFIRM ───────────────────────────────────────────────
    private void confirmDelete(User user) {
        if (user == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer l'utilisateur");

        VBox root = new VBox(18);
        root.setPadding(new Insets(32)); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#1a1e2e;");

        Label ico = new Label("⚠");
        ico.setFont(Font.font(52)); ico.setTextFill(Color.web("#ef4444"));

        Label msg = new Label("Êtes-vous sûr de vouloir supprimer définitivement\n" + user.getName() + " ?");
        msg.setTextFill(Color.WHITE); msg.setFont(Font.font("System", FontWeight.BOLD, 15));
        msg.setWrapText(true);

        Label warn = new Label("△ Attention : Cette action est irréversible. Toutes les données de\n" +
                "l'utilisateur (posts, commentaires, abonnements, etc.) seront perdues.\n\nEmail : " + user.getEmail());
        warn.setTextFill(Color.web("#fca5a5")); warn.setWrapText(true);
        warn.setStyle("-fx-background-color:#2d1111;-fx-padding:12;-fx-background-radius:8;");

        HBox btns = new HBox(12); btns.setAlignment(Pos.CENTER);
        Button cancel  = modalBtn("✕ Annuler", "#374151");
        Button confirm = modalBtn("🗑 Oui, supprimer définitivement", "#dc2626");
        cancel.setOnAction(e -> dialog.close());
        confirm.setOnAction(e -> {
            userService.delete(user.getId()); dialog.close(); loadUsers();
        });
        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(ico, msg, warn, btns);
        dialog.setScene(new Scene(root, 490, 370)); dialog.show();
    }

    private Button modalBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;-fx-background-radius:8;" +
                "-fx-padding:10 20;-fx-cursor:hand;-fx-font-weight:bold;");
        return b;
    }

    // ── DATA ────────────────────────────────────────────────────────
    private void loadUsers() {
        allUsers.clear();
        try { allUsers.addAll(userService.findAll()); } catch (Exception e) { e.printStackTrace(); }
        filteredList = new FilteredList<>(allUsers, p -> true);
        currentPage = 1;
        applyFilters();
    }

    private void applyFilters() {
        if (filteredList == null) return;
        String search = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";
        String role   = roleFilter   != null ? roleFilter.getValue()   : null;
        String status = statusFilter != null ? statusFilter.getValue() : null;

        filteredList.setPredicate(user -> {
            boolean searchOk = search.isEmpty()
                    || (user.getName() != null && user.getName().toLowerCase().contains(search))
                    || (user.getEmail() != null && user.getEmail().toLowerCase().contains(search));
            boolean roleOk = role == null || role.contains("Tous")
                    || (role.contains("Administrateur") && user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN"))
                    || (role.contains("Utilisateur") && user.getRoles() != null && !user.getRoles().contains("ROLE_ADMIN"));
            boolean statusOk = status == null || status.contains("Tous")
                    || ("Actif".equals(status) && user.isActive())
                    || ("Inactif".equals(status) && !user.isActive());
            return searchOk && roleOk && statusOk;
        });
        updateTablePage();
    }

    private void updateTablePage() {
        if (filteredList == null) return;
        List<User> all = new ArrayList<>(filteredList);
        if (sortFilter != null) {
            String sort = sortFilter.getValue();
            if ("Nom A→Z".equals(sort)) all.sort(Comparator.comparing(u -> safeLower(u.getName())));
            else if ("Nom Z→A".equals(sort)) all.sort(Comparator.comparing((User u) -> safeLower(u.getName())).reversed());
        }
        int total      = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        usersTable.setItems(FXCollections.observableArrayList(all.subList(from, to)));

        if (paginationInfo != null)
            paginationInfo.setText("Affichage de " + (total == 0 ? 0 : from + 1) + " à " + to + " sur " + total + " utilisateurs");

        if (btnFirst != null) {
            btnFirst.setDisable(currentPage <= 1); btnPrev.setDisable(currentPage <= 1);
            btnNext.setDisable(currentPage >= totalPages); btnLast.setDisable(currentPage >= totalPages);
        }
        if (pageNumbers != null) {
            pageNumbers.getChildren().clear();
            int start = Math.max(1, currentPage - 2);
            int end   = Math.min(totalPages, start + 4);
            if (end - start < 4) start = Math.max(1, end - 4);
            for (int i = start; i <= end; i++) {
                final int page = i;
                Button pb = new Button(String.valueOf(i));
                pb.setStyle(i == currentPage
                        ? "-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:5 10;-fx-font-weight:bold;"
                        : "-fx-background-color:#21273a;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:5 10;-fx-cursor:hand;");
                if (i != currentPage) pb.setOnAction(e -> { currentPage = page; updateTablePage(); });
                pageNumbers.getChildren().add(pb);
            }
        }
    }

    private static String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }

    @FXML private void goToFirstPage() { currentPage = 1; updateTablePage(); }
    @FXML private void goToPrevPage()  { if (currentPage > 1) { currentPage--; updateTablePage(); } }
    @FXML private void goToNextPage()  { currentPage++; updateTablePage(); }
    @FXML private void goToLastPage()  {
        if (filteredList == null) return;
        currentPage = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        updateTablePage();
    }

    @FXML private void searchUsers()     { currentPage = 1; applyFilters(); }
    @FXML private void handleRefresh()   { loadUsers(); if (searchField != null) searchField.clear(); }
    @FXML private void openNewUserForm() { loadContent("/com/gamilha/interfaces/Admin/add_user.fxml"); }

    private void showDetail(User user) {
        if (user == null) return;
        loadWithController("/com/gamilha/interfaces/Admin/user-detail.fxml",
                c -> ((UserDetailController) c).setUser(user));
    }
    private void editUser(User user) {
        if (user == null) return;
        loadWithController("/com/gamilha/interfaces/Admin/edit_user.fxml",
                c -> ((EditUserController) c).setUser(user));
    }

    // ── NAVIGATION ──────────────────────────────────────────────────
    private BorderPane getContentArea() {
        return (BorderPane) usersTable.getScene().lookup("#contentArea");
    }
    void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            BorderPane area = getContentArea();
            if (area != null) area.setCenter(content);
        } catch (IOException e) { e.printStackTrace(); }
    }
    private void loadWithController(String path, java.util.function.Consumer<Object> consumer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent content = loader.load();
            consumer.accept(loader.getController());
            BorderPane area = getContentArea();
            if (area != null) area.setCenter(content);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
