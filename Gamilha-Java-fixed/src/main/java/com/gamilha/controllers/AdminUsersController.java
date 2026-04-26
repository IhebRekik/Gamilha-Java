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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String>  colAvatar;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRoles;
    @FXML private TableColumn<User, Boolean> colActive;
    @FXML private TableColumn<User, Void>    colActions;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortFilter;

    // Pagination
    @FXML private Label  paginationInfo;
    @FXML private HBox   pageNumbers;
    @FXML private Button btnFirst;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button btnLast;

    private static final int PAGE_SIZE = 5;
    private int currentPage = 1;

    private final UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User>   filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();

        // ✅ Remplir les ComboBox AVANT de charger les users
        roleFilter.getItems().addAll("Tous", "USER", "ADMIN");
        roleFilter.setValue("Tous");

        statusFilter.getItems().addAll("Tous", "Actif", "Inactif");
        statusFilter.setValue("Tous");

        if (sortFilter != null) {
            sortFilter.getItems().addAll("Tri par défaut", "Nom A→Z", "Nom Z→A");
            sortFilter.setValue("Tri par défaut");
        }

        // ✅ Charger les users APRÈS avoir rempli les ComboBox
        loadUsers();

        // Listeners
        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; applyFilters(); });
        roleFilter.valueProperty().addListener((obs, o, n)  -> { currentPage = 1; applyFilters(); });
        statusFilter.valueProperty().addListener((obs, o, n) -> { currentPage = 1; applyFilters(); });

        // ✅ Listener du tri (manquait) => applique le tri
        if (sortFilter != null) {
            sortFilter.valueProperty().addListener((obs, o, n) -> { currentPage = 1; updateTablePage(); });
        }
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // ✅ IMPORTANT: alimenter la colonne roles (sinon la cellFactory reçoit null)
        colRoles.setCellValueFactory(new PropertyValueFactory<>("roles"));

        // Avatar
        colAvatar.setCellValueFactory(new PropertyValueFactory<>("profileImage"));
        colAvatar.setCellFactory(tc -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(40); iv.setFitHeight(40); }
            @Override protected void updateItem(String imgPath, boolean empty) {
                super.updateItem(imgPath, empty);
                if (empty) { setGraphic(null); return; }

                Image image = null;
                try {
                    if (imgPath != null && !imgPath.isBlank()) {
                        Path p = ImageStorage.resolveToPath(imgPath);
                        if (p != null) {
                            image = new Image(p.toUri().toString(), 40, 40, true, true);
                        }
                    }
                    if (image == null) {
                        var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                        if (is != null) image = new Image(is, 40, 40, true, true);
                    }

                    if (image != null) {
                        iv.setImage(image);
                        iv.setClip(new Circle(20, 20, 20));
                        setGraphic(iv);
                    } else {
                        setGraphic(new Label("👤"));
                    }
                } catch (Exception e) {
                    Label ph = new Label("👤");
                    ph.setStyle("-fx-font-size:22px;");
                    setGraphic(ph);
                }
            }
        });

        // Rôle
        colRoles.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String roles, boolean empty) {
                super.updateItem(roles, empty);
                if (empty || roles == null) { setGraphic(null); setText(null); return; }
                boolean isAdmin = roles.contains("ADMIN");
                Label badge = new Label(isAdmin ? "ADMIN" : "USER");
                badge.setStyle(isAdmin
                        ? "-fx-background-color:#eab308;-fx-text-fill:black;-fx-padding:4 12;-fx-background-radius:20;"
                        : "-fx-background-color:#6366f1;-fx-text-fill:white;-fx-padding:4 12;-fx-background-radius:20;");
                setText(null);
                setGraphic(badge);
            }
        });

        // Statut
        colActive.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) { setGraphic(null); return; }
                Label lbl = new Label(active ? "✅ Actif" : "⛔ Inactif");
                lbl.setStyle(active
                        ? "-fx-background-color:#22c55e;-fx-text-fill:white;-fx-padding:4 12;-fx-background-radius:20;"
                        : "-fx-background-color:#ef4444;-fx-text-fill:white;-fx-padding:4 12;-fx-background-radius:20;");
                setGraphic(lbl);
            }
        });

        // Actions
        colActions.setCellFactory(createActions());
    }

    private Callback<TableColumn<User, Void>, TableCell<User, Void>> createActions() {
        return col -> new TableCell<>() {
            private final Button btnView   = makeBtn("👁", "#1d4ed8");
            private final Button btnEdit   = makeBtn("✏", "#7c3aed");
            private final Button btnDelete = makeBtn("🗑", "#dc2626");

            {
                btnView.setOnAction(e   -> showDetail(getTableRow().getItem()));
                btnEdit.setOnAction(e   -> editUser(getTableRow().getItem()));
                btnDelete.setOnAction(e -> deleteUser(getTableRow().getItem()));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox hbox = new HBox(6, btnView, btnEdit, btnDelete);
                hbox.setAlignment(Pos.CENTER);
                setGraphic(hbox);
            }
        };
    }

    private Button makeBtn(String icon, String color) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                "-fx-background-radius:8;-fx-padding:5 9;-fx-cursor:hand;-fx-font-size:13px;");
        return b;
    }

    private void loadUsers() {
        allUsers.clear();
        try { allUsers.addAll(userService.findAll()); } catch (Exception e) { e.printStackTrace(); }
        filteredList = new FilteredList<>(allUsers, p -> true);
        currentPage = 1;
        applyFilters();
    }

    private void applyFilters() {
        if (filteredList == null) return; // ✅ sécurité

        // ✅ Utiliser "Tous".equals(role) au lieu de role.equals("Tous") → évite NullPointerException
        String search = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";
        String role   = (roleFilter   != null) ? roleFilter.getValue()   : null;
        String status = (statusFilter != null) ? statusFilter.getValue() : null;

        filteredList.setPredicate(user -> {
            // Filtre recherche
            boolean searchOk = search.isEmpty()
                    || user.getName().toLowerCase().contains(search)
                    || user.getEmail().toLowerCase().contains(search);

            // ✅ Filtre rôle — "Tous".equals(role) fonctionne même si role est null
            boolean roleOk = role == null
                    || "Tous".equals(role)
                    || ("ADMIN".equals(role) && user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN"))
                    || ("USER".equals(role)  && user.getRoles() != null && !user.getRoles().contains("ROLE_ADMIN"));

            // ✅ Filtre statut — même logique null-safe
            boolean statusOk = status == null
                    || "Tous".equals(status)
                    || ("Actif".equals(status)   && user.isActive())
                    || ("Inactif".equals(status) && !user.isActive());

            return searchOk && roleOk && statusOk;
        });

        updateTablePage();
    }

    // ── PAGINATION ──────────────────────────────────────────────────
    private void updateTablePage() {
        if (filteredList == null) return;

        // 1) Base: éléments filtrés
        List<User> all = new ArrayList<>(filteredList);

        // 2) Tri selon le choix
        if (sortFilter != null) {
            String sort = sortFilter.getValue();
            if ("Nom A→Z".equals(sort)) {
                all.sort(Comparator.comparing(u -> safeLower(u.getName())));
            } else if ("Nom Z→A".equals(sort)) {
                all.sort(Comparator.comparing((User u) -> safeLower(u.getName())).reversed());
            }
            // "Tri par défaut" => ne rien faire (ordre DB)
        }

        int total      = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)          currentPage = 1;

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        usersTable.setItems(FXCollections.observableArrayList(all.subList(from, to)));

        if (paginationInfo != null)
            paginationInfo.setText("Affichage de " + (total == 0 ? 0 : from + 1)
                    + " à " + to + " sur " + total + " utilisateurs");

        if (btnFirst != null) {
            btnFirst.setDisable(currentPage <= 1);
            btnPrev.setDisable(currentPage  <= 1);
            btnNext.setDisable(currentPage  >= totalPages);
            btnLast.setDisable(currentPage  >= totalPages);
        }

        if (pageNumbers != null) {
            pageNumbers.getChildren().clear();
            int start = Math.max(1, currentPage - 2);
            int end   = Math.min(totalPages, start + 4);
            if (end - start < 4) start = Math.max(1, end - 4);

            for (int i = start; i <= end; i++) {
                final int page = i;
                Button pb = new Button(String.valueOf(i));
                if (i == currentPage) {
                    pb.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;" +
                            "-fx-background-radius:8;-fx-padding:5 10;-fx-font-weight:bold;");
                } else {
                    pb.setStyle("-fx-background-color:#21273a;-fx-text-fill:white;" +
                            "-fx-background-radius:8;-fx-padding:5 10;-fx-cursor:hand;");
                    pb.setOnAction(e -> { currentPage = page; updateTablePage(); });
                }
                pageNumbers.getChildren().add(pb);
            }
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    @FXML private void goToFirstPage() { currentPage = 1; updateTablePage(); }
    @FXML private void goToPrevPage()  { if (currentPage > 1) { currentPage--; updateTablePage(); } }
    @FXML private void goToNextPage()  { currentPage++; updateTablePage(); }
    @FXML private void goToLastPage()  {
        if (filteredList == null) return;
        currentPage = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        updateTablePage();
    }

    // ── ACTIONS FXML ────────────────────────────────────────────────
    @FXML private void searchUsers()     { currentPage = 1; applyFilters(); }
    @FXML private void handleRefresh()   { loadUsers(); if (searchField != null) searchField.clear(); }
    @FXML private void openNewUserForm() { loadContent("/com/gamilha/interfaces/Admin/add_user.fxml"); }

    @FXML private void handleEditUser() {
        User s = usersTable.getSelectionModel().getSelectedItem();
        if (s != null) editUser(s);
    }
    @FXML private void handleDeleteUser() {
        User s = usersTable.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Erreur", "Sélectionnez un utilisateur", Alert.AlertType.WARNING); return; }
        deleteUser(s);
    }

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

    private void deleteUser(User user) {
        if (user == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + user.getName() + " ?");
        alert.setTitle("Confirmation");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) { userService.delete(user.getId()); loadUsers(); }
        });
    }

    // ── NAVIGATION ──────────────────────────────────────────────────
    private BorderPane getContentArea(){

        return (BorderPane)

                usersTable

                        .getScene()

                        .lookup("#contentArea");

    }



    void loadContent(String fxmlPath){

        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    fxmlPath

                            )

                    );


            Parent content =
                    loader.load();


            BorderPane area =
                    getContentArea();


            if(area != null)

                area.setCenter(content);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }



    private void loadWithController(

            String path,

            java.util.function.Consumer<Object> consumer

    ){

        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(path)

                    );


            Parent content =
                    loader.load();


            consumer.accept(

                    loader.getController()

            );


            BorderPane area =
                    getContentArea();


            if(area != null)

                area.setCenter(content);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

