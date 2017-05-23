package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

public final class WeakPlayerStateListener implements PlayerStateListener {

  @Nonnull
  private final PlayerStateListener listener;

  public WeakPlayerStateListener(@Nonnull PlayerStateListener listener) {
    this.listener = listener;
  }

  @Override
  public void onChanged(@Nonnull PlayerState state) {
    listener.onChanged(state);
  }
}
