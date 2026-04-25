package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.infrastructure.AudioPlayer;
import com.fly.uloganalyzer.domain.SoundType;
import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.HashMap;
import java.util.Map;

public class JavaFXAudioPlayer implements AudioPlayer {
    
    private final Map<SoundType, MediaPlayer> players;
    private final ConfigLoader configLoader;
    
    public JavaFXAudioPlayer(ConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.players = new HashMap<>();
        initializePlayers();
    }
    

    private void initializePlayers() {
        for (SoundType type : SoundType.values()) {
            try {
                String resourcePath = "/" + type.getDefaultPath(); 

                java.net.URL soundUrl = getClass().getResource(resourcePath);

                if (soundUrl == null) {
                    System.err.println("❌ Ses dosyası bulunamadı: " + resourcePath);
                    continue;
                }

                Media media = new Media(soundUrl.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                
                
                players.put(type, player);
                System.out.println("✅ Ses yüklendi: " + type);
                
            } catch (Exception e) {
                System.err.println("Ses yükleme hatası: " + type + " -> " + e.getMessage());
            }
        }
    }
    
    @Override
    public void playSound(SoundType type) {
        MediaPlayer player = players.get(type);
        if (player != null) {
            try {
                player.stop();
                player.play();
                System.out.println("Playing sound: " + type);
            } catch (Exception e) {
                System.err.println("Error playing sound: " + type + " - " + e.getMessage());
            }
        }
    }
    
    @Override
    public void stopAllSounds() {
        players.values().forEach(MediaPlayer::stop);
    }
}