package com.gamilha.services;

import com.gamilha.entity.Playlist;

import java.util.List;

public interface IPlaylistService {
    void ajouterPlaylist(Playlist playlist);
    void modifierPlaylist(Playlist playlist);
    void supprimerPlaylist(int id);
    List<Playlist> afficherPlaylists();
}
