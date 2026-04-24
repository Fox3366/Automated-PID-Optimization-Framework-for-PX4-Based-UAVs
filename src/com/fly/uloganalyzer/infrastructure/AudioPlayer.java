package com.fly.uloganalyzer.infrastructure;

import com.fly.uloganalyzer.domain.SoundType;

public interface AudioPlayer {
    void playSound(SoundType type);
    void stopAllSounds();
}