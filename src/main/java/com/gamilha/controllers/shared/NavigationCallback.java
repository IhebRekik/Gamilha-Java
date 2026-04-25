package com.gamilha.controllers.shared;

/**
 * Callback interface so sub-controllers can trigger navigation
 * without depending on DashboardController directly.
 */
@FunctionalInterface
public interface NavigationCallback {
    /**
     * Navigate to the given page key.
     * Keys: evenements_list | evenements_form | equipes_list | equipes_form |
     *       brackets_list | brackets_form | matchs_list | matchs_form
     */
    void navigateTo(String pageKey);
}

