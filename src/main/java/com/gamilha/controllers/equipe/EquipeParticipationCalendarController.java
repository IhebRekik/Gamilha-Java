package com.gamilha.controllers.equipe;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.User;
import com.gamilha.services.EvenementService;
import com.gamilha.utils.QrCodeUtil;
import com.gamilha.utils.SessionContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipeParticipationCalendarController extends BaseController {
    private final EvenementService evenementService = new EvenementService();
    private NavigationCallback nav;

    public void setNav(NavigationCallback nav) {
        this.nav = nav;
    }

    public Node build() {
        VBox root = pageScaffold("Calendrier des participations", "");

        Button back = new Button("Retour aux Equipes");
        back.setOnAction(e -> {
            if (nav != null) {
                nav.navigateTo("equipes_list");
            }
        });

        User user = SessionContext.getCurrentUser();
        if (user == null) {
            root.getChildren().addAll(back, new Label("Session utilisateur introuvable."));
            return root;
        }

        List<EvenementService.EquipeParticipation> participations =
                evenementService.findParticipationsByUser(user.getId());

        Calendar calendar = new Calendar("Participations");
        Map<Entry<?>, EvenementService.EquipeParticipation> entryParticipationMap = new HashMap<>();

        for (EvenementService.EquipeParticipation participation : participations) {
            LocalDate start = participation.getDateDebut();
            if (start == null) {
                continue;
            }

            LocalDate end = participation.getDateFin() == null ? start : participation.getDateFin();
            Entry<String> entry = new Entry<>(participation.getEquipeNom() + " - " + participation.getEvenementNom());
            entry.changeStartDate(start);
            entry.changeEndDate(end);
            entry.setFullDay(true);

            calendar.addEntry(entry);
            entryParticipationMap.put(entry, participation);
        }

        CalendarSource source = new CalendarSource("Mes Equipes");
        source.getCalendars().add(calendar);

        CalendarView calendarView = new CalendarView();
        calendarView.getStyleClass().add("participation-calendar");
        calendarView.showMonthPage();
        calendarView.getCalendarSources().setAll(source);
        calendarView.setEntryDetailsPopOverContentCallback(param ->
                buildParticipationDetails(entryParticipationMap.get(param.getEntry()), user));
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        root.getChildren().addAll(back, calendarView);
        return root;
    }

    private Node buildParticipationDetails(EvenementService.EquipeParticipation participation, User user) {
        if (participation == null) {
            return new Label("Participation introuvable.");
        }

        VBox content = new VBox(12);
        content.getStyleClass().add("qr-invitation-card");
        content.setPadding(new Insets(16));
        content.setPrefWidth(360);
        content.setMaxWidth(420);

        Label title = new Label(participation.getEvenementNom());
        title.getStyleClass().add("entity-card-title");
        title.setWrapText(true);

        Label team = new Label("Equipe: " + nullSafe(participation.getEquipeNom()));
        Label type = new Label("Type: " + nullSafe(participation.getTypeEvenement()));
        Label game = new Label("Jeu: " + nullSafe(participation.getJeu()));
        Label dates = new Label("Dates: " + formatDate(participation.getDateDebut())
                + " - " + formatDate(participation.getDateFin()));
        Label player = new Label("Participant: " + nullSafe(user.getName()) + " <" + nullSafe(user.getEmail()) + ">");

        team.getStyleClass().add("entity-card-line");
        type.getStyleClass().add("entity-card-line");
        game.getStyleClass().add("entity-card-line");
        dates.getStyleClass().add("entity-card-line");
        player.getStyleClass().add("entity-card-line");

        content.getChildren().addAll(title, team, type, game, dates, player);

        if (!isOffline(participation)) {
            Label info = new Label("Cet evenement est online. Le QR code d'invitation n'est affiche que pour les evenements offline.");
            info.getStyleClass().add("entity-card-line");
            info.setWrapText(true);
            content.getChildren().add(info);
            return content;
        }

        Label qrTitle = new Label("Invitation QR");
        qrTitle.getStyleClass().add("section-title");

        ImageView qrView = new ImageView(QrCodeUtil.generate(buildInvitationPayload(participation, user), 240));
        qrView.setFitWidth(240);
        qrView.setFitHeight(240);
        qrView.setPreserveRatio(true);

        StackPane qrBox = new StackPane(qrView);
        qrBox.getStyleClass().add("qr-image-box");
        qrBox.setPadding(new Insets(14));
        qrBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label hint = new Label("Presentez ce code QR comme invitation de votre equipe pour cet evenement offline.");
        hint.getStyleClass().add("entity-card-line");
        hint.setWrapText(true);

        HBox qrRow = new HBox(qrBox);
        qrRow.setAlignment(Pos.CENTER);

        content.getChildren().addAll(qrTitle, qrRow, hint);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefWidth(400);
        scrollPane.getStyleClass().add("qr-invitation-scroll");
        return scrollPane;
    }

    private boolean isOffline(EvenementService.EquipeParticipation participation) {
        return participation.getTypeEvenement() != null
                && "offline".equalsIgnoreCase(participation.getTypeEvenement().trim());
    }

    private String buildInvitationPayload(EvenementService.EquipeParticipation participation, User user) {
        return """
                invitation_type=evenement_offline
                evenement_nom=%s
                equipe_nom=%s
                participant_nom=%s
                participant_email=%s
                jeu=%s
                statut=%s
                date_debut=%s
                date_fin=%s
                """.formatted(
                nullSafe(participation.getEvenementNom()),
                nullSafe(participation.getEquipeNom()),
                nullSafe(user.getName()),
                nullSafe(user.getEmail()),
                nullSafe(participation.getJeu()),
                nullSafe(participation.getStatut()),
                formatDate(participation.getDateDebut()),
                formatDate(participation.getDateFin())
        );
    }
}
