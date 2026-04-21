package com.gamilha.controllers.equipe;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.User;
import com.gamilha.services.EvenementService;
import com.gamilha.utils.SessionContext;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

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
        for (EvenementService.EquipeParticipation p : participations) {
            LocalDate start = p.getDateDebut();
            if (start == null) {
                continue;
            }
            LocalDate end = p.getDateFin() == null ? start : p.getDateFin();
            Entry<String> entry = new Entry<>(
                    p.getEquipeNom() + " - " + p.getEvenementNom()
            );
            entry.changeStartDate(start);
            entry.changeEndDate(end);
            entry.setFullDay(true);
            calendar.addEntry(entry);
        }

        CalendarSource source = new CalendarSource("Mes Equipes");
        source.getCalendars().add(calendar);

        CalendarView calendarView = new CalendarView();
        calendarView.getStyleClass().add("participation-calendar");
        calendarView.showMonthPage();
        calendarView.getCalendarSources().setAll(source);
        VBox.setVgrow(calendarView, Priority.ALWAYS);

        root.getChildren().addAll(back, calendarView);
        return root;
    }
}
