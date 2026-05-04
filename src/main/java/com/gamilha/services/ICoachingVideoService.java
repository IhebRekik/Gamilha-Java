package com.gamilha.services;

import com.gamilha.entity.CoachingVideo;

import java.util.List;

public interface ICoachingVideoService {
    void ajouterVideo(CoachingVideo video);
    void modifierVideo(CoachingVideo video);
    void supprimerVideo(int id);
    List<CoachingVideo> afficherVideos();
}
